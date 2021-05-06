package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.SftpTransport
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import org.jooq.Configuration
import org.jooq.JSON
import org.junit.jupiter.api.BeforeEach
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SendFunctionTests {
    private val context = mockkClass(ExecutionContext::class)
    private val metadata = Metadata(Metadata.defaultMetadataDirectory)
    private val settings = FileSettings(FileSettings.defaultSettingsDirectory, "-local")
    private val logger = mockkClass(Logger::class)
    private val workflowEngine = mockkClass(WorkflowEngine::class)
    private val sftpTransport = mockkClass(SftpTransport::class)
    private val reportId = UUID.randomUUID()
    private val task = Task(
        reportId,
        TaskAction.send,
        null,
        null,
        "ignore.CSV",
        0,
        "",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )
    private val reportFile = ReportFile(
        reportId,
        null,
        TaskAction.send,
        null,
        null,
        null,
        "ignore",
        "CSV",
        null, null, null, null, null, null, null, null, 0, null, null, null
    )

    private fun setupLogger() {
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { logger.warning(any<String>()) }.returns(Unit)
        every { logger.info(any<String>()) }.returns(Unit)
    }

    private fun setupWorkflow() {
        every { workflowEngine.metadata }.returns(metadata)
        every { workflowEngine.settings }.returns(settings)
        every { workflowEngine.readBody(any()) }.returns("body".toByteArray())
        every { workflowEngine.sftpTransport }.returns(sftpTransport)
    }

    private fun makeHeader(retryToken: String? = null): WorkflowEngine.Header {
        val newTask = Task(task)
        newTask.retryToken = JSON.valueOf(retryToken)
        return WorkflowEngine.Header(
            newTask,
            reportFile,
            null,
            settings.findOrganization("ignore"),
            settings.findReceiver("ignore.CSV"),
            metadata.findSchema("covid-19"), "hello".toByteArray()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test with message`() {
        // Setup
        var receiverResult: WorkflowEngine.ReceiverResult? = null
        setupLogger()
        setupWorkflow()
        every { workflowEngine.handleReceiverEvent(any(), any(), any(), any()) }.answers {
            val block = lastArg() as
                (receiver: Receiver, headers: List<WorkflowEngine.Header>, txn: Configuration?) ->
                WorkflowEngine.ReceiverResult
            val header = makeHeader()
            val receiver = header.receiver ?: error("missing receiver")
            receiverResult = block(receiver, listOf(header), null)
        }
        every { sftpTransport.send(any(), any(), any(), any(), any()) }.returns(null)
        every { sftpTransport.startSession(any()) }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        // Invoke
        val event = ReceiverEvent(Event.EventAction.SEND, "az-phd.elr-test")
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)
        // Verify
        assertNotNull(receiverResult)
        assertEquals(1, receiverResult!!.retryTokens.size)
        assertNull(receiverResult!!.retryTokens[0])
    }

    @Test
    fun `Test with sftp error`() {
        // Setup
        var receiverResult: WorkflowEngine.ReceiverResult? = null
        setupLogger()
        every { workflowEngine.handleReceiverEvent(any(), any(), any(), any()) }.answers {
            val block = lastArg() as
                (receiver: Receiver, headers: List<WorkflowEngine.Header>, txn: Configuration?) ->
                WorkflowEngine.ReceiverResult
            val header = makeHeader()
            val receiver = header.receiver ?: error("missing receiver")
            receiverResult = block(receiver, listOf(header), null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { sftpTransport.startSession(any()) }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReceiverEvent(Event.EventAction.SEND, "az-phd.elr-test")
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertNotNull(receiverResult)
        assertEquals(1, receiverResult!!.retryTokens.size)
        assertEquals(1, receiverResult!!.retryTokens[0]?.retryCount)
        assertEquals(receiverResult!!.retryAction, Event.EventAction.SEND)
    }

    @Test
    fun `Test with third sftp error`() {
        // Setup
        var receiverResult: WorkflowEngine.ReceiverResult? = null
        setupLogger()
        every { workflowEngine.handleReceiverEvent(any(), any(), any(), any()) }.answers {
            val block = lastArg() as
                (receiver: Receiver, headers: List<WorkflowEngine.Header>, txn: Configuration?) ->
                WorkflowEngine.ReceiverResult
            val header = makeHeader(retryToken = RetryToken(2, listOf("*")).toJSON())
            val receiver = header.receiver ?: error("missing receiver")
            receiverResult = block(receiver, listOf(header), null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { sftpTransport.startSession(any()) }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReceiverEvent(Event.EventAction.SEND, "az-phd.elr-test")
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertNotNull(receiverResult)
        assertNotNull(receiverResult!!.retryTokens[0])
        assertEquals(3, receiverResult!!.retryTokens[0]?.retryCount)
        assertTrue(receiverResult!!.retryActionAt!!.isAfter(OffsetDateTime.now().plusMinutes(2)))
        receiverResult!!.retryTokens[0]?.toJSON()?.let { assertTrue(it.contains("\"retryCount\":3")) }
    }

    @Test
    fun `Test with 100th sftp error`() {
        // Setup
        var receiverResult: WorkflowEngine.ReceiverResult? = null
        setupLogger()
        every { workflowEngine.handleReceiverEvent(any(), any(), any(), any()) }.answers {
            val block = lastArg() as
                (receiver: Receiver, headers: List<WorkflowEngine.Header>, txn: Configuration?) ->
                WorkflowEngine.ReceiverResult
            val header = makeHeader(retryToken = RetryToken(99, listOf("*")).toJSON())
            val receiver = header.receiver ?: error("missing receiver")
            receiverResult = block(receiver, listOf(header), null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { sftpTransport.startSession(any()) }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReceiverEvent(Event.EventAction.SEND, "az-phd.elr-test")
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertNotNull(receiverResult)
        assertNotNull(receiverResult!!.retryTokens[0])
        assertEquals(100, receiverResult!!.retryTokens[0]?.retryCount)
        assertTrue(receiverResult!!.retryActionAt!!.isAfter(OffsetDateTime.now().plusMinutes(2)))
        assertEquals(Event.EventAction.SEND_ERROR, receiverResult!!.retryAction)
    }

    @Test
    fun `Test with a bad message`() {
        // Setup
        setupLogger()
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        SendFunction(workflowEngine).run("", context)
        // Verify
        verify(atLeast = 1) { logger.log(Level.SEVERE, any(), any<Throwable>()) }
    }
}