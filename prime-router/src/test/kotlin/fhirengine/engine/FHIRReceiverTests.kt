package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.reportstream.shared.SubmissionsEntity
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SenderNotFound
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test

private const val BLOB_URL = "http://blobstore.example/file.hl7"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val SCHEMA_NAME = "classpath:/test-schema.yml"
private const val VALID_DATA_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"
private const val BATCH_VALID_DATA_URL = "src/test/resources/fhirengine/engine/batch_valid_data.fhir"
private const val BLOB_FHIR_URL = "http://blobstore.example/file.fhir"

class FHIRReceiverTests {

    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val reportService: ReportService = mockk<ReportService>()
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(Receiver("elr", "co-phd", Topic.TEST, CustomerStatus.INACTIVE, "one"))
    )

    val settings = FileSettings().loadOrganizations(oneOrganization)
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one)

    private val validHl7 = "" +
        "MSH|^~\\&|CDC PRIME - Atlanta,^2.16.840.1.114222.4.1.237821^ISO|Winchester House^05D2222542^ISO|CDPH FL " +
        "REDIE^2.16.840.1.114222.4.3.3.10.1.1^ISO|CDPH_CID^2.16.840.1.114222.4.1.214104^ISO|20210803131511.0147+0000" +
        "||ORU^R01^ORU_R01|1234d1d1-95fe-462c-8ac6-46728dba581c|P|2.5.1|||NE|NE|USA|UNICODE UTF-8|||PHLabReport-NoAck" +
        "^ELR_Receiver^2.16.840.1.113883.9.11^ISO"

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .reportService(reportService).blobAccess(blobMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // good hl7, check actionHistory, item lineages, upload was called, task, queue message
    @Test
    fun `test processHl7 and handle sender not found`() {
        mockkObject(BlobAccess)
        mockkClass(SubmissionsEntity::class)
        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = ActionLogger()
        val settingsProvider = mockk<SettingsProvider>()

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.receive) as FHIRReceiver)

        val queueMessage = mockk<FhirReceiveQueueMessage>(relaxed = true)
        every { queueMessage.headers["client_id"] } returns "unknown_client_id"
        every { queueMessage.headers["payloadname"] } returns ""
        every { queueMessage.headers["Content-Type"] } returns ""
        val reportId = UUID.randomUUID()
        every { queueMessage.reportId } returns reportId
        every { settingsProvider.findSender(any()) } returns null
        val action = Action()
        action.actionName = TaskAction.receive
        every { actionHistory.action } returns action
        every { actionHistory.trackActionResult(any<HttpStatus>()) } returns Unit
        every { actionHistory.trackActionParams(any<String>()) } returns Unit
        every { actionHistory.trackReceivedNoReport(any(), any(), any(), any(), any()) } returns Unit

        // act
        accessSpy.transact { txn ->
            engine.run(queueMessage, actionLogger, actionHistory, txn)
        }

        // assert
        assertThat(
            actionLogger.errors[0].equals(
                InvalidParamMessage("Sender not found matching client_id: unknown_client_id")
            )
        )
        verify(exactly = 1) {
            BlobAccess.Companion.insertTableEntity(any())
            SubmissionsEntity(reportId.toString(), "Rejected").toTableEntity()
            actionHistory.trackActionResult(HttpStatus.BAD_REQUEST)
        }
    }

    // good hl7, check actionHistory, item lineages, upload was called, task, queue message
    @Test
    fun `test processHl7 and handle inactive sender`() {
        mockkObject(BlobAccess)
        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = ActionLogger()
        val sender = CovidSender(
            "Test Sender",
            "test",
            MimeFormat.CSV,
            schemaName = "one",
            customerStatus = CustomerStatus.INACTIVE
        )

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.receive) as FHIRReceiver)

        val queueMessage = mockk<FhirReceiveQueueMessage>(relaxed = true)
        every { queueMessage.headers["client_id"] } returns "test_client_id"
        every { queueMessage.headers["payloadname"] } returns "test_name"
        every { queueMessage.headers["Content-Type"] } returns ""
        val reportId = UUID.randomUUID()
        every { queueMessage.reportId } returns reportId
        val action = Action()
        action.actionName = TaskAction.receive
        every { actionHistory.action } returns action
        every { actionHistory.trackActionResult(any<HttpStatus>()) } returns Unit
        every { actionHistory.trackActionParams(any<String>()) } returns Unit
        every { actionHistory.trackActionSenderInfo(any(), any()) } returns Unit
        every { actionHistory.trackReceivedNoReport(any(), any(), any(), any(), any()) } returns Unit
        every { engine.settings.findSender(any()) } returns sender

        // act
        accessSpy.transact { txn ->
            engine.run(queueMessage, actionLogger, actionHistory, txn)
        }

        // assert
        assertThat(actionLogger.errors[0].equals(SenderNotFound(queueMessage.headers["client_id"].toString())))
        verify(exactly = 1) {
            BlobAccess.Companion.insertTableEntity(any())
            SubmissionsEntity(reportId.toString(), "Rejected").toTableEntity()
            actionHistory.trackActionResult(HttpStatus.NOT_ACCEPTABLE)
            actionHistory.trackActionSenderInfo(sender.fullName, "test_name")
        }
    }

    // good hl7, check actionHistory, item lineages, upload was called, task, queue message
    @Test
    fun `test processHl7 happy path with queueMessage headers`() {
        mockkObject(BlobAccess)
        mockkObject(Report)

        val sender = CovidSender(
            "Test Sender",
            "test",
            MimeFormat.CSV,
            schemaName = "one",
            customerStatus = CustomerStatus.ACTIVE
        )

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val transformer = mockk<FhirTransformer>()

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                emptyList()
            )
        )
        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.receive) as FHIRReceiver)
        val message = spyk(
            FhirReceiveQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER_NAME
            )
        )

        every { message.headers["client_id"] } returns "test_client_id"
        every { message.headers["payloadname"] } returns "test_message"
        every { message.headers["Content-Type"] } returns "application/fhir+ndjson;test"
        every { message.headers.isEmpty() } returns false

        val bodyFormat = MimeFormat.FHIR
        val bodyUrl = "https://anyblob.com"

        val reportId = UUID.randomUUID()
        every { message.reportId } returns reportId

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.getItemLogger(any(), any()) } returns actionLogger
        every { actionLogger.warn(any<List<ActionLogDetail>>()) } just runs
        every { actionLogger.setReportId(any()) } returns actionLogger
        every { BlobAccess.downloadContent(any(), any()) }.returns(validHl7)
        every { Report.getFormatFromBlobURL(message.blobURL) } returns MimeFormat.HL7
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackActionResult(any<HttpStatus>()) } returns Unit
        every { actionHistory.trackActionParams(any<String>()) } returns Unit
        every { actionHistory.trackActionSenderInfo(any(), any()) } returns Unit
        every { actionHistory.trackReceivedNoReport(any(), any(), any(), any(), any()) } returns Unit
        val action = Action()
        action.actionName = TaskAction.receive
        every { actionHistory.action } returns action
        every { transformer.process(any()) } returnsArgument (0)
        every { engine.settings.findSender(any()) } returns sender

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        // assert
        verify(exactly = 1) {
            actionHistory.trackActionResult(HttpStatus.CREATED)
            actionHistory.trackActionSenderInfo(sender.fullName, "test_message")
            actionHistory.trackReceivedNoReport(
                reportId,
                BLOB_URL,
                bodyFormat.toString(),
                TaskAction.none,
                "test_message"
            )
            SubmissionsEntity(reportId.toString(), "Accepted").toTableEntity()
            transformer.process(any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
            BlobAccess.Companion.uploadBlob(any(), any(), any())
        }
    }
}