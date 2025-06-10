package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.transport.NullTransport
import gov.cdc.prime.router.transport.RESTTransport
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.SftpTransport
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.jooq.Configuration
import org.junit.jupiter.api.AfterEach
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
    val restTransport = mockkClass(RESTTransport::class)
    val nullTransport = mockkClass(NullTransport::class)
    val reportId = UUID.randomUUID()
    val maxRetryCount = retryDurationInMin.size
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
        null,
        null,
        null,
        null
    )
    val restTask = Task(
        reportId,
        TaskAction.send,
        null,
        null,
        "ignore.REST_TEST",
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
        "test",
        "test-sender",
        "test",
        "test-receiver",
        null, null, "one", Topic.TEST, "", "test.csv", "CSV", null, 0, null, OffsetDateTime.now(), null, null
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
        every { workflowEngine.restTransport }.returns(restTransport)
        every { workflowEngine.nullTransport }.returns(nullTransport)
        every { workflowEngine.azureEventService }.returns(InMemoryAzureEventService())
        every { workflowEngine.reportService }.returns(mockk<ReportService>(relaxed = true))
    }

    fun mockAzureEvents() {
        mockkConstructor(ActionHistory::class)
        mockkConstructor(ReportStreamEventService::class)

        every {
            anyConstructed<ReportStreamEventService>().sendReportEvent(
                any(),
                any<ReportFile>(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Unit
        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) } returns Unit
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        every { workflowEngine.db } returns mockk<DatabaseAccess>(relaxed = true)
    }

    fun makeIgnoreDotCSVHeader(): WorkflowEngine.Header = WorkflowEngine.Header(
        task,
        reportFile,
        null,
        settings.findOrganization("ignore"),
        settings.findReceiver("ignore.CSV"),
        metadata.findSchema("covid-19"),
        "hello".toByteArray(),
        true
    )

    fun makeIgnoreDotHL7NullHeader(): WorkflowEngine.Header = WorkflowEngine.Header(
        task,
        reportFile,
        null,
        settings.findOrganization("ignore"),
        settings.findReceiver("ignore.HL7_NULL"),
        metadata.findSchema("covid-19"),
        "hello".toByteArray(),
        true
    )

    fun makeIgnoreDotRESTHeader(): WorkflowEngine.Header = WorkflowEngine.Header(
        restTask,
        reportFile,
        null,
        settings.findOrganization("ignore"),
        settings.findReceiver("ignore.REST_TEST"),
        metadata.findSchema("covid-19"),
        "hello".toByteArray(),
        true
    )

    @AfterEach
    fun reset() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `Test with message`() {
        var nextEvent: ReportEvent? = null
        val reportList = listOf(reportFile)
        setupLogger()
        setupWorkflow()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotCSVHeader()
            nextEvent = block(header, null, null)
        }
        every {
            sftpTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        every { workflowEngine.azureEventService.trackEvent(any()) }.returns(Unit)
        every { workflowEngine.reportService.getRootReports(any()) } returns reportList
        every { workflowEngine.db } returns mockk<DatabaseAccess>()
        mockkObject(Report.Companion)
        every { Report.formExternalFilename(any(), any(), any(), any(), any(), any(), any()) } returns ""

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent).isNotNull()
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.NONE)
        assertThat(nextEvent!!.retryToken).isNull()
    }

    @Test
    fun `Test with null transport`() {
        var nextEvent: ReportEvent? = null
        val reportList = listOf(reportFile)
        setupLogger()
        setupWorkflow()
        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotHL7NullHeader()
            nextEvent = block(header, null, null)
        }
        every {
            nullTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        every { workflowEngine.azureEventService.trackEvent(any()) }.returns(Unit)
        every { workflowEngine.reportService.getRootReports(any()) } returns reportList
        every { workflowEngine.db } returns mockk<DatabaseAccess>()
        mockkObject(Report.Companion)
        every { Report.formExternalFilename(any(), any(), any(), any(), any(), any(), any()) } returns ""

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        verify { nullTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify { workflowEngine.recordAction(match { it.action.actionName == TaskAction.send }) }
        assertThat(nextEvent).isNotNull()
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.NONE)
        assertThat(nextEvent!!.retryToken).isNull()
    }

    @Test // No Send Errors
    fun `Test handleRetry queues no further action when no items are left to retry and no send error was logged`() {
        // Setup
        val stillRetriesLeft = maxRetryCount - 1
        var nextEvent: ReportEvent? = null
        setupLogger()
        setupWorkflow()

        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotCSVHeader()
            nextEvent = block(header, RetryToken(stillRetriesLeft, RetryToken.allItems), null)
        }
        // transport.send() returns no items left to retry
        every {
            sftpTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }.returns(null)
        every { workflowEngine.recordAction(any()) }.returns(Unit)
        every { workflowEngine.db } returns mockk<DatabaseAccess>()

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.NONE)
        assertThat(nextEvent!!.retryToken).isNull()
    }

    @Test // Keep Retrying
    fun `Test handleRetry logs send attempt failed when send fails but retry limit is not yet reached`() {
        // Setup
        val stillRetriesLeft = maxRetryCount - 1
        var nextEvent: ReportEvent? = null
        setupLogger()
        setupWorkflow()
        mockAzureEvents()

        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotCSVHeader()
            nextEvent = block(header, RetryToken(stillRetriesLeft, RetryToken.allItems), null)
        }
        every { sftpTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            .returns(RetryToken.allItems)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        verify(exactly = 1) {
            anyConstructed<ActionHistory>().trackItemSendState(
                ReportStreamEventName.ITEM_SEND_ATTEMPT_FAIL, any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND)
        assertThat(nextEvent!!.retryToken?.retryCount).isEqualTo(stillRetriesLeft + 1)
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_warning) }
        assertThat(nextEvent!!.at!!.isAfter(OffsetDateTime.now())).isTrue()
        nextEvent!!.retryToken?.toJSON()?.let {
            assertThat(it.contains("\"retryCount\":${stillRetriesLeft + 1}")).isTrue()
        }
    }

    @Test // Last Mile Failure B
    fun `Test handleRetry logs send failure (last mile failure) when retry limit is met`() {
        // Setup
        val retryLimitMet = maxRetryCount
        var nextEvent: ReportEvent? = null
        setupLogger()
        setupWorkflow()
        mockAzureEvents()

        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotCSVHeader()
            nextEvent = block(header, RetryToken(retryLimitMet, RetryToken.allItems), null)
        }
        every { sftpTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            .returns(RetryToken.allItems)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        verify(exactly = 1) {
            anyConstructed<ActionHistory>().trackItemSendState(
                ReportStreamEventName.ITEM_LAST_MILE_FAILURE, any(), any(), any(), any(), any(), any(), any(), any()
            )
            anyConstructed<ReportStreamEventService>().sendReportEvent(
                ReportStreamEventName.REPORT_LAST_MILE_FAILURE, any<ReportFile>(), any(), any(), any(), any()
            )
        }
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND_ERROR)
        assertThat(nextEvent!!.retryToken).isNull()
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) }
    }

    @Test // Last Mile Failure B
    fun `Test handleRetry logs send failure (last mile failure) when retry limit is exceeded`() {
        // Setup
        val noRetriesLeft = maxRetryCount + 1
        var nextEvent: ReportEvent? = null
        setupLogger()
        setupWorkflow()
        mockAzureEvents()

        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotCSVHeader()
            nextEvent = block(header, RetryToken(noRetriesLeft, RetryToken.allItems), null)
        }
        every { sftpTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            .returns(RetryToken.allItems)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        verify(exactly = 1) {
            anyConstructed<ActionHistory>().trackItemSendState(
                ReportStreamEventName.ITEM_LAST_MILE_FAILURE, any(), any(), any(), any(), any(), any(), any(), any()
            )
            anyConstructed<ReportStreamEventService>().sendReportEvent(
                ReportStreamEventName.REPORT_LAST_MILE_FAILURE, any<ReportFile>(), any(), any(), any(), any()
            )
        }
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND_ERROR)
        assertThat(nextEvent!!.retryToken).isNull()
        verify(exactly = 1) { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) }
    }

    @Test // Last Mile Failure A
    fun `Test handleRetry logs attempt failed and send failure (last mile failure) when transport logs a send_error`() {
        // Setup
        val stillRetriesLeft = maxRetryCount - 1
        var nextEvent: ReportEvent? = null
        setupLogger()
        setupWorkflow()
        mockAzureEvents()

        every { anyConstructed<ActionHistory>().action.actionName } returns TaskAction.send_error
        every { anyConstructed<ActionHistory>().action.actionResult } returns Unit.toString()

        every { workflowEngine.handleReportEvent(any(), any()) }.answers {
            val block = secondArg() as
                    (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
            val header = makeIgnoreDotRESTHeader()
            nextEvent = block(header, RetryToken(stillRetriesLeft, RetryToken.allItems), null)
        }
        // In this scenario transport.send() is setting the send_error
        every { restTransport.send(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            .returns(null)

        // Invoke
        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
        SendFunction(workflowEngine).run(event.toQueueMessage(), context)

        // Verify
        verify(exactly = 1) {
            anyConstructed<ActionHistory>().trackItemSendState(
                ReportStreamEventName.ITEM_SEND_ATTEMPT_FAIL, any(), any(), any(), any(), any(), any(), any(), any()
            )
            anyConstructed<ActionHistory>().trackItemSendState(
                ReportStreamEventName.ITEM_LAST_MILE_FAILURE, any(), any(), any(), any(), any(), any(), any(), any()
            )
            anyConstructed<ReportStreamEventService>().sendReportEvent(
                ReportStreamEventName.REPORT_LAST_MILE_FAILURE, any<ReportFile>(), any(), any(), any(), any()
            )
        }
        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND_ERROR)
        assertThat(nextEvent!!.retryToken).isNull()
    }
}