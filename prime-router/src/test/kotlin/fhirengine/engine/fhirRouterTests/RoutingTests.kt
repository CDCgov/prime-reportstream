package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.engine.FHIREngine
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.UUID
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingTests {
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
                "full-elr",
                CustomerStatus.ACTIVE,
                "one"
            ),
            Receiver
            (
                "full-elr-hl7-2",
                "co-phd",
                "full-elr",
                CustomerStatus.INACTIVE,
                "one"
            )
        )
    )

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `0 test qualFilter default - succeed, basic covid FHIR`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)

        // act
        val qualDefaultResult = engine.evaluateFilterCondition(engine.qualityFilterDefault, bundle, false)

        // assert
        assert(qualDefaultResult)
    }

    @Test
    fun `fail - jurisfilter does not pass`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        var qualFilter = emptyList<String>()
        var routingFilter = emptyList<String>()
        var procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter does not`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        var routingFilter = emptyList<String>()
        var procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter does not`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")
        var procModeFilter = emptyList<String>()

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `fail - jurisfilter passes, qual filter passes, routing filter passes, proc mode does not`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() = 10")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            FHIRBundleHelpers.addReceivers(any(), any())
        }
    }

    @Test
    fun `success - jurisfilter passes, qual filter passes, routing filter passes, proc mode passes`() {
        val fhirData = File("src/test/resources/fhirengine/engine/routing/valid.fhir").readText()

        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = "full-elr", elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.route) as FHIRRouter)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        // filters
        val jurisFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var qualFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var routingFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")
        var procModeFilter = listOf("Bundle.entry.resource.ofType(Provenance).count() > 0")

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }.returns(fhirData)
        every { BlobAccess.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { FHIRBundleHelpers.addReceivers(any(), any()) } returns Unit
        every { engine.getJurisFilters(any(), any()) } returns jurisFilter
        every { engine.getQualityFilters(any(), any()) } returns qualFilter
        every { engine.getRoutingFilter(any(), any()) } returns routingFilter
        every { engine.getProcessingModeFilter(any(), any()) } returns procModeFilter

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
}