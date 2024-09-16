package gov.cdc.prime.router.history

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.NullTransportType
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.UniversalPipelineReceiver
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BatchEvent
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.SendFunction
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.batch.UniversalBatchFunction
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.LocalAzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.azure.FHIRFunctions
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FHIRDestinationFilter
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.engine.FHIRTranslator
import gov.cdc.prime.router.history.azure.DatabaseSubmissionsAccess
import gov.cdc.prime.router.history.azure.SubmissionsFacade
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.apache.logging.log4j.kotlin.Logging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.Base64
import java.util.logging.Logger

private const val MULTIPLE_OBSERVATIONS_FHIR_URL =
    "src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class SubmissionHistoryIntegrationTests : Logging {
    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    @BeforeEach
    fun beforeEach() {
        mockkObject(QueueAccess)
        mockkObject(BlobAccess)
        every {
            BlobAccess getProperty "defaultBlobMetadata"
        } returns UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
        mockkObject(BlobAccess.BlobContainerMetadata)
        every {
            BlobAccess.BlobContainerMetadata.build(any(), any())
        } returns UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
        // TODO consider not mocking DatabaseLookupTableAccess
        mockkConstructor(DatabaseLookupTableAccess::class)
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `run full pipeline and check submission history`() {
        // set up
        val convertQueueMessages = mutableListOf<String>()
        val destinationFilterQueueMessages = mutableListOf<String>()
        val receiverFilterQueueMessages = mutableListOf<String>()
        val translateQueueMessages = mutableListOf<String>()
        val sendQueueMessages = mutableListOf<String>()
        every { QueueAccess.sendMessage(QueueMessage.elrConvertQueueName, capture(convertQueueMessages)) } returns Unit
        every {
            QueueAccess.sendMessage(QueueMessage.elrDestinationFilterQueueName, capture(destinationFilterQueueMessages))
        } returns Unit
        every {
            QueueAccess.sendMessage(QueueMessage.elrReceiverFilterQueueName, capture(receiverFilterQueueMessages))
        } returns Unit
        every {
            QueueAccess.sendMessage(QueueMessage.elrTranslationQueueName, capture(translateQueueMessages))
        } returns Unit
        every {
            QueueAccess.sendMessage(QueueMessage.elrSendQueueName, capture(sendQueueMessages))
        } returns Unit

        val reportContents = File(MULTIPLE_OBSERVATIONS_FHIR_URL).readText()
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.FHIR,
                schemaName = "",
                transportType = NullTransportType()
            ),
            UniversalPipelineTestUtils.ReceiverSetupData(
                "y",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("false"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.FHIR,
                schemaName = "",
                transportType = NullTransportType()
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val sender = UniversalPipelineSender(
            "sender",
            "org",
            MimeFormat.FHIR,
            CustomerStatus.ACTIVE,
            "classpath:/metadata/fhir_transforms/senders/baseline-sender-transform.yml",
            topic = Topic.FULL_ELR
        )

        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val settings = FileSettings().loadOrganizations(org)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val engine = WorkflowEngine(
            metadata,
            settings,
            db = ReportStreamTestDatabaseContainer.testDatabaseAccess,
            azureEventService = LocalAzureEventServiceImpl(),
            reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess)),
        )
        val fhirFunctions = FHIRFunctions(engine, databaseAccess = engine.db)
        val actionHistory = ActionHistory(TaskAction.receive)
        val submissionReceiver = UniversalPipelineReceiver(engine, actionHistory)
        actionHistory.trackActionSenderInfo(sender.fullName, "some_report")

        val report = submissionReceiver.validateAndMoveToProcessing(
            sender,
            reportContents,
            emptyMap(),
            Options.None,
            emptyList(),
            false,
            true,
            reportContents.toByteArray(),
            "some_report"
        )
        actionHistory.trackActionResult(HttpStatusCode.OK)
        engine.recordAction(actionHistory)
        val receiveAction = engine.db.fetchAction(actionHistory.action.actionId)!!

        val submissionFacade = SubmissionsFacade(DatabaseSubmissionsAccess(engine.db))
        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }

        val fhirConverter = FHIRConverter(
            metadata,
            settings,
            engine.db,
            azureEventService = engine.azureEventService,
        )

        // execute
        convertQueueMessages.forEach {
            fhirFunctions.process(
                Base64.getDecoder().decode(it).decodeToString(),
                1,
                fhirConverter,
                ActionHistory(TaskAction.convert)
            )
        }

        // check results
        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED)
        }

        val fhirDestinationFilter = FHIRDestinationFilter(
            metadata,
            settings,
            engine.db,
            azureEventService = engine.azureEventService,
            reportService = engine.reportService
        )

        destinationFilterQueueMessages.forEach {
            fhirFunctions.process(
                Base64.getDecoder().decode(it).decodeToString(),
                1,
                fhirDestinationFilter,
                ActionHistory(TaskAction.destination_filter)
            )
        }

        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }

        val fhirReceiverFilter = FHIRReceiverFilter(
            metadata,
            settings,
            engine.db,
            azureEventService = engine.azureEventService,
            reportService = engine.reportService
        )

        receiverFilterQueueMessages.forEach {
            fhirFunctions.process(
                Base64.getDecoder().decode(it).decodeToString(),
                1,
                fhirReceiverFilter,
                ActionHistory(TaskAction.receiver_filter)
            )
        }

        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            with(history.destinations.find { it.service == "y" }) {
                assertThat(this).isNotNull()
                assertThat(this!!.filteredReportRows).isNotNull().isNotEmpty()
                assertThat(this.filteredReportItems).isNotNull().isNotEmpty()
            }
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }

        // translate
        val fhirTranslator = FHIRTranslator(
            metadata,
            settings,
            engine.db,
            azureEventService = engine.azureEventService,
            reportService = engine.reportService
        )

        translateQueueMessages.forEach {
            fhirFunctions.process(
                Base64.getDecoder().decode(it).decodeToString(),
                1,
                fhirTranslator,
                ActionHistory(TaskAction.translate)
            )
        }

        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }

        // batch
        val batchFunction = UniversalBatchFunction(engine)
        val event = BatchEvent(Event.EventAction.BATCH, receivers.first().fullName, false)
        batchFunction.doBatch(
            event.toQueueMessage(),
            event,
            ActionHistory(event.eventAction.toTaskAction())
        )

        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER)
        }

        // send
        val sendFunction = SendFunction(
            engine,
            ReportStreamEventService(
                engine.db,
                engine.azureEventService,
                reportService = engine.reportService
            )
        )

        sendQueueMessages.forEach {
            sendFunction.run(
                Base64.getDecoder().decode(it).decodeToString(),
                object : ExecutionContext {
                        override fun getLogger(): Logger {
                            return Logger.getGlobal()
                        }

                        override fun getInvocationId(): String {
                            return ""
                        }

                        override fun getFunctionName(): String {
                            return "SEND_FUNCTION"
                        }
                    }
            )
        }

        engine.db.transact { txn ->
            val history = submissionFacade.findDetailedSubmissionHistory(
                txn,
                report.id,
                receiveAction
            )!!
            assertThat(history.overallStatus).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED)
        }
    }
}