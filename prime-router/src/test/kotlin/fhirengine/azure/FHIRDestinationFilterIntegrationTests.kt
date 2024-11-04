package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigestLabResult
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ItemEventData
import gov.cdc.prime.router.azure.observability.event.ReportEventData
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEvent
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fetchChildReports
import gov.cdc.prime.router.common.validFHIRRecord1
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRDestinationFilter
import gov.cdc.prime.router.fhirengine.engine.FhirReceiverFilterQueueMessage
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import gov.cdc.prime.router.version.Version
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

private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRDestinationFilterIntegrationTests : Logging {
    // patient must reside in Colorado
    val jurisdictionalFilterCo: ReportStreamFilter = listOf("Bundle.entry.resource.ofType(Patient).address.state='CO'")

    // patient must reside in Illinois
    val jurisdictionalFilterIl: ReportStreamFilter = listOf("Bundle.entry.resource.ofType(Patient).address.state='IL'")

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    val azureEventsService = InMemoryAzureEventService()

    @BeforeEach
    fun beforeEach() {
        mockkObject(QueueAccess)
        mockkObject(BlobAccess)
        mockkObject(BlobAccess.BlobContainerMetadata)

        every { QueueAccess.sendMessage(any(), any()) } returns ""
        every { BlobAccess getProperty "defaultBlobMetadata" } returns UniversalPipelineTestUtils
            .getBlobContainerMetadata(azuriteContainer)
        every { BlobAccess.BlobContainerMetadata.build(any(), any()) } returns
            UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
        mockkConstructor(DatabaseLookupTableAccess::class)
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
        azureEventsService.events.clear()
    }

    fun createDestinationFilter(
        azureEventService: AzureEventService,
        org: DeepOrganization? = null,
    ): FHIRDestinationFilter {
        val settings = FileSettings().loadOrganizations(org ?: UniversalPipelineTestUtils.universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        return FHIRDestinationFilter(
            metadata,
            settings,
            db = ReportStreamTestDatabaseContainer.testDatabaseAccess,
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

    fun generateQueueMessage(action: TaskAction, report: Report, blobContents: String, sender: Sender): String {
        return """
            {
                "type": "${action.literal}",
                "reportId": "${report.id}",
                "blobURL": "${report.bodyURL}",
                "digest": "${BlobUtils.digestToString(BlobUtils.sha256Digest(blobContents.toByteArray()))}",
                "blobSubFolderName": "${sender.fullName}",
                "topic": "${sender.topic.jsonVal}",
                "schemaName": "${sender.schemaName}" 
            }
        """.trimIndent()
    }

    @Test
    fun `should send valid FHIR report only to receivers listening to full-elr`() {
        // set up
        val reportContents = validFHIRRecord1
        val report = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.destination_filter,
            Event.EventAction.DESTINATION_FILTER,
            azuriteContainer
        )
        val queueMessage = generateQueueMessage(
            TaskAction.destination_filter,
            report,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()
        val receiverList = UniversalPipelineTestUtils.createReceivers(
            listOf(
                UniversalPipelineTestUtils.ReceiverSetupData(
                    "x",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true")
                ),
                UniversalPipelineTestUtils.ReceiverSetupData(
                    "y",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true")
                ),
                UniversalPipelineTestUtils.ReceiverSetupData(
                    "z",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true"),
                    topic = Topic.TEST
                ),
                UniversalPipelineTestUtils.ReceiverSetupData(
                    "inactive-receiver",
                    jurisdictionalFilter = listOf("true"),
                    qualityFilter = listOf("true"),
                    routingFilter = listOf("true"),
                    topic = Topic.TEST,
                    status = CustomerStatus.INACTIVE
                )
            )
        )
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receiverList)
        val destinationFilter = createDestinationFilter(azureEventsService, org)

        // execute
        fhirFunctions.process(queueMessage, 1, destinationFilter, ActionHistory(TaskAction.destination_filter))

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = fetchChildReports(report, txn, 2, 2)
            with(routedReports.first()) {
                assertThat(this.nextAction).isEqualTo(TaskAction.receiver_filter)
                assertThat(this.receivingOrg).isEqualTo("phd")
                assertThat(this.receivingOrgSvc).isEqualTo("x")
                assertThat(this.schemaName).isEqualTo("None")
                assertThat(this.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(this.bodyFormat).isEqualTo("FHIR")
            }
            with(routedReports.last()) {
                assertThat(this.nextAction).isEqualTo(TaskAction.receiver_filter)
                assertThat(this.receivingOrg).isEqualTo("phd")
                assertThat(this.receivingOrgSvc).isEqualTo("y")
                assertThat(this.schemaName).isEqualTo("None")
                assertThat(this.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(this.bodyFormat).isEqualTo("FHIR")
            }

            val routedBundles = routedReports.map {
                String(
                    BlobAccess.downloadBlobAsByteArray(
                    it.bodyUrl,
                    UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
                )
                )
            }
            assertThat(routedBundles).containsOnly(validFHIRRecord1)

            // check queue message
            val expectedRouteQueueMessages = routedReports.flatMap { report ->
                listOf(
                    FhirReceiverFilterQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobUtils.digestToString(report.blobDigest),
                        "phd.fhir-elr-no-transform",
                        UniversalPipelineTestUtils.fhirSenderWithNoTransform.topic,
                        "phd.x"
                    ),
                    FhirReceiverFilterQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobUtils.digestToString(report.blobDigest),
                        "phd.fhir-elr-no-transform",
                        UniversalPipelineTestUtils.fhirSenderWithNoTransform.topic,
                        "phd.y"
                    )
                )
            }.map {
                it.serialize()
            }

            verify(exactly = 2) {
                QueueAccess.sendMessage(
                    QueueMessage.elrReceiverFilterQueueName,
                    match {
                        expectedRouteQueueMessages.contains(it)
                    }
                )
            }

            // check events
            assertThat(azureEventsService.reportStreamEvents[ReportStreamEventName.ITEM_ROUTED]!!).hasSize(2)
            assertThat(
                azureEventsService
                .reportStreamEvents[ReportStreamEventName.ITEM_ROUTED]!!.first()
            ).isInstanceOf<ReportStreamItemEvent>()

            // check action table
            UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.destination_filter))
        }
    }

    @Test
    fun `should respect jurisdictional filter and send message`() {
        // set up
        val reportContents = File(VALID_FHIR_URL).readText()
        val report = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.destination_filter,
            Event.EventAction.DESTINATION_FILTER,
            azuriteContainer
        )
        val queueMessage = generateQueueMessage(
            TaskAction.destination_filter,
            report,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()
        val receivers = UniversalPipelineTestUtils.createReceivers(
            listOf(
                UniversalPipelineTestUtils.ReceiverSetupData(
                    "x",
                    jurisdictionalFilter = jurisdictionalFilterCo
                )
            )
        )
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val destinationFilter = createDestinationFilter(azureEventsService, org)

        // execute
        fhirFunctions.process(queueMessage, 1, destinationFilter, ActionHistory(TaskAction.destination_filter))

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReport = fetchChildReports(report, txn, 1).single()
            assertThat(routedReport.nextAction).isEqualTo(TaskAction.receiver_filter)
            assertThat(routedReport.receivingOrg).isEqualTo("phd")
            assertThat(routedReport.receivingOrgSvc).isEqualTo("x")
            assertThat(routedReport.schemaName).isEqualTo("None")
            assertThat(routedReport.schemaTopic).isEqualTo(Topic.FULL_ELR)
            assertThat(routedReport.bodyFormat).isEqualTo("FHIR")
            val routedBundle = String(
                BlobAccess.downloadBlobAsByteArray(
                routedReport.bodyUrl,
                UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
            )
            )
            assertThat(reportContents).isEqualTo(routedBundle)

            // check queue message
            val expectedQueueMessage = FhirReceiverFilterQueueMessage(
                routedReport.reportId,
                routedReport.bodyUrl,
                BlobUtils.digestToString(routedReport.blobDigest),
                "phd.fhir-elr-no-transform",
                UniversalPipelineTestUtils.fhirSenderWithNoTransform.topic,
                "phd.x"
            )

            // filter should permit message and should not mangle message
            verify(exactly = 1) {
                QueueAccess.sendMessage(
                    QueueMessage.elrReceiverFilterQueueName,
                    expectedQueueMessage.serialize()
                )
            }

            // check events
            val bundle = FhirTranscoder.decode(reportContents)
            assertThat(azureEventsService.reportStreamEvents[ReportStreamEventName.ITEM_ROUTED]!!).hasSize(1)
            assertThat(
                azureEventsService
                .reportStreamEvents[ReportStreamEventName.ITEM_ROUTED]!!.first()
            ).isInstanceOf<ReportStreamItemEvent>()
            val event: ReportStreamItemEvent = azureEventsService
                .reportStreamEvents[ReportStreamEventName.ITEM_ROUTED]!!.first() as ReportStreamItemEvent
            assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                ReportEventData(
                    routedReport.reportId,
                    report.id,
                    listOf(report.id),
                    Topic.FULL_ELR,
                    routedReport.bodyUrl,
                    TaskAction.destination_filter,
                    OffsetDateTime.now(),
                    Version.commitId
                ),
                ReportEventData::timestamp
            )
            assertThat(event.itemEventData).isEqualTo(
                ItemEventData(
                    1,
                    1,
                    1,
                    "MT_COCNB_ORU_NBPHELR.1.5348467",
                    "phd.Test Sender"
                )
            )
            assertThat(event.params).isEqualTo(
                mapOf(
                    ReportStreamEventProperties.RECEIVER_NAME to "phd.x",
                    ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                        observationSummaries = AzureEventUtils.getObservationSummaries(bundle),
                        eventType = "ORU/ACK - Unsolicited transmission of an observation message",
                        patientState = listOf("CO"),
                        performerState = emptyList(),
                        orderingFacilityState = listOf("CO")
                    )
                )
            )

            // check action table
            UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.destination_filter))
        }
    }

    @Test
    fun `should respect jurisdictional filter and not send message`() {
        // set up
        val reportContents = File(VALID_FHIR_URL).readText()
        val report = UniversalPipelineTestUtils.createReport(
            reportContents,
            TaskAction.destination_filter,
            Event.EventAction.DESTINATION_FILTER,
            azuriteContainer
        )
        val queueMessage = generateQueueMessage(
            TaskAction.destination_filter,
            report,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = jurisdictionalFilterIl
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val destinationFilter = createDestinationFilter(azureEventsService, org)

        // execute
        fhirFunctions.process(queueMessage, 1, destinationFilter, ActionHistory(TaskAction.destination_filter))

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(QueueMessage.elrReceiverFilterQueueName, any())
        }

        // check action table
        UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.destination_filter))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val childReport = fetchChildReports(report, txn, 1).first()
            assertThat(childReport.nextAction).isEqualTo(TaskAction.none)
            assertThat(childReport.receivingOrg).isNull()
            assertThat(childReport.receivingOrgSvc).isNull()
            assertThat(childReport.schemaName).isEqualTo("None")
            assertThat(childReport.schemaTopic).isEqualTo(Topic.FULL_ELR)
            assertThat(childReport.bodyFormat).isEqualTo("FHIR")

            // we don't log applications of jurisdictional filter to ACTION_LOG at this time
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)
            assertThat(actionLogRecords).isEmpty()
        }

        // check events
        val bundle = FhirTranscoder.decode(reportContents)
        assertThat(azureEventsService.reportStreamEvents[ReportStreamEventName.ITEM_NOT_ROUTED]!!).hasSize(1)
        assertThat(azureEventsService.reportStreamEvents[ReportStreamEventName.ITEM_NOT_ROUTED]!!.first())
            .isInstanceOf<ReportStreamItemEvent>()
        val event: ReportStreamItemEvent = azureEventsService
            .reportStreamEvents[ReportStreamEventName.ITEM_NOT_ROUTED]?.first() as ReportStreamItemEvent
        assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
            ReportEventData(
                UUID.randomUUID(),
                report.id,
                listOf(report.id),
                Topic.FULL_ELR,
                "",
                TaskAction.destination_filter,
                OffsetDateTime.now(),
                Version.commitId
            ),
            ReportEventData::timestamp,
            ReportEventData::childReportId
        )
        assertThat(event.itemEventData).isEqualTo(
            ItemEventData(
                1,
                1,
                1,
                "MT_COCNB_ORU_NBPHELR.1.5348467",
                "phd.Test Sender"
            )
        )
        assertThat(event.params).isEqualTo(
            mapOf(
            ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                observationSummaries = AzureEventUtils.getObservationSummaries(bundle),
                eventType = "ORU/ACK - Unsolicited transmission of an observation message",
                patientState = listOf("CO"),
                performerState = emptyList(),
                orderingFacilityState = listOf("CO")
            )
        )
        )
    }
}