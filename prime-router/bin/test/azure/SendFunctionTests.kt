package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.SftpTransport
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.verify
import org.jooq.Configuration
import org.junit.jupiter.api.BeforeEach
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test

class SendFunctionTests {
    val context = mockkClass(ExecutionContext::class)
    val metadata = UnitTestUtils.simpleMetadata
    val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    val logger = mockkClass(Logger::class)
    val workflowEngine = mockkClass(WorkflowEngine::class)
    val sftpTransport = mockkClass(SftpTransport::class)
    val reportId = UUID.randomUUID()
    val task = Task(
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
        null, null, null, null, null, null, null, null, 0, null, null, null, null
    )

    fun setupLogger() {
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { logger.warning(any<String>()) }.returns(Unit)
        every { logger.info(any<String>()) }.returns(Unit)
    }

    fun setupWorkflow() {
        every { workflowEngine.metadata }.returns(metadata)
        every { workflowEngine.settings }.returns(settings)
        every { workflowEngine.readBody(any()) }.returns("body".toByteArray())
        every { workflowEngine.sftpTransport }.returns(sftpTransport)
    }

    fun makeHeader(): WorkflowEngine.Header {
        return WorkflowEngine.Header(
            task, reportFile,
            null,
            settings.findOrganization("ignore"),
            settings.findReceiver("ignore.CSV"),
            metadata.findSchema("covid-19"), "hello".toByteArray(),
            true
        )
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
        setupWorkflow()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeHeader()
            nextEvent = block(header, null, null)
        }
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)
        // Verify
        assertThat(nextEvent).isNotNull()
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.NONE)
        assertThat(nextEvent!!.retryToken).isNull()
    }

    @Test
    fun `Test with sftp error`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) } returns Unit
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeHeader()
            nextEvent = block(header, null, null)
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent).isNotNull()
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND)
        assertThat(nextEvent!!.retryToken).isNotNull()
        assertThat(nextEvent!!.retryToken?.retryCount).isEqualTo(1)
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) }
    }

    @Test
    fun `Test with third sftp error`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) } returns Unit
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent

            val header = makeHeader()
            nextEvent = block(
                header, RetryToken(2, RetryToken.allItems), null
            )
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent).isNotNull()
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND)
        assertThat(nextEvent!!.retryToken).isNotNull()
        assertThat(nextEvent!!.retryToken?.retryCount).isEqualTo(3)
        assertThat(nextEvent!!.at!!.isAfter(OffsetDateTime.now().plusMinutes(2))).isTrue()
        nextEvent!!.retryToken?.toJSON()?.let { assertThat(it.contains("\"retryCount\":3")).isTrue() }
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) }
    }

    @Test
    fun `Test with 100th sftp error`() {
        // Setup
        var nextEvent: ReportEvent? = null
        setupLogger()
        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) } returns Unit
        val reportId = UUID.randomUUID()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeHeader()
            // Should be high enough retry count that the next action should have an error
            nextEvent = block(
                header, RetryToken(100, RetryToken.allItems), null
            )
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent).isNotNull()
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND_ERROR)
        assertThat(nextEvent!!.retryToken).isNull()
        verify { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) }
    }

    // TODO CD: Should this test even work?  it looks like we changed to 'error' instead of 'logger.severe'
//    @Test
//    fun `Test with a bad message`() {
//        // Setup
//        setupLogger()
//        every { workflowEngine.recordAction(any()) }.returns(Unit)
//
//        // Invoke
//        SendFunction(workflowEngine).run("", context)
//        // Verify
//        verify(atLeast = 1) { logger.log(Level.SEVERE, any(), any<Throwable>()) }
//    }
}