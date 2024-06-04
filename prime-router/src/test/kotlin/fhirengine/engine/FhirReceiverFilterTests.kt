package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.matchesPredicate
import ca.uhn.fhir.context.FhirContext
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CodeStringConditionFilter
import gov.cdc.prime.router.ConditionFilter
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.PrunedObservationsLogMessage
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamConditionFilter
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.ConditionSummary
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ObservationSummary
import gov.cdc.prime.router.azure.observability.event.ReportRouteEvent
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.conditionCodeExtensionURL
import gov.cdc.prime.router.fhirengine.utils.filterMappedObservations
import gov.cdc.prime.router.fhirengine.utils.filterObservations
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlin.test.Test

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val QUALITY_TEST_PATH = "src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir"
private const val OBSERVATION_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Observation).count() > 0"
private const val PROVENANCE_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Provenance).count() > 0"
private const val PROVENANCE_COUNT_EQUAL_TO_TEN = "Bundle.entry.resource.ofType(Provenance).count() = 10"
private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val BODY_URL = "https://anyblob.com"
private const val PROVENANCE_COUNT_GREATER_THAN_10 = "Bundle.entry.resource.ofType(Provenance).count() > 10"
private const val CONDITION_FILTER = "%resource.code.coding.code = '95418-0'"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirReceiverFilterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    private val actionHistory = ActionHistory(TaskAction.route)
    private val azureEventService = InMemoryAzureEventService()
    private val reportServiceMock = mockk<ReportService>()

    val csv = """
            variable,fhirPath
            processingId,Bundle.entry.resource.ofType(MessageHeader).meta.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/source-processing-id').value.coding.code
            messageId,Bundle.entry.resource.ofType(MessageHeader).id
            patient,Bundle.entry.resource.ofType(Patient)
            performerState,Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state
            patientState,Bundle.entry.resource.ofType(Patient).address.state
            specimen,Bundle.entry.resource.ofType(Specimen)
            serviceRequest,Bundle.entry.resource.ofType(ServiceRequest)
            observation,Bundle.entry.resource.ofType(Observation)
            test-dash,Bundle.test.dash
            test_underscore,Bundle.test.underscore
            test'apostrophe,Bundle.test.apostrophe
    """.trimIndent()

    private val shorthandTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one).loadLookupTable("fhirpath_filter_shorthand", shorthandTable)
    val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
    val receiver = Receiver(
        "myRcvr",
        "topic",
        Topic.TEST,
        CustomerStatus.ACTIVE,
        "mySchema"
    )

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider): FHIREngine {
        every { reportServiceMock.getSenderName(any()) } returns "sendingOrg.sendingOrgClient"

        return FHIREngine.Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(accessSpy)
            .blobAccess(blobMock)
            .azureEventService(azureEventService)
            .reportService(reportServiceMock)
            .build(TaskAction.receiver_filter)
    }

    private fun createOrganizationWithFilteredReceivers(
        jurisFilter: List<String> = emptyList(),
        qualFilter: List<String> = emptyList(),
        routingFilter: List<String> = emptyList(),
        processingModeFilter: List<String> = emptyList(),
        conditionFilter: List<String> = emptyList(),
        mappedConditionFilter: ReportStreamConditionFilter = emptyList(),
    ) = DeepOrganization(
            ORGANIZATION_NAME,
            "test",
            Organization.Jurisdiction.FEDERAL,
            receivers = listOf(
                Receiver(
                    RECEIVER_NAME,
                    ORGANIZATION_NAME,
                    Topic.FULL_ELR,
                    CustomerStatus.ACTIVE,
                    "one",
                    jurisdictionalFilter = jurisFilter,
                    qualityFilter = qualFilter,
                    routingFilter = routingFilter,
                    processingModeFilter = processingModeFilter,
                    conditionFilter = conditionFilter,
                    mappedConditionFilter = mappedConditionFilter
                ),
                Receiver(
                    "full-elr-hl7-2",
                    ORGANIZATION_NAME,
                    Topic.FULL_ELR,
                    CustomerStatus.INACTIVE,
                    "one",
                    jurisdictionalFilter = jurisFilter,
                    qualityFilter = qualFilter,
                    routingFilter = routingFilter,
                    processingModeFilter = processingModeFilter,
                    conditionFilter = conditionFilter,
                    mappedConditionFilter = mappedConditionFilter
                )
            )
        )

    @BeforeEach
    fun reset() {
        actionHistory.reportsIn.clear()
        actionHistory.reportsOut.clear()
        actionHistory.actionLogs.clear()
        azureEventService.clear()
        clearAllMocks()
    }

    @Test
    fun `fail - qual filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val routingFilter = emptyList<String>()
        val processingModeFilter = emptyList<String>()

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, emptyList(), emptyList()
            )
        )
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    it.fullName
                )
            )
        }

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        messages.forEach { message ->
            every { message.downloadContent() }.returns(fhirData)
            // act
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        azureEventService.getEvents().forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportRouteEvent>()
                .matchesPredicate {
                    it.receiver == null
                }
        }

        assertThat(actionLogger.logs).hasSize(2)
        actionLogger.logs.forEach {
            assertThat(it.detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.QUALITY_FILTER }
        }
    }

    @Test
    fun `fail - routing filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val processingModeFilter = emptyList<String>()
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, emptyList(), emptyList()
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    it.fullName
                )
            )
        }

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        // act
        messages.forEach { message ->
            every { message.downloadContent() }.returns(fhirData)
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        azureEventService.getEvents().forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportRouteEvent>()
                .matchesPredicate {
                    it.receiver == null
                }
        }

        assertThat(actionLogger.logs).hasSize(2)
        actionLogger.logs.forEach {
            assertThat(it.detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.ROUTING_FILTER }
        }
    }

    @Test
    fun `fail - proc mode filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, emptyList(), emptyList()
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    it.fullName
                )
            )
        }

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        messages.forEach { message ->
            every { message.downloadContent() }.returns(fhirData)
            // act
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        azureEventService.getEvents().forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportRouteEvent>()
                .matchesPredicate {
                    it.receiver == null
                }
        }

        assertThat(actionLogger.logs).hasSize(2)
        actionLogger.logs.forEach {
            assertThat(it.detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.PROCESSING_MODE_FILTER }
        }
    }

    @Test
    fun `fail - condition filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    it.fullName
                )
            )
        }

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        messages.forEach { message ->
            every { message.downloadContent() }.returns(fhirData)
            // act
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        azureEventService.getEvents().forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportRouteEvent>()
                .matchesPredicate {
                    it.receiver == null
                }
        }
        assertThat(actionLogger.logs).hasSize(2)
        actionLogger.logs.forEach {
            assertThat(it.detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.CONDITION_FILTER }
        }
    }

    @Test
    fun `fail - mapped condition filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = listOf(CodeStringConditionFilter("foo,bar"))

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionLogger.logs).hasSize(1)
            assertThat(actionLogger.logs.first().detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.MAPPED_CONDITION_FILTER }
        }
    }

    @Test
    fun `fail - bundle of only AOEs do not pass mappedConditionFilter`() {
        val fhirData = File(VALID_FHIR_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        bundle.getObservations().forEach {
            val coding = it.code.coding.first()
            if (coding.extension.isEmpty()) {
                coding.addExtension(
                    conditionCodeExtensionURL,
                        Coding(
                        "system", "AOE", "name"
                    )
                )
            }
        }

        mockkObject(BlobAccess)

        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = listOf(CodeStringConditionFilter("AOE"))

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        // act
        accessSpy.transact { _ ->
            val results = engine.doWork(message, actionLogger, actionHistory)
            assertThat(results).isEmpty()
            assertThat(actionLogger.logs).hasSize(1)
            assertThat(actionLogger.logs.first().detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.MAPPED_CONDITION_FILTER }
        }
    }

    @Test
    fun `success - jurisfilter, qualfilter, routing filter, proc mode passes, and condition filter passes`() {
        val fhirData = File(VALID_FHIR_URL).readText()
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirData)
        bundle.entry.filter { it.resource is Observation }.forEach {
            val observation = (it.resource as Observation)
            observation.code.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("SNOMEDCT", "6142004", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("Condition Code System", "foobar", "Condition Name")
            )
        }

        mockkObject(BlobAccess)

        // set up
        val actionLogger = mockk<ActionLogger>()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val messages = settings.receivers.map {
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    it.fullName
                )
            )
        }

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<ActionLogDetail>()) } just runs
        every { actionLogger.warn(any<ActionLogDetail>()) } just runs
        every { actionLogger.error(any<ActionLogDetail>()) } just runs
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        mockkStatic(Bundle::filterObservations)
        every { any<Bundle>().filterObservations(any(), any()) } returns bundle

        messages.forEachIndexed { i, message ->
            every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
            // act
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).hasSize(1)
                assertThat(results.first())
                    .isInstanceOf<FhirTranslateQueueMessage>()
                    .matchesPredicate {
                        it.receiverFullName == settings.receivers.toList()[i].fullName
                    }
            }
        }

        // assert
        verify(exactly = 2) {
            BlobAccess.uploadBlob(any(), any(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test the bundle queued for the translate function is filtered to conditions the receiver wants`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val actionLogger = ActionLogger()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(CONDITION_FILTER)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        val originalBundle = FhirTranscoder.decode(fhirData)
        val expectedBundle = originalBundle
            .filterObservations(listOf(CONDITION_FILTER), engine.loadFhirPathShorthandLookupTable())

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(1)
            assertThat(actionHistory.actionLogs).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(1)
        }

        // assert
        verify(exactly = 1) {
            BlobAccess.uploadBlob(any(), FhirTranscoder.encode(expectedBundle).toByteArray(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test the bundle queued for the translate function is filtered to mapped conditions the receiver wants`() {
        @Suppress("ktlint:standard:max-line-length")
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.getObservations().forEach { observation ->
            observation.code.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("SNOMEDCT", "6142004", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("Condition Code System", "Some Condition Code", "Condition Name")
            )
        }

        mockkObject(BlobAccess)

        // set up
        val actionLogger = mockk<ActionLogger>()

        // filters
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = listOf(CodeStringConditionFilter("6142004,Some Condition Code"))

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        val expectedBundle = bundle
            .filterMappedObservations(listOf(CodeStringConditionFilter("6142004"))).second

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(1)
            assertThat(actionHistory.actionLogs).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(1)

            val reportId = (messages.first() as ReportPipelineMessage).reportId
            val expectedObservationSummary = listOf(
                ObservationSummary(
                    listOf(
                        ConditionSummary("6142004", "Influenza (disorder)"),
                        ConditionSummary("Some Condition Code", "Condition Name")
                    )
                )
            )
            val expectedAzureEvents = listOf(
                ReportRouteEvent(
                    message.reportId,
                    reportId,
                    message.topic,
                    "sendingOrg.sendingOrgClient",
                    "$ORGANIZATION_NAME.$RECEIVER_NAME",
                    expectedObservationSummary,
                    emptyList(),
                    1945,
                    AzureEventUtils.MessageID(
                        "1234d1d1-95fe-462c-8ac6-46728dba581c",
                        null
                    )
                )
            )

            val actualEvents = azureEventService.getEvents()
            assertThat(actualEvents).hasSize(1)
            assertThat(actualEvents).isEqualTo(expectedAzureEvents)
        }

        // assert
        verify(exactly = 1) {
            BlobAccess.uploadBlob(any(), FhirTranscoder.encode(expectedBundle).toByteArray(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test a receiver can receive a report when no condition filters are configured`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val actionLogger = mockk<ActionLogger>()

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
            )
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        val originalBundle = FhirTranscoder.decode(fhirData)

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<ActionLogDetail>()) } just runs
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(1)
        }

        // assert
        verify(exactly = 1) {
            BlobAccess.uploadBlob(any(), FhirTranscoder.encode(originalBundle).toByteArray(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test bundle with no receivers is not routed to translate function`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
            emptyList(),
            listOf("false")
        )
        )
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)
        val message =
            spyk(
                FhirReceiverFilterQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR,
                    "$ORGANIZATION_NAME.$RECEIVER_NAME"
                )
            )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.getItemLogger(any(), any()) } returns actionLogger
        every { actionLogger.warn(any<ActionLogDetail>()) } just runs
        every { actionLogger.error(any<ActionLogDetail>()) } just runs
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        mockkStatic(Bundle::filterObservations)
        every { any<Bundle>().filterObservations(any(), any()) } returns FhirTranscoder.decode(fhirData)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()

            azureEventService.getEvents().forEach { event ->
                assertThat(event)
                    .isInstanceOf<ReportRouteEvent>()
                    .matchesPredicate {
                        it.receiver == null
                    }
            }
        }

        // assert
        verify(exactly = 0) {
            accessSpy.insertTask(any(), any(), any(), any())
            BlobAccess.uploadBlob(any(), any())
        }
    }

    @Test
    fun `test logging for mapped condition filters resulting in full prune`() {
        @Suppress("ktlint:standard:max-line-length")
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""
        val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.entry.filter { it.resource is Observation }.forEach {
            val observation = (it.resource as Observation)
            observation.code.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("SNOMEDCT", "foo", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("Condition Code System", "bar", "Condition Name")
            )
        }
        // Using receiver with reverseFilter set to true
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        // This quality filter evaluates to true
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        // This routing filter evaluates to true
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        // This processing mode filter evaluates to true
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        // This condition filter evaluates to false
        val condFilter = emptyList<String>()
        val mappedCondFilter = listOf(CodeStringConditionFilter("6142004,Some Condition Code"))

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, condFilter, mappedCondFilter
        )
        )

        val actionLogger = ActionLogger()
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)

        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        // act
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        // act
        accessSpy.transact { txn ->
            val results = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(results).isEmpty()
        }

        azureEventService.getEvents().forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportRouteEvent>()
                .matchesPredicate {
                    it.receiver == null
                }
        }
        assertThat(actionLogger.logs).hasSize(1)
        assertThat(actionLogger.logs.first().detail)
            .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
            .matchesPredicate { it.filterType == ReportStreamFilterType.MAPPED_CONDITION_FILTER }
    }

    @Test
    fun `test logging for condition filters resulting in full prune`() {
        val fhirData = File(QUALITY_TEST_PATH).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This quality filter evaluates to true
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This routing filter evaluates to true
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This processing mode filter evaluates to true
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This condition filter evaluates to false
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_10)
        val mappedCondFilter = emptyList<ConditionFilter>()

        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, condFilter, mappedCondFilter
        )
        )

        val actionLogger = ActionLogger()
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRReceiverFilter)

        val message = spyk(
            FhirReceiverFilterQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
                "$ORGANIZATION_NAME.$RECEIVER_NAME"
            )
        )

        // act
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        // act
        accessSpy.transact { txn ->
            val results = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(results).isEmpty()
        }

        assertThat(actionLogger.logs).hasSize(1)
        assertThat(actionLogger.logs.first().detail)
            .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
            .matchesPredicate { it.filterType == ReportStreamFilterType.CONDITION_FILTER }
    }
}