package gov.cdc.prime.router.azure

import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
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
import io.mockk.mockkObject
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
    val oneOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(Receiver("elr", "phd", "topic", "one"))
    )

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return WorkflowEngine(
            metadata,
            settings,
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
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.EventAction.NONE, UUID.randomUUID())
        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"
        val actionHistory = mockk<ActionHistory>()
        val receiver = Receiver("myRcvr", "topic", "mytopic", "mySchema")

        every { blobMock.uploadBody(report = eq(report1), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, "".toByteArray()))
        every { accessSpy.insertTask(report = eq(report1), bodyFormat.toString(), bodyUrl, eq(event)) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.dispatchReport(event, report1, actionHistory, receiver, context = null)

        verify(exactly = 1) {
            accessSpy.insertTask(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            actionHistory.trackCreatedReport(any(), any(), any(), any())
            blobMock.uploadBody(report = any(), any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test dispatchReport with Error`() {

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val event = ReportEvent(Event.EventAction.NONE, report1.id)
        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"
        val actionHistory = mockk<ActionHistory>()
        val receiver = Receiver("MyRcvr", "topic", "mytopic", "mySchema")

        every { blobMock.uploadBody(report = eq(report1), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, "".toByteArray()))
        every { accessSpy.insertTask(report = eq(report1), bodyFormat.toString(), bodyUrl, eq(event)) }.returns(Unit)

// todo clean up this test      every { queueMock.sendMessage(eq(event)) }.answers { throw Exception("problem") }
        every { blobMock.deleteBlob(eq(bodyUrl)) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.dispatchReport(event, report1, actionHistory, receiver, context = null)

        verify(exactly = 1) {
            accessSpy.insertTask(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            blobMock.uploadBody(report = any(), any(), any())
            actionHistory.trackCreatedReport(any(), any(), any(), any())
// todo           queueMock.sendMessage(event = any())
// todo           blobMock.deleteBlob(blobUrl = any())
        }
        confirmVerified(accessSpy, blobMock, queueMock) // todo
    }

    @Test
    fun `test receiveReport`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings()
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val actionHistory = mockk<ActionHistory>()
        val sender = Sender("senderName", "org", Sender.Format.CSV, "covid-19", one.name)

        every { blobMock.uploadBody(Report.Format.CSV, any(), any(), sender.fullName, Event.EventAction.RECEIVE) }
            .returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))
        every { actionHistory.trackExternalInputReport(any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.recordReceivedReport(report1, "body".toByteArray(), sender, actionHistory, engine)

        verify(exactly = 1) {
            actionHistory.trackExternalInputReport(any(), any())
            blobMock.uploadBody(any(), any(), any(), any(), any())
        }
        confirmVerified(blobMock)
    }

    @Test
    fun `test handleReportEvent`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(
            one, listOf(listOf("1", "2"), listOf("3", "4")),
            source = TestSource, destination = oneOrganization.receivers[0]
        )
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"
        val event = ReportEvent(Event.EventAction.SEND, report1.id)
        val nextAction = ReportEvent(Event.EventAction.NONE, report1.id)
        val task = DatabaseAccess.createTask(report1, bodyFormat, bodyUrl, event)
        val actionHistoryMock = mockk<ActionHistory>()
        mockkObject(ActionHistory.Companion)
        val engine = makeEngine(metadata, settings)

        every { accessSpy.fetchAndLockTask(reportId = eq(report1.id), any()) }.returns(task)
        every { accessSpy.fetchReportFile(eq(report1.id), null, any()) }.returns(
            ReportFile().setReportId(report1.id).setItemCount(0)
        )
        every { accessSpy.fetchItemLineagesForReport(eq(report1.id), any(), any()) }.returns(emptyList())
        every {
            accessSpy.updateTask(
                reportId = eq(report1.id),
                eq(event.eventAction.toTaskAction()),
                any(),
                any(),
                any(),
                any()
            )
        }.returns(Unit)
        every { queueMock.sendMessage(eq(nextAction)) }
            .returns(Unit)
        every { actionHistoryMock.saveToDb(any()) }.returns(Unit)
        every { actionHistoryMock.trackActionResult(any() as String) }.returns(Unit)
        every { ActionHistory.Companion.sanityCheckReport(any(), any(), any()) }.returns(Unit)

        engine.handleReportEvent(event, null) { header, _, _ ->
            assertEquals(task, header.task)
            nextAction
        }

        verify(exactly = 1) {
            queueMock.sendMessage(event = any())
            accessSpy.transact(block = any())
            accessSpy.updateTask(reportId = any(), any(), any(), any(), any(), any())
            accessSpy.fetchAndLockTask(reportId = any(), any())
            accessSpy.fetchItemLineagesForReport(reportId = any(), any(), any())
            accessSpy.fetchReportFile(reportId = any(), any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }
}