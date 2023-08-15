package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
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
private const val TOPIC_TEST_ORG_NAME = "topic-test"
private const val QUALITY_TEST_URL = "src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir"
private const val PROVENANCE_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Provenance).count() > 0"
private const val PROVENANCE_COUNT_EQUAL_TO_TEN = "Bundle.entry.resource.ofType(Provenance).count() = 10"
private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val BODY_URL = "https://anyblob.com"
private const val PERFORMER_OR_PATIENT_CA = "(%performerState.exists() and %performerState = 'CA') " +
    "or (%patientState.exists() and %patientState = 'CA')"
private const val PROVENANCE_COUNT_GREATER_THAN_10 = "Bundle.entry.resource.ofType(Provenance).count() > 10"
private const val EXCEPTION_FOUND = "exception found"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val oneOrganization = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                RECEIVER_NAME,
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver(
                "full-elr-hl7-2",
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    private val etorOrganization = DeepOrganization(
        "etor-test",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "simulatedlab",
                "etor-test",
                Topic.ETOR_TI,
                CustomerStatus.ACTIVE,
                "one"
            )
        )
    )

    private val elimsOrganization = DeepOrganization(
        "elims-test",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "elims-test",
                Topic.ELR_ELIMS,
                CustomerStatus.ACTIVE,
                "one"
            )
        )
    )

    private val orgWithReversedQualityFilter = DeepOrganization(
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
                reverseTheQualityFilter = true
            ),
            Receiver(
                "full-elr-hl7-2",
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    private val etorAndElrOrganizations = DeepOrganization(
        TOPIC_TEST_ORG_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                RECEIVER_NAME,
                TOPIC_TEST_ORG_NAME,
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver(
                "simulatedlab",
                TOPIC_TEST_ORG_NAME,
                Topic.ETOR_TI,
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver(
                "full-elr-hl7-inactive",
                TOPIC_TEST_ORG_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            ),
            Receiver(
                "simulatedlab-inactive",
                TOPIC_TEST_ORG_NAME,
                Topic.ETOR_TI,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

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
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(TaskAction.route)
    }

    /**
     * Private extension function on FHIRRouter as helper to allow setting all filter mocks in one call
     */
    private fun FHIRRouter.setFiltersOnEngine(
        jurisFilter: List<String>,
        qualFilter: List<String>,
        routingFilter: List<String>,
        procModeFilter: List<String>,
        conditionFilter: List<String> = emptyList(),
    ) {
        every { getJurisFilters(any(), any()) } returns jurisFilter
        every { getQualityFilters(any(), any()) } returns qualFilter
        every { getRoutingFilter(any(), any()) } returns routingFilter
        every { getProcessingModeFilter(any(), any()) } returns procModeFilter
        every { getConditionFilter(any(), any()) } returns conditionFilter
    }

    @BeforeEach
    fun reset() {
        report.filteringResults.clear()
        clearAllMocks()
    }

    @Test
    fun `test applyFilters receiver setting - (reverseTheQualityFilter = true) `() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(orgWithReversedQualityFilter)
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (reverseTheQualityFilter = false) `() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (conditionFilter no observations) `() {
        val fhirData = File("src/test/resources/fhirengine/engine/lab_order_no_observations.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (conditionFilter multiple filters, all true) `() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // with multiple condition filters, we are looking for any of them passing
        val conditionFilter = listOf(
            PROVENANCE_COUNT_GREATER_THAN_ZERO, // true
            "Bundle.entry.resource.ofType(Provenance).count() >= 1" // also true
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (conditionFilter multiple filters, one true) `() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // with multiple condition filters, we are looking for any of them passing
        val conditionFilter = listOf(
            PROVENANCE_COUNT_GREATER_THAN_ZERO, // true
            "Bundle.entry.resource.ofType(Provenance).count() > 100" // not true
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (conditionFilter multiple filters, none true) `() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // with multiple condition filters, we are looking for any of them passing
        val conditionFilter = listOf(
            "Bundle.entry.resource.ofType(Provenance).count() > 50", // not true
            "Bundle.entry.resource.ofType(Provenance).count() > 100" // not true
        )

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test reverseQualityFilter flag only reverses the quality filter`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        // Using receiver with reverseQualityFilter set to true
        val settings = FileSettings().loadOrganizations(orgWithReversedQualityFilter)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This quality filter evaluates to true, but is reversed to false
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This routing filter evaluates to true
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This processing mode filter evaluates to true
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert only the quality filter didn't pass
        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.QUALITY_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `0 test qualFilter default - succeed, basic covid FHIR`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterConditionAsAnd(
            engine.qualityFilterDefaults[Topic.FULL_ELR],
            bundle,
            false
        )

        // assert
        assertThat(qualDefaultResult.first).isTrue()
        assertThat(qualDefaultResult.second).isNull()
    }

    @Test
    fun `fail - jurisfilter does not pass`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val qualFilter = emptyList<String>()
        val routingFilter = emptyList<String>()
        val procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter does not`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val routingFilter = emptyList<String>()
        val procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter does not`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter passes, proc mode does not`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any(), any())
        }
    }

    @Test
    fun `fail - all pass other than the condition filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any(), any())
        }
    }

    @Test
    fun `success - jurisfilter, qualfilter, routing filter, proc mode passes, and condition filter passes`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any(), engine.queueVisibilityTimeout) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, conditionFilter)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), any())
            BlobAccess.Companion.uploadBlob(any(), any())
            queueMock.sendMessage(any(), any(), engine.queueVisibilityTimeout)
            accessSpy.insertTask(any(), any(), any(), any())
            FHIRBundleHelpers.addReceivers(any(), any(), any())
        }
    }

    @Test
    fun `test bundle with no receivers is not routed to translate function`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message =
            spyk(RawSubmission(UUID.randomUUID(), BLOB_URL, "test", BLOB_SUB_FOLDER_NAME, topic = Topic.FULL_ELR))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { engine.applyFilters(any(), any(), any()) } returns emptyList()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), any())
        }
        verify(exactly = 0) {
            queueMock.sendMessage(any(), any())
            accessSpy.insertTask(any(), any(), any(), any())
            FHIRBundleHelpers.addReceivers(any(), any(), any())
            BlobAccess.Companion.uploadBlob(any(), any())
        }
    }

    @Test
    fun ` test constantResolver for routing constants - succeed`() {
        val fhirData = File(VALID_FHIR_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val filter = listOf(
            PERFORMER_OR_PATIENT_CA
        )
        val result = engine.evaluateFilterConditionAsAnd(
            filter,
            bundle,
            false
        )

        // assert
        assertThat(result.first).isTrue()
        assertThat(result.second).isNull()
    }

    @Test
    fun ` test constants - succeed, no patient state`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/no_patient_state.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val filter = listOf(
            PERFORMER_OR_PATIENT_CA
        )
        val result = engine.evaluateFilterConditionAsAnd(
            filter,
            bundle,
            false
        )

        // assert
        assertThat(result.first).isTrue()
        assertThat(result.second).isNull()
    }

    @Test
    fun ` test constants - succeed, no performer state`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/no_performer_state.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val filter = listOf(
            PERFORMER_OR_PATIENT_CA
        )
        val result = engine.evaluateFilterConditionAsAnd(
            filter,
            bundle,
            false
        )

        // assert
        assertThat(result.first).isTrue()
        assertThat(result.second).isNull()
    }

    @Test
    fun `test logFilterResults`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        assertThat(report.filteringResults.count()).isEqualTo(0)
        engine.logFilterResults(
            qualFilter.toString(),
            bundle,
            report,
            receiver,
            ReportStreamFilterType.QUALITY_FILTER,
            bundle
        )
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterName).isEqualTo(qualFilter.toString())
    }

    @Test
    fun `test actionLogger trigger during evaluateFilterCondition`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)
        mockkObject(FhirPathUtils)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val actionHistory = mockk<ActionHistory>()
        val actionLogger = ActionLogger()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            RawSubmission(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)

        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any(), any()) } returns Unit
        engine.setFiltersOnEngine(
            jurisFilter,
            engine.qualityFilterDefaults[Topic.FULL_ELR]!!,
            routingFilter = emptyList(),
            engine.processingModeDefaults[Topic.FULL_ELR]!!,
        )

        val nonBooleanMsg = "Condition did not evaluate to a boolean type"
        every { FhirPathUtils.evaluateCondition(any(), any(), any(), any()) } throws SchemaException(nonBooleanMsg)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        assertThat(actionLogger.hasWarnings()).isTrue()
        assertThat(actionLogger.warnings[0].detail.message).isEqualTo(nonBooleanMsg)
    }

    @Test
    fun `test evaluateFilterAndLogResult`() {
        val fhirData = File("src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val report = Report(one, listOf(listOf("1", "2")), TestSource, metadata = UnitTestUtils.simpleMetadata)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val filter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val type = ReportStreamFilterType.QUALITY_FILTER

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        every {
            engine.evaluateFilterConditionAsAnd(any(), any(), true, any(), any())
        } returns Pair(true, null)
        every {
            engine.evaluateFilterConditionAsAnd(any(), any(), false, any(), any())
        } returns Pair(false, filter.toString())

        every {
            engine.evaluateFilterConditionAsOr(any(), any(), true, any(), any())
        } returns Pair(true, null)
        every {
            engine.evaluateFilterConditionAsOr(any(), any(), false, any(), any())
        } returns Pair(false, filter.toString())

        engine.evaluateFilterAndLogResult(filter, bundle, report, receiver, type, true)
        verify(exactly = 0) {
            engine.logFilterResults(any(), any(), any(), any(), any(), any())
        }
        engine.evaluateFilterAndLogResult(filter, bundle, report, receiver, type, false)
        verify(exactly = 1) {
            engine.logFilterResults(any(), any(), any(), any(), any(), any())
        }

        // use case for condition filter
        val observation = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle, emptyMap<String, String>().toMutableMap()),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport).result.resolve()"
        ).first()

        engine.evaluateFilterAndLogResult(
            filter,
            bundle,
            report,
            receiver,
            ReportStreamFilterType.CONDITION_FILTER,
            defaultResponse = true,
            reverseFilter = false,
            focusResource = observation,
            useOr = true
        )
        verify(exactly = 1) {
            engine.logFilterResults(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test etor topic routing`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(etorOrganization)
        // All filters evaluate to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // when doing routing for full-elr, verify that etor receiver isn't included (not even in logged results)
        var receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()

        // when doing routing for etor, verify that etor receiver is included
        receivers = engine.applyFilters(bundle, report, Topic.ETOR_TI)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0]).isEqualTo(etorOrganization.receivers[0])
    }

    @Test
    fun `test elr topic routing`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // All filters evaluate to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // when doing routing for etor, verify that full-elr receiver isn't included (not even in logged results)
        var receivers = engine.applyFilters(bundle, report, Topic.ETOR_TI)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()

        // when doing routing for full-elr, verify that full-elr receiver is included
        receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0]).isEqualTo(oneOrganization.receivers[0])
    }

    @Test
    fun `test elr-elims topic routing`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(elimsOrganization)
        // All filters evaluate to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // when doing routing for full-elr, verify that elims receiver isn't included (not even in logged results)
        var receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()

        // when doing routing for elims, verify that elims receiver is included
        receivers = engine.applyFilters(bundle, report, Topic.ELR_ELIMS)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0]).isEqualTo(elimsOrganization.receivers[0])
    }

    @Test
    fun `test combined topic routing`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(etorAndElrOrganizations)
        // All filters evaluate to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter)

        // when routing for etor, verify that only the active etor receiver is included (even in logged results)
        var receivers = engine.applyFilters(bundle, report, Topic.ETOR_TI)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0].name).isEqualTo("simulatedlab")

        // when routing for full-elr, verify that only the active full-elr receiver is included (even in logged results)
        receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0].name).isEqualTo(RECEIVER_NAME)

        // Verify error when using non-UP topic
        assertThat { engine.applyFilters(bundle, report, Topic.COVID_19) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `test applyFilters logs results for routing filters`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This quality filter evaluates to true
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This routing filter evaluates to false
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_10)
        // This processing mode filter evaluates to true
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This condition filter evaluates to true
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, condFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert only the quality filter didn't pass

        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.ROUTING_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test applyFilters logs results for processing mode filters`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This quality filter evaluates to true
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This routing filter evaluates to true
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This processing mode filter evaluates to false
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_10)
        // This condition filter evaluates to true
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, condFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert only the processing mode filter didn't pass

        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test applyFilters logs results for condition filters`() {
        val fhirData = File(QUALITY_TEST_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)
        // Using receiver with reverseFilter set to true
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This Jurisdictional filter evaluates to true.
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This quality filter evaluates to true
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This routing filter evaluates to true
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This processing mode filter evaluates to true
        val procModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This condition filter evaluates to false
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_10)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, procModeFilter, condFilter)

        // act
        val receivers = engine.applyFilters(bundle, report, Topic.FULL_ELR)

        // assert only the condition mode filter didn't pass
        // and check that the observation was logged
        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(5)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.CONDITION_FILTER)
        assertThat(report.filteringResults[0].filteredTrackingElement.contains("loinc.org"))
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test tagging default filter in logs`() {
        // content is not important, just get a Bundle
        val bundle = Bundle()
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This processing mode filter evaluates to false, is equivalent to the default processindModeFilter
        val procModeFilter = listOf("%processingId = 'P'")
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        every { engine.evaluateFilterConditionAsAnd(any(), any(), any(), any(), any()) } returns Pair<Boolean, String?>(
            false,
            procModeFilter.toString()
        )

        // act
        val result = engine.evaluateFilterAndLogResult(
            procModeFilter,
            bundle,
            report,
            oneOrganization.receivers[0],
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            false,
        )

        assertThat(result).isFalse()
        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(report.filteringResults[0].message).doesNotContain("default filter")

        report.filteringResults.clear()

        val result2 = engine.evaluateFilterAndLogResult(
            engine.processingModeDefaults[Topic.FULL_ELR]!!,
            bundle,
            report,
            oneOrganization.receivers[0],
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            false,
        )

        assertThat(result2).isFalse()
        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(report.filteringResults[0].message).contains("default filter")
    }

    @Test
    fun `test tagging exceptions filter in logs`() {
        // content is not important, just get a Bundle
        val bundle = Bundle()
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This processing mode filter evaluates to false, is equivalent to the default processindModeFilter
        val nonBooleanFilter = listOf("'Non-Boolean Filter'")
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        val nonBooleanMsg = "Condition did not evaluate to a boolean type"
        mockkObject(FhirPathUtils)
        every { FhirPathUtils.evaluateCondition(any(), any(), any(), any()) } throws SchemaException(nonBooleanMsg)

        // act
        val result = engine.evaluateFilterAndLogResult(
            nonBooleanFilter,
            bundle,
            report,
            oneOrganization.receivers[0],
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            false,
        )

        assertThat(result).isFalse()
        assertThat(report.filteringResults).isNotEmpty()
        assertThat(report.filteringResults.count()).isEqualTo(1)
        assertThat(report.filteringResults[0].filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(report.filteringResults[0].message).contains(EXCEPTION_FOUND)

        val result2 = engine.evaluateFilterConditionAsAnd(
            nonBooleanFilter,
            bundle,
            defaultResponse = false,
            reverseFilter = false,
            bundle,
        )

        assertThat(result2.first).isFalse()
        assertThat(result2.second).isNotNull()
        assertThat(result2.second!!).contains(EXCEPTION_FOUND)

        val result3 = engine.evaluateFilterConditionAsOr(
            nonBooleanFilter,
            bundle,
            defaultResponse = false,
            reverseFilter = false,
            bundle,
        )

        assertThat(result3.first).isFalse()
        assertThat(result3.second).isNotNull()
        assertThat(result3.second!!).contains(EXCEPTION_FOUND)

        val result4 = engine.evaluateFilterConditionAsOr(
            nonBooleanFilter,
            bundle,
            defaultResponse = false,
            reverseFilter = true,
            bundle,
        )

        assertThat(result4.first).isFalse()
        assertThat(result4.second).isNotNull()
        assertThat(result4.second!!).contains(EXCEPTION_FOUND)
    }

    @Test
    fun `test is default filter`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val nonDefaultEquivalentQualityFilter: List<String> = ArrayList(engine.qualityFilterDefaults[Topic.FULL_ELR]!!)
        val nonDefaultEquivalentQualityFilter2: List<String> = ArrayList(engine.qualityFilterDefaults[Topic.ETOR_TI]!!)
        val nonDefaultEquivalentProcFilter: List<String> = ArrayList(engine.processingModeDefaults[Topic.FULL_ELR]!!)
        val nonDefaultQualityFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val nonDefaultProcModeFilter = listOf("%processingId = 'P'")
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)

        assertThat(
            engine.isDefaultFilter(
                ReportStreamFilterType.QUALITY_FILTER, engine.qualityFilterDefaults[Topic.FULL_ELR]!!
            )
        ).isTrue()
        assertThat(
            engine.isDefaultFilter(ReportStreamFilterType.QUALITY_FILTER, engine.qualityFilterDefaults[Topic.ETOR_TI]!!)
        ).isTrue()
        assertThat(
            engine.isDefaultFilter(ReportStreamFilterType.QUALITY_FILTER, nonDefaultEquivalentQualityFilter)
        ).isFalse()
        assertThat(
            engine.isDefaultFilter(ReportStreamFilterType.QUALITY_FILTER, nonDefaultEquivalentQualityFilter2)
        ).isFalse()
        assertThat(
            engine.isDefaultFilter(ReportStreamFilterType.QUALITY_FILTER, nonDefaultQualityFilter)
        ).isFalse()

        assertThat(engine.isDefaultFilter(ReportStreamFilterType.ROUTING_FILTER, routingFilter)).isFalse()

        assertThat(
            engine.isDefaultFilter(
                ReportStreamFilterType.PROCESSING_MODE_FILTER, engine.processingModeDefaults[Topic.FULL_ELR]!!
            )
        ).isTrue()
        assertThat(
            engine.isDefaultFilter(
                ReportStreamFilterType.PROCESSING_MODE_FILTER, engine.processingModeDefaults[Topic.ETOR_TI]!!
            )
        ).isTrue()
        assertThat(
            engine.isDefaultFilter(ReportStreamFilterType.PROCESSING_MODE_FILTER, nonDefaultEquivalentProcFilter)
        ).isFalse()
        assertThat(
            engine.isDefaultFilter(ReportStreamFilterType.PROCESSING_MODE_FILTER, nonDefaultProcModeFilter)
        ).isFalse()

        assertThat(engine.isDefaultFilter(ReportStreamFilterType.CONDITION_FILTER, conditionFilter)).isFalse()
    }
}