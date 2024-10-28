package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.reportstream.shared.Submission
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.cleanHL7Record
import gov.cdc.prime.router.report.ReportService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.Test

class FHIRReceiverTest {

    // Common mock objects and setup
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val reportService: ReportService = mockk<ReportService>()
    private val submissionTableService: SubmissionTableService = mockk<SubmissionTableService>()

    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(Receiver("elr", "co-phd", Topic.TEST, CustomerStatus.INACTIVE, "one"))
    )
    val settings = FileSettings().loadOrganizations(oneOrganization)
    val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
    val metadata = Metadata(schema = one)

    private fun makeFhirReceiver(metadata: Metadata, settings: SettingsProvider): FHIRReceiver {
        return FHIRReceiver(
            metadata,
            settings,
            accessSpy,
            blobMock,
            reportService = reportService,
            submissionTableService = submissionTableService
        )

//        FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
//            .reportService(reportService).blobAccess(blobMock).build(taskAction)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    data class FHIRTestSetup(
        val engine: FHIRReceiver,
        val actionLogger: ActionLogger,
        val actionHistory: ActionHistory,
        val message: FhirReceiveQueueMessage,
    )

    private fun setupMocksForProcessingTest(
        clientId: String,
        contentType: String,
        customerStatus: CustomerStatus,
        hasErrors: Boolean,
        reportID: UUID = UUID.randomUUID(),

    ): FHIRTestSetup {
        mockkObject(BlobAccess)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val sender = CovidSender(
            "Test Sender",
            "test",
            MimeFormat.HL7,
            schemaName = "one",
            customerStatus = customerStatus
        )

        val engine = spyk(makeFhirReceiver(metadata, settings))
        val message = mockk<FhirReceiveQueueMessage>(relaxed = true)
        val action = Action()
        action.actionName = TaskAction.receive

        val headers = mapOf(
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to clientId,
            "content-type" to contentType
        )

        every { message.headers } returns headers
        every { message.reportId } returns reportID
        every { actionLogger.hasErrors() } returns hasErrors
        every { actionLogger.setReportId(any()) } returns actionLogger
        every { actionLogger.error(any<ActionLogDetail>()) } returns Unit
        every { engine.settings.findSender(any()) } returns sender
        every { actionHistory.trackActionResult(any<HttpStatus>()) } returns Unit
        every { actionHistory.trackActionParams(any<String>()) } returns Unit
        every { actionHistory.trackActionSenderInfo(any(), any()) } returns Unit
        every { actionHistory.trackExternalInputReport(any(), any()) } returns Unit
        every { actionHistory.trackLogs(any<List<ActionLog>>()) } returns Unit
        every { submissionTableService.insertSubmission(any()) } returns Unit
        every { actionHistory.action } returns action
        every { BlobAccess.downloadBlob(any(), any()) }.returns(cleanHL7Record)

        return FHIRTestSetup(engine, actionLogger, actionHistory, message)
    }

    @Test
    fun `test handle sender not found`() {
        val fhirSetup =
            setupMocksForProcessingTest(
                "unknown_client_id",
                "application/hl7-v2;test",
                CustomerStatus.ACTIVE,
                true
            )
        val engine = fhirSetup.engine
        val queueMessage = fhirSetup.message
        val actionLogger = ActionLogger()
        val actionHistory = fhirSetup.actionHistory

        every { engine.settings.findSender(any()) } returns null

        assertThrows<SubmissionSenderNotFound> {
            accessSpy.transact { txn ->
                engine.run(queueMessage, actionLogger, actionHistory, txn)
            }
        }

        val reportId = queueMessage.reportId.toString()
        val blobURL = queueMessage.blobURL
        verify(exactly = 1) {
            Submission(
                reportId,
                "Rejected",
                blobURL,
                "Sender not found matching client_id: unknown_client_id"
            )
            submissionTableService.insertSubmission(any())
        }
    }

    @Test
    fun `test successful processing`() {
        val reportID = UUID.randomUUID()
        val fhirSetup =
            setupMocksForProcessingTest(
                "known_client_id",
                "application/hl7-v2;test",
                CustomerStatus.ACTIVE,
                false,
                reportID
            )
        val engine = fhirSetup.engine
        val queueMessage = fhirSetup.message
        val actionLogger = fhirSetup.actionLogger
        val actionHistory = fhirSetup.actionHistory
        every { actionLogger.errors } returns emptyList()

        accessSpy.transact { txn ->
            engine.run(queueMessage, actionLogger, actionHistory, txn)
        }

        verify(exactly = 1) {
            actionHistory.trackActionResult(HttpStatus.CREATED)
            actionHistory.trackActionSenderInfo("test.Test Sender", "test_message")
            actionHistory.trackExternalInputReport(any(), any())
            submissionTableService.insertSubmission(any())
        }
    }

    @Test
    fun `test invalid MIME type`() {
        val fhirSetup =
            setupMocksForProcessingTest(
                "known_client_id",
                "invalid/mime-type",
                CustomerStatus.ACTIVE,
                true
            )
        val engine = fhirSetup.engine
        val queueMessage = fhirSetup.message
        val actionLogger = ActionLogger()
        val actionHistory = fhirSetup.actionHistory

        var exception: Exception? = null
        try {
            accessSpy.transact { txn ->
                engine.run(queueMessage, actionLogger, actionHistory, txn)
            }
        } catch (e: Exception) {
            exception = e
        }

        assertThat(exception!!.javaClass.name).isEqualTo("java.lang.IllegalArgumentException")
        assertThat(actionLogger.errors).hasSize(1)
        assertThat(actionLogger.errors[0].detail.message).isEqualTo("Unexpected MIME type invalid/mime-type.")
    }
}