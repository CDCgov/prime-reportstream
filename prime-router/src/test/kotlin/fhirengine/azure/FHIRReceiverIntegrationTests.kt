package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import com.azure.data.tables.TableServiceClient
import com.azure.data.tables.TableServiceClientBuilder
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.LocalAzureEventServiceImpl
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransformInactive
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7SenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.cleanHL7Record
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

    val azureEventService = LocalAzureEventServiceImpl()
    private lateinit var tableServiceClient: TableServiceClient

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

        tableServiceClient = TableServiceClientBuilder()
            .connectionString(getBlobContainerMetadata().connectionString)
            .buildClient()
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Suppress("ktlint:standard:max-line-length")
    private fun getBlobContainerMetadata(): BlobAccess.BlobContainerMetadata {
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10000)}/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10001)}/devstoreaccount1;TableEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10002)}/devstoreaccount1;"""
        return BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )
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

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            receivedReportContents,
            fhirSenderWithNoTransformInactive,
            headers = mapOf(
                "content-type" to "application/fhir+ndjson;test",
                "x-azure-clientip" to "0.0.0.0",
                "payloadname" to "test_message",
                "client_id" to fhirSenderWithNoTransformInactive.fullName,
            )
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

        val tableRow = tableServiceClient
            .getTableClient("submission")
            .getEntity(reportId.toString(), "Accepted")

        assertNotNull(tableRow)
        assertThat(tableRow.getProperty("detail")).isEqualTo(
        "[Sender has customer status INACTIVE: phd.fhir-elr-no-transform-inactive]"
        )
        assertThat(tableRow.getProperty("body_url")).isEqualTo(receiveBlobUrl)
    }

    @Test
    fun `should handle sender not found gracefully`() {
        val submissionMessageContents = validFHIRRecord1
        val submissionBlobUrl = "http://anyblob.com"

        val reportId = UUID.randomUUID()

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            submissionBlobUrl,
            submissionMessageContents,
            fhirSenderWithNoTransformInactive,
            headers = mapOf(
                "content-type" to "application/fhir+ndjson;test",
                "x-azure-clientip" to "0.0.0.0",
                "payloadname" to "test_message",
                "client_id" to "unknown_sender",
            )
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

        val tableRow = tableServiceClient
            .getTableClient("submission")
            .getEntity(reportId.toString(), "Rejected")

        assertNotNull(tableRow)
        assertThat(tableRow.getProperty("detail")).isEqualTo("Sender not found matching client_id: unknown_sender")
        assertThat(tableRow.getProperty("body_url")).isEqualTo(submissionBlobUrl)
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

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            receivedReportContents,
            fhirSenderWithNoTransform,
            headers = mapOf(
                "content-type" to "application/fhir+ndjson;test",
                "x-azure-clientip" to "0.0.0.0",
                "payloadname" to "test_message",
                "client_id" to fhirSenderWithNoTransform.fullName,
            )
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

        val tableRow = tableServiceClient
            .getTableClient("submission")
            .getEntity(reportId.toString(), "Accepted")

        assertNotNull(tableRow)
        assertThat(tableRow.getProperty("body_url")).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.getProperty("detail")).isNull()
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

        val receiveQueueMessage = generateReceiveQueueMessage(
            reportId.toString(),
            receiveBlobUrl,
            receivedReportContents,
            hl7SenderWithNoTransform,
            headers = mapOf(
                "content-type" to "application/hl7-v2;test",
                "x-azure-clientip" to "0.0.0.0",
                "payloadname" to "test_message",
                "client_id" to hl7SenderWithNoTransform.fullName,
            )
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

        val tableRow = tableServiceClient
            .getTableClient("submission")
            .getEntity(reportId.toString(), "Accepted")

        assertNotNull(tableRow)
        assertThat(tableRow.getProperty("body_url")).isEqualTo(receiveBlobUrl)
        assertThat(tableRow.getProperty("detail")).isNull()
    }
}