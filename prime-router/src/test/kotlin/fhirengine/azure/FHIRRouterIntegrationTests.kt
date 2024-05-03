package gov.cdc.prime.router.fhirengine.azure

// import assertk.assertThat
// import assertk.assertions.containsOnly
// import assertk.assertions.each
// import assertk.assertions.hasSize
// import assertk.assertions.matchesPredicate
// import gov.cdc.prime.router.*
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.matchesPredicate
import gov.cdc.prime.router.*
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.ActionLogType
// import gov.cdc.prime.router.azure.db.Tables
// import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.*
// import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7SenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles
// import gov.cdc.prime.router.common.UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles
// import gov.cdc.prime.router.common.badEncodingHL7Record
// import gov.cdc.prime.router.common.cleanHL7Record
// import gov.cdc.prime.router.common.cleanHL7RecordConverted
// import gov.cdc.prime.router.common.invalidHL7Record
// import gov.cdc.prime.router.common.invalidHL7RecordConverted
// import gov.cdc.prime.router.common.unparseableHL7Record
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.FhirRouteQueueMessage
import gov.cdc.prime.router.history.DetailedActionLog
// import gov.cdc.prime.router.fhirengine.engine.FhirRouteQueueMessage
// import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
// import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.*
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.impl.DSL
// import io.mockk.verify
// import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.*

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val TOPIC_TEST_ORG_NAME = "topic-test"
private const val QUALITY_TEST_URL = "src/test/resources/fhirengine/engine/routerDefaults/qual_test_0.fhir"
private const val OBSERVATION_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Observation).count() > 0"
private const val PROVENANCE_COUNT_GREATER_THAN_ZERO = "Bundle.entry.resource.ofType(Provenance).count() > 0"
private const val PROVENANCE_COUNT_EQUAL_TO_TEN = "Bundle.entry.resource.ofType(Provenance).count() = 10"
private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val BLOB_URL = "https://blob.url"
private const val BLOB_SUB_FOLDER_NAME = "test-sender"
private const val BODY_URL = "https://anyblob.com"
private const val PERFORMER_OR_PATIENT_CA = "(%performerState.exists() and %performerState = 'CA') " +
    "or (%patientState.exists() and %patientState = 'CA')"
private const val PROVENANCE_COUNT_GREATER_THAN_10 = "Bundle.entry.resource.ofType(Provenance).count() > 10"
private const val EXCEPTION_FOUND = "exception found"
private const val CONDITION_FILTER = "%resource.code.coding.code = '95418-0'"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRRouterIntegrationTests : Logging {

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    private fun createFHIRFunctionsInstance(): FHIRFunctions {
        val settings = FileSettings().loadOrganizations(universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val workflowEngine = WorkflowEngine
            .Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            .build()
        return FHIRFunctions(workflowEngine, databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess)
    }

    private fun createFHIRRouter(): FHIRRouter {
        val settings = FileSettings().loadOrganizations(universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        return FHIRRouter(
            metadata,
            settings,
            reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess)),
        )
    }

    private fun createReport(
        format: Report.Format,
        sender: Sender,
        receiveReportBlobUrl: String,
        itemCount: Int,
    ): Report {
        return ReportStreamTestDatabaseContainer.testDatabaseAccess.transactReturning { txn ->
            val report = Report(
                format,
                emptyList(),
                itemCount,
                metadata = UnitTestUtils.simpleMetadata,
                nextAction = TaskAction.convert,
                topic = sender.topic,
            )
            report.bodyURL = receiveReportBlobUrl
            val receiveAction = Action().setActionName(TaskAction.receive)
            val receiveActionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, receiveAction)
            val reportFile = ReportFile()
                .setSchemaTopic(sender.topic)
                .setReportId(report.id)
                .setActionId(receiveActionId)
                .setSchemaName("")
                .setBodyFormat(sender.format.toString())
                .setItemCount(itemCount)
                .setExternalName("test-external-name")
                .setBodyUrl(receiveReportBlobUrl)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                reportFile, txn, receiveAction
            )
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertTask(
                report,
                format.toString().lowercase(),
                report.bodyURL,
                nextAction = ProcessEvent(
                    Event.EventAction.ROUTE,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                ),
                txn
            )

            report
        }
    }

    private fun generateQueueMessage(report: Report, blobContents: String, sender: Sender): String {
        return """
            {
                "type": "route",
                "reportId": "${report.id}",
                "blobURL": "${report.bodyURL}",
                "digest": "${BlobAccess.digestToString(BlobAccess.sha256Digest(blobContents.toByteArray()))}",
                "blobSubFolderName": "${sender.fullName}",
                "topic": "${sender.topic.jsonVal}",
                "schemaName": "${sender.schemaName}" 
            }
        """.trimIndent()
    }

    private fun getBlobContainerMetadata(): BlobAccess.BlobContainerMetadata {
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
        return BlobAccess.BlobContainerMetadata(
            "container1",
            blobConnectionString
        )
    }

    private fun seedTask(
        fileFormat: Report.Format,
        currentAction: TaskAction,
        nextAction: TaskAction,
        nextEventAction: Event.EventAction,
        topic: Topic,
        taskIndex: Long = 0,
        childReport: Report? = null,
        bodyURL: String? = null,
    ): Report {

        val report = Report(
            fileFormat,
            listOf(ClientSource(organization = universalPipelineOrganization.name, client = "Test Sender")),
            1,
            metadata = UnitTestUtils.simpleMetadata,
            nextAction = nextAction,
            topic = topic
        )
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val action = Action().setActionName(currentAction)
            val actionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            report.bodyURL = bodyURL ?: "http://${report.id}.${fileFormat.toString().lowercase()}"

            val reportFile = ReportFile().setSchemaTopic(topic)
                                         .setReportId(report.id)
                                         .setActionId(actionId)
                                         .setSchemaName("")
                                         .setBodyFormat(fileFormat.toString())
                                         .setItemCount(1)
                                         .setExternalName("test-external-name")
                                         .setBodyUrl(report.bodyURL)
                                         .setSendingOrg(universalPipelineOrganization.name)
                                         .setSendingOrgClient("Test Sender")

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
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns getBlobContainerMetadata()
        mockkObject(BlobAccess.BlobContainerMetadata)
        every { BlobAccess.BlobContainerMetadata.build(any(), any()) } returns getBlobContainerMetadata()
        mockkConstructor(DatabaseLookupTableAccess::class)
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `should tralalalalala down the happiest of happy paths`() {
        val reportContents =
            listOf(
                validFHIRRecord1
            ).joinToString()

        val receivedBlobUrl = BlobAccess.uploadBlob(
            "receive/tralalalalala.fhir",
            reportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val convertedBlobUrl = BlobAccess.uploadBlob(
            "convert/tralalalalala.fhir",
            reportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        // Seed the steps backwards so report lineage can be correctly generated

        val convertReport = seedTask(
            Report.Format.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            Topic.FULL_ELR,
            0,
            null,
            convertedBlobUrl
        )

        val receiveReport = seedTask(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            Topic.FULL_ELR,
            0,
            convertReport,
            receivedBlobUrl
        )
logger.info("receiveReport: ${receiveReport.id}")
logger.info("convertReport: ${convertReport.id}")
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()
        fhirFunctions.doRoute(queueMessage, 1, createFHIRRouter())

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 1)
logger.info("number of routedReports: ${routedReports.size}")
logger.info("routed report index zero id: ${routedReports.get(0).reportId}")


logger.info("routedReports is: ${routedReports}")

            val reportAndBundles =
                routedReports.map {
                    logger.info("it.bodyUrl is: ${it.bodyUrl}")
                    logger.info("BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata()).toString(Charsets.UTF_8)")
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }
logger.info("reportAndBundles is: ${reportAndBundles}")

            assertThat(reportAndBundles).transform { pairs -> pairs.map { it.second } }.each {
                it.matchesPredicate { bytes ->
logger.info("bytes as str: ${bytes.toString(Charsets.UTF_8)}")
logger.info("validFHIRRecord1 as str: $validFHIRRecord1")
                    val result = CompareData().compare(
                        bytes.inputStream(),
                        validFHIRRecord1.byteInputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    result.passed
                }
            }

//            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
//                FhirRouteQueueMessage(
//                    report.reportId,
//                    report.bodyUrl,
//                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
//                    fhirSenderWithNoTransform.fullName,
//                    fhirSenderWithNoTransform.topic
//                )
//            }.map { it.serialize() }
//
//            verify(exactly = 1) {
//                QueueAccess.sendMessage("elr-fhir-translate", match {
//                    logger.info("it is: ${it}")
//                    logger.info("expectedRouteQueueMessages: $expectedRouteQueueMessages")
//                    expectedRouteQueueMessages.contains(it)
//                })
//            }


        }
    }
}