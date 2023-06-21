package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isFailure
import gov.cdc.prime.router.CustomConfiguration
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FHIRConfiguration
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.HL7MessageHelpers
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.jooq.Configuration
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class BatchFunctionTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)

    val oneOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.COVID_19,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        ),
    )

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
                .blobAccess(blobMock).queueAccess(queueMock).build()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test running batchFunction as 'empty batch'`() {
        // Setup
        every { timing1.isValid() } returns true
        every { timing1.maxReportCount } returns 500
        every { timing1.numberPerDay } returns 1440
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(metadata, settings)

        every { engine.generateEmptyReport(any(), any()) } returns Unit

        // the message that will be passed to batchFunction
        val message = "receiver&BATCH&phd.elr&true"

        // Invoke batch function run
        BatchFunction(engine).run(message, context = null)

        // empty pathway should be called
        verify(exactly = 1) { engine.generateEmptyReport(any(), any()) }
        // standard batch handling should not be called
        verify(exactly = 0) { engine.handleBatchEvent(any(), any(), any(), any()) }
    }

    @Test
    fun `test batching HL7 from universal pipeline`() {
        val mockMetadata = mockk<Metadata>()
        val mockWorkflowEngine = mockk<WorkflowEngine>()
        val mockActionHistory = mockk<ActionHistory>()
        val mockTxn = mockk<Configuration>()
        val mockReportFile = mockk<ReportFile>()
        val mockReport = mockk<Report>()
        val mockOrganization = mockk<Organization>()
        val mockTask = mockk<Task>()
        every { mockTask.reportId } returns UUID.randomUUID()
        every { mockTask.bodyUrl } returns "someurl"
        val mockEvent = mockk<Event>()
        mockkObject(BlobAccess)
        mockkObject(HL7MessageHelpers)
        mockkObject(Report)

        val batchFunction = BatchFunction(mockWorkflowEngine)

        // Test sending HL7 (no HL7 batch) files
        var receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                useBatchHeaders = false,
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            ),
            timing = null
        )
        var headers = listOf(
            WorkflowEngine.Header(
                mockTask, mockReportFile, null, mockOrganization,
                receiver, null, "content".toByteArray(), true
            ),
            WorkflowEngine.Header(
                mockTask, mockReportFile, null, mockOrganization,
                receiver, null, "content2".toByteArray(), true
            )
        )

        fun resetMocks() {
            clearMocks(mockWorkflowEngine, mockActionHistory, BlobAccess, HL7MessageHelpers, Report)
            every { mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any()) } returns Unit
            every { mockWorkflowEngine.metadata } returns mockMetadata
            every { mockActionHistory.trackExistingInputReport(any()) } returns Unit
            every { BlobAccess.Companion.downloadBlob(any()) } returns "somecontent".toByteArray()
            every { Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any()) } returns
                Triple(mockReport, mockEvent, BlobAccess.BlobInfo(Report.Format.HL7, "someurl", "digest".toByteArray()))
            every { HL7MessageHelpers.batchMessages(any(), receiver) } returns "batchstring"
        }
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 2) {
            mockActionHistory.trackExistingInputReport(any())
            BlobAccess.Companion.downloadBlob(any())
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // Single HL7s, but timing is set to merge
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                useBatchHeaders = false,
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.NONE)
            )
        )
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 2) {
            mockActionHistory.trackExistingInputReport(any())
            BlobAccess.Companion.downloadBlob(any())
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // Test batching HL7 batch file
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                useBatchHeaders = true,
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.NONE)
            )
        )
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 2) {
            mockActionHistory.trackExistingInputReport(any())
            BlobAccess.Companion.downloadBlob(any())
        }
        verify(exactly = 1) {
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            HL7MessageHelpers.batchMessages(any(), receiver)
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // batch sending when there is no data to send, but receiver wants empty batch
        headers = emptyList()
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = Hl7Configuration(
                useBatchHeaders = true,
                receivingApplicationName = null, receivingApplicationOID = null,
                receivingFacilityName = null, receivingFacilityOID = null, receivingOrganization = null,
                messageProfileId = null
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.SEND)
            )
        )
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 1) {
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            HL7MessageHelpers.batchMessages(any(), receiver)
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // Finito!
        unmockkObject(BlobAccess, HL7MessageHelpers, Report)
    }

    @Test
    fun `test batching FHIR from universal pipeline`() {
        val mockMetadata = mockk<Metadata>()
        val mockWorkflowEngine = mockk<WorkflowEngine>()
        val mockActionHistory = mockk<ActionHistory>()
        val mockTxn = mockk<Configuration>()
        val mockReportFile = mockk<ReportFile>()
        val mockReport = mockk<Report>()
        val mockOrganization = mockk<Organization>()
        val mockTask = mockk<Task>()
        every { mockTask.reportId } returns UUID.randomUUID()
        every { mockTask.bodyUrl } returns "someurl"
        val mockEvent = mockk<Event>()
        mockkObject(BlobAccess)
        mockkObject(FHIRBundleHelpers)
        mockkObject(Report)

        val batchFunction = BatchFunction(mockWorkflowEngine)

        // Test sending unbatched FHIR files
        var receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = FHIRConfiguration(
                useBatching = false,
                receivingOrganization = null,
            ),
            timing = null
        )
        var headers = listOf(
            WorkflowEngine.Header(
                mockTask, mockReportFile, null, mockOrganization,
                receiver, null, "content".toByteArray(), true
            ),
            WorkflowEngine.Header(
                mockTask, mockReportFile, null, mockOrganization,
                receiver, null, "content2".toByteArray(), true
            )
        )

        fun resetMocks() {
            clearMocks(mockWorkflowEngine, mockActionHistory, BlobAccess, FHIRBundleHelpers, Report)
            every { mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any()) } returns Unit
            every { mockWorkflowEngine.metadata } returns mockMetadata
            every { mockActionHistory.trackExistingInputReport(any()) } returns Unit
            every { BlobAccess.Companion.downloadBlob(any()) } returns "somecontent".toByteArray()
            every { Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any()) } returns
                Triple(mockReport, mockEvent, BlobAccess.BlobInfo(Report.Format.HL7, "someurl", "digest".toByteArray()))
            every { FHIRBundleHelpers.batchMessages(any()) } returns "batchstring"
        }
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 2) {
            mockActionHistory.trackExistingInputReport(any())
            BlobAccess.Companion.downloadBlob(any())
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // Don't batch, but timing is set to merge
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = FHIRConfiguration(
                useBatching = false,
                receivingOrganization = null,
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.NONE)
            )
        )
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 2) {
            mockActionHistory.trackExistingInputReport(any())
            BlobAccess.Companion.downloadBlob(any())
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // Test batching FHIR file
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = FHIRConfiguration(
                useBatching = true,
                receivingOrganization = null,
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.NONE)
            )
        )
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 2) {
            mockActionHistory.trackExistingInputReport(any())
            BlobAccess.Companion.downloadBlob(any())
        }
        verify(exactly = 1) {
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            FHIRBundleHelpers.batchMessages(any())
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        // batch sending when there is no data to send, but receiver wants empty batch
        headers = emptyList()
        receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = FHIRConfiguration(
                useBatching = true,
                receivingOrganization = null,
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.SEND)
            )
        )
        resetMocks()
        batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn)
        verify(exactly = 1) {
            Report.generateReportAndUploadBlob(any(), any(), any(), any(), any(), any(), any())
            FHIRBundleHelpers.batchMessages(any())
            mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any())
        }

        unmockkObject(BlobAccess, FHIRBundleHelpers, Report)
    }

    @Test
    fun `test universal pipeline batching errors`() {
        val mockMetadata = mockk<Metadata>()
        val mockWorkflowEngine = mockk<WorkflowEngine>()
        val mockActionHistory = mockk<ActionHistory>()
        val mockTxn = mockk<Configuration>()
        val mockReportFile = mockk<ReportFile>()
        val mockOrganization = mockk<Organization>()
        val mockTask = mockk<Task>()
        every { mockTask.reportId } returns UUID.randomUUID()
        every { mockTask.bodyUrl } returns "someurl"
        mockkObject(BlobAccess)

        val batchFunction = BatchFunction(mockWorkflowEngine)

        // Test sending unsupported file type (CSV)
        var receiver = Receiver(
            "name", "org", Topic.FULL_ELR,
            translation = CustomConfiguration(
                schemaName = "",
                format = Report.Format.CSV,
                receivingOrganization = null,
                useBatching = true,
            ),
            timing = Receiver.Timing(
                Receiver.BatchOperation.MERGE, whenEmpty = Receiver.WhenEmpty(Receiver.EmptyOperation.NONE)
            )
        )
        var headers = listOf(
            WorkflowEngine.Header(
                mockTask, mockReportFile, null, mockOrganization,
                receiver, null, "content".toByteArray(), true
            ),
            WorkflowEngine.Header(
                mockTask, mockReportFile, null, mockOrganization,
                receiver, null, "content2".toByteArray(), true
            )
        )
        every { mockWorkflowEngine.db.insertTask(any(), any(), any(), any(), any()) } returns Unit
        every { mockWorkflowEngine.metadata } returns mockMetadata
        every { mockActionHistory.trackExistingInputReport(any()) } returns Unit
        every { BlobAccess.Companion.downloadBlob(any()) } returns "somecontent".toByteArray()

        assertThat { batchFunction.batchUniversalData(headers, mockActionHistory, receiver, mockTxn) }.isFailure()
            .hasClass(java.lang.IllegalStateException::class.java)

        unmockkObject(BlobAccess)
    }
}