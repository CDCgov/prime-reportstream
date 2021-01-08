package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.azure.db.tables.pojos.TaskSource
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.RetryTransport
import gov.cdc.prime.router.transport.SftpTransport
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import org.jooq.Configuration
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
    val context = mockkClass(ExecutionContext::class)
    val metadata = Metadata("./metadata", "-local")
    val logger = mockkClass(Logger::class)
    val workflowEngine = mockkClass(WorkflowEngine::class)
    val sftpTransport = mockkClass(SftpTransport::class)
    val reportId = UUID.randomUUID()
    val task = Task(
        reportId,
        TaskAction.send,
        null,
        null,
        "az-phd.elr-test",
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

    fun setupLogger() {
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { logger.info(any<String>()) }.returns(Unit)
    }

    fun setupWorkflow() {
        every { workflowEngine.metadata }.returns(metadata)
        every { workflowEngine.readBody(any()) }.returns("body".toByteArray())
        every { workflowEngine.sftpTransport }.returns(sftpTransport)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test with message`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as (header: DatabaseAccess.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = DatabaseAccess.Header(task, emptyList<TaskSource>())
            nextEvent = block(header, null, null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(null)

        // Invoke
        val event = ReportEvent(Event.Action.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)
        // Verify
        assertNotNull(nextEvent)
        assertEquals(Event.Action.NONE, nextEvent!!.action)
        assertNull(nextEvent!!.retryToken)
    }

    @Test
    fun `Test with sftp error`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as (header: DatabaseAccess.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = DatabaseAccess.Header(task, emptyList<TaskSource>())
            nextEvent = block(header, null, null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)

        // Invoke
        val event = ReportEvent(Event.Action.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertNotNull(nextEvent)
        assertEquals(Event.Action.SEND, nextEvent!!.action)
        assertNotNull(nextEvent!!.retryToken)
        assertEquals(1, nextEvent!!.retryToken?.retryCount)
    }

    @Test
    fun `Test with third sftp error`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as (header: DatabaseAccess.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val task = Task(reportId, TaskAction.send, null, null, "az-phd.elr-test", 0, "", "", null, null, null, null, null, null, null)
            val header = DatabaseAccess.Header(task, emptyList<TaskSource>())
            nextEvent = block(header, RetryToken(2, listOf(RetryTransport(0, RetryToken.allItems))), null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)

        // Invoke
        val event = ReportEvent(Event.Action.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertNotNull(nextEvent)
        assertEquals(Event.Action.SEND, nextEvent!!.action)
        assertNotNull(nextEvent!!.retryToken)
        assertEquals(3, nextEvent!!.retryToken?.retryCount)
        assertTrue(nextEvent!!.at!!.isAfter(OffsetDateTime.now().plusMinutes(2)))
        nextEvent!!.retryToken?.toJSON()?.let { assertTrue(it.contains("\"retryCount\":3")) }
    }

    @Test
    fun `Test with 100th sftp error`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        val reportId = UUID.randomUUID()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block =
                secondArg() as (header: DatabaseAccess.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent

            val header = DatabaseAccess.Header(task, emptyList<TaskSource>())
            // Should be high enough retry count that the next action should have an error
            nextEvent = block(header, RetryToken(100, listOf(RetryTransport(0, RetryToken.allItems))), null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)

        // Invoke
        val event = ReportEvent(Event.Action.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertNotNull(nextEvent)
        assertEquals(Event.Action.SEND_ERROR, nextEvent!!.action)
        assertNull(nextEvent!!.retryToken)
    }

    @Test
    fun `Test with a bad message`() {
        // Setup
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        // Invoke
        SendFunction(workflowEngine).run("", context)
        // Verify
        verify(atLeast = 1) { logger.log(Level.SEVERE, any(), any<Throwable>()) }
    }
}