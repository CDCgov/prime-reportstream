package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.ReportFile
import gov.cdc.prime.router.azure.db.tables.Task
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.LocalAzureEventServiceImpl
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.getBlobContainerMetadata
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

private const val MULTIPLE_TARGETS_FHIR_PATH =
    "src/test/resources/fhirengine/engine/valid_data_multiple_targets.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRTranslatorIntegrationTests : Logging {
    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    val azureEventService = LocalAzureEventServiceImpl()

    @BeforeEach
    fun beforeEach() {
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns UniversalPipelineTestUtils
            .getBlobContainerMetadata(azuriteContainer)
        mockkObject(BlobAccess.BlobContainerMetadata)
        every {
            BlobAccess.BlobContainerMetadata.build(
                any(),
                any()
            )
        } returns UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
        azureEventService.events.clear()
    }

    private fun createFHIRTranslator(
        azureEventService: AzureEventService,
        org: DeepOrganization? = null,
    ): FHIRTranslator {
        val settings = FileSettings().loadOrganizations(org ?: UniversalPipelineTestUtils.universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        return FHIRTranslator(
            metadata,
            settings,
            reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess)),
            azureEventService = azureEventService
        )
    }

    private fun generateQueueMessage(
        report: Report,
        blobContents: String,
        sender: Sender,
        receiverName: String,
    ): String {
        return """
            {
                "type": "${TaskAction.translate.literal}",
                "reportId": "${report.id}",
                "blobURL": "${report.bodyURL}",
                "digest": "${BlobAccess.digestToString(BlobAccess.sha256Digest(blobContents.toByteArray()))}",
                "blobSubFolderName": "${sender.fullName}",
                "topic": "${sender.topic.jsonVal}",
                "receiverFullName": "$receiverName" 
            }
        """.trimIndent()
    }

    @Test
    fun `successfully translate for HL7 receiver when isSendOriginal is false`() {
        // set up
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.HL7
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val translator = createFHIRTranslator(azureEventService, org)
        val reportContents = File(MULTIPLE_TARGETS_FHIR_PATH).readText()
        val receiveReport = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.receive,
            Event.EventAction.CONVERT,
            azuriteContainer
        )
        val translateReport = UniversalPipelineTestUtils.createReport(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.translate,
            Event.EventAction.SEND,
            Topic.FULL_ELR,
            receiveReport,
            BlobAccess.uploadBlob(
                "${TaskAction.translate.literal}/mr_fhir_face.fhir",
                reportContents.toByteArray(),
                getBlobContainerMetadata(azuriteContainer)
            )
        )

        @Suppress("ktlint:standard:max-line-length")
        val expectedOutput = "MSH|^~\\&|||||||ORU/ACK - Unsolicited transmission of an observation message|849547|P|2.5.1|||||USA\r" +
            "SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210622\r" +
            "PID|1||||Steuber||20150707|O||^^^^^^^^Native Hawaiian or Other Pacific Islander|^^^IG^^s4fgh||~|||||||||^^^^^^^^Non Hispanic or Latino|||||||20210614\r" +
            "ORC|||||||||||||||||||||Any facility USA|^^^IG||^^^IG\r" +
            "OBR|1|||^^^^^^^^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay|||||||||||||||||||||C\r" +
            "OBX|1||^^^^^^^^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay|770814|^^^^^^^^Not detected||Abnormal|||||||||||LumiraDx Platform_LumiraDx\r" +
            "NTE|1|L|ciu1se|^^^^^^^^Remark\r" +
            "OBX|2||^^^^^^^^Whether patient is employed in a healthcare setting||^^^^^^^^Yes\r" +
            "OBX|3||^^^^^^^^First test for condition of interest||^^^^^^^^Yes\r" +
            "OBX|4||^^^^^^^^Patient was hospitalized because of this condition||^^^^^^^^Unknown\r" +
            "OBX|5||^^^^^^^^Admitted to intensive care unit for condition of interest||^^^^^^^^Yes\r" +
            "OBX|6||^^^^^^^^Date and time of symptom onset\r" +
            "OBX|7||^^^^^^^^Age\r" +
            "OBX|8||^^^^^^^^Pregnancy status||^^^^^^^^Unknown\r" +
            "OBX|9||^^^^^^^^Resides in a congregate care setting||^^^^^^^^Yes\r" +
            "OBX|10||^^^^^^^^Has symptoms related to condition of interest||^^^^^^^^No\r" +
            "SPM|1|||^^^^^^^^Sputum specimen|||^^^^^^^^Pinworm Prep|^^^^^^^^Nasopharyngeal structure (body structure)|||||||||20210617070000-0400|20210613045200-0400\r"

        val queueMessage = generateQueueMessage(
            translateReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            "phd.x"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // execute
        fhirFunctions.doTranslate(queueMessage, 1, translator)

        // verify task and report_file tables were updated correctly in the Translate function (new task and new
        // record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val batchTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                .fetchOneInto(Task.TASK)

            // verify batch queue task exists
            assertThat(batchTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(ReportFile.REPORT_FILE.asterisk())
                    .from(ReportFile.REPORT_FILE)
                    .where(
                        ReportFile.REPORT_FILE.REPORT_ID
                            .eq(batchTask!!.reportId)
                    )
                    .fetchOneInto(ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify message format is HL7
            assertThat(batchTask.bodyFormat).isEqualTo("HL7")

            // verify message matches the expected HL7 output
            val translatedValue = BlobAccess.downloadBlobAsBinaryData(
                sendReportFile!!.bodyUrl,
                UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
            ).toString()
            assertThat(translatedValue).isEqualTo(expectedOutput)
        }
    }

    @Test
    fun `successfully translate for HL7 receiver with enrichments when isSendOriginal is false`() {
        // set up
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.HL7,
                enrichmentSchemaNames = listOf(
                    "classpath:/enrichments/testing.yml",
                    "classpath:/enrichments/testing2.yml"
                )
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val translator = createFHIRTranslator(azureEventService, org)
        val reportContents = File(MULTIPLE_TARGETS_FHIR_PATH).readText()
        val receiveReport = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.receive,
            Event.EventAction.CONVERT,
            azuriteContainer
        )
        val translateReport = UniversalPipelineTestUtils.createReport(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.translate,
            Event.EventAction.SEND,
            Topic.FULL_ELR,
            receiveReport,
            BlobAccess.uploadBlob(
                "${TaskAction.translate.literal}/mr_fhir_face.fhir",
                reportContents.toByteArray(),
                getBlobContainerMetadata(azuriteContainer)
            )
        )

        @Suppress("ktlint:standard:max-line-length")
        val expectedOutput = "MSH|^~\\&|||||||ORU/ACK - Unsolicited transmission of an observation message|849547|P|2.5.1|||||USA\r" +
            "SFT|Orange Software Vendor Name|0.2-YELLOW|Purple PRIME ReportStream|0.1-SNAPSHOT||20210622\r" +
            "PID|1||||Steuber||20150707|O||^^^^^^^^Native Hawaiian or Other Pacific Islander|^^^IG^^s4fgh||~|||||||||^^^^^^^^Non Hispanic or Latino|||||||20210614\r" +
            "ORC|||||||||||||||||||||Any facility USA|^^^IG||^^^IG\r" +
            "OBR|1|||^^^^^^^^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay|||||||||||||||||||||C\r" +
            "OBX|1||^^^^^^^^SARS-CoV+SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay|770814|^^^^^^^^Not detected||Abnormal|||||||||||LumiraDx Platform_LumiraDx\r" +
            "NTE|1|L|ciu1se|^^^^^^^^Remark\r" +
            "OBX|2||^^^^^^^^Whether patient is employed in a healthcare setting||^^^^^^^^Yes\r" +
            "OBX|3||^^^^^^^^First test for condition of interest||^^^^^^^^Yes\r" +
            "OBX|4||^^^^^^^^Patient was hospitalized because of this condition||^^^^^^^^Unknown\r" +
            "OBX|5||^^^^^^^^Admitted to intensive care unit for condition of interest||^^^^^^^^Yes\r" +
            "OBX|6||^^^^^^^^Date and time of symptom onset\r" +
            "OBX|7||^^^^^^^^Age\r" +
            "OBX|8||^^^^^^^^Pregnancy status||^^^^^^^^Unknown\r" +
            "OBX|9||^^^^^^^^Resides in a congregate care setting||^^^^^^^^Yes\r" +
            "OBX|10||^^^^^^^^Has symptoms related to condition of interest||^^^^^^^^No\r" +
            "SPM|1|||^^^^^^^^Sputum specimen|||^^^^^^^^Pinworm Prep|^^^^^^^^Nasopharyngeal structure (body structure)|||||||||20210617070000-0400|20210613045200-0400\r"

        val queueMessage = generateQueueMessage(
            translateReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            "phd.x"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // execute
        fhirFunctions.doTranslate(queueMessage, 1, translator)

        // verify task and report_file tables were updated correctly in the Translate function (new task and new
        // record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val batchTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                .fetchOneInto(Task.TASK)

            // verify batch queue task exists
            assertThat(batchTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(ReportFile.REPORT_FILE.asterisk())
                    .from(ReportFile.REPORT_FILE)
                    .where(
                        ReportFile.REPORT_FILE.REPORT_ID
                            .eq(batchTask!!.reportId)
                    )
                    .fetchOneInto(ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify message format is HL7
            assertThat(batchTask.bodyFormat).isEqualTo("HL7")

            // verify message matches the expected HL7 output
            val translatedValue = BlobAccess.downloadBlobAsBinaryData(
                sendReportFile!!.bodyUrl,
                UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
            ).toString()
            assertThat(translatedValue).isEqualTo(expectedOutput)
        }
    }

    @Test
    fun `successfully translate for FHIR receiver when isSendOriginal is false`() {
        // set up
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.FHIR,
                schemaName = ""
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val translator = createFHIRTranslator(azureEventService, org)
        val reportContents = File(MULTIPLE_TARGETS_FHIR_PATH).readText()
        val receiveReport = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.receive,
            Event.EventAction.CONVERT,
            azuriteContainer
        )
        val translateReport = UniversalPipelineTestUtils.createReport(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.translate,
            Event.EventAction.SEND,
            Topic.FULL_ELR,
            receiveReport,
            BlobAccess.uploadBlob(
                "${TaskAction.translate.literal}/mr_fhir_face.fhir",
                reportContents.toByteArray(),
                getBlobContainerMetadata(azuriteContainer)
            )
        )

        val queueMessage = generateQueueMessage(
            translateReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            "phd.x"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // execute
        fhirFunctions.doTranslate(queueMessage, 1, translator)

        // verify task and report_file tables were updated correctly in the Translate function (new task and new
        // record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val batchTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                .fetchOneInto(Task.TASK)

            // verify batch queue task exists
            assertThat(batchTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(ReportFile.REPORT_FILE.asterisk())
                    .from(ReportFile.REPORT_FILE)
                    .where(
                        ReportFile.REPORT_FILE.REPORT_ID
                            .eq(batchTask!!.reportId)
                    )
                    .fetchOneInto(ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify message format is FHIR
            assertThat(batchTask.bodyFormat).isEqualTo("FHIR")

            // verify message does not match the original FHIR input
            val translatedValue = BlobAccess.downloadBlobAsByteArray(
                sendReportFile!!.bodyUrl,
                UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
            )
            assertThat(translatedValue).isNotEqualTo(reportContents.toByteArray())
        }
    }

    @Test
    fun `successfully translate when isSendOriginal is true`() {
        // set up
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.FHIR
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val translator = createFHIRTranslator(azureEventService, org)
        val reportContents = File(MULTIPLE_TARGETS_FHIR_PATH).readText()
        val receiveReport = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.receive,
            Event.EventAction.CONVERT,
            azuriteContainer
        )
        val translateReport = UniversalPipelineTestUtils.createReport(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.translate,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            receiveReport,
            BlobAccess.uploadBlob(
                "${TaskAction.translate.literal}/mr_fhir_face.fhir",
                reportContents.toByteArray(),
                getBlobContainerMetadata(azuriteContainer)
            )
        )

        val queueMessage = generateQueueMessage(
            translateReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithSendOriginal,
            "phd.x"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // execute
        fhirFunctions.doTranslate(queueMessage, 1, translator)

        // verify task and report_file tables were updated correctly in the Translate function (new task and new
        // record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val batchTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                .fetchOneInto(Task.TASK)
            // verify batch queue task does not exist
            assertThat(batchTask).isNull()

            val sendTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.send))
                .fetchOneInto(Task.TASK)
            // verify send queue task exists
            assertThat(sendTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(ReportFile.REPORT_FILE.asterisk())
                    .from(ReportFile.REPORT_FILE)
                    .where(
                        ReportFile.REPORT_FILE.REPORT_ID
                            .eq(sendTask!!.reportId)
                    )
                    .fetchOneInto(ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify message format is FHIR
            assertThat(sendTask.bodyFormat).isEqualTo("FHIR")

            // verify message matches the original FHIR input
            val translatedValue = BlobAccess.downloadBlobAsByteArray(
                sendReportFile!!.bodyUrl,
                UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
            )
            assertThat(translatedValue).isEqualTo(reportContents.toByteArray())
        }
    }
}