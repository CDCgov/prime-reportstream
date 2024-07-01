package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.Task
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.fhirengine.engine.QueueMessage
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.OffsetDateTime

private const val MULTIPLE_TARGETS_FHIR_PATH = "src/test/resources/fhirengine/engine/valid_data_multiple_targets.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FhirTranslatorIntegrationTests : Logging {
    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    val oneOrganization = DeepOrganization(
        "phd", "test", Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = Receiver.Timing(numberPerDay = 1, maxReportCount = 1, whenEmpty = Receiver.WhenEmpty())
            ),
            Receiver(
                "elr2",
                "phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml",
                timing = Receiver.Timing(numberPerDay = 1, maxReportCount = 1, whenEmpty = Receiver.WhenEmpty()),
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                processingModeFilter = listOf("true"),
                format = Report.Format.HL7,
            )
        ),
    )

    private fun makeWorkflowEngine(
        metadata: Metadata,
        settings: SettingsProvider,
        databaseAccess: DatabaseAccess,
    ): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(databaseAccess)
                .build()
        )
    }

    private fun seedTask(
        fileFormat: Report.Format,
        currentAction: TaskAction,
        nextAction: TaskAction,
        nextEventAction: Event.EventAction,
        topic: Topic,
        taskIndex: Long = 0,
        organization: DeepOrganization,
        childReport: Report? = null,
        bodyURL: String? = null,
    ): Report {
        val report = Report(
            fileFormat,
            listOf(ClientSource(organization = organization.name, client = "Test Sender")),
            1,
            metadata = UnitTestUtils.simpleMetadata,
            nextAction = nextAction,
            topic = topic
        )
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val action = Action().setActionName(currentAction)
            val actionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            report.bodyURL = bodyURL ?: "http://${report.id}.${fileFormat.toString().lowercase()}"
            val reportFile = ReportFile().setSchemaTopic(topic).setReportId(report.id)
                .setActionId(actionId).setSchemaName("").setBodyFormat(fileFormat.toString()).setItemCount(1)
                .setExternalName("test-external-name")
                .setBodyUrl(report.bodyURL)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                reportFile, txn, action
            )
            if (childReport != null) {
                ReportStreamTestDatabaseContainer.testDatabaseAccess
                    .insertReportLineage(
                        ReportLineage(
                            taskIndex,
                            actionId,
                            report.id,
                            childReport.id,
                            OffsetDateTime.now()
                        ),
                        txn
                    )
            }

            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertTask(
                report,
                fileFormat.toString().lowercase(),
                report.bodyURL,
                nextAction = ProcessEvent(
                    nextEventAction,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                ),
                txn
            )
        }

        return report
    }

    @BeforeEach
    fun beforeEach() {
        unmockkAll()
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    /*
    Send a FHIR message to an HL7v2 receiver and ensure the message receiver receives is translated to HL7v2
     */
    @Test
    fun `test successfully processes a translate message when isSendOriginal is false`() {
        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata

        // upload reports
        val receiveBlobName = "receiveBlobName"
        val translateFhirBytes = File(
            MULTIPLE_TARGETS_FHIR_PATH
        ).readBytes()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            receiveBlobName,
            translateFhirBytes,
            blobContainerMetadata
        )

        // Seed the steps backwards so report lineage can be correctly generated
        val translateReport = seedTask(
            Report.Format.FHIR,
            TaskAction.translate,
            TaskAction.send,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            100,
            oneOrganization
        )
        val routeReport = seedTask(
            Report.Format.FHIR,
            TaskAction.route,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.ELR_ELIMS,
            99,
            oneOrganization,
            translateReport
        )
        val convertReport = seedTask(
            Report.Format.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            Topic.ELR_ELIMS,
            98,
            oneOrganization,
            routeReport
        )
        val receiveReport = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.ELR_ELIMS,
            97,
            oneOrganization,
            convertReport,
            receiveBlobUrl
        )

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRTranslator(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess))
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess.BlobContainerMetadata)
        every { BlobAccess.BlobContainerMetadata.build("metadata", any()) } returns BlobAccess.BlobContainerMetadata(
            "metadata",
            blobConnectionString
        )

        // The topic param of queueMessage is what should determine how the Translate function runs
        val queueMessage = "{\"type\":\"translate\",\"reportId\":\"${translateReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        translateFhirBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"full-elr\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )

        fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

        // verify task and report_file tables were updated correctly in the Translate function (new task and new
        // record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val queueTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.batch))
                .fetchOneInto(Task.TASK)
            assertThat(queueTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(
                        gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                            .eq(queueTask!!.reportId)
                    )
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify sendReportFile message does not match the original message from receive step
            assertThat(BlobAccess.downloadBlobAsByteArray(sendReportFile!!.bodyUrl, blobContainerMetadata))
                .isNotEqualTo(BlobAccess.downloadBlobAsByteArray(receiveReport.bodyURL, blobContainerMetadata))
        }

        // verify we did not call the sendOriginal function
        verify(exactly = 0) {
            fhirEngine.sendOriginal(any(), any(), any())
        }

        // verify we called the sendTranslated function
        verify(exactly = 1) {
            fhirEngine.sendTranslated(any(), any(), any())
        }

        // verify sendMessage did not get called because next action should be Batch
        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
    }

    /*
    Send a FHIR message to an HL7v2 receiver and ensure the message receiver receives is the original FHIR and NOT
    translated to HL7v2
     */
    @Test
    fun `test successfully processes a translate message when isSendOriginal is true`() {
        // set up and seed azure blobstore
        val blobConnectionString =
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10000
                )
            }/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${
                azuriteContainer.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        val blobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )

        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns blobContainerMetadata

        // upload reports
        val receiveBlobName = "receiveBlobName"
        val translateFhirBytes = File(
            MULTIPLE_TARGETS_FHIR_PATH
        ).readBytes()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            receiveBlobName,
            translateFhirBytes,
            blobContainerMetadata
        )

        // Seed the steps backwards so report lineage can be correctly generated
        val translateReport = seedTask(
            Report.Format.FHIR,
            TaskAction.translate,
            TaskAction.send,
            Event.EventAction.SEND,
            Topic.ELR_ELIMS,
            100,
            oneOrganization
        )
        val routeReport = seedTask(
            Report.Format.FHIR,
            TaskAction.route,
            TaskAction.translate,
            Event.EventAction.TRANSLATE,
            Topic.ELR_ELIMS,
            99,
            oneOrganization,
            translateReport
        )
        val convertReport = seedTask(
            Report.Format.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            Topic.ELR_ELIMS,
            98,
            oneOrganization,
            routeReport
        )
        val receiveReport = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.ELR_ELIMS,
            97,
            oneOrganization,
            convertReport,
            receiveBlobUrl
        )

        val settings = FileSettings().loadOrganizations(oneOrganization)
        val fhirEngine = spyk(
            FHIRTranslator(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess,
                reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess))
            )
        )

        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val workflowEngine =
            makeWorkflowEngine(
                UnitTestUtils.simpleMetadata,
                settings,
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )

        mockkObject(QueueMessage)
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit

        // The topic param of queueMessage is what should determine how the Translate function runs
        val queueMessage = "{\"type\":\"translate\",\"reportId\":\"${translateReport.id}\"," +
            "\"blobURL\":\"" + receiveBlobUrl +
            "\",\"digest\":\"${
                BlobAccess.digestToString(
                    BlobAccess.sha256Digest(
                        translateFhirBytes
                    )
                )
            }\",\"blobSubFolderName\":" +
            "\"ignore.ignore-full-elr\",\"schemaName\":\"\",\"topic\":\"elr-elims\"," +
            "\"receiverFullName\":\"phd.elr2\"}"

        val fhirFunc = FHIRFunctions(
            workflowEngine,

            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess
        )

        fhirFunc.doTranslate(queueMessage, 1, fhirEngine, actionHistory)

        // verify task and report_file tables were updated correctly in the Translate function
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val sendTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.send))
                .fetchOneInto(Task.TASK)
            assertThat(sendTask).isNotNull()

            val sendReportFile =
                DSL.using(txn).select(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.asterisk())
                    .from(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
                    .where(
                        gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.REPORT_ID
                            .eq(sendTask!!.reportId)
                    )
                    .fetchOneInto(gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE)
            assertThat(sendReportFile).isNotNull()

            // verify sendReportFile message matches the original message from receive step
            assertThat(BlobAccess.downloadBlobAsByteArray(sendReportFile!!.bodyUrl, blobContainerMetadata))
                .isEqualTo(BlobAccess.downloadBlobAsByteArray(receiveReport.bodyURL, blobContainerMetadata))
        }

        // verify we called the sendOriginal function
        verify(exactly = 1) {
            fhirEngine.sendOriginal(any(), any(), any())
        }

        // verify we did not call the sendTranslated function
        verify(exactly = 0) {
            fhirEngine.sendTranslated(any(), any(), any())
        }

        // verify sendMessage did get called because next action should be Send since isOriginal skips the batch
        // step
        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }
    }
}