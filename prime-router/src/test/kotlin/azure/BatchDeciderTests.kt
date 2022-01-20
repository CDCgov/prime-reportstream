package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SettingsProvider
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach

class BatchDeciderTests {
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
                "topic",
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        ),
    )

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build()
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

//    @Test
//    fun `Test with no receiver getting empty batch file`() {
//        // Setup
//        every { queueMock.sendMessage(any()) } returns Unit
//        every { timing1.isValid()} returns true
//        every { timing1.batchInPrevious60Seconds(any())} returns true
//
//        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
//        val metadata = Metadata(schema = one)
//        val settings = FileSettings().loadOrganizations(oneOrganization)
//        val engine = makeEngine(metadata, settings)
//
//        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
//            0
//        }
//        every { queueMock.sendMessage(any()) }.returns(Unit)
//
//        // Invoke batch decider run
//        BatchDeciderFunction(engine).run("", context = null)
//
//        // Verify that QueueAccess.sendMessage was not called
//        verify(exactly = 0) {queueMock.sendMessage(any())}
//
//        confirmVerified(queueMock)
//    }

    // TODO CD: This needs to be built out, but having issues with Mockk
//    @Test
//    fun `Test with receiver getting empty on every batch`() {
//        // Setup
//        every { anyConstructed<QueueAccess>().sendMessage(any()) } returns Unit
//        every { timing1.isValid()} returns true
//        every { timing1.batchInPrevious60Seconds(any())} returns true
//        every { timing1.numberPerDay} returns 1440
//        every { timing1.maxReportCount} returns 500
//        every { timing1.whenEmpty }.answers {
//            Receiver.WhenEmpty(
//                Receiver.EmptyOperation.SEND,
//                false
//            )
//        }
//
//        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
//        val metadata = Metadata(schema = one)
//        val settings = FileSettings().loadOrganizations(oneOrganization)
//        val engine = makeEngine(metadata, settings)
//
//        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
//            0
//        }
//
//        // Invoke batch decider run
//        BatchDeciderFunction(engine).run("", context = null)
//        BatchDeciderFunction(engine).run("", context = null)
//
//        // Verify that QueueAccess.sendMessage was not called
//        verify(exactly = 2) {queueMock.sendMessage(any())}
//
//        confirmVerified(queueMock)
//    }

//    // TODO CD
//    @Test
//    fun `Test with receiver getting empty once per day - has not received`() {
// Setup
//    every { queueMock.sendMessage(any()) } returns Unit
//    every { timing1.isValid()} returns true
//    every { timing1.batchInPrevious60Seconds(any())} returns true
//    every { timing1.numberPerDay} returns 1440
//    every { timing1.maxReportCount} returns 500
//    every { timing1.whenEmpty }.answers {
//        Receiver.WhenEmpty(
//            Receiver.EmptyOperation.SEND,
//            false
//        )
//    }
//
//    val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
//    val metadata = Metadata(schema = one)
//    val settings = FileSettings().loadOrganizations(oneOrganization)
//    val engine = makeEngine(metadata, settings)
//
//    every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
//        0
//    }
//    every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers {
//        false
//    }
//
//    every { queueMock.sendMessage(any()) }.returns(Unit)
//
//    // Invoke batch decider run
//    BatchDeciderFunction(engine).run("", context = null)
//    BatchDeciderFunction(engine).run("", context = null)
//
//    // Verify that QueueAccess.sendMessage was not called
//    verify(exactly = 2) {queueMock.sendMessage(any())}
//
//    confirmVerified(queueMock)
//    }
//
//    // TODO CD
//    @Test
//    fun `Test with receiver getting empty once per day - has received`() {
//        // Setup
//        var nextEvent: ReportEvent? = null
//        setupLogger()
//        mockkConstructor(ActionHistory::class)
//        every { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) } returns Unit
//        val reportId = UUID.randomUUID()
//        every { workflowEngine.handleReportEvent(any(), context, any()) }.answers {
//            val block = thirdArg() as
//                (header: WorkflowEngine.Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent
//            val header = makeHeader()
//            // Should be high enough retry count that the next action should have an error
//            nextEvent = block(
//                header, RetryToken(100, RetryToken.allItems), null
//            )
//        }
//        setupWorkflow()
//        every { sftpTransport.send(any(), any(), any(), any(), any(), any()) }.returns(RetryToken.allItems)
//        every { workflowEngine.recordAction(any()) }.returns(Unit)
//
//        // Invoke
//        val event = ReportEvent(Event.EventAction.SEND, reportId, false)
//        SendFunction(workflowEngine).run(event.toQueueMessage(), context)
//
//        // Verify
//        assertThat(nextEvent).isNotNull()
//        assertThat(nextEvent!!.eventAction).isEqualTo(Event.EventAction.SEND_ERROR)
//        assertThat(nextEvent!!.retryToken).isNull()
//        verify { anyConstructed<ActionHistory>().setActionType(TaskAction.send_error) }
//    }
}