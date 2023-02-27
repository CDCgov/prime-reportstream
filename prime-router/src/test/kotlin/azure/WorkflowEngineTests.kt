package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
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
        "phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(Receiver("elr", "phd", Topic.TEST, CustomerStatus.INACTIVE, "one"))
    )

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build()
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test dispatchReport`() {
        mockkObject(ReportWriter)
        mockkObject(BaseEngine)

        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
        val event = ReportEvent(Event.EventAction.NONE, UUID.randomUUID(), false)
        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"
        val actionHistory = mockk<ActionHistory>()
        val receiver = Receiver("myRcvr", "topic", Topic.TEST, CustomerStatus.INACTIVE, "mySchema")
        val bodyBytes = "".toByteArray()
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata, settings)
        every { BaseEngine.csvSerializerSingleton } returns csvSerializer
        every { BaseEngine.hl7SerializerSingleton } returns hl7Serializer

        every { ReportWriter.getBodyBytes(any(), any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(report = eq(report1), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, bodyBytes))
        every { accessSpy.insertTask(report = eq(report1), bodyFormat.toString(), bodyUrl, eq(event)) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.dispatchReport(event, report1, actionHistory, receiver)

        verify(exactly = 1) {
            accessSpy.insertTask(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            actionHistory.trackCreatedReport(any(), any(), any(), any())
            ReportWriter.getBodyBytes(any(), any(), any(), any())
            blobMock.uploadReport(any(), any(), any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test dispatchReport - empty`() {
        mockkObject(ReportWriter)
        mockkObject(BaseEngine)
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
        val event = ReportEvent(Event.EventAction.NONE, UUID.randomUUID(), false)
        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"
        val actionHistory = mockk<ActionHistory>()
        val receiver = Receiver("myRcvr", "topic", Topic.TEST, CustomerStatus.INACTIVE, "mySchema")
        val bodyBytes = "".toByteArray()
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata, settings)
        every { BaseEngine.csvSerializerSingleton } returns csvSerializer
        every { BaseEngine.hl7SerializerSingleton } returns hl7Serializer
        every { ReportWriter.getBodyBytes(any(), any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(report = eq(report1), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, bodyBytes))
        every { accessSpy.insertTask(report = eq(report1), bodyFormat.toString(), bodyUrl, eq(event)) }.returns(Unit)
        every { actionHistory.trackGeneratedEmptyReport(any(), any(), any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.dispatchReport(event, report1, actionHistory, receiver, isEmptyReport = true)

        verify(exactly = 1) {
            accessSpy.insertTask(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            actionHistory.trackGeneratedEmptyReport(any(), any(), any(), any())
            ReportWriter.getBodyBytes(any(), any(), any(), any())
            blobMock.uploadReport(any(), any(), any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test dispatchReport with Error`() {
        mockkObject(ReportWriter)
        mockkObject(BaseEngine)
        mockkObject(BlobAccess.Companion)

        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource, metadata = metadata)
        val event = ReportEvent(Event.EventAction.NONE, report1.id, false)
        val bodyFormat = Report.Format.CSV
        val bodyUrl = "http://anyblob.com"
        val actionHistory = mockk<ActionHistory>()
        val receiver = Receiver("MyRcvr", "topic", Topic.TEST, CustomerStatus.INACTIVE, "mySchema")
        val bodyBytes = "".toByteArray()
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata, settings)
        every { BaseEngine.csvSerializerSingleton } returns csvSerializer
        every { BaseEngine.hl7SerializerSingleton } returns hl7Serializer
        every { ReportWriter.getBodyBytes(any(), any(), any(), any()) }.returns(bodyBytes)
        every { blobMock.uploadReport(report = eq(report1), any(), any()) }
            .returns(BlobAccess.BlobInfo(bodyFormat, bodyUrl, bodyBytes))
        every { accessSpy.insertTask(report = eq(report1), bodyFormat.toString(), bodyUrl, eq(event)) }.returns(Unit)

// todo clean up this test      every { queueMock.sendMessage(eq(event)) }.answers { throw Exception("problem") }
        every { BlobAccess.Companion.deleteBlob(eq(bodyUrl)) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.dispatchReport(event, report1, actionHistory, receiver)

        verify(exactly = 1) {
            accessSpy.insertTask(
                report = any(),
                bodyFormat = any(),
                bodyUrl = any(),
                nextAction = any()
            )
            ReportWriter.getBodyBytes(any(), any(), any(), any())
            blobMock.uploadReport(any(), any(), any(), any())
            actionHistory.trackCreatedReport(any(), any(), any(), any())
// todo           queueMock.sendMessage(event = any())
// todo           blobMock.deleteBlob(blobUrl = any())
        }
        confirmVerified(accessSpy, blobMock, queueMock) // todo
    }

    // TODO: Will need to copy this test for Full ELR senders once receiving full ELR is implemented (see #5051)
    @Test
    fun `test receiveReport`() {
        mockkObject(BlobAccess.Companion)

        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings()
        val report1 = Report(
            one,
            listOf(listOf("1", "2"), listOf("3", "4")),
            source = TestSource,
            metadata = metadata,
            bodyFormat = Report.Format.CSV
        )
        val actionHistory = mockk<ActionHistory>()
        val sender = CovidSender("senderName", "org", Sender.Format.CSV, CustomerStatus.INACTIVE, one.name)

        every {
            BlobAccess.Companion.uploadBody(
                report1.bodyFormat,
                any(),
                any(),
                sender.fullName,
                Event.EventAction.RECEIVE
            )
        }.returns(BlobAccess.BlobInfo(Report.Format.CSV, "http://anyblob.com", "".toByteArray()))
        every { actionHistory.trackExternalInputReport(any(), any()) }.returns(Unit)

        val engine = makeEngine(metadata, settings)
        engine.recordReceivedReport(report1, "body".toByteArray(), sender, actionHistory)

        verify(exactly = 1) {
            actionHistory.trackExternalInputReport(any(), any())
            BlobAccess.Companion.uploadBody(any(), any(), any(), any(), any())
        }
        confirmVerified(blobMock)
    }

    @Test
    fun `test handleReportEvent`() {
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(
            one,
            listOf(listOf("1", "2"), listOf("3", "4")),
            source = TestSource,
            destination = oneOrganization.receivers[0],
            metadata = metadata
        )
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"
        val event = ReportEvent(Event.EventAction.SEND, report1.id, false)
        val nextAction = ReportEvent(Event.EventAction.NONE, report1.id, false)
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

        every { accessSpy.saveActionHistoryToDb(any(), any()) } returns Unit
        every { actionHistoryMock.trackActionResult(any() as String) }.returns(Unit)
        every { ActionHistory.Companion.sanityCheckReport(any(), any(), any()) }.returns(Unit)

        engine.handleReportEvent(event) { header, _, _ ->
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

    @Test
    fun `test handleProcessEvent queue vs task table mismatch error`() {
        // This only tests an error case in handleProcessEvent, not the 'happy path'.
        val one = Schema(name = "one", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val report1 = Report(
            one,
            listOf(listOf("1", "2"), listOf("3", "4")),
            source = TestSource,
            destination = oneOrganization.receivers[0],
            metadata = metadata
        )
        val bodyFormat = "CSV"
        val bodyUrl = "http://anyblob.com"
        // The event in the queue is a Process event.
        val processEvent = ProcessEvent(Event.EventAction.PROCESS, report1.id, Options.None, emptyMap(), emptyList())
        // Mismatch:  The event in the TASK table is a NONE event.
        val mismatchedNotProcessEvent = ReportEvent(Event.EventAction.NONE, report1.id, false)
        val mismatchedNotProcessTask = DatabaseAccess.createTask(
            report1, bodyFormat, bodyUrl, mismatchedNotProcessEvent
        )
        val actionHistoryMock = mockk<ActionHistory>()
        mockkObject(ActionHistory.Companion)
        val engine = makeEngine(metadata, settings)

        every { accessSpy.fetchAndLockTask(reportId = eq(report1.id), any()) }.returns(mismatchedNotProcessTask)
        // Run the test:
        engine.handleProcessEvent(processEvent, actionHistoryMock)

        // The code should just run these two methods, then log an error and return.
        verify(exactly = 1) {
            accessSpy.transact(block = any())
            accessSpy.fetchAndLockTask(reportId = any(), any())
        }
        confirmVerified(accessSpy, blobMock, queueMock)
    }

    @Test
    fun `test getBatchLookbackMins`() {
        // batch once a day, two retries
        assertThat(BaseEngine.getBatchLookbackMins(1, 2))
            .isEqualTo(4320 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
        // batch every minute, two retries
        assertThat(BaseEngine.getBatchLookbackMins(1440, 2))
            .isEqualTo(3 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
        // batch every two hours, two retries
        assertThat(BaseEngine.getBatchLookbackMins(12, 2))
            .isEqualTo(360 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
        // batch 3 times a day, two retries
        assertThat(BaseEngine.getBatchLookbackMins(3, 2))
            .isEqualTo(1440 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
        // bogus batches per day value, 1 retry
        assertThat(BaseEngine.getBatchLookbackMins(0, 1))
            .isEqualTo(2880 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
        // Batch every minute, no retries
        assertThat(BaseEngine.getBatchLookbackMins(1440, 0))
            .isEqualTo(1 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
        // Batch every minute, 10 retries
        assertThat(BaseEngine.getBatchLookbackMins(1440, 10))
            .isEqualTo(11 + BaseEngine.BATCH_LOOKBACK_PADDING_MINS)
    }
}