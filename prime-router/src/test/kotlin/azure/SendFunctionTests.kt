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
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
        var nextEvent1: ReportEvent? = null
        setupLogger()
        setupWorkflow()
        every { workflowEngine.handleReportEvent(any(), context, any()) }.answers {
            val block = thirdArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeHeader()
            nextEvent1 = block(header, null, null)
            nextEvent1
        }
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)
        // Verify
        assertThat(nextEvent1).isNotNull()
        assertThat(nextEvent1!!.eventAction).isEqualTo(Event.EventAction.NONE)
        assertThat(nextEvent1!!.retryToken).isNull()
    }

    @Test
    fun `Test with sftp error`() {
        // Setup
        var nextEvent2: ReportEvent? = null
        setupLogger()
        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) } returns Unit
        every { workflowEngine.handleReportEvent(any(), context, any()) }.answers {
            val block = thirdArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeHeader()
            nextEvent2 = block(header, null, null)
            nextEvent2
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent2).isNotNull()
        assertThat(nextEvent2!!.eventAction).isEqualTo(Event.EventAction.SEND)
        assertThat(nextEvent2!!.retryToken).isNotNull()
        assertThat(nextEvent2!!.retryToken?.retryCount).isEqualTo(1)
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) }
    }

    @Test
    fun `Test with third sftp error`() {
        // Setup
        var nextEvent3: ReportEvent? = null
        setupLogger()
        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) } returns Unit
        every { workflowEngine.handleReportEvent(any(), context, any()) }.answers {
            val block = thirdArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent

            val header = makeHeader()
            nextEvent3 = block(
                header, RetryToken(2, RetryToken.allItems), null
            )
            nextEvent3
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent3).isNotNull()
        assertThat(nextEvent3!!.eventAction).isEqualTo(Event.EventAction.SEND)
        assertThat(nextEvent3!!.retryToken).isNotNull()
        assertThat(nextEvent3!!.retryToken?.retryCount).isEqualTo(3)
        assertThat(nextEvent3!!.at!!.isAfter(OffsetDateTime.now().plusMinutes(2))).isTrue()
        nextEvent3!!.retryToken?.toJSON()?.let { assertThat(it.contains("\"retryCount\":3")).isTrue() }
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) }
    }

    @Test
    fun `Test with 100th sftp error`() {
        // Setup
        var nextEvent4: ReportEvent? = null
        setupLogger()
        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) } returns Unit
        val reportId = UUID.randomUUID()
        every { workflowEngine.handleReportEvent(any(), context, any()) }.answers {
            val block = thirdArg() as
                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeHeader()
            // Should be high enough retry count that the next action should have an error
            nextEvent4 = block(
                header, RetryToken(100, RetryToken.allItems), null
            )
            nextEvent4
        }
        setupWorkflow()
        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId)

        // Catch the thrown exception - to keep the test from failing
        val er = assertFailsWith<IllegalStateException> {
            SendFunction(workflowEngine).run(event.toQueueMessage(), context)
        }
        assertThat(er.message!!.startsWith("All retries failed."))

        // Verify
        assertThat(nextEvent4).isNotNull()
        assertThat(nextEvent4!!.eventAction).isEqualTo(Event.EventAction.SEND_ERROR)
        assertThat(nextEvent4!!.retryToken).isNull()
        verify { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) }
    }

    @Test
    fun `Test with a bad message`() {
        // Setup
        setupLogger()
        every { workflowEngine.recordAction(any()) }.returns(Unit)

        // Invoke (Catch the thrown exception)
        assertFailsWith<IllegalStateException> { SendFunction(workflowEngine).run("", context) }

        // Verify
        verify(atLeast = 1) { logger.log(Level.SEVERE, any(), any<Throwable>()) }
    }
}