package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilters
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Provenance
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirRouterTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val metadata = Metadata(schema = Schema(name = "None", topic = "full-elr", elements = emptyList()))
    private val bodyFormat = Report.Format.FHIR
    val bodyUrl = "http://anyblob.com"
    private val defaultReceivers = listOf(
        Receiver(
            "full-elr-hl7",
            "co-phd",
            "full-elr",
            CustomerStatus.ACTIVE,
            "one"
        ),
        Receiver(
            "full-elr-hl7-2",
            "co-phd",
            "full-elr",
            CustomerStatus.INACTIVE,
            "one"
        )
    )
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = defaultReceivers
    )
    private val orgJurisFilter = listOf(
        ReportStreamFilters(
            topic = Topic.FULL_ELR.json_val,
            jurisdictionalFilter = listOf("testOrg"),
            qualityFilter = null,
            routingFilter = null,
            processingModeFilter = null
        )
    )
    private val jurisOrgFilterOrg = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        filters = orgJurisFilter,
        receivers = defaultReceivers
    )
    private val jurisFilterReceivers = listOf(
        Receiver(
            "full-elr-hl7",
            "co-phd",
            "topic",
            CustomerStatus.INACTIVE,
            "one",
            jurisdictionalFilter = listOf("testRec")
        )
    )
    private val jurisBothFiltersOrg = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        filters = orgJurisFilter,
        receivers = jurisFilterReceivers
    )
    private val jurisRecFilterOrg = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = jurisFilterReceivers
    )
    private val actionHistory = mockk<ActionHistory>()
    private val actionLogger = mockk<ActionLogger>()
    val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

    private val validFhirWithProvenance = """
    {
        "resourceType": "Bundle",
        "id": "1666038428133786000.94addcb6-835c-4883-a095-0c50cf113744",
        "meta": {
        "lastUpdated": "2022-10-17T20:27:08.149+00:00",
        "security": [
            {
                "code": "SECURITY",
                "display": "SECURITY"
            }
        ]
    },
        "identifier": {
        "value": "MT_COCAA_ORU_AAPHELR.1.6214638"
    },
        "type": "message",
        "timestamp": "2028-08-08T15:28:05.000+00:00",
        "entry": [
            {
                "fullUrl": "Provenance/1666038430962443000.9671377b-8f2b-4f5c-951c-b43ca8fd1a25",
                "resource": {
                    "resourceType": "Provenance",
                    "id": "1666038430962443000.9671377b-8f2b-4f5c-951c-b43ca8fd1a25",
                    "occurredDateTime": "2028-08-08T09:28:05-06:00",
                    "recorded": "2028-08-08T09:28:05-06:00",
                    "activity": {
                        "coding": [
                            {
                                "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                "code": "R01",
                                "display": "ORU_R01"
                            }
                        ]
                    }
                }
            }
        ]
    }"""

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // valid fhir, read file, update with destinations, write file, put message on queue
    @Test
    fun `test full elr routing happy path`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        // condition passes
        val jFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        val qFilter = listOf("Bundle.entry.resource.ofType(Provenance)[0].activity.coding[0].code = 'R01'")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jFilter
        every { engine.getQualityFilters(any(), any()) } returns qFilter
        every { engine.getRoutingFilter(any(), any()) } returns jFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns jFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), any())
            BlobAccess.Companion.uploadBlob(any(), any())
            queueMock.sendMessage(any(), any())
            accessSpy.insertTask(any(), any(), any(), any())
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `test adding receivers to bundle`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = listOf(oneOrganization.receivers[0])

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "co-phd.full-elr-hl7")
    }

    @Test
    fun `test skipping inactive receivers (only inactive)`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = listOf(oneOrganization.receivers[1])

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isEmpty())
    }

    @Test
    fun `test skipping inactive receivers (mixed)`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = oneOrganization.receivers

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "co-phd.full-elr-hl7")
    }

    @Test
    fun `test getJurisFilter no filters`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(defaultReceivers[0], emptyList())

        // assert
        assert(filters.isEmpty())
    }

    @Test
    fun `test getJurisFilter org filter, no receiver`() {
        val settings = FileSettings().loadOrganizations(jurisOrgFilterOrg)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(defaultReceivers[0], orgJurisFilter)

        // assert
        assert(filters[0] == "testOrg")
    }

    @Test
    fun `test getJurisFilter org filter, receiver override`() {
        val settings = FileSettings().loadOrganizations(jurisOrgFilterOrg)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(jurisFilterReceivers[0], orgJurisFilter)

        // assert
        assert(filters.any { it == "testRec" })
        assert(filters.any { it == "testOrg" })
    }

    @Test
    fun `test getJurisFilter receiver filter, no org`() {
        val settings = FileSettings().loadOrganizations(jurisRecFilterOrg)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // do work
        val filters = engine.getJurisFilters(jurisFilterReceivers[0], emptyList())

        // assert
        assert(filters.any { it == "testRec" })
        assert(filters.none { it == "testOrg" })
    }

    @Test
    fun `test getJurisFilter receiver multi line`() {
        val receiver = Receiver(
            "full-elr-hl7",
            "co-phd",
            "topic",
            CustomerStatus.INACTIVE,
            "one",
            jurisdictionalFilter = listOf("testRec", "testRec2")
        )
        val testOrg = DeepOrganization(
            "co-phd",
            "test",
            Organization.Jurisdiction.FEDERAL,
            filters = listOf(
                ReportStreamFilters(
                    topic = Topic.FULL_ELR.json_val,
                    jurisdictionalFilter = listOf("testOrg"),
                    qualityFilter = null,
                    routingFilter = null,
                    processingModeFilter = null
                )
            ),
            receivers = listOf(
                receiver
            )
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val orgFilters = emptyList<ReportStreamFilters>()

        // do work
        val filter = engine.getJurisFilters(receiver, orgFilters)

        // assert
        assert(filter.any { it == "testRec" })
        assert(filter.any { it == "testRec2" })
        assert(filter.none { it == "testOrg" })
    }

    @Test
    fun `test no filter (allowNone)`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val filter: List<String> = emptyList()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns filter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `test passes`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        // condition passes
        val filter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns filter
        every { engine.getQualityFilters(any(), any()) } returns filter
        every { engine.getRoutingFilter(any(), any()) } returns filter
        every { engine.getProcessingModeFilter(any(), any()) } returns filter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `test no pass`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        // condition does not pass
        val filter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 0")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns filter
        every { engine.getQualityFilters(any(), any()) } returns filter
        every { engine.getRoutingFilter(any(), any()) } returns filter
        every { engine.getProcessingModeFilter(any(), any()) } returns filter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `test multiline passes`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        // both conditions pass
        val filter =
            listOf(
                "Bundle.entry.resource.ofType(Provenance).count() > 0",
                "Bundle.entry.resource.ofType(Provenance)[0].activity.coding[0].code = 'R01'"
            )

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns filter
        every { engine.getQualityFilters(any(), any()) } returns filter
        every { engine.getRoutingFilter(any(), any()) } returns filter
        every { engine.getProcessingModeFilter(any(), any()) } returns filter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `test multiline no pass`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        // first condition passes, 2nd doesn't
        val filter =
            listOf(
                "Bundle.entry.resource.ofType(Provenance).count() > 0",
                "Bundle.entry.resource.ofType(Provenance)[0].activity.coding[0].code = 'R02'"
            )

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns filter
        every { engine.getQualityFilters(any(), any()) } returns filter
        every { engine.getRoutingFilter(any(), any()) } returns filter
        every { engine.getProcessingModeFilter(any(), any()) } returns filter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `test jurisfilter pass, qualfilter no pass`() {
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        // first condition passes, 2nd doesn't
        val jurisFilter =
            listOf(
                "Bundle.entry.resource.ofType(Provenance).count() > 0"
            )

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(validFhirWithProvenance)
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns emptyList()
        every { engine.getRoutingFilter(any(), any()) } returns emptyList()
        every { engine.getProcessingModeFilter(any(), any()) } returns emptyList()

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }
}