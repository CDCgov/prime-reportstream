package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsOnly
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilterResult
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
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
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.getObservations
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
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
import java.util.UUID
import kotlin.test.assertEquals

private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"

private const val MULTIPLE_OBSERVATIONS_FHIR_URL =
    "src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir"

// TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRRouterIntegrationTests : Logging {

    // Must have message ID, patient last name, patient first name, DOB, specimen type
    // At least one of patient street, patient zip code, patient phone number, patient email
    // At least one of order test date, specimen collection date/time, test result date
    val fullElrQualityFilterSample: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).id.exists()",
        "Bundle.entry.resource.ofType(Patient).name.family.exists()",
        "Bundle.entry.resource.ofType(Patient).name.given.count() > 0",
        "Bundle.entry.resource.ofType(Patient).birthDate.exists()",
        "Bundle.entry.resource.ofType(Specimen).type.exists()",
        "(Bundle.entry.resource.ofType(Patient).address.line.exists() or " +
            "Bundle.entry.resource.ofType(Patient).address.postalCode.exists() or " +
            "Bundle.entry.resource.ofType(Patient).telecom.exists())",
        "(" +
            "(Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or " +
            "Bundle.entry.resource.ofType(Specimen).collection.collected.exists()" +
            ") or " +
            "Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or " +
            "Bundle.entry.resource.ofType(Observation).effective.exists())",
    )

    // requires only an id exists in the message header
    val simpleElrQualifyFilter: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).id.exists()"
    )

    // patient must reside in Colorado
    val jurisdictionalFilterCo: ReportStreamFilter =
        listOf("Bundle.entry.resource.ofType(Patient).address.state='CO'")

    // patient must reside in Illinois
    val jurisdictionalFilterIl: ReportStreamFilter =
        listOf("Bundle.entry.resource.ofType(Patient).address.state='IL'")

    // Must have a processing mode set to production
    val processingModeFilterProduction: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(" +
            "system = 'http://terminology.hl7.org/CodeSystem/v2-0103'" +
            ").code.exists() " +
            "and " +
            "Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(" +
            "system = 'http://terminology.hl7.org/CodeSystem/v2-0103'" +
            ").code = 'P'"
    )

    // Must have a processing mode id of debugging
    val processingModeFilterDebugging: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(" +
            "system = 'http://terminology.hl7.org/CodeSystem/v2-0103'" +
            ").code.exists() " +
            "and " +
            "Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(" +
            "system = 'http://terminology.hl7.org/CodeSystem/v2-0103'" +
            ").code = 'D'"
    )

    // only allow observations that have 94558-5.
    val observationFilter: ReportStreamFilter = listOf(
        "%resource.code.coding.code='94558-5'"
    )

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
        val jurisdictionalFilter: List<String> = emptyList(),
        val qualityFilter: List<String> = emptyList(),
        val routingFilter: List<String> = emptyList(),
        val processingModeFilter: List<String> = emptyList(),
        val conditionFilter: List<String> = emptyList(),
        val reverseQuality: Boolean = false,
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
                jurisdictionalFilter = it.jurisdictionalFilter,
                qualityFilter = it.qualityFilter,
                routingFilter = it.routingFilter,
                processingModeFilter = it.processingModeFilter,
                conditionFilter = it.conditionFilter,
                reverseTheQualityFilter = it.reverseQuality
            )
        }
    }

    private fun createOrganizationWithReceivers(receiverList: List<Receiver>): DeepOrganization {
        return DeepOrganization(
            "phd",
            "test",
            Organization.Jurisdiction.FEDERAL,
            senders = listOf(
                hl7Sender,
                fhirSender,
                hl7SenderWithNoTransform,
                fhirSenderWithNoTransform,
                senderWithValidation
            ),
            receivers = receiverList
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
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;
                    BlobEndpoint=http://${azuriteContainer.host}:${

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
        fileFormat: MimeFormat,
        currentAction: TaskAction,
        nextAction: TaskAction,
        nextEventAction: Event.EventAction,
        topic: Topic,
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
                            0,
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
            MimeFormat.FHIR,
            TaskAction.convert,
            TaskAction.route,
            Event.EventAction.ROUTE,
            topic,
            null,
            convertedBlobUrl
        )

        val receiveReport = createReport(
            MimeFormat.FHIR,
            TaskAction.receive,
            TaskAction.convert,
            Event.EventAction.CONVERT,
            topic,
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

    private data class ActionLogRecordContent(
        val index: Int,
        val reportId: UUID?,
        val type: ActionLogLevel?,
        val scope: ActionLogScope?,
        val actionLogDetail: ActionLogDetail?,
    )

    // https://github.com/CDCgov/prime-reportstream/issues/14450
    private val expectedButStrangeObservationFilterEntry = listOf(
        ActionLogRecordContent(
            1,
            reportId = null,
            scope = ActionLogScope.report,
            type = ActionLogLevel.info,
            actionLogDetail = null
        )
    )

    private fun checkActionLogTable(expectedContentList: List<ActionLogRecordContent>) {
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)

            assert(actionLogRecords.size >= expectedContentList.size)

            for (expectedActionLogRecordContent in expectedContentList) {
                // -1 because the indexing is not zero-based and we need to modify it to address the right element
                // in the actionLogRecords array
                val actualIndex = expectedActionLogRecordContent.index - 1
                val actualActionLogRecord = actionLogRecords[actualIndex]

                assertEquals(expectedActionLogRecordContent.reportId, actualActionLogRecord.reportId)
                assertEquals(expectedActionLogRecordContent.type, actualActionLogRecord.type)
                assertEquals(expectedActionLogRecordContent.scope, actualActionLogRecord.scope)

                expectedActionLogRecordContent.actionLogDetail?.let {
                    val expected = expectedActionLogRecordContent.actionLogDetail
                    assertEquals(expected.javaClass, actualActionLogRecord.detail.javaClass)
                    assertEquals(expected.scope, actualActionLogRecord.detail.scope)
                    assertEquals(expected.message, actualActionLogRecord.detail.message)
                    assertEquals(expected.errorCode, actualActionLogRecord.detail.errorCode)
                }
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
        // TODO consider not mocking DatabaseLookupTableAccess
        mockkConstructor(DatabaseLookupTableAccess::class)
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `should send valid FHIR report only to receivers listening to full-elr`() {
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
        val receiverList = createReceivers(
            listOf(
                ReceiverSetupData(
                    "x",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true")
                ),
                ReceiverSetupData(
                    "y",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true")
                ),
                ReceiverSetupData(
                    "z",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true"),
                    topic = Topic.TEST
                )
            )
        )

        val org = createOrganizationWithReceivers(receiverList)

        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // did the report get pushed to blob store correctly and intact?
            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 2)
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
            val expectedRouteQueueMessages = reportAndBundles.flatMap { (report, fhirBundle) ->
                listOf(
                    FhirTranslateQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                        "phd.fhir-elr-no-transform",
                        fhirSenderWithNoTransform.topic,
                        "phd.x"
                    ),
                    FhirTranslateQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                        "phd.fhir-elr-no-transform",
                        fhirSenderWithNoTransform.topic,
                        "phd.y"
                    )
                )
            }.map {
                it.serialize()
            }

            verify(exactly = 2) {
                QueueAccess.sendMessage(
                    elrTranslationQueueName,
                    match {
                        expectedRouteQueueMessages.contains(it)
                    }
                )
            }

            // make sure action table has a new entry
            checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))

            // ACTION_LOG table is expected to have a record with no record_id identifying an observation filter action
            // that may or may not have happened.
            checkActionLogTable(expectedButStrangeObservationFilterEntry)
        }
    }

    @Test
    fun `should send valid FHIR report filtered by condition code 94558-5`() {
        // set up
        val reportContents =
            listOf(
                File(MULTIPLE_OBSERVATIONS_FHIR_URL).readText()
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receiverSetupData = listOf(
            ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = observationFilter
            ),
            ReceiverSetupData(
                "y",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true")
            ),
        )
        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // did the report get pushed to blob store correctly and intact?
            val routedReports = verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 2)
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }

            val fhirBundlesAsObjectsOnly = reportAndBundles.map { it.second.toString(Charsets.UTF_8) }
                .map { FhirTranscoder.decode(it) }

            val fhirBundleReceiverX = fhirBundlesAsObjectsOnly[0]
            val fhirBundleReceiverY = fhirBundlesAsObjectsOnly[1]

            // there should only be one observation of five remaining, and the code of that observation
            // should be 94558-5
            assertEquals(1, fhirBundleReceiverX.getObservations().size)
            assertEquals(1, fhirBundleReceiverX.getObservations()[0].code.coding.size)
            assertThat(fhirBundleReceiverX.getObservations()[0].code.coding[0].code.equals("94558-5"))

            // for receiver Y all five observations should be intact
            assertEquals(5, fhirBundleReceiverY.getObservations().size)
            val expectedCodes = listOf("94558-5", "95418-0", "95417-2", "95421-4", "95419-8")
            for (i in 0..<fhirBundleReceiverY.getObservations().size) {
                // in this bundle the array "coding" in every "Observation" only ever has one element
                assertEquals(1, fhirBundleReceiverY.getObservations()[i].code.coding.size)
                assertEquals(expectedCodes[i], fhirBundleReceiverY.getObservations()[i].code.coding[0].code)
            }
            assertThat(fhirBundleReceiverY.getObservations()[0].code.coding[0].code.equals("94558-5"))

            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.flatMap { (report, fhirBundle) ->
                listOf(
                    FhirTranslateQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                        "phd.fhir-elr-no-transform",
                        fhirSenderWithNoTransform.topic,
                        "phd.x"
                    ),
                    FhirTranslateQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                        "phd.fhir-elr-no-transform",
                        fhirSenderWithNoTransform.topic,
                        "phd.y"
                    )
                )
            }.map {
                it.serialize()
            }

            verify(exactly = 2) {
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

    @Test
    fun `should respect jurisdictional filter and send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receivers = createReceivers(listOf(ReceiverSetupData("x", jurisdictionalFilter = jurisdictionalFilterCo)))
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

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
            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirTranslateQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    "phd.fhir-elr-no-transform",
                    fhirSenderWithNoTransform.topic,
                    "phd.x"
                )
            }.map {
                it.serialize()
            }

            // filter should permit message and should not mangle message
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

            // check that the ACTION_LOG table has the expected but strange entry
            checkActionLogTable(expectedButStrangeObservationFilterEntry)
        }
    }

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
        val receiverSetupData = listOf(ReceiverSetupData("x", jurisdictionalFilter = jurisdictionalFilterIl))
        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))

        // we don't log applications of jurisdictional filter to ACTION_LOG at this time
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)

            assert(actionLogRecords.isEmpty())
        }

        // we don't log jurisdictional filter actions
        checkActionLogTable(listOf())
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
        val receiverSetupData = listOf(
            ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                routingFilter = listOf("true"),
                qualityFilter = fullElrQualityFilterSample
            )
        )
        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)

            assert(actionLogRecords.size == 1)

            val actionLogRecord = actionLogRecords[0]
            assertEquals(actionLogRecord.reportId, convertReport.id)
            assertEquals(actionLogRecord.type, ActionLogLevel.filter)
            assertEquals(actionLogRecord.scope, ActionLogScope.translation)
        }

        // ACTION_LOG should have an entry for the filter action along with the weird observation filter one
        val expectedContentList = listOf(
            ActionLogRecordContent(
                index = 1,
                reportId = convertReport.id,
                type = ActionLogLevel.filter,
                scope = ActionLogScope.translation,
                actionLogDetail = null
            )
        )
        checkActionLogTable(expectedContentList)
    }

    @Test
    fun `should respect simple quality filter and send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receiverSetupData = listOf(
            ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                routingFilter = listOf("true"),
                qualityFilter = simpleElrQualifyFilter
            )
        )

        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

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
            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirTranslateQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    "phd.fhir-elr-no-transform",
                    fhirSenderWithNoTransform.topic,
                    "phd.x"
                )
            }.map {
                it.serialize()
            }

            // filter should permit message and should not mangle message
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

            // check that the ACTION_LOG table has the expected but strange entry
            checkActionLogTable(expectedButStrangeObservationFilterEntry)
        }
    }

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
        val receiverSetupData = listOf(
            ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                routingFilter = listOf("true"),
                qualityFilter = simpleElrQualifyFilter,
                reverseQuality = true
            )
        )
        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))

        // ACTION_LOG should have an entry for the filter action
        val detail = ReportStreamFilterResult(
            receiverName = "phd.x",
            originalCount = 1,
            filterName = "(reversed) [Bundle.entry.resource.ofType(MessageHeader).id.exists()]",
            filterArgs = listOf(),
            filteredTrackingElement = "MT_COCNB_ORU_NBPHELR.1.5348467",
            filterType = ReportStreamFilterType.QUALITY_FILTER
        )

        val expectedContentList = listOf(
            ActionLogRecordContent(
                index = 1,
                reportId = convertReport.id,
                type = ActionLogLevel.filter,
                scope = ActionLogScope.translation,
                actionLogDetail = detail
            )
        )
        checkActionLogTable(expectedContentList)
    }

    @Test
    fun `should respect processing mode filter and send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = createReportsWithLineage(reportContents)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = generateQueueMessage(convertReport, reportContents, fhirSenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive))

        // execute
        val receiverSetupData = listOf(
            ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                processingModeFilter = processingModeFilterProduction
            )
        )
        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

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
            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirTranslateQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    "phd.fhir-elr-no-transform",
                    fhirSenderWithNoTransform.topic,
                    "phd.x"
                )
            }.map {
                it.serialize()
            }

            // filter should permit message and should not mangle message
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

            // check that the ACTION_LOG table has the expected but strange entry
            checkActionLogTable(expectedButStrangeObservationFilterEntry)
        }
    }

    @Test
    fun `should respect processing mode filter and not send message`() {
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
        val receiverSetupData = listOf(
            ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                processingModeFilter = processingModeFilterDebugging,
            )
        )

        val receivers = createReceivers(receiverSetupData)
        val org = createOrganizationWithReceivers(receivers)
        val fhirRouter = createFHIRRouter(org)
        //fhirFunctions.doRoute(queueMessage, 1, fhirRouter)
        fhirFunctions.process(queueMessage, 1, fhirRouter, ActionHistory(TaskAction.route))

        // check results
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        checkActionTable(listOf(TaskAction.convert, TaskAction.receive, TaskAction.route))

        // ACTION_LOG should have an entry for the filter action
        val detail = ReportStreamFilterResult(
            receiverName = "phd.x",
            originalCount = 1,
            filterName =
            "[Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(system = " +
                "'http://terminology.hl7.org/CodeSystem/v2-0103').code.exists() and " +
                "Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(system = " +
                "'http://terminology.hl7.org/CodeSystem/v2-0103').code = 'D']",
            filterArgs = listOf(),
            filteredTrackingElement = "MT_COCNB_ORU_NBPHELR.1.5348467",
            filterType = ReportStreamFilterType.PROCESSING_MODE_FILTER
        )

        val expectedContentList = listOf(
            ActionLogRecordContent(
                index = 1,
                reportId = convertReport.id,
                type = ActionLogLevel.filter,
                scope = ActionLogScope.translation,
                actionLogDetail = detail
            )
        )
        checkActionLogTable(expectedContentList)
    }
}