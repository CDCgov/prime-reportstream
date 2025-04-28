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
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.test.BeforeTest
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

    @BeforeTest
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test with no receiver getting empty batch file`() {
        // Setup
        every { queueMock.sendMessageToQueue(any(), any()) } returns Unit
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
        every { timing1.batchInPrevious60Seconds(any()) } returns true

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(settings)

        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
            0
        }
        every { queueMock.sendMessage(any()) }.returns(Unit)

        // Invoke batch decider run
        var result = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)

        // Verify that the reciever will not be put in the batch queue
        assertEquals(0, result.first)
        assertEquals(false, result.second)
    }

    @Test
    fun `Test with receiver getting empty on every batch`() {
        // Setup
        every { queueMock.sendMessageToQueue(any(), any()) } returns Unit
        every { timing1.isValid() } returns true
        every { timing1.batchInPrevious60Seconds(any()) } returns true
        every { timing1.numberPerDay } returns 1440
        every { timing1.maxReportCount } returns 500
        every { timing1.whenEmpty }.answers {
            Receiver.WhenEmpty(
                Receiver.EmptyOperation.SEND,
                false
            )
        }

        val settings = FileSettings().loadOrganizations(oneOrganization, universalPipelineOrg)
        val engine = makeEngine(settings)

        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
            0
        }

        // Invoke batch decider run
        var result1 = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)
        var result2 = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)
        val result3 = BatchDeciderFunction(engine).determineQueueMessageCount(universalPipelineOrg.receivers[0], null)
        BatchDeciderFunction(engine).run("", context = null)
        BatchDeciderFunction(engine).run("", context = null)

        assertEquals(1, result1.first)
        assertEquals(true, result1.second)
        assertEquals(1, result2.first)
        assertEquals(true, result2.second)
        assertEquals(result3, Pair(1, true))
    }

    @Test
    fun `Test with receiver getting empty once per day`() {
        // Setup
        every { queueMock.sendMessageToQueue(any(), any()) } returns Unit
        every { timing1.isValid() } returns true
        every { timing1.batchInPrevious60Seconds(any()) } returns true
        every { timing1.numberPerDay } returns 1440
        every { timing1.maxReportCount } returns 500
        every { timing1.whenEmpty }.answers {
            Receiver.WhenEmpty(
                Receiver.EmptyOperation.SEND,
                true
            )
        }

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(settings)

        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
            0
        }
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers {
            false
        }

        // Invoke batch decider run
        var result1 = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)

        // change response of recentlySent
        every { engine.db.checkRecentlySent(any(), any(), any(), any()) }.answers {
            true
        }

        var result2 = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)

        assertEquals(1, result1.first)
        assertEquals(true, result1.second)
        assertEquals(0, result2.first)
        assertEquals(false, result2.second)
    }

    @Test
    fun `run should enqueue correct number of batch messages with appropriate delays`() {
        // ensure this receiver is considered valid and due to batch
        every { timing1.isValid() } returns true
        // stub the default‐arg overload for batchInPrevious60Seconds()
        every { timing1.batchInPrevious60Seconds(any<OffsetDateTime>()) } returns true

        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 2
        every { timing1.timeBetweenBatches } returns 120
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(settings)

        // 5 records → ceil(5/2) = 3 messages
        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) } returns 5

        // record all three invocations
        every { queueMock.sendMessageToQueue(any(), any(), any()) } returns Unit

        // run the code under test
        BatchDeciderFunction(engine).run("", null)

        // verify three enqueues with delays of 0, 2, and 4 minutes to the COVID queue
        verifyOrder {
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