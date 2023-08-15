package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilters
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.metadata.LookupTable
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayInputStream
import kotlin.test.Test

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFilterTests {
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
            Receiver
            (
                RECEIVER_NAME,
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver
            (
                "full-elr-hl7-2",
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    private val fullElrReceiverNoFilters = Receiver(
        RECEIVER_NAME,
        ORGANIZATION_NAME,
        Topic.FULL_ELR,
        CustomerStatus.ACTIVE,
        "one"
    )

    private val etorTiReceiverNoFilters = Receiver(
        "etor-ti-hl7",
        "etor-org",
        Topic.ETOR_TI,
        CustomerStatus.ACTIVE,
        "one"
    )

    private val elrElimsReceiverNoFilters = Receiver(
        "elr-elims-hl7",
        "elims-org",
        Topic.ELR_ELIMS,
        CustomerStatus.ACTIVE,
        "one"
    )

    private val receiverWithFilters = Receiver(
        RECEIVER_NAME,
        ORGANIZATION_NAME,
        Topic.FULL_ELR,
        CustomerStatus.ACTIVE,
        "one",
        jurisdictionalFilter = listOf("testJuris"),
        qualityFilter = listOf("testQual"),
        routingFilter = listOf("testRouting"),
        processingModeFilter = listOf("testProcMode"),
        conditionFilter = listOf("testCondition")
    )

    private val orgFilters = listOf(
        ReportStreamFilters(
            topic = Topic.FULL_ELR,
            jurisdictionalFilter = listOf("testOrgJuris"),
            qualityFilter = listOf("testOrgQuality"),
            routingFilter = listOf("testOrgRouting"),
            processingModeFilter = listOf("testOrgProcMode")
        ),
        ReportStreamFilters(
            topic = Topic.COVID_19,
            jurisdictionalFilter = listOf("testCovidJuris"),
            qualityFilter = listOf("testCovidQuality"),
            routingFilter = listOf("testCovidRouting"),
            processingModeFilter = listOf("testCovidProcMode")
        )
    )

    private val orgNoFilters = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL
    )
    private val orgWithFilters = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        filters = orgFilters
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
    """.trimIndent()

    private val shorthandTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one).loadLookupTable("fhirpath_filter_shorthand", shorthandTable)

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(TaskAction.route)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // JURIS FILTERS
    @Test
    fun `test getJurisFilter no filters`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(fullElrReceiverNoFilters, emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getJurisFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(fullElrReceiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgJuris")
    }

    @Test
    fun `test getJurisFilter org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(receiverWithFilters, orgFilters)

        // assert
        assert(filters.size == 2)
        assert(filters.any { it == "testJuris" })
        assert(filters.any { it == "testOrgJuris" })
    }

    @Test
    fun `test getJurisFilter receiver filter, no org filter`() {
        val settings = FileSettings().loadOrganizations(orgNoFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(receiverWithFilters, emptyList())

        // assert
        assert(filters.size == 1)
        assert(filters.any { it == "testJuris" })
        assert(filters.none { it == "testOrgJuris" })
    }

    @Test
    fun `test getJurisFilter receiver multi line`() {
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.INACTIVE,
            "one",
            jurisdictionalFilter = listOf("testRec", "testRec2")
        )
        val testOrg = DeepOrganization(
            ORGANIZATION_NAME,
            "test",
            Organization.Jurisdiction.FEDERAL,
            filters = orgFilters,
            receivers = listOf(
                receiver
            )
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filter = engine.getJurisFilters(receiver, orgFilters)

        // assert
        assert(filter.any { it == "testRec" })
        assert(filter.any { it == "testRec2" })
        assert(filter.any { it == "testOrgJuris" })
    }

    // QUALITY FILTERS
    @Test
    fun `test getQualFilter no filters`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        var filters = engine.getQualityFilters(fullElrReceiverNoFilters, emptyList())
        assert(filters === engine.qualityFilterDefaults[Topic.FULL_ELR])

        filters = engine.getQualityFilters(etorTiReceiverNoFilters, emptyList())
        assert(filters === engine.qualityFilterDefaults[Topic.ETOR_TI])

        filters = engine.getQualityFilters(elrElimsReceiverNoFilters, emptyList())
        assert(filters === engine.qualityFilterDefaults[Topic.ELR_ELIMS])
    }

    @Test
    fun `test getQualFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getQualityFilters(fullElrReceiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgQuality")
    }

    @Test
    fun `test getQualFilter org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getQualityFilters(receiverWithFilters, orgFilters)

        // assert
        assert(filters.size == 2)
        assert(filters.any { it == "testQual" })
        assert(filters.any { it == "testOrgQuality" })
    }

    @Test
    fun `test getQualFilter receiver filter, no org filter`() {
        val settings = FileSettings().loadOrganizations(orgNoFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getQualityFilters(receiverWithFilters, emptyList())

        // assert
        assert(filters.size == 1)
        assert(filters.any { it == "testQual" })
        assert(filters.none { it == "testOrgQuality" })
    }

    // ROUTING FILTERS
    @Test
    fun `test getRoutingFilter no filters`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getRoutingFilter(fullElrReceiverNoFilters, emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getRoutingFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getRoutingFilter(fullElrReceiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgRouting")
    }

    @Test
    fun `test getRoutingFilter org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getRoutingFilter(receiverWithFilters, orgFilters)

        // assert
        assert(filters.size == 2)
        assert(filters.any { it == "testRouting" })
        assert(filters.any { it == "testOrgRouting" })
    }

    @Test
    fun `test getRoutingFilter receiver filter, no org filter`() {
        val settings = FileSettings().loadOrganizations(orgNoFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getRoutingFilter(receiverWithFilters, emptyList())

        // assert
        assert(filters.size == 1)
        assert(filters.any { it == "testRouting" })
        assert(filters.none { it == "testOrgRouting" })
    }

    // PROCESSING MODE FILTERS
    @Test
    fun `test getProcessingModeFilters no filters`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        var filters = engine.getProcessingModeFilter(fullElrReceiverNoFilters, emptyList())
        assert(filters === engine.processingModeDefaults[Topic.FULL_ELR])

        filters = engine.getProcessingModeFilter(etorTiReceiverNoFilters, emptyList())
        assert(filters === engine.processingModeDefaults[Topic.ETOR_TI])
    }

    @Test
    fun `test getProcessingModeFilters org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getProcessingModeFilter(fullElrReceiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgProcMode")
    }

    @Test
    fun `test getProcessingModeFilters org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getProcessingModeFilter(receiverWithFilters, orgFilters)

        // assert
        assert(filters.size == 2)
        assert(filters.any { it == "testProcMode" })
        assert(filters.any { it == "testOrgProcMode" })
    }

    @Test
    fun `test getProcessingModeFilters receiver filter, no org filter`() {
        val settings = FileSettings().loadOrganizations(orgNoFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        // do work
        val filters = engine.getProcessingModeFilter(receiverWithFilters, emptyList())

        // assert
        assert(filters.size == 1)
        assert(filters.any { it == "testProcMode" })
        assert(filters.none { it == "testOrgProcMode" })
    }

    @Test
    fun `test getConditionFilter`() {
        val settings = FileSettings().loadOrganizations(orgNoFilters)
        val engine = spyk(makeFhirEngine(metadata, settings) as FHIRRouter)

        val filters = engine.getConditionFilter(receiverWithFilters, orgFilters)
        assert(filters.size == 1)
        assert(filters.any { it == receiverWithFilters.conditionFilter[0] })
    }
}