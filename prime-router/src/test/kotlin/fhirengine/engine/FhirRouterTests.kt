package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasClass
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.fhir.context.FhirContext
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
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilterResult
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.ConditionSummary
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ObservationSummary
import gov.cdc.prime.router.azure.observability.event.ReportAcceptedEvent
import gov.cdc.prime.router.azure.observability.event.ReportRouteEvent
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
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
private const val TOPIC_TEST_ORG_NAME = "topic-test"
private const val QUALITY_TEST_URL = "src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir"
private const val OBSERVATION_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Observation).count() > 0"
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
private const val CONDITION_FILTER = "%resource.code.coding.code = '95418-0'"

data object SampleFilters {
    /**
     * Below Filters were used as default filters for a topic until issue: #11441
     * when topic level default filters become deprecated, they are renamed as:
     * 1. fullElrQualityFilterSample
     * 2. etorTiQualityFilterSample
     * 3. elrElimsQualityFilterSample
     * 4. processingModeFilterSample
     * and parked here for reference
     *
     * Now, receivers need to explicitly specify their filters
     *
     */

    /**
     *   Quality filter sample for receivers on FULL_ELR topic:
     *   Must have message ID, patient last name, patient first name, DOB, specimen type
     *   At least one of patient street, patient zip code, patient phone number, patient email
     *   At least one of order test date, specimen collection date/time, test result date
     */
    val fullElrQualityFilterSample: ReportStreamFilter = listOf(
        "%messageId.exists()",
        "%patient.name.family.exists()",
        "%patient.name.given.count() > 0",
        "%patient.birthDate.exists()",
        "%specimen.type.exists()",
        "(%patient.address.line.exists() or " +
            "%patient.address.postalCode.exists() or " +
            "%patient.telecom.exists())",
        "(" +
            "(%specimen.collection.collectedPeriod.exists() or " +
            "%specimen.collection.collected.exists()" +
            ") or " +
            "%serviceRequest.occurrence.exists() or " +
            "%observation.effective.exists())",
    )

    /**
     *   Quality filter sample for receivers on ETOR_TI topic:
     *   Must have message ID
     */
    val etorTiQualityFilterSample: ReportStreamFilter = listOf(
        "%messageId.exists()",
    )

    /**
     *   Quality filter sample for receivers on ELR_ELIMS topic:
     *   no rules; completely open
     */
    val elrElimsQualityFilterSample: ReportStreamFilter = listOf(
        "true",
    )

    /**
     *  Processing mode filter sample for receivers on ETOR_TI or FULL_ELR:
     *  Must have a processing mode id of 'P'
     */
    val processingModeFilterSample: ReportStreamFilter = listOf(
        "%processingId.exists() and %processingId = 'P'"
    )
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirRouterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    private val actionHistory = ActionHistory(TaskAction.route)
    private val azureEventService = InMemoryAzureEventService()
    private val reportServiceMock = mockk<ReportService>()
    private val submittedId = UUID.randomUUID()

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
                "one",
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

    val secondElrOrganization = DeepOrganization(
        "second org",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "second-receiver-2",
                "second org",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one",
                conditionFilter = listOf(CONDITION_FILTER),
            ),
            Receiver(
                "full-elr-hl7-2",
                "second org",
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

    val orgWithMappedConditionFilter = DeepOrganization(
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
                mappedConditionFilter = listOf(CodeStringConditionFilter("6142004,Some Condition Code"))
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

    val orgWithAOEMappedConditionFilter = DeepOrganization(
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
                mappedConditionFilter = listOf(CodeStringConditionFilter("AOE"))
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
        val rootReport = mockk<ReportFile>()
        every { rootReport.reportId } returns submittedId
        every { rootReport.sendingOrg } returns "sendingOrg"
        every { rootReport.sendingOrgClient } returns "sendingOrgClient"
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getSenderName(any()) } returns "sendingOrg.sendingOrgClient"

        return FHIREngine.Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(accessSpy)
            .blobAccess(blobMock)
            .azureEventService(azureEventService)
            .reportService(reportServiceMock)
            .build(TaskAction.route)
    }

    /**
     * Private extension function on FHIRRouter as helper to allow setting all filter mocks in one call
     */
    private fun FHIRRouter.setFiltersOnEngine(
        jurisFilter: List<String>,
        qualFilter: List<String>,
        routingFilter: List<String>,
        processingModeFilter: List<String>,
        conditionFilter: List<String> = emptyList(),
        mappedConditionFilter: ReportStreamConditionFilter = emptyList(),
    ) {
        every { getJurisFilters(any(), any()) } returns jurisFilter
        every { getQualityFilters(any(), any()) } returns qualFilter
        every { getRoutingFilter(any(), any()) } returns routingFilter
        every { getProcessingModeFilter(any(), any()) } returns processingModeFilter
        every { getConditionFilter(any(), any()) } returns conditionFilter
        every { getMappedConditionFilter(any(), any()) } returns mappedConditionFilter
    }

    @BeforeEach
    fun reset() {
        actionHistory.reportsIn.clear()
        actionHistory.reportsOut.clear()
        actionHistory.actionLogs.clear()
        azureEventService.clear()
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // with multiple condition filters, we are looking for any of them passing
        val conditionFilter = listOf(
            PROVENANCE_COUNT_GREATER_THAN_ZERO, // true
            "Bundle.entry.resource.ofType(Provenance).count() >= 1" // also true
        )
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (mappedConditionFilter multiple filters, all true) `() {
        @Suppress("ktlint:standard:max-line-length")
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""
        val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.entry.filter { it.resource is Observation }.forEach {
            val observation = (it.resource as Observation)
            observation.code.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("SNOMEDCT", "6142004", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("Condition Code System", "Some Condition Code", "Condition Name")
            )
        }
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        // with multiple mapped condition filters, we are looking for any of them passing
        val mappedConditionFilter = listOf(CodeStringConditionFilter("6142004,Some Condition Code"))
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // with multiple condition filters, we are looking for any of them passing
        val conditionFilter = listOf(
            PROVENANCE_COUNT_GREATER_THAN_ZERO, // true
            "Bundle.entry.resource.ofType(Provenance).count() > 100" // not true
        )
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isNotEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (mappedConditionFilter multiple filters, one true) `() {
        @Suppress("ktlint:standard:max-line-length")
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""
        val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.entry.filter { it.resource is Observation }.forEach {
            val observation = (it.resource as Observation)
            observation.code.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("SNOMEDCT", "6142004", "Influenza (disorder)")
            )
            observation.valueCodeableConcept.coding[0].addExtension(
                conditionCodeExtensionURL,
                Coding("Condition Code System", "No Match", "Condition Name")
            )
        }
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>() // filter of interest is set on organization
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // with multiple condition filters, we are looking for any of them passing
        val conditionFilter = listOf(
            "Bundle.entry.resource.ofType(Provenance).count() > 50", // not true
            "Bundle.entry.resource.ofType(Provenance).count() > 100" // not true
        )
        val mappedConditionFilter = emptyList<ConditionFilter>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test applyFilters receiver setting - (mappedConditionFilter multiple filters, none true) `() {
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
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>() // filter of interest is set on organization
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert only the quality filter didn't pass
        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.QUALITY_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `fail - jurisfilter does not pass`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter does not`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.actionLogs).hasSize(1)
            val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
            assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.QUALITY_FILTER)
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter does not`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.actionLogs).hasSize(1)
            val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
            assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.ROUTING_FILTER)
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter passes, proc mode does not`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.actionLogs).hasSize(1)
            val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
            assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        }
    }

    @Test
    fun `fail - all pass other than the condition filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            // There are five observations in the bundle, none of which pass the filter and each one is logged once
            assertThat(actionHistory.actionLogs).hasSize(5)
            val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
            assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.CONDITION_FILTER)
        }
    }

    @Test
    fun `fail - all pass other than mapped condition filter fails`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_EQUAL_TO_TEN)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            // There are five observations in the bundle, none of which pass the filter and each one is logged once
            assertThat(actionHistory.actionLogs).hasSize(5)
            val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
            assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.CONDITION_FILTER)
        }
    }

    @Test
    fun `fail - all pass other than mapped condition filter fails due to unmapped observation`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = listOf(CodeStringConditionFilter("foo,bar"))

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.actionLogs).hasSize(1) // bundle did not pass filter
            val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
            assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.MAPPED_CONDITION_FILTER)
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
        val settings = FileSettings().loadOrganizations(orgWithAOEMappedConditionFilter)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = listOf(CodeStringConditionFilter("AOE"))

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        accessSpy.transact { _ ->
            val results = engine.doWork(message, actionLogger, actionHistory)
            assertThat(results).isEmpty()
        }
    }

    @Test
    fun `fail - all pass other than the mapped condition filter fails`() {
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

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = emptyList<String>()
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.actionLogs).hasSize(1) // bundle did not pass filter
             val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
             assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.MAPPED_CONDITION_FILTER)
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
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        mockkStatic(Bundle::filterObservations)
        every { any<Bundle>().filterObservations(any(), any()) } returns bundle
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

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
            BlobAccess.uploadBlob(any(), any(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test a message is queued per receiver that will have the report delivered`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization, secondElrOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        mockkStatic(Bundle::filterObservations)
        every { any<Bundle>().filterObservations(any(), any()) } returns FhirTranscoder.decode(fhirData)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).hasSize(2)
            assertThat(actionHistory.actionLogs).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(2)
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
        val settings = FileSettings().loadOrganizations(secondElrOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val originalBundle = FhirTranscoder.decode(fhirData)
        val expectedBundle = originalBundle
            .filterObservations(listOf(CONDITION_FILTER), engine.loadFhirPathShorthandLookupTable())

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

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
        val fhirRecord = """{"resourceType":"Bundle","id":"1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347","meta":{"lastUpdated":"2022-11-07T22:56:07.832+00:00"},"identifier":{"value":"1234d1d1-95fe-462c-8ac6-46728dba581c", "system": "https://reportstream.cdc.gov/prime-router"},"type":"message","timestamp":"2021-08-03T13:15:11.015+00:00","entry":[{"fullUrl":"Observation/d683b42a-bf50-45e8-9fce-6c0531994f09","resource":{"resourceType":"Observation","id":"d683b42a-bf50-45e8-9fce-6c0531994f09","status":"final","code":{"coding":[{"system":"http://loinc.org","code":"80382-5"}],"text":"Flu A"},"subject":{"reference":"Patient/9473889b-b2b9-45ac-a8d8-191f27132912"},"performer":[{"reference":"Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"}],"valueCodeableConcept":{"coding":[{"system":"http://snomed.info/sct","code":"260373001","display":"Detected"}]},"interpretation":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v2-0078","code":"A","display":"Abnormal"}]}],"method":{"extension":[{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/testkit-name-id","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}},{"url":"https://reportstream.cdc.gov/fhir/StructureDefinition/equipment-uid","valueCoding":{"code":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B_Becton, Dickinson and Company (BD)"}}],"coding":[{"display":"BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B*"}]},"specimen":{"reference":"Specimen/52a582e4-d389-42d0-b738-bee51cf5244d"},"device":{"reference":"Device/78dc4d98-2958-43a3-a445-76ceef8c0698"}}}]}"""

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
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val expectedBundle = bundle
            .filterMappedObservations(listOf(CodeStringConditionFilter("6142004"))).second

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(OBSERVATION_COUNT_GREATER_THAN_ZERO)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(FhirTranscoder.encode(bundle))
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

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
                ReportAcceptedEvent(
                    message.reportId,
                    submittedId,
                    message.topic,
                    "sendingOrg.sendingOrgClient",
                    listOf(
                        ObservationSummary(
                            listOf(
                                ConditionSummary("6142004", "Influenza (disorder)"),
                                ConditionSummary("Some Condition Code", "Condition Name")
                            )
                        )
                    ),
                    1998,
                    AzureEventUtils.MessageID(
                        "1234d1d1-95fe-462c-8ac6-46728dba581c",
                        "https://reportstream.cdc.gov/prime-router"
                    )
                ),
                ReportRouteEvent(
                    reportId,
                    message.reportId,
                    submittedId,
                    message.topic,
                    "sendingOrg.sendingOrgClient",
                    orgWithMappedConditionFilter.receivers.first().fullName,
                    expectedObservationSummary,
                    expectedObservationSummary,
                    1998,
                    AzureEventUtils.MessageID(
                        "1234d1d1-95fe-462c-8ac6-46728dba581c",
                        "https://reportstream.cdc.gov/prime-router"
                    )
                )
            )

            val actualEvents = azureEventService.getEvents()
            assertThat(actualEvents).hasSize(2)
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
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME,
                topic = Topic.FULL_ELR,
            )
        )

        val originalBundle = FhirTranscoder.decode(fhirData)

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // filters
        val jurisFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val qualFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val routingFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val conditionFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedConditionFilter = emptyList<ConditionFilter>()

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.info(any<PrunedObservationsLogMessage>()) } just runs
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, conditionFilter, mappedConditionFilter
        )

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
            BlobAccess.uploadBlob(any(), FhirTranscoder.encode(originalBundle).toByteArray(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `test bundle with no receivers is not routed to translate function`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message =
            spyk(
                FhirConvertQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER_NAME,
                    topic = Topic.FULL_ELR
                )
            )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { engine.findReceiversForBundle(any(), any(), any(), any()) } returns emptyList()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)

        mockkStatic(Bundle::filterObservations)
        every { any<Bundle>().filterObservations(any(), any()) } returns FhirTranscoder.decode(fhirData)

        // act
        accessSpy.transact { txn ->
            val messages = engine.run(message, actionLogger, actionHistory, txn)
            assertThat(messages).isEmpty()
            assertThat(actionHistory.reportsIn).hasSize(1)
            assertThat(actionHistory.reportsOut).hasSize(1)

            val azureEvents = azureEventService.getEvents()
            val expectedAcceptedEvent = ReportAcceptedEvent(
                message.reportId,
                submittedId,
                message.topic,
                "sendingOrg.sendingOrgClient",
                listOf(
                    ObservationSummary(
                        ConditionSummary(
                            "840539006",
                            "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
                        )
                    ),
                    ObservationSummary(code = "95418-0"),
                    ObservationSummary(code = "95417-2"),
                    ObservationSummary(code = "95421-4"),
                    ObservationSummary(code = "95419-8"),
                ),
                38803,
                AzureEventUtils.MessageID(
                    "1234d1d1-95fe-462c-8ac6-46728dba581c",
                    "https://reportstream.cdc.gov/prime-router"
                )
            )
            val observationSummaries = listOf(
                ObservationSummary(
                    ConditionSummary(
                        "840539006",
                        "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
                    )
                ),
                ObservationSummary.EMPTY,
                ObservationSummary.EMPTY,
                ObservationSummary.EMPTY,
                ObservationSummary.EMPTY
            )
            val expectedRoutedEvent = ReportRouteEvent(
                UUID.randomUUID(),
                message.reportId,
                submittedId,
                message.topic,
                "sendingOrg.sendingOrgClient",
                null,
                observationSummaries,
                observationSummaries,
                36995,
                AzureEventUtils.MessageID(
                    "1234d1d1-95fe-462c-8ac6-46728dba581c",
                    "https://reportstream.cdc.gov/prime-router"
                )
            )
            assertThat(azureEvents).hasSize(2)
            assertThat(azureEvents.first())
                .isEqualTo(expectedAcceptedEvent)
            assertThat(azureEvents[1])
                .isInstanceOf<ReportRouteEvent>()
                .isEqualToIgnoringGivenProperties(
                    expectedRoutedEvent,
                    ReportRouteEvent::reportId // unable to access generated report ID since no message is generated
                )
        }

        // assert
        verify(exactly = 0) {
            accessSpy.insertTask(any(), any(), any(), any())
            BlobAccess.uploadBlob(any(), any())
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

        assertThat(actionHistory.actionLogs.count()).isEqualTo(0)
        engine.logFilterResults(
            qualFilter.toString(),
            bundle,
            report.id,
            actionHistory,
            receiver,
            ReportStreamFilterType.QUALITY_FILTER,
            bundle
        )
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterName).isEqualTo(qualFilter.toString())
    }

    @Test
    fun `test actionLogger trigger during evaluateFilterCondition`() {
        val fhirData = File(VALID_FHIR_URL).readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)
        mockkObject(FhirPathUtils)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)

        val actionLogger = ActionLogger()

        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        val message = spyk(
            FhirConvertQueueMessage(
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

        mockkStatic(Bundle::filterObservations)
        every { any<Bundle>().filterObservations(any(), any()) } returns FhirTranscoder.decode(fhirData)
        engine.setFiltersOnEngine(
            jurisFilter,
            qualFilter = emptyList(),
//            engine.qualityFilterDefaults[Topic.FULL_ELR]!!,
            routingFilter = emptyList(),
//            engine.processingModeDefaults[Topic.FULL_ELR]!!,
            processingModeFilter = emptyList()
        )

        val nonBooleanMsg = "Condition did not evaluate to a boolean type"
        every { FhirPathUtils.evaluateCondition(any(), any(), any(), any(), any()) } throws SchemaException(
            nonBooleanMsg
        )

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

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

        engine.evaluateFilterAndLogResult(filter, bundle, report.id, actionHistory, receiver, type, true)
        verify(exactly = 0) {
            engine.logFilterResults(any(), any(), any(), any(), any(), any(), any())
        }
        engine.evaluateFilterAndLogResult(filter, bundle, report.id, actionHistory, receiver, type, false)
        verify(exactly = 1) {
            engine.logFilterResults(any(), any(), any(), any(), any(), any(), any())
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
            report.id,
            actionHistory,
            receiver,
            ReportStreamFilterType.CONDITION_FILTER,
            defaultResponse = true,
            reverseFilter = false,
            focusResource = observation,
            useOr = true
        )
        verify(exactly = 1) {
            engine.logFilterResults(any(), any(), any(), any(), any(), any(), any())
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // when doing routing for full-elr, verify that etor receiver isn't included (not even in logged results)
        var receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()

        // when doing routing for etor, verify that etor receiver is included
        receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.ETOR_TI)
        assertThat(actionHistory.actionLogs).isEmpty()
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // when doing routing for etor, verify that full-elr receiver isn't included (not even in logged results)
        var receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.ETOR_TI)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()

        // when doing routing for full-elr, verify that full-elr receiver is included
        receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)
        assertThat(actionHistory.actionLogs).isEmpty()
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // when doing routing for full-elr, verify that elims receiver isn't included (not even in logged results)
        var receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)
        assertThat(report.filteringResults).isEmpty()
        assertThat(receivers).isEmpty()

        // when doing routing for elims, verify that elims receiver is included
        receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.ELR_ELIMS)
        assertThat(actionHistory.actionLogs).isEmpty()
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(jurisFilter, qualFilter, routingFilter, processingModeFilter)

        // when routing for etor, verify that only the active etor receiver is included (even in logged results)
        var receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.ETOR_TI)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0].name).isEqualTo("simulatedlab")

        // when routing for full-elr, verify that only the active full-elr receiver is included (even in logged results)
        receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)
        assertThat(actionHistory.actionLogs).isEmpty()
        assertThat(receivers.size).isEqualTo(1)
        assertThat(receivers[0].name).isEqualTo(RECEIVER_NAME)

        // Verify error when using non-UP topic
        assertFailure { engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.COVID_19) }
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This condition filter evaluates to true
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedCondFilter = emptyList<ConditionFilter>()
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, condFilter, mappedCondFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert only the quality filter didn't pass

        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.ROUTING_FILTER)
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_10)
        // This condition filter evaluates to true
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        val mappedCondFilter = emptyList<ConditionFilter>()
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, condFilter, mappedCondFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert only the processing mode filter didn't pass

        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test applyFilters logs results for mapped condition filters`() {
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
        val settings = FileSettings().loadOrganizations(orgWithMappedConditionFilter)
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
        val mappedCondFilter = emptyList<ConditionFilter>()
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, condFilter, mappedCondFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert only the mapped condition filter didn't pass
        // and check that the observation was logged
        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.MAPPED_CONDITION_FILTER)
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
        val processingModeFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_ZERO)
        // This condition filter evaluates to false
        val condFilter = listOf(PROVENANCE_COUNT_GREATER_THAN_10)
        val mappedCondFilter = emptyList<ConditionFilter>()
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        engine.setFiltersOnEngine(
            jurisFilter, qualFilter, routingFilter, processingModeFilter, condFilter, mappedCondFilter
        )

        // act
        val receivers = engine.findReceiversForBundle(bundle, report.id, actionHistory, Topic.FULL_ELR)

        // assert only the condition mode filter didn't pass
        // and check that the observation was logged
        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(5)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.CONDITION_FILTER)
        assertThat(reportStreamFilterResult.filteredTrackingElement.contains("loinc.org"))
        assertThat(receivers).isEmpty()
    }

    @Test
    fun `test tagging default filter in logs`() {
        // content is not important, just get a Bundle
        val bundle = Bundle()
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This processing mode filter evaluates to false, is equivalent to the default processingModeFilter
        val processingModeFilter = listOf("%processingId = 'P'")
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)
        every { engine.evaluateFilterConditionAsAnd(any(), any(), any(), any(), any()) } returns Pair<Boolean, String?>(
            false,
            processingModeFilter.toString()
        )

        // act
        val result = engine.evaluateFilterAndLogResult(
            processingModeFilter,
            bundle,
            report.id,
            actionHistory,
            oneOrganization.receivers[0],
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            false,
        )

        assertThat(result).isFalse()
        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(reportStreamFilterResult.message).doesNotContain("default filter")

        actionHistory.actionLogs.clear()

        val result2 = engine.evaluateFilterAndLogResult(
            SampleFilters.fullElrQualityFilterSample,
            bundle,
            report.id,
            actionHistory,
            oneOrganization.receivers[0],
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            false,
        )

        assertThat(result2).isFalse()
        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult2 = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult2.filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(reportStreamFilterResult2.message).contains("")
    }

    @Test
    fun `test tagging exceptions filter in logs`() {
        // content is not important, just get a Bundle
        val bundle = Bundle()
        val settings = FileSettings().loadOrganizations(oneOrganization)
        // This processing mode filter evaluates to false, is equivalent to the default processingModeFilter
        val nonBooleanFilter = listOf("'Non-Boolean Filter'")
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        val nonBooleanMsg = "Condition did not evaluate to a boolean type"
        mockkObject(FhirPathUtils)
        every { FhirPathUtils.evaluateCondition(any(), any(), any(), any(), any()) } throws SchemaException(
            nonBooleanMsg
        )

        // act
        val result = engine.evaluateFilterAndLogResult(
            nonBooleanFilter,
            bundle,
            report.id,
            actionHistory,
            oneOrganization.receivers[0],
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            false,
        )

        assertThat(result).isFalse()
        assertThat(actionHistory.actionLogs).isNotEmpty()
        assertThat(actionHistory.actionLogs.count()).isEqualTo(1)
        val reportStreamFilterResult = actionHistory.actionLogs[0].detail as ReportStreamFilterResult
        assertThat(reportStreamFilterResult.filterType).isEqualTo(ReportStreamFilterType.PROCESSING_MODE_FILTER)
        assertThat(reportStreamFilterResult.message).contains(EXCEPTION_FOUND)

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
}