package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
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

    private fun makeEngine(settings: SettingsProvider): WorkflowEngine {
        return WorkflowEngine.Builder().metadata(UnitTestUtils.simpleMetadata).settingsProvider(settings)
            .databaseAccess(accessSpy).blobAccess(blobMock).queueAccess(queueMock).build()
    }

    @BeforeTest
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test with no receiver getting empty batch file`() {
        // Setup
        every { queueMock.sendMessage(any()) } returns Unit
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
        every { queueMock.sendMessage(any()) } returns Unit
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

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val engine = makeEngine(settings)

        every { engine.db.fetchNumReportsNeedingBatch(any(), any(), any()) }.answers {
            0
        }

        // Invoke batch decider run
        var result1 = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)
        var result2 = BatchDeciderFunction(engine).determineQueueMessageCount(oneOrganization.receivers[0], null)
        BatchDeciderFunction(engine).run("", context = null)
        BatchDeciderFunction(engine).run("", context = null)

        assertEquals(1, result1.first)
        assertEquals(true, result1.second)
        assertEquals(1, result2.first)
        assertEquals(true, result2.second)
    }

    @Test
    fun `Test with receiver getting empty once per day`() {
        // Setup
        every { queueMock.sendMessage(any()) } returns Unit
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
}