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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFilterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver
            (
                "full-elr-hl7",
                "co-phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver
            (
                "full-elr-hl7-2",
                "co-phd",
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    private val receiverNoFilters = Receiver(
        "full-elr-hl7",
        "co-phd",
        Topic.FULL_ELR,
        CustomerStatus.ACTIVE,
        "one"
    )

    private val receiverWithFilters = Receiver(
        "full-elr-hl7",
        "co-phd",
        Topic.FULL_ELR,
        CustomerStatus.ACTIVE,
        "one",
        jurisdictionalFilter = listOf("testJuris"),
        qualityFilter = listOf("testQual"),
        routingFilter = listOf("testRouting"),
        processingModeFilter = listOf("testProcMode")
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
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL
    )
    private val orgWithFilters = DeepOrganization(
        "co-phd",
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

    val shorthandTable = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one).loadLookupTable("fhirpath_filter_shorthand", shorthandTable)

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // JURIS FILTERS
    @Test
    fun `test getJurisFilter no filters`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(receiverNoFilters, emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getJurisFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(receiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgJuris")
    }

    @Test
    fun `test getJurisFilter org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
            "full-elr-hl7",
            "co-phd",
            Topic.TEST,
            CustomerStatus.INACTIVE,
            "one",
            jurisdictionalFilter = listOf("testRec", "testRec2")
        )
        val testOrg = DeepOrganization(
            "co-phd",
            "test",
            Organization.Jurisdiction.FEDERAL,
            filters = orgFilters,
            receivers = listOf(
                receiver
            )
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getQualityFilters(receiverNoFilters, emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getQualFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getQualityFilters(receiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgQuality")
    }

    @Test
    fun `test getQualFilter org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getRoutingFilter(receiverNoFilters, emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getRoutingFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getRoutingFilter(receiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgRouting")
    }

    @Test
    fun `test getRoutingFilter org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getProcessingModeFilter(receiverNoFilters, emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getProcessingModeFilters org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getProcessingModeFilter(receiverNoFilters, orgFilters)

        // assert
        assert(filters.size == 1)
        assert(filters[0] == "testOrgProcMode")
    }

    @Test
    fun `test getProcessingModeFilters org filter + receiver`() {
        val settings = FileSettings().loadOrganizations(orgWithFilters)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

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
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getProcessingModeFilter(receiverWithFilters, emptyList())

        // assert
        assert(filters.size == 1)
        assert(filters.any { it == "testProcMode" })
        assert(filters.none { it == "testOrgProcMode" })
    }
}