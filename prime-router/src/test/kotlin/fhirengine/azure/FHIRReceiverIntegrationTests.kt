package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.TableAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.LocalAzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.ReportEventData
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamReportEvent
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.csvSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransformInactive
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7SenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.cleanHL7Record
import gov.cdc.prime.router.common.invalidMalformedFHIRRecord
import gov.cdc.prime.router.common.unparseableHL7Record
import gov.cdc.prime.router.common.validFHIRRecord1
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiver
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedReport
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertNotNull

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRReceiverIntegrationTests {

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirreceiverintegration",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    private val azureEventService = LocalAzureEventServiceImpl()
    private lateinit var submissionTableService: SubmissionTableService

    private fun createFHIRFunctionsInstance(): FHIRFunctions {
        val settings = FileSettings().loadOrganizations(universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        val workflowEngine = WorkflowEngine
            .Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            .build()
        return FHIRFunctions(workflowEngine, databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess)
    }

    private fun createFHIRReceiver(): FHIRReceiver {
        val settings = FileSettings().loadOrganizations(universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        return FHIRReceiver(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            azureEventService = azureEventService,
            submissionTableService = submissionTableService
        )
    }

    private fun generateReceiveQueueMessage(
        reportId: String,
        blobURL: String,
        blobContents: String,
        sender: Sender,
        headers: Map<String, String>,
    ): String {
        val headersStringMap = headers.entries.joinToString(separator = ",\n") { (key, value) ->
            """"$key": "$value""""
        }
        val headersString = "[\"java.util.LinkedHashMap\",{$headersStringMap}]"

        return """{"type":"receive-fhir","blobURL":"$blobURL",
        "digest":"${BlobUtils.digestToString(BlobUtils.sha256Digest(blobContents.toByteArray()))}",
            "blobSubFolderName":"${sender.fullName}","reportId":"$reportId","headers":$headersString}
    """.trimIndent()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns getBlobContainerMetadata()
        mockkObject(BlobAccess.BlobContainerMetadata)
        every { BlobAccess.BlobContainerMetadata.build(any(), any()) } returns getBlobContainerMetadata()

        mockkObject(TableAccess)
        every { TableAccess.getConnectionString() } returns getConnString()

        submissionTableService = SubmissionTableService.getInstance()
        submissionTableService.reset()
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    private fun getBlobContainerMetadata(): BlobAccess.BlobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
        getConnString()
        )

    private fun getConnString(): String {
        @Suppress("ktlint:standard:max-line-length")
        return """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10000)}/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10001)}/devstoreaccount1;TableEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10002)}/devstoreaccount1;"""
    }

    @Test
    fun `should handle inactive sender gracefully`() {
        val receivedReportContents =
            listOf(validFHIRRecord1)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.fhir",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/fhir+ndjson;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to fhirSenderWithNoTransformInactive.fullName,
            "content-length" to "100"
        )

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            receivedReportContents,
            fhirSenderWithNoTransformInactive,
            headers = headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(
            receiveQueueMessage,
            1,
            createFHIRReceiver(),
            ActionHistory(TaskAction.receive)
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(reportId))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs.first()).transform { it.detail.message }
                .isEqualTo("Sender has customer status INACTIVE: phd.fhir-elr-no-transform-inactive")

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(actionLogs.count()).isEqualTo(1)
            assertThat(reportFile.count()).isEqualTo(1)
        }

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(reportId.toString(), "Accepted")

        assertNotNull(tableRow)
        assertThat(tableRow.detail).isEqualTo(
        "[Sender has customer status INACTIVE: phd.fhir-elr-no-transform-inactive]"
        )
        assertThat(tableRow.bodyURL).isEqualTo(receiveBlobUrl)

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                Topic.FULL_ELR,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.FHIR,
                ReportStreamEventProperties.SENDER_NAME to fhirSenderWithNoTransformInactive.fullName,
                ReportStreamEventProperties.FILE_LENGTH to headers["content-length"],
                ReportStreamEventProperties.SENDER_IP to headers["x-azure-clientip"],
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_NOT_PROCESSABLE]!!).hasSize(1)
        val notProcessableEvent =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_NOT_PROCESSABLE]!!.last() as ReportStreamReportEvent
        assertThat(notProcessableEvent.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                Topic.FULL_ELR,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(notProcessableEvent.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.PROCESSING_ERROR to
                    "Submitted report was either empty or could not be parsed.",
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )
    }

    @Test
    fun `should handle sender not found gracefully`() {
        val submissionMessageContents = validFHIRRecord1
        val submissionBlobUrl = "http://anyblob.com"

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/fhir+ndjson;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to "unknown_sender",
            "content-length" to "100"
        )

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            submissionBlobUrl,
            submissionMessageContents,
            fhirSenderWithNoTransformInactive,
            headers = headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(
            receiveQueueMessage,
            1,
            createFHIRReceiver(),
            ActionHistory(TaskAction.receive)
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs).isEmpty()

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(reportFile).isEmpty()
        }

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(
            reportId.toString(),
            "Rejected"
        )

        assertNotNull(tableRow)
        assertThat(tableRow.detail).isEqualTo("Sender not found matching client_id: unknown_sender")
        assertThat(tableRow.bodyURL).isEqualTo(submissionBlobUrl)

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_NOT_RECEIVABLE]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_NOT_RECEIVABLE]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                null,
                submissionBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.PROCESSING_ERROR to
                    "Sender is not found in matching client id: unknown_sender.",
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )
    }

    @Test
    fun `should successfully process valid FHIR message`() {
        val receivedReportContents =
            listOf(validFHIRRecord1)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.fhir",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/fhir+ndjson;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to fhirSenderWithNoTransform.fullName,
            "content-length" to "100"
        )

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            receivedReportContents,
            fhirSenderWithNoTransform,
            headers = headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(
            receiveQueueMessage,
            1,
            createFHIRReceiver(),
            ActionHistory(TaskAction.receive)
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(reportId))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs).isEmpty()

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(reportFile).isNotEmpty()
        }

        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(
            reportId.toString(),
            "Accepted"
        )

        assertNotNull(tableRow)
        assertThat(tableRow.bodyURL).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.detail).isNull()

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                Topic.FULL_ELR,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.FHIR,
                ReportStreamEventProperties.SENDER_NAME to fhirSenderWithNoTransform.fullName,
                ReportStreamEventProperties.FILE_LENGTH to headers["content-length"],
                ReportStreamEventProperties.SENDER_IP to headers["x-azure-clientip"],
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )
    }

    @Test
    fun `should successfully process valid HL7 message`() {
        val receivedReportContents =
            listOf(cleanHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/hl7-v2;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to hl7SenderWithNoTransform.fullName,
            "content-length" to "100"
        )
        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            receivedReportContents,
            hl7SenderWithNoTransform,
            headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(
            receiveQueueMessage,
            1,
            createFHIRReceiver(),
            ActionHistory(TaskAction.receive)
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(reportId))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs).isEmpty()

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(reportFile).isNotEmpty()
        }

        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(
            reportId.toString(),
            "Accepted"
        )

        assertNotNull(tableRow)
        assertThat(tableRow.bodyURL).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.detail).isNull()

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                Topic.FULL_ELR,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.HL7,
                ReportStreamEventProperties.SENDER_NAME to hl7SenderWithNoTransform.fullName,
                ReportStreamEventProperties.FILE_LENGTH to headers["content-length"],
                ReportStreamEventProperties.SENDER_IP to headers["x-azure-clientip"],
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )
    }

    @Test
    fun `test process invalid FHIR message`() {
        val invalidReceivedReportContents =
            listOf(invalidMalformedFHIRRecord)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/fail-path.fhir",
            invalidReceivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/fhir+ndjson;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to fhirSenderWithNoTransform.fullName,
            "content-length" to "100"
        )

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            invalidReceivedReportContents,
            fhirSenderWithNoTransform,
            headers = headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(
            receiveQueueMessage,
            1,
            createFHIRReceiver(),
            ActionHistory(TaskAction.receive)
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(reportId))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs.count()).isEqualTo(1)
            assertThat(actionLogs.first().detail.message).isEqualTo("1: Unable to parse FHIR data.")

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(reportFile).isNotEmpty()
        }

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(
            reportId.toString(),
            "Accepted"
        )

        assertNotNull(tableRow)
        assertThat(tableRow.bodyURL).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.detail).isEqualTo("[1: Unable to parse FHIR data.]")

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                Topic.FULL_ELR,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.FHIR,
                ReportStreamEventProperties.SENDER_NAME to fhirSenderWithNoTransform.fullName,
                ReportStreamEventProperties.FILE_LENGTH to headers["content-length"],
                ReportStreamEventProperties.SENDER_IP to headers["x-azure-clientip"],
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )
    }

    @Test
    fun `test process invalid HL7 message`() {
        val invalidReceivedReportContents =
            listOf(unparseableHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/fail-path.hl7",
            invalidReceivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/hl7-v2;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to hl7SenderWithNoTransform.fullName,
            "content-length" to "100"
        )

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            invalidReceivedReportContents,
            hl7SenderWithNoTransform,
            headers = headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(
            receiveQueueMessage,
            1,
            createFHIRReceiver(),
            ActionHistory(TaskAction.receive)
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(reportId))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs.count()).isEqualTo(2)

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(reportFile).isNotEmpty()
        }

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(
            reportId.toString(),
            "Accepted"
        )

        assertNotNull(tableRow)
        assertThat(tableRow.bodyURL).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.detail).isEqualTo("[Failed to parse message, Failed to parse message]")

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_RECEIVED]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                Topic.FULL_ELR,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.HL7,
                ReportStreamEventProperties.SENDER_NAME to hl7SenderWithNoTransform.fullName,
                ReportStreamEventProperties.FILE_LENGTH to headers["content-length"],
                ReportStreamEventProperties.SENDER_IP to headers["x-azure-clientip"],
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString()
            )
        )
    }

    @Test
    fun `test process CSV message`() {
        val invalidReceivedReportContents =
            listOf(unparseableHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/fail-path.hl7",
            invalidReceivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val reportId = UUID.randomUUID()
        val headers = mapOf(
            "content-type" to "application/hl7-v2;test",
            "x-azure-clientip" to "0.0.0.0",
            "payloadname" to "test_message",
            "client_id" to csvSenderWithNoTransform.fullName,
            "content-length" to "100"
        )

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            invalidReceivedReportContents,
            csvSenderWithNoTransform,
            headers = headers
        )

        val fhirFunctions = createFHIRFunctionsInstance()

        var exception: Exception? = null
        try {
            fhirFunctions.process(
                receiveQueueMessage,
                1,
                createFHIRReceiver(),
                ActionHistory(TaskAction.receive)
            )
        } catch (e: Exception) {
            exception = e
        }

        assertThat(exception!!.javaClass.name).isEqualTo("java.lang.IllegalStateException")

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(reportId))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(DetailedActionLog::class.java)

            assertThat(actionLogs.count()).isEqualTo(0)

            val reportFile = DSL.using(txn).select(Tables.REPORT_FILE.asterisk())
                .from(Tables.REPORT_FILE)
                .where(Tables.REPORT_FILE.REPORT_ID.eq(reportId))
                .fetchInto(DetailedReport::class.java)

            assertThat(reportFile).isEmpty()
        }

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }

        val tableRow = submissionTableService.getSubmission(
            reportId.toString(),
            "Rejected"
        )

        assertNotNull(tableRow)
        assertThat(tableRow.bodyURL).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.detail).isEqualTo("[Unsupported sender format: CSV]")

        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.REPORT_NOT_PROCESSABLE]!!).hasSize(1)
        val event =
            azureEventService
                .reportStreamEvents[ReportStreamEventName.REPORT_NOT_PROCESSABLE]!!.last() as ReportStreamReportEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                reportId,
                null,
                emptyList(),
                null,
                receiveBlobUrl,
                TaskAction.receive,
                OffsetDateTime.now()
            ),
            ReportEventData::timestamp
        )
        assertThat(event.params).isEqualTo(
            mapOf(
                ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.CSV,
                ReportStreamEventProperties.SENDER_NAME to csvSenderWithNoTransform.fullName,
                ReportStreamEventProperties.FILE_LENGTH to headers["content-length"],
                ReportStreamEventProperties.SENDER_IP to headers["x-azure-clientip"],
                ReportStreamEventProperties.REQUEST_PARAMETERS to headers.toString(),
                ReportStreamEventProperties.PROCESSING_ERROR to "Unsupported sender format CSV."
            )
        )
    }
}