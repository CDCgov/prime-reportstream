package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WorkflowEngineTests {

    @Test
    fun `test dispatchReport`() {
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider)
        val accessSpy = spyk(DatabaseAccess(connection))
        val blobMock = mockkClass(BlobAccess::class)
        val queueMock = mockkClass(QueueAccess::class)

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.Action.NONE, UUID.randomUUID())
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"

        every { blobMock.uploadBody(report = eq(report1)) }.returns(Pair(bodyFormat, bodyUrl))
        every { accessSpy.insertHeader(report = eq(report1), bodyFormat, bodyUrl, eq(event)) }.returns(Unit)
        every { queueMock.sendMessage(eq(event)) }.returns(Unit)

        val engine = WorkflowEngine(metadata, db = accessSpy, blob = blobMock, queue = queueMock)
        engine.dispatchReport(event, report1)

        verify(exactly = 1) {
            accessSpy.insertHeader(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            blobMock.uploadBody(report = any())
            queueMock.sendMessage(event = any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test dispatchReport with Error`() {
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider)
        val accessSpy = spyk(DatabaseAccess(connection))
        val blobMock = mockkClass(BlobAccess::class)
        val queueMock = mockkClass(QueueAccess::class)

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.Action.NONE, report1.id)
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"

        every { blobMock.uploadBody(report = eq(report1)) }.returns(Pair(bodyFormat, bodyUrl))
        every { accessSpy.insertHeader(report = eq(report1), bodyFormat, bodyUrl, eq(event)) }.returns(Unit)
        every { queueMock.sendMessage(eq(event)) }.answers { throw Exception("problem") }
        every { blobMock.deleteBlob(eq(bodyUrl)) }.returns(Unit)

        val engine = WorkflowEngine(metadata, db = accessSpy, blob = blobMock, queue = queueMock)
        assertFails {
            engine.dispatchReport(event, report1)
        }

        verify(exactly = 1) {
            accessSpy.insertHeader(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            blobMock.uploadBody(report = any())
            queueMock.sendMessage(event = any())
            blobMock.deleteBlob(blobUrl = any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test handleReportEvent`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"
        val event = ReportEvent(Event.Action.SEND, report1.id)
        val nextAction = ReportEvent(Event.Action.NONE, report1.id)
        val task = DatabaseAccess.createTask(report1, bodyFormat, bodyUrl, event)

        // The data provider is jooq mock that allows dev to mock the DB
        val provider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(provider)
        val accessSpy = spyk(DatabaseAccess(connection))
        val blobMock = mockkClass(BlobAccess::class)
        val queueMock = mockkClass(QueueAccess::class)

        every { accessSpy.fetchAndLockHeader(reportId = eq(report1.id), any()) }
            .returns(DatabaseAccess.Header(task, emptyList()))
        every {
            accessSpy.updateHeader(
                reportId = eq(report1.id),
                eq(event.action),
                eq(nextAction.action),
                any(),
                any()
            )
        }.returns(Unit)
        every { queueMock.sendMessage(eq(nextAction)) }
            .returns(Unit)

        val engine = WorkflowEngine(metadata, db = accessSpy, blob = blobMock, queue = queueMock)
        engine.handleReportEvent(event) { header, _ ->
            assertEquals(task, header.task)
            assertEquals(0, header.sources.size)
            nextAction
        }

        verify(exactly = 1) {
            queueMock.sendMessage(event = any())
            accessSpy.transact(block = any())
            accessSpy.updateHeader(reportId = any(), any(), any(), any(), any())
            accessSpy.fetchAndLockHeader(reportId = any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }
}