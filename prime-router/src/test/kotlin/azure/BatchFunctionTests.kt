package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.fhirengine.utils.HL7MessageHelpers
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
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
                "covid",
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        ),
    )

    val upOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                "full-elr",
                CustomerStatus.ACTIVE,
                "one",
                timing = timing1
            )
        ),
    )

    val reportId = UUID.randomUUID()
    val batchTask = Task(
        reportId,
        TaskAction.batch,
        null,
        "None",
        "co-phd.elr",
        1,
        "HL7",
        "http://body.url",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )

    val reportFile = ReportFile(
        reportId,
        null,
        TaskAction.batch,
        null,
        null,
        null,
        "co-phd",
        "elr",
        null,
        null,
        "None",
        null,
        "http://body.url",
        null,
        "HL7",
        null,
        1, // pretend we have 4 items to send.
        null, OffsetDateTime.now(),
        null,
        null
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
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
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
    fun `test universal pipeline batch`() {
        mockkObject(BlobAccess)
        mockkObject(HL7MessageHelpers)

        // Setup
        every { timing1.isValid() } returns true
        every { timing1.maxReportCount } returns 1
        every { timing1.numberPerDay } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
        every { timing1.nextTime(any()) } returns OffsetDateTime.now()

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(upOrganization)
        val engine = makeEngine(metadata, settings)
        val taskList = listOf(batchTask)
        val body = """
            A,B,C
            0,1,2
        """.trimIndent()

        // set up calls as needed
        every { engine.generateEmptyReport(any(), any()) } returns Unit
        every { accessSpy.fetchAndLockBatchTasksForOneReceiver(any(), any(), any(), any(), any(),) } returns taskList
        every { accessSpy.fetchReportFile(any(), any(), any()) } returns reportFile
        every { BlobAccess.Companion.downloadBlob(any()) } returns body.toByteArray()
        every { BlobAccess.Companion.exists(any()) } returns true
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.updateTask(any(), any(), any(), any(), any(), any()) } returns Unit
        every { accessSpy.insertTask(any(), any(), any(), any(), any()) } returns Unit
        every { engine.recordAction(any(), any()) } returns Unit
        every { queueMock.sendMessage(any()) } returns Unit

        // the message that will be passed to batchFunction
        val message = "receiver&BATCH&phd.elr&false"
        // Invoke batch function run
        val event = Event.parseQueueMessage(message) as BatchEvent
        val actionHistory = spyk(
            ActionHistory(
                event.eventAction.toTaskAction(),
                event.isEmptyBatch
            )
        )
        BatchFunction(engine).doBatch(message, event, actionHistory)

        // validate the correct functions were called
        verify(exactly = 1) {
            HL7MessageHelpers.takeHL7GetReport(any(), any(), any(), any(), any(), any())
            actionHistory.trackExistingInputReport(any())
            actionHistory.queueMessages(any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
        }
        assert(actionHistory.messages.size == 1)
    }
}