package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.Task
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEvent
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fetchChildReports
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverEnrichment
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
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
class FHIRReceiverEnrichmentIntegrationTests : Logging {
    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    val azureEventService = InMemoryAzureEventService()

    @BeforeEach
    fun beforeEach() {
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns ""
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

    private fun createFHIRReceiverEnrichment(
        azureEventService: AzureEventService,
        org: DeepOrganization? = null,
    ): FHIRReceiverEnrichment {
        val settings = FileSettings().loadOrganizations(org ?: UniversalPipelineTestUtils.universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        return FHIRReceiverEnrichment(
            metadata,
            settings,
            reportService = ReportService(ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess)),
            azureEventService = azureEventService,
            reportStreamEventService = ReportStreamEventService(
                ReportStreamTestDatabaseContainer.testDatabaseAccess, azureEventService,
                ReportService(
                    ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                    ReportStreamTestDatabaseContainer.testDatabaseAccess
                )
            )
        )
    }

    private fun generateQueueMessage(
        report: Report,
        blobContents: String,
        sender: Sender,
        receiverName: String,
    ): String = """
            {
                "type": "${TaskAction.receiver_enrichment.literal}",
                "reportId": "${report.id}",
                "blobURL": "${report.bodyURL}",
                "digest": "${BlobUtils.digestToString(BlobUtils.sha256Digest(blobContents.toByteArray()))}",
                "blobSubFolderName": "${sender.fullName}",
                "topic": "${sender.topic.jsonVal}",
                "receiverFullName": "$receiverName" 
            }
        """.trimIndent()

    @Test
    fun `successfully add enrichments`() {
        // set up
        // the selected transform alters the software name and software version
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true"),
                format = MimeFormat.FHIR,
                enrichmentSchemaNames = listOf(
                    "classpath:/enrichments/testing.yml",
                    "classpath:/enrichments/testing2.yml"
                )
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val fhirReceiverEnrichment = createFHIRReceiverEnrichment(azureEventService, org)
        val reportContents = File(MULTIPLE_TARGETS_FHIR_PATH).readText()
        val receiveReport = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.receiver_enrichment,
            Event.EventAction.RECEIVER_ENRICHMENT,
            azuriteContainer
        )

        val queueMessage = generateQueueMessage(
            receiveReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            "phd.x"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // execute
        fhirFunctions.process(queueMessage, 1, fhirReceiverEnrichment, ActionHistory(TaskAction.receiver_enrichment))

        // no queue messages should have been sent
        verify(exactly = 1) {
            QueueAccess.sendMessage(any(), any())
        }

        // check events
        assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.ITEM_TRANSFORMED]!!).hasSize(1)
        assertThat(
            azureEventService
                .reportStreamEvents[ReportStreamEventName.ITEM_TRANSFORMED]!!.first()
        ).isInstanceOf<ReportStreamItemEvent>()
        val event = azureEventService
            .reportStreamEvents[ReportStreamEventName.ITEM_TRANSFORMED]!!.first() as ReportStreamItemEvent
        assertThat(event.params[ReportStreamEventProperties.RECEIVER_NAME]).isEqualTo("phd.x")
        val enrichments = event.params[ReportStreamEventProperties.ENRICHMENTS] as List<*>
        assertThat(enrichments).hasSize(2)
        assertThat(enrichments).isEqualTo(receiverSetupData.first().enrichmentSchemaNames)

        // check action table
        UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.receiver_enrichment))

        // verify task and report_file tables were updated correctly in the FHIRReceiverEnrichment function
        // (new task and new record file created)
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val report = fetchChildReports(receiveReport, txn, 1).single()
            assertThat(report.nextAction).isEqualTo(TaskAction.receiver_filter)
            assertThat(report.receivingOrg).isEqualTo("phd")
            assertThat(report.receivingOrgSvc).isEqualTo("x")
            assertThat(report.schemaName).isEqualTo("None")
            assertThat(report.schemaTopic).isEqualTo(Topic.FULL_ELR)
            assertThat(report.bodyFormat).isEqualTo("FHIR")

            val receiverFilterTask = DSL.using(txn).select(Task.TASK.asterisk()).from(Task.TASK)
                .where(Task.TASK.NEXT_ACTION.eq(TaskAction.receiver_filter))
                .fetchOneInto(Task.TASK)

            // verify receiver filter queue task exists
            assertThat(receiverFilterTask).isNotNull()
            assertThat(receiverFilterTask!!.reportId).isEqualTo(report.reportId)

            // verify message format is FHIR and is for the expected receiver
            assertThat(receiverFilterTask.receiverName).isEqualTo("phd.x")
            assertThat(receiverFilterTask.bodyFormat).isEqualTo("FHIR")

            // verify message matches the expected output
            val blob = BlobAccess.downloadBlobAsBinaryData(
                report.bodyUrl,
                UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
            ).toString()
            val enrichedBundle = FhirTranscoder.decode(blob)
            assertThat(enrichedBundle).isNotNull()
            val software = FhirPathUtils.evaluate(
                null,
                enrichedBundle,
                enrichedBundle,
                "Bundle.entry.resource.ofType(MessageHeader).source.software"
            )
                .filterIsInstance<StringType>()
                .firstOrNull()
                ?.value
            val version = FhirPathUtils.evaluate(
                null,
                enrichedBundle,
                enrichedBundle,
                "Bundle.entry.resource.ofType(MessageHeader).source.version"
            )
                .filterIsInstance<StringType>()
                .firstOrNull()
                ?.value
            assertThat(software).isEqualTo("Purple PRIME ReportStream")
            assertThat(version).isEqualTo("0.2-YELLOW")
            val vendorOrganization = FhirPathUtils.evaluate(
                null,
                enrichedBundle,
                enrichedBundle,
                "Bundle.entry.resource.ofType(MessageHeader).source" +
                    ".extension('https://reportstream.cdc.gov/fhir/StructureDefinition/software-vendor-org').value"
            )
                .filterIsInstance<Reference>()
                .firstOrNull()
                ?.resource as org.hl7.fhir.r4.model.Organization
            assertThat(vendorOrganization.name.toString()).isEqualTo("Orange Software Vendor Name")
        }
    }
}