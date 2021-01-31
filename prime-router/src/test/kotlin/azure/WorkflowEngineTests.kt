package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkflowEngineTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)

    fun makeEngine(metadata: Metadata): WorkflowEngine {
        return WorkflowEngine(
            metadata,
            csvSerializer = CsvSerializer(metadata),
            hl7Serializer = Hl7Serializer(metadata),
            redoxSerializer = RedoxSerializer(metadata),
            db = accessSpy,
            blob = blobMock,
            queue = queueMock
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test dispatchReport`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.EventAction.NONE, UUID.randomUUID())
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"

        every { blobMock.uploadBody(report = eq(report1)) }.returns(Pair(bodyFormat, bodyUrl))
        every { accessSpy.insertHeader(report = eq(report1), bodyFormat, bodyUrl, eq(event)) }.returns(Unit)
//        every { queueMock.sendMessage(eq(event)) }.returns(Unit)

        val engine = makeEngine(metadata)
        engine.dispatchReport(event, report1)

        verify(exactly = 1) {
            accessSpy.insertHeader(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            blobMock.uploadBody(report = any())
//            queueMock.sendMessage(event = any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test dispatchReport with Error`() {

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.EventAction.NONE, report1.id)
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"

        every { blobMock.uploadBody(report = eq(report1)) }.returns(Pair(bodyFormat, bodyUrl))
        every { accessSpy.insertHeader(report = eq(report1), bodyFormat, bodyUrl, eq(event)) }.returns(Unit)
// todo clean up this test      every { queueMock.sendMessage(eq(event)) }.answers { throw Exception("problem") }
        every { blobMock.deleteBlob(eq(bodyUrl)) }.returns(Unit)

        val engine = makeEngine(metadata)
        engine.dispatchReport(event, report1)

        verify(exactly = 1) {
            accessSpy.insertHeader(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            blobMock.uploadBody(report = any())
// todo           queueMock.sendMessage(event = any())
// todo           blobMock.deleteBlob(blobUrl = any())
        }
        confirmVerified(accessSpy, blobMock, queueMock) // todo
    }

    @Test
    fun `test receiveReport`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.EventAction.RECEIVE, UUID.randomUUID())
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"

        every { blobMock.uploadBody(report = eq(report1)) }.returns(Pair(bodyFormat, bodyUrl))
        every { accessSpy.insertHeader(report = eq(report1), bodyFormat, bodyUrl, eq(event)) }.returns(Unit)
        every { queueMock.sendMessage(eq(event)) }.returns(Unit)

        val engine = makeEngine(metadata)
        engine.receiveReport(report1)

        verify(exactly = 1) {
            accessSpy.insertHeader(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            blobMock.uploadBody(report = any())
        }
        verify(exactly = 0) {
            queueMock.sendMessage(event = any())
        }
        confirmVerified(blobMock, accessSpy, queueMock)
    }

    @Test
    fun `test handleReportEvent`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"
        val event = ReportEvent(Event.EventAction.SEND, report1.id)
        val nextAction = ReportEvent(Event.EventAction.NONE, report1.id)
        val task = DatabaseAccess.createTask(report1, bodyFormat, bodyUrl, event)
        val actionHistoryMock = mockk<ActionHistory>()
        val engine = makeEngine(metadata)

        every { accessSpy.fetchAndLockHeader(reportId = eq(report1.id), any()) }
            .returns(DatabaseAccess.Header(task, emptyList(), ReportFile(), engine))
        every {
            accessSpy.updateHeader(
                reportId = eq(report1.id),
                eq(event.eventAction),
                eq(nextAction.eventAction),
                any(),
                any(),
                any()
            )
        }.returns(Unit)
        every { queueMock.sendMessage(eq(nextAction)) }
            .returns(Unit)
        every { actionHistoryMock.saveToDb(any()) }.returns(Unit)
        every { actionHistoryMock.trackActionResult(any() as String) }.returns(Unit)

        engine.handleReportEvent(event, actionHistoryMock) { header, _, _ ->
            assertEquals(task, header.task)
            assertEquals(0, header.sources.size)
            nextAction
        }

        verify(exactly = 1) {
            queueMock.sendMessage(event = any())
            accessSpy.transact(block = any())
            accessSpy.updateHeader(reportId = any(), any(), any(), any(), any(), any())
            accessSpy.fetchAndLockHeader(reportId = any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }
}