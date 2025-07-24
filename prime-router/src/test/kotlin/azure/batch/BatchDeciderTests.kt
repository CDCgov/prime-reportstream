package gov.cdc.prime.router.azure.batch

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BatchEvent
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.jooq.Configuration
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

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
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        ),
    )

    val universalPipelineOrg = DeepOrganization(
        "upOrg",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "upOrg",
                Topic.FULL_ELR,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        ),
    )

    private fun makeEngine(settings: SettingsProvider): WorkflowEngine =
        WorkflowEngine.Builder()
            .metadata(UnitTestUtils.simpleMetadata)
            .settingsProvider(settings)
            .databaseAccess(accessSpy)
            .blobAccess(blobMock)
            .queueAccess(queueMock)
            .build()

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test receivers with no WhenEmpty action set to will not queue an empty batch`() {
        // Setup
        val doesNotQueueEmptyBatch = Pair(0, false)
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 10
        every { timing1.maxReportCount } returns 10
        // Receiver defaults to NONE action when empty, will not send empty batches
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
        every { timing1.batchInPrevious60Seconds(any()) } returns true
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(settings)
        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers { 0 }

        var result = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)

        // Verify that the reciever will not queue an empty batch
        assertEquals(result, doesNotQueueEmptyBatch)
    }

    @Test
    fun `Test receivers with onlyOncePerDay set to false will queue multiple empty batches per day`() {
        // Setup
        val willQueueEmptyBatch = Pair(1, true)
        every { timing1.isValid() } returns true
        every { timing1.batchInPrevious60Seconds(any()) } returns true
        every { timing1.numberPerDay } returns 1440
        every { timing1.maxReportCount } returns 500
        every { timing1.timeBetweenBatches } returns 10
        val settings = FileSettings().loadOrganizations(oneOrganization, universalPipelineOrg)
        val engine = makeEngine(settings)
        val covidReceiver = oneOrganization.receivers[0]
        val upReceiver = universalPipelineOrg.receivers[0]

        // Receiver is set to SEND when empty, only send once per day is FALSE
        every { timing1.whenEmpty }.answers {
            Receiver.WhenEmpty(
                Receiver.EmptyOperation.SEND,
                false
            )
        }

        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers { 0 }

        // No batches have been sent yet today
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers { false }

        // Check that the receivers will queue first empty batch of the day
        val firstCovidReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(covidReceiver, null)
        val firstUPReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(upReceiver, null)
        assertEquals(firstCovidReceiverResult, willQueueEmptyBatch)
        assertEquals(firstUPReceiverResult, willQueueEmptyBatch)

        // Update mocking to show that a batch has already been sent in last 24 hours
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers { true }

        // Now check that receivers will still queue a second empty batch
        val secondCovidReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(covidReceiver, null)
        val secondUPReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(upReceiver, null)
        assertEquals(secondCovidReceiverResult, willQueueEmptyBatch)
        assertEquals(secondUPReceiverResult, willQueueEmptyBatch)
    }

    @Test
    fun `Test receivers with onlyOncePerDay set to true will only queue one empty batch per day`() {
        // Setup
        val willQueueEmptyBatch = Pair(1, true)
        val doesNotQueueEmptyBatch = Pair(0, false)
        every { timing1.isValid() } returns true
        every { timing1.batchInPrevious60Seconds(any()) } returns true
        every { timing1.numberPerDay } returns 1440
        every { timing1.maxReportCount } returns 500
        val settings = FileSettings().loadOrganizations(oneOrganization, universalPipelineOrg)
        val engine = makeEngine(settings)
        val covidReceiver = oneOrganization.receivers[0]
        val upReceiver = universalPipelineOrg.receivers[0]

        // Receiver is set to SEND when empty, only send once per day is TRUE
        every { timing1.whenEmpty }.answers {
            Receiver.WhenEmpty(
                Receiver.EmptyOperation.SEND,
                true
            )
        }

        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers { 0 }

        // No batches have been sent yet today
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers { false }

        // Check that the receivers will queue first empty batch of the day
        val firstCovidReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(covidReceiver, null)
        val firstUPReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(upReceiver, null)
        assertEquals(firstCovidReceiverResult, willQueueEmptyBatch)
        assertEquals(firstUPReceiverResult, willQueueEmptyBatch)

        // Update mocking to show that a batch has already been sent in last 24 hours
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers { true }

        // Now check that receivers will NOT queue a second empty batch
        val secondCovidReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(covidReceiver, null)
        val secondUPReceiverResult = BatchDeciderFunction(engine).determineQueueMessageCount(upReceiver, null)
        assertEquals(secondCovidReceiverResult, doesNotQueueEmptyBatch)
        assertEquals(secondUPReceiverResult, doesNotQueueEmptyBatch)
    }

    @Test // todo
    fun `run should enqueue correct number of batch messages with appropriate delays`() {
        // ensure this receiver is considered valid and due to batch
        every { timing1.isValid() } returns true
        // stub the default‐arg overload for batchInPrevious60Seconds()
        every { timing1.batchInPrevious60Seconds(any<OffsetDateTime>()) } returns true

        every { timing1.numberPerDay } returns 10
        every { timing1.maxReportCount } returns 2
        every { timing1.timeBetweenBatches } returns 120
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        every { accessSpy.transact(any()) } answers {
            val txn = mockk<Configuration>()
            firstArg<(Configuration?) -> Unit>().invoke(txn)
        }

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(settings)

        // 5 records → ceil(5/2) = 3 messages
        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) } returns 5
        // Testing this out to see if this is where DatabaseAccess.Transact is being called
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers { true }

        // record all three invocations
        every { queueMock.sendMessageToQueue(any(), any(), any()) } returns Unit

        // run the code under test
        BatchDeciderFunction(engine).run("", null)

        // verify three enqueues with delays of 0, 2, and 4 minutes to the COVID queue
        coVerifyOrder {
            queueMock.sendMessageToQueue(
                any<BatchEvent>(), BatchConstants.Queue.COVID_BATCH_QUEUE, Duration.ZERO
            )
            queueMock.sendMessageToQueue(
                any<BatchEvent>(), BatchConstants.Queue.COVID_BATCH_QUEUE, Duration.ofMinutes(2)
            )
            queueMock.sendMessageToQueue(
                any<BatchEvent>(), BatchConstants.Queue.COVID_BATCH_QUEUE, Duration.ofMinutes(4)
            )
        }
        verify(exactly = 3) { queueMock.sendMessageToQueue(any(), any(), any()) }
    }
}