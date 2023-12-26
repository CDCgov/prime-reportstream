package gov.cdc.prime.router.azure.batch

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BatchEvent
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CovidBatchFunctionTests {
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
                Topic.TEST,
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
        val message = BatchEvent(Event.EventAction.BATCH, "phd.elr", true)

        // Invoke batch function run
        CovidBatchFunction(engine).run(message.toQueueMessage(), context = null)

        // empty pathway should be called
        verify(exactly = 1) { engine.generateEmptyReport(any(), any()) }
        // standard batch handling should not be called
        verify(exactly = 0) { engine.handleBatchEvent(any(), any(), any(), any()) }
    }

    /**
     * todo: remove in
     * https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/12614
     */
    @Test
    fun `Old Test running batchFunction as 'empty batch'`() {
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
        val message = BatchEvent(Event.EventAction.BATCH, "phd.elr", true)

        // Invoke batch function run
        CovidBatchFunction(engine).run(message.toQueueMessage(), context = null)

        // empty pathway should be called
        verify(exactly = 1) { engine.generateEmptyReport(any(), any()) }
        // standard batch handling should not be called
        verify(exactly = 0) { engine.handleBatchEvent(any(), any(), any(), any()) }
    }

    @Test
    fun `Test for repeated downloads during batch step`() {
        // Setup
        every { timing1.isValid() } returns true
        every { timing1.maxReportCount } returns 500
        every { timing1.numberPerDay } returns 1440
        every { timing1.operation } returns Receiver.BatchOperation.NONE
        mockkObject(BlobAccess.Companion)
        mockkObject(ActionHistory)
        every { BlobAccess.Companion.downloadBlobAsByteArray(any()) } returns ByteArray(4)
        every { BlobAccess.Companion.deleteBlob(any()) } just runs
        every { BlobAccess.Companion.exists(any()) } returns true
        every { ActionHistory.sanityCheckReports(any(), any(), any()) } just runs
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(UnitTestUtils.simpleMetadata, settings)
        val mockReportFile = mockk<ReportFile>()
        val randomUUID = UUID.randomUUID()
        val bodyURL = "someurl"
        val bodyFormat = "CSV"
        val schemaName = "one"
        val mockBlobInfo = mockk<BlobAccess.BlobInfo>()
        every { mockBlobInfo.blobUrl } returns bodyURL
        every { mockBlobInfo.format } returns Report.Format.CSV
        every { mockBlobInfo.digest } returns ByteArray(4)
        every { blobMock.uploadReport(any(), any(), any(), any()) } returns mockBlobInfo
        every { mockReportFile.reportId } returns randomUUID
        every { mockReportFile.bodyUrl } returns bodyURL
        every { mockReportFile.schemaName } returns schemaName
        every { mockReportFile.bodyFormat } returns bodyFormat
        val mockTask = mockk<Task>()
        every { mockTask.reportId } returns randomUUID
        every { mockTask.bodyUrl } returns bodyURL
        every { mockTask.schemaName } returns schemaName
        every { mockTask.bodyFormat } returns bodyFormat
        every { engine.generateEmptyReport(any(), any()) } returns Unit
        every { engine.db.fetchAndLockBatchTasksForOneReceiver(any(), any(), any(), any(), any()) } returns listOf(
            mockTask
        )
        every { engine.db.fetchReportFile(any(), any(), any()) } returns mockReportFile

        // the message that will be passed to batchFunction
        val message = BatchEvent(Event.EventAction.BATCH, "phd.elr", false)

        // invoke batch function run for legacy pipeline
        CovidBatchFunction(engine).run(message.toQueueMessage(), context = null)

        // verify that we only download blobs once in legacy pipeline
        verify(exactly = 1) { BlobAccess.Companion.downloadBlobAsByteArray(bodyURL, any(), any()) }
    }

    /**
     * todo: remove in
     * https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/12614
     */
    @Test
    fun `Old Test for repeated downloads during batch step`() {
        // Setup
        every { timing1.isValid() } returns true
        every { timing1.maxReportCount } returns 500
        every { timing1.numberPerDay } returns 1440
        every { timing1.operation } returns Receiver.BatchOperation.NONE
        mockkObject(BlobAccess.Companion)
        mockkObject(ActionHistory)
        every { BlobAccess.Companion.downloadBlobAsByteArray(any()) } returns ByteArray(4)
        every { BlobAccess.Companion.deleteBlob(any()) } just runs
        every { BlobAccess.Companion.exists(any()) } returns true
        every { ActionHistory.sanityCheckReports(any(), any(), any()) } just runs
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(UnitTestUtils.simpleMetadata, settings)
        val mockReportFile = mockk<ReportFile>()
        val randomUUID = UUID.randomUUID()
        val bodyURL = "someurl"
        val bodyFormat = "CSV"
        val schemaName = "one"
        val mockBlobInfo = mockk<BlobAccess.BlobInfo>()
        every { mockBlobInfo.blobUrl } returns bodyURL
        every { mockBlobInfo.format } returns Report.Format.CSV
        every { mockBlobInfo.digest } returns ByteArray(4)
        every { blobMock.uploadReport(any(), any(), any(), any()) } returns mockBlobInfo
        every { mockReportFile.reportId } returns randomUUID
        every { mockReportFile.bodyUrl } returns bodyURL
        every { mockReportFile.schemaName } returns schemaName
        every { mockReportFile.bodyFormat } returns bodyFormat
        val mockTask = mockk<Task>()
        every { mockTask.reportId } returns randomUUID
        every { mockTask.bodyUrl } returns bodyURL
        every { mockTask.schemaName } returns schemaName
        every { mockTask.bodyFormat } returns bodyFormat
        every { engine.generateEmptyReport(any(), any()) } returns Unit
        every { engine.db.fetchAndLockBatchTasksForOneReceiver(any(), any(), any(), any(), any()) } returns listOf(
            mockTask
        )
        every { engine.db.fetchReportFile(any(), any(), any()) } returns mockReportFile

        // the message that will be passed to batchFunction
        val message = BatchEvent(Event.EventAction.BATCH, "phd.elr", false).toQueueMessage()

        // invoke batch function run for legacy pipeline
        CovidBatchFunction(engine).run(message, context = null)

        // verify that we only download blobs once in legacy pipeline
        verify(exactly = 1) { BlobAccess.Companion.downloadBlobAsByteArray(bodyURL, any(), any()) }
    }
}