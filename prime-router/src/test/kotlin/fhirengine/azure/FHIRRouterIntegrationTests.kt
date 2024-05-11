package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsAtLeast
import assertk.assertions.containsOnly
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSender
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7Sender
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7SenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.senderWithValidation
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles
import gov.cdc.prime.router.common.validFHIRRecord1
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import gov.cdc.prime.router.fhirengine.engine.FhirTranslateQueueMessage
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
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
import kotlin.test.assertEquals

private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/fhir_without_birth_time.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRRouterIntegrationTests : Logging {

    /**
     *   Quality filter sample for receivers on FULL_ELR topic:
     *   Must have message ID, patient last name, patient first name, DOB, specimen type
     *   At least one of patient street, patient zip code, patient phone number, patient email
     *   At least one of order test date, specimen collection date/time, test result date
     */
    val fullElrQualityFilterSample: ReportStreamFilter = listOf(
        "%messageId.exists()",
        "%patient.name.family.exists()",
        "%patient.name.given.count() > 0",
        "%patient.birthDate.exists()",
        "%specimen.type.exists()",
        "(%patient.address.line.exists() or " +
                "%patient.address.postalCode.exists() or " +
                "%patient.telecom.exists())",
        "(" +
                "(%specimen.collection.collectedPeriod.exists() or " +
                "%specimen.collection.collected.exists()" +
                ") or " +
                "%serviceRequest.occurrence.exists() or " +
                "%observation.effective.exists())",
    )

    val simpleElrQualifyFilter: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).id.exists()"
    )

    val jurisdictionalFilterTx: ReportStreamFilter =
        listOf("Bundle.entry.resource.ofType(Patient).address.state='TX'")

    val jurisdictionalFilterIl: ReportStreamFilter =
        listOf("Bundle.entry.resource.ofType(Patient).address.state='IL'")

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    data class ReceiverSetupData(
        val name: String,
        val orgName: String = "phd",
        val topic: Topic = Topic.FULL_ELR,
        val filter: List<String> = listOf("true"),
        val reverseQuality: Boolean = false
    )

    private fun createReceivers(receiverSetupDataList: List<ReceiverSetupData>): List<Receiver> {
        return receiverSetupDataList.map {
            Receiver(
                it.name,
                it.orgName,
                it.topic,
                CustomerStatus.ACTIVE,
                "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml",
                timing = Receiver.Timing(numberPerDay = 1, maxReportCount = 1, whenEmpty = Receiver.WhenEmpty()),
                jurisdictionalFilter = listOf("true"),
                processingModeFilter = listOf("true"),
                qualityFilter = it.filter, // it doesn't matter to the engine how the filter gets applied,
                reverseTheQualityFilter = it.reverseQuality
            )
        }
    }

    private fun createOrganizationWithReceivers(receiverList: List<Receiver>): DeepOrganization {
        return DeepOrganization(
            "phd", "test", Organization.Jurisdiction.FEDERAL,
            senders = listOf(
                hl7Sender,
                fhirSender,
                hl7SenderWithNoTransform,
                fhirSenderWithNoTransform,
                senderWithValidation
            ),
            receivers = receiverList
//            listOf(
//                Receiver(
//                    "elr2",
//                    "phd",
//                    Topic.FULL_ELR,
//                    CustomerStatus.ACTIVE,
//                    "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml",
//                    timing = Receiver.Timing(numberPerDay = 1, maxReportCount = 1, whenEmpty = Receiver.WhenEmpty()),
//                    jurisdictionalFilter = listOf("true"),
//                    processingModeFilter = listOf("true"),
//                    qualityFilter = filter, // it doesn't matter to the engine how the filter gets applied,
//                    reverseTheQualityFilter = reverseQuality
//                )
//            ),
        )
    }

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

    private fun createFHIRRouter(
        org: DeepOrganization? = null,
    ): FHIRRouter {
        val settings = FileSettings().loadOrganizations(org ?: universalPipelineOrganization)
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

    private fun createReport(
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

    private fun createReportsWithLineage(
        reportContents: String,
        topic: Topic = Topic.FULL_ELR,
    ): Pair<Report, Report> {
        val receivedBlobUrl = BlobAccess.uploadBlob(
            "receive/mr_fhir_face.fhir",
            reportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val convertedBlobUrl = BlobAccess.uploadBlob(
            "convert/mr_fhir_face.fhir",
            reportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val convertReport = createReport(
            Report.Format.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            topic,
            0,
            null,
            convertedBlobUrl
        )

        val receiveReport = createReport(
            Report.Format.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            topic,
            0,
            convertReport,
            receivedBlobUrl
        )

        return Pair(receiveReport, convertReport)
    }

    private fun checkActionTable(expectedTaskActions: List<TaskAction>) {
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionRecords = DSL.using(txn)
                                   .select(Tables.ACTION.asterisk())
                                   .from(Tables.ACTION)
                                   .fetchInto(
                                       Action::class.java
                                   )

            for (i in 0 until actionRecords.size) {
                assertEquals(expectedTaskActions.get(i), actionRecords.get(i).actionName)
            }
        }
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
        unmockkAll()
    }

    @Test
    fun `should send valid FHIR report only to receiver listening to full-elr`() {
        // set up
        val reportContents =
            listOf(
                validFHIRRecord1
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        fhirFunctions.doRoute(queueMessage, 1, createFHIRRouter())

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // did the report get pushed to blob store correctly and intact?
            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 1)
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }

            assertThat(reportAndBundles).transform { pairs ->
                pairs.map {
                    it.second.toString(Charsets.UTF_8)
                }
            }.containsOnly(validFHIRRecord1)

            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirTranslateQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    "phd.fhir-elr-no-transform",
                    fhirSenderWithNoTransform.topic,
                    "phd.elr2"
                )
            }.map {
                it.serialize()
            }

            verify(exactly = 1) {
                QueueAccess.sendMessage(
                    elrTranslationQueueName,
                    match {
                        expectedRouteQueueMessages.contains(it)
                    }
                )
            }

            // make sure action table has a new entry
            checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
        }
    }

//    @Test
//    fun `should send valid FHIR report only to receivers listening to full-elr`() {
//        // set up
//        val reportContents =
//            listOf(
//                validFHIRRecord1
//            ).joinToString()
//
//        val reportPair = createReportsWithLineage(reportContents)
//        val receiveReport = reportPair.first
//        val convertReport = reportPair.second
//        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
//        val fhirFunctions = createFHIRFunctionsInstance()
//
//        // make sure action table has only what we put in there
//        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))
//
//        // execute
//        val receiverList = createReceivers(
//            listOf(
//                ReceiverSetupData("x", topic = Topic.TEST),
//                ReceiverSetupData("y"),
//                ReceiverSetupData("z")
//            )
//        )
//
//        val org = createOrganizationWithReceivers(receiverList)
//
//        val fhirRouter = createFHIRRouter(org)
//        fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
//
//        // check results
//        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
//            // did the report get pushed to blob store correctly and intact?
//            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 2)
//            val reportAndBundles =
//                routedReports.map {
//                    Pair(
//                        it,
//                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
//                    )
//                }
//
//            assertThat(reportAndBundles).transform { pairs ->
//                pairs.map {
//                    it.second.toString(Charsets.UTF_8)
//                }
//            }.containsOnly(validFHIRRecord1)
//
//            // is the queue messaging what we expect it to be?
//            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
//                FhirTranslateQueueMessage(
//                    report.reportId,
//                    report.bodyUrl,
//                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
//                    "phd.fhir-elr-no-transform",
//                    fhirSenderWithNoTransform.topic,
//                    "phd.elr2"
//                )
//            }.map {
//                it.serialize()
//            }
//
//            verify(exactly = 2) {
//                QueueAccess.sendMessage(
//                    elrTranslationQueueName,
//                    match {
//                        expectedRouteQueueMessages.contains(it)
//                    }
//                )
//            }
//
//            // make sure action table has a new entry
//            checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
//        }
//    }

//    @Test
//    fun `should respect jurisdictional filter and send message`() {
//        // set up
//        val reportContents =
//            listOf(
//                File(VALID_FHIR_URL).readText()
//            ).joinToString()
//
//        val reportPair = createReportsWithLineage(reportContents)
//        val receiveReport = reportPair.first
//        val convertReport = reportPair.second
//        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
//        val fhirFunctions = createFHIRFunctionsInstance()
//
//        // make sure action table has only what we put in there
//        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))
//
//        // execute
//        val receivers = createReceivers(listOf(ReceiverSetupData("x", filter = jurisdictionalFilterTx)))
//        val org = createOrganizationWithReceivers(receivers)
//        val fhirRouter = createFHIRRouter(org)
//        fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
//
//        // check results
//        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
//
//            // did the report get pushed to blob store correctly and intact?
//            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 1)
//            val reportAndBundles =
//                routedReports.map {
//                    Pair(
//                        it,
//                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
//                    )
//                }
//            // is the queue messaging what we expect it to be?
//            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
//                FhirTranslateQueueMessage(
//                    report.reportId,
//                    report.bodyUrl,
//                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
//                    "phd.fhir-elr-no-transform",
//                    fhirSenderWithNoTransform.topic,
//                    "x"
//                )
//            }.map {
//                it.serialize()
//            }
//
//            // filter should permit message and should not mangle message
//            verify(exactly = 1) {
//                QueueAccess.sendMessage(
//                    elrTranslationQueueName,
//                    match {
//                        expectedRouteQueueMessages.contains(it)
//                    }
//                )
//            }
//
//            // make sure action table has a new entry
//            checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
//        }
//    }

    @Test
    fun `should respect jurisdictional filter and not send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receivers = createReceivers(listOf(ReceiverSetupData("x", filter = jurisdictionalFilterIl)))
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        fhirFunctions.doRoute(queueMessage, 1, fhirRouter)

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
    }

    @Test
    fun `should respect full quality filter and not send message`() {
        // set up
        val reportContents =
            listOf(
                validFHIRRecord1
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receivers = createReceivers(listOf(ReceiverSetupData("x", filter = fullElrQualityFilterSample)))
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        fhirFunctions.doRoute(queueMessage, 1, fhirRouter)

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
    }

//    @Test
//    fun `should respect simple quality filter and send message`() {
//        // set up
//        val reportContents =
//            listOf(
//                File(VALID_FHIR_URL).readText()
//            ).joinToString()
//
//        val reportPair = createReportsWithLineage(reportContents)
//        val receiveReport = reportPair.first
//        val convertReport = reportPair.second
//        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
//        val fhirFunctions = createFHIRFunctionsInstance()
//
//        // make sure action table has only what we put in there
//        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))
//
//        // execute
//        val receivers = createReceivers(listOf(ReceiverSetupData("x", filter = simpleElrQualifyFilter)))
//        val org = createOrganizationWithReceivers(receivers)
//        val fhirRouter = createFHIRRouter(org)
//        fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
//
//        // check results
//        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
//
//            // did the report get pushed to blob store correctly and intact?
//            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 1)
//            val reportAndBundles =
//                routedReports.map {
//                    Pair(
//                        it,
//                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
//                    )
//                }
//            // is the queue messaging what we expect it to be?
//            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
//                FhirTranslateQueueMessage(
//                    report.reportId,
//                    report.bodyUrl,
//                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
//                    "phd.fhir-elr-no-transform",
//                    fhirSenderWithNoTransform.topic,
//                    "phd.elr2"
//                )
//            }.map {
//                it.serialize()
//            }
//
//            // filter should permit message and should not mangle message
//            verify(exactly = 1) {
//                QueueAccess.sendMessage(
//                    elrTranslationQueueName,
//                    match {
//                        expectedRouteQueueMessages.contains(it)
//                    }
//                )
//            }
//
//            // make sure action table has a new entry
//            checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
//        }
//    }

    @Test
    fun `should respect reversed simple quality filter and not send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receivers = createReceivers(
            listOf(ReceiverSetupData("x", filter = simpleElrQualifyFilter, reverseQuality = true))
        )
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        fhirFunctions.doRoute(queueMessage, 1, fhirRouter)

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))
    }

    @Test
    fun `should respect processing mode filter and send message`() {

    }

    @Test
    fun `should respect processing mode filter and not send message`() {

    }
}