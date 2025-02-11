package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.matchesPredicate
import ca.uhn.fhir.context.FhirContext
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CodeStringConditionFilter
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.ReportStreamConditionFilter
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.ConditionStamper.Companion.CONDITION_CODE_EXTENSION_URL
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEvent
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.filterMappedObservations
import gov.cdc.prime.router.fhirengine.utils.filterObservations
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
private const val VALID_FHIR_FILEPATH = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val BODY_URL = "https://anyblob.com"
private const val CONDITION_FILTER = "%resource.code.coding.code = '95418-0'"
private val FILTER_PASS: ReportStreamFilter = listOf("true")
private val FILTER_FAIL: ReportStreamFilter = listOf("false")
private val MAPPED_CONDITION_FILTER_FAIL = CodeStringConditionFilter("foo,bar")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirReceiverFilterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    private val actionHistory = ActionHistory(TaskAction.receiver_filter)
    private val azureEventService = InMemoryAzureEventService()
    private val reportServiceMock = mockk<ReportService>()
    private val submittedId = UUID.randomUUID()

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

    private var actionLogger = ActionLogger()

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider): FHIREngine {
        val rootReport = mockk<ReportFile>()
        every { rootReport.reportId } returns submittedId
        every { rootReport.sendingOrg } returns "sendingOrg"
        every { rootReport.sendingOrgClient } returns "sendingOrgClient"
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getRootReports(any()) } returns listOf(rootReport)
        every { reportServiceMock.getRootItemIndex(any(), any()) } returns 1

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
        jurisdictionFilter: List<String> = emptyList(),
        qualityFilter: List<String> = emptyList(),
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
                jurisdictionalFilter = jurisdictionFilter,
                qualityFilter = qualityFilter,
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
                jurisdictionalFilter = jurisdictionFilter,
                qualityFilter = qualityFilter,
                routingFilter = routingFilter,
                processingModeFilter = processingModeFilter,
                conditionFilter = conditionFilter,
                mappedConditionFilter = mappedConditionFilter
            )
        )
    )

    @BeforeEach
    fun reset() {
        actionLogger = ActionLogger()
        actionHistory.reportsIn.clear()
        actionHistory.reportsOut.clear()
        actionHistory.reportsReceived.clear()
        actionHistory.actionLogs.clear()
        azureEventService.events.clear()
        mockkObject(BlobAccess)
        clearAllMocks()
    }

    @Test
    fun `fail - qual filter fails`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                qualityFilter = FILTER_FAIL
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

        // data + mock setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        mockkObject(BlobAccess)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId in messages.map { it.reportId }) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act on each message (with assert)
        messages.forEach { message ->
            every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
            // act + assert
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        // assert
        azureEventService.events.forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportStreamItemEvent>()
                .matchesPredicate {
                    it.params[ReportStreamEventProperties.FAILING_FILTERS] == FILTER_FAIL &&
                        it.params[ReportStreamEventProperties.FILTER_TYPE] == ReportStreamFilterType.QUALITY_FILTER.name
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
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(routingFilter = FILTER_FAIL)
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

        // data + mock setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        mockkObject(BlobAccess)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId in messages.map { it.reportId }) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act
        messages.forEach { message ->
            every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        azureEventService.events.forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportStreamItemEvent>()
                .matchesPredicate {
                    it.params[ReportStreamEventProperties.FAILING_FILTERS] == FILTER_FAIL &&
                        it.params[ReportStreamEventProperties.FILTER_TYPE] == ReportStreamFilterType.ROUTING_FILTER.name
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
    fun `fail - processing mode filter fails`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(processingModeFilter = FILTER_FAIL)
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

        // data + mock setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        mockkObject(BlobAccess)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId in messages.map { it.reportId }) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act on each message (with assert)
        messages.forEach { message ->
            every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
            // act + assert
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        // assert
        azureEventService.events.forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportStreamItemEvent>()
                .matchesPredicate {
                    it.params[ReportStreamEventProperties.FAILING_FILTERS] == FILTER_FAIL &&
                        it.params[ReportStreamEventProperties.FILTER_TYPE] ==
                        ReportStreamFilterType.PROCESSING_MODE_FILTER.name
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
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(conditionFilter = FILTER_FAIL)
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

        // data + mock setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        mockkObject(BlobAccess)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId in messages.map { it.reportId }) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act on each message (with assert)
        messages.forEach { message ->
            every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
            // act + assert
            accessSpy.transact { txn ->
                val results = engine.run(message, actionLogger, actionHistory, txn)
                assertThat(results).isEmpty()
            }
        }

        // assert
        azureEventService.events.forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportStreamItemEvent>()
                .matchesPredicate {
                    it.params[ReportStreamEventProperties.FAILING_FILTERS] == FILTER_FAIL &&
                        it.params[ReportStreamEventProperties.FILTER_TYPE] ==
                        ReportStreamFilterType.CONDITION_FILTER.name
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
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                mappedConditionFilter = listOf(MAPPED_CONDITION_FILTER_FAIL)
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

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(File(VALID_FHIR_FILEPATH).readText())
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId == message.reportId) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()

            azureEventService.events.forEach { event ->
                assertThat(event)
                    .isInstanceOf<ReportStreamItemEvent>()
                    .matchesPredicate {
                        it.params[ReportStreamEventProperties.FAILING_FILTERS] ==
                            listOf(MAPPED_CONDITION_FILTER_FAIL.value) &&
                            it.params[ReportStreamEventProperties.FILTER_TYPE] ==
                            ReportStreamFilterType.MAPPED_CONDITION_FILTER.name
                    }
            }

            assertThat(actionLogger.logs).hasSize(1)
            assertThat(actionLogger.logs.first().detail)
                .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                .matchesPredicate { it.filterType == ReportStreamFilterType.MAPPED_CONDITION_FILTER }
        }
    }

    @Test
    fun `fail - bundle of only AOEs do not pass mappedConditionFilter`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                mappedConditionFilter = listOf(CodeStringConditionFilter("AOE"))
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

        // data setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        bundle.getObservations().forEach {
            val coding = it.code.coding.first()
            if (coding.extension.isEmpty()) {
                coding.addExtension(
                    CONDITION_CODE_EXTENSION_URL,
                    Coding(
                        "system", "AOE", "name"
                    )
                )
            }
        }

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId == message.reportId) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act + assert
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
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                FILTER_PASS, FILTER_PASS, FILTER_PASS, FILTER_PASS, FILTER_PASS
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

        // data setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirData)
        bundle.entry.filter { it.resource is Observation }.forEach {
            val observation = (it.resource as Observation)
            observation.code.coding[0].addExtension(
                CONDITION_CODE_EXTENSION_URL,
                Coding("SNOMEDCT", "6142004", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                CONDITION_CODE_EXTENSION_URL,
                Coding("Condition Code System", "foobar", "Condition Name")
            )
        }

        // mock setup
        mockkObject(BlobAccess)
        mockkStatic(Bundle::filterObservations)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { any<Bundle>().filterObservations(any(), any()) } returns bundle

        // act on each message (with assert)
        messages.forEachIndexed { i, message ->
            every { BlobAccess.downloadBlob(any(), any()) }.returns(FhirTranscoder.encode(bundle))
            // act + assert
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
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                conditionFilter = listOf(CONDITION_FILTER)
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

        // data setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        val originalBundle = FhirTranscoder.decode(fhirData)
        val expectedBundle = originalBundle
            .filterObservations(listOf(CONDITION_FILTER), engine.loadFhirPathShorthandLookupTable())

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)

        // act + assert
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
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                mappedConditionFilter = listOf(CodeStringConditionFilter("6142004,Some Condition Code"))
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

        // data setup
        @Suppress("ktlint:standard:max-line-length")
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.getObservations().forEach { observation ->
            observation.code.coding[0].addExtension(
                CONDITION_CODE_EXTENSION_URL,
                Coding("SNOMEDCT", "6142004", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                CONDITION_CODE_EXTENSION_URL,
                Coding("Condition Code System", "Some Condition Code", "Condition Name")
            )
        }
        val expectedBundle = bundle
            .filterMappedObservations(listOf(CodeStringConditionFilter("6142004"))).second

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(1)
            assertThat(actionHistory.actionLogs).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(1)

            val actualEvents = azureEventService.events
            assertThat(actualEvents).hasSize(0)
        }

        // assert
        verify(exactly = 1) {
            BlobAccess.uploadBlob(any(), FhirTranscoder.encode(expectedBundle).toByteArray(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test a receiver can receive a report when no condition filters are configured`() {
        // engine set up
        val settings = FileSettings().loadOrganizations(createOrganizationWithFilteredReceivers())
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

        // data setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()
        val originalBundle = FhirTranscoder.decode(fhirData)

        // mock setup
        mockkObject(BlobAccess)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)

        // act + assert
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
        // engine set up
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(qualityFilter = FILTER_FAIL)
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

        // data setup
        val fhirData = File(VALID_FHIR_FILEPATH).readText()

        // mock setup
        mockkObject(BlobAccess)
        mockkStatic(Bundle::filterObservations)
        every { BlobAccess.downloadBlob(any(), any()) }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), MimeFormat.FHIR.toString(), BODY_URL, any()) }.returns(Unit)
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId == message.reportId) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }
        every { any<Bundle>().filterObservations(any(), any()) } returns FhirTranscoder.decode(fhirData)

        // act + assert
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()

            azureEventService.events.forEach { event ->
                assertThat(event)
                    .isInstanceOf<ReportStreamItemEvent>()
                    .matchesPredicate {
                        it.params[ReportStreamEventProperties.FAILING_FILTERS] == FILTER_FAIL &&
                            it.params[ReportStreamEventProperties.FILTER_TYPE] ==
                            ReportStreamFilterType.QUALITY_FILTER.name
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
        val mappedConditionFilter = CodeStringConditionFilter("6142004,Some Condition Code")
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                mappedConditionFilter = listOf(mappedConditionFilter)
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

        // data setup
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

        // mock setup
        every { BlobAccess.downloadBlob(any(), any()) }.returns(FhirTranscoder.encode(bundle))
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId == message.reportId) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act + assert
        accessSpy.transact { txn ->
            val results = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(results).isEmpty()
        }

        // assert
        azureEventService.events.forEach { event ->
            assertThat(event)
                .isInstanceOf<ReportStreamItemEvent>()
                .matchesPredicate {
                    it.params[ReportStreamEventProperties.FAILING_FILTERS] == listOf(mappedConditionFilter.value) &&
                        it.params[ReportStreamEventProperties.FILTER_TYPE] ==
                        ReportStreamFilterType.MAPPED_CONDITION_FILTER.name
                }
        }
        assertThat(actionLogger.logs).hasSize(1)
        assertThat(actionLogger.logs.first().detail)
            .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
            .matchesPredicate { it.filterType == ReportStreamFilterType.MAPPED_CONDITION_FILTER }
    }

    @Test
    fun `test logging for condition filters resulting in full prune`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(conditionFilter = listOf("false"))
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

        // mock setup
        every { BlobAccess.downloadBlob(any(), any()) }.returns(File(VALID_FHIR_FILEPATH).readText())
        every { accessSpy.fetchReportFile(any<ReportId>()) } answers {
            val reportId = firstArg<ReportId>()
            if (reportId == message.reportId) {
                ReportFile().setReportId(reportId).setItemCount(1)
            } else {
                callOriginal()
            }
        }

        // act + assert
        accessSpy.transact { txn ->
            val results = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(results).isEmpty()
        }

        // assert
        assertThat(actionLogger.logs).hasSize(1)
        assertThat(actionLogger.logs.first().detail)
            .isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
            .matchesPredicate { it.filterType == ReportStreamFilterType.CONDITION_FILTER }
    }

    @Test
    fun `test for error when mixing condition filters and mapped condition filters`() {
        // engine setup
        val settings = FileSettings().loadOrganizations(
            createOrganizationWithFilteredReceivers(
                conditionFilter = listOf("false"),
                mappedConditionFilter = listOf(MAPPED_CONDITION_FILTER_FAIL)
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

        // mock setup
        every { BlobAccess.downloadBlob(any(), any()) }.returns(File(VALID_FHIR_FILEPATH).readText())

        // act + assert
        accessSpy.transact {
            assertFailure { engine.run(message, actionLogger, actionHistory, it) }
                .isInstanceOf<FHIRReceiverFilter.MisconfiguredReceiverConditionFilters>()
                .matchesPredicate { it.receiver == settings.receivers.first() }
        }
    }
}