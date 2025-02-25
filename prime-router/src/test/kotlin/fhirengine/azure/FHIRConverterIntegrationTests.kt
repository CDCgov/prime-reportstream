package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.matchesPredicate
import gov.cdc.prime.reportstream.shared.BlobUtils
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.bundleDigest.BundleDigestLabResult
import gov.cdc.prime.router.azure.observability.event.AzureEventUtils
import gov.cdc.prime.router.azure.observability.event.InMemoryAzureEventService
import gov.cdc.prime.router.azure.observability.event.ItemEventData
import gov.cdc.prime.router.azure.observability.event.OrderingFacilitySummary
import gov.cdc.prime.router.azure.observability.event.ReportEventData
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventName
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventProperties
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamItemEvent
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fetchChildReports
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7Sender
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7SenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.senderWithValidation
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.badEncodingHL7Record
import gov.cdc.prime.router.common.cleanHL7Record
import gov.cdc.prime.router.common.cleanHL7RecordConverted
import gov.cdc.prime.router.common.cleanHL7RecordConvertedAndTransformed
import gov.cdc.prime.router.common.conditionCodedValidFHIRRecord1
import gov.cdc.prime.router.common.garbledHL7Record
import gov.cdc.prime.router.common.invalidEmptyFHIRRecord
import gov.cdc.prime.router.common.invalidHL7Record
import gov.cdc.prime.router.common.invalidHL7RecordConverted
import gov.cdc.prime.router.common.invalidHL7RecordConvertedAndTransformed
import gov.cdc.prime.router.common.invalidMalformedFHIRRecord
import gov.cdc.prime.router.common.invalidRadxMarsHL7Message
import gov.cdc.prime.router.common.unparseableHL7Record
import gov.cdc.prime.router.common.validFHIRRecord1
import gov.cdc.prime.router.common.validFHIRRecord2
import gov.cdc.prime.router.common.validRadxMarsHL7Message
import gov.cdc.prime.router.common.validRadxMarsHL7MessageConverted
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.fhirengine.engine.FhirDestinationFilterQueueMessage
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import gov.cdc.prime.router.version.Version
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import java.nio.charset.Charset
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRConverterIntegrationTests {

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirfunctionintegration1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    val azureEventService = InMemoryAzureEventService()
    val mockSubmissionTableService = mockk<SubmissionTableService>()
    val reportStreamEventService = ReportStreamEventService(
        ReportStreamTestDatabaseContainer.testDatabaseAccess, azureEventService,
        ReportService(
            ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
            ReportStreamTestDatabaseContainer.testDatabaseAccess
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

        return FHIRFunctions(
            workflowEngine,
            databaseAccess = ReportStreamTestDatabaseContainer.testDatabaseAccess,
            submissionTableService = mockSubmissionTableService,
            azureEventService = azureEventService,
            reportStreamEventService = reportStreamEventService
        )
    }

    private fun createFHIRConverter(): FHIRConverter {
        val settings = FileSettings().loadOrganizations(universalPipelineOrganization)
        val metadata = UnitTestUtils.simpleMetadata
        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        return FHIRConverter(
            metadata,
            settings,
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            azureEventService = azureEventService,
            reportStreamEventService = reportStreamEventService
        )
    }

    private fun generateFHIRConvertQueueMessage(
        report: Report,
        blobContents: String,
        sender: Sender,
    ): String = """
        {
            "type": "convert",
            "reportId": "${report.id}",
            "blobURL": "${report.bodyURL}",
            "digest": "${BlobUtils.digestToString(BlobUtils.sha256Digest(blobContents.toByteArray()))}",
            "blobSubFolderName": "${sender.fullName}",
            "topic": "${sender.topic.jsonVal}",
            "schemaName": "${sender.schemaName}"
        }
    """.trimIndent()

    private fun generateFHIRConvertSubmissionQueueMessage(
        report: Report,
        blobContents: String,
        sender: Sender,
    ): String {
        // TODO: something is wrong with the Jackson configuration as it should not require the type to parse this
        val headers = mapOf("client_id" to sender.fullName)
        val headersStringMap = headers.entries.joinToString(separator = ",\n") { (key, value) ->
            """"$key": "$value""""
        }
        val headersString = "[\"java.util.LinkedHashMap\",{$headersStringMap}]"
        return """
        {
            "type": "receive-fhir",
            "reportId": "${report.id}",
            "blobURL": "${report.bodyURL}",
            "digest": "${BlobUtils.digestToString(BlobUtils.sha256Digest(blobContents.toByteArray()))}",
            "blobSubFolderName": "${sender.fullName}",
            "headers":$headersString
        }
    """.trimIndent()
    }

    @BeforeEach
    fun beforeEach() {
        mockkObject(QueueAccess)
        every { QueueAccess.sendMessage(any(), any()) } returns ""
        mockkObject(BlobAccess)
        every { BlobAccess getProperty "defaultBlobMetadata" } returns getBlobContainerMetadata()
        mockkObject(BlobAccess.BlobContainerMetadata)
        every { BlobAccess.BlobContainerMetadata.build(any(), any()) } returns getBlobContainerMetadata()
        mockkConstructor(DatabaseLookupTableAccess::class)
        every { mockSubmissionTableService.insertSubmission(any()) } returns Unit
        mockkObject(Metadata)
        every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
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

    // TODO https://github.com/CDCgov/prime-reportstream/issues/14256
    private fun setupConvertStep(
        format: MimeFormat,
        sender: Sender,
        receiveReportBlobUrl: String,
        itemCount: Int,
    ): Report = ReportStreamTestDatabaseContainer.testDatabaseAccess.transactReturning { txn ->
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
                .setSendingOrg(sender.organizationName)
                .setSendingOrgClient(sender.name)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                reportFile, txn, receiveAction
            )
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertTask(
                report,
                format.toString().lowercase(),
                report.bodyURL,
                nextAction = ProcessEvent(
                    Event.EventAction.CONVERT,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList()
                ),
                txn
            )

            report
        }

    @Test
    fun `should add a message to the poison queue if the sender is not found and not do any work`() {
        val receivedReportContents =
            listOf(cleanHL7Record, invalidHL7Record, unparseableHL7Record, badEncodingHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = Report(
            hl7SenderWithNoTransform.format,
            listOf(
                ClientSource(
                    organization = hl7SenderWithNoTransform.organizationName,
                    client = hl7SenderWithNoTransform.name
                )
            ),
            1,
            metadata = UnitTestUtils.simpleMetadata,
            nextAction = TaskAction.convert,
            topic = hl7SenderWithNoTransform.topic,
            id = UUID.randomUUID(),
            bodyURL = receiveBlobUrl
        )
        val missingSender = UniversalPipelineSender(
            "foo",
            "phd",
            MimeFormat.HL7,
            CustomerStatus.ACTIVE,
            topic = Topic.FULL_ELR,
        )
        val queueMessage =
            generateFHIRConvertSubmissionQueueMessage(receiveReport, receivedReportContents, missingSender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            assertThrows<IllegalStateException> {
                ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchReportFile(receiveReport.id, txn = txn)
            }
            val processedReports = fetchChildReports(
                receiveReport, txn, 0, 0, parentIsRoot = true
            )
            assertThat(processedReports).hasSize(0)
            verify(exactly = 1) {
                QueueAccess.sendMessage(
                    "${QueueMessage.elrSubmissionConvertQueueName}-poison",
                    Base64.getEncoder().encodeToString(queueMessage.toByteArray())
                )
            }
        }
    }

    @Test
    fun `should successfully process a FhirConvertSubmissionQueueMessage`() {
        val receivedReportContents =
            listOf(cleanHL7Record, invalidHL7Record, unparseableHL7Record, badEncodingHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = Report(
            hl7SenderWithNoTransform.format,
            listOf(
                ClientSource(
                    organization = hl7SenderWithNoTransform.organizationName,
                    client = hl7SenderWithNoTransform.name
                )
            ),
            1,
            metadata = UnitTestUtils.simpleMetadata,
            nextAction = TaskAction.convert,
            topic = hl7SenderWithNoTransform.topic,
            id = UUID.randomUUID(),
            bodyURL = receiveBlobUrl
        )
        val queueMessage =
            generateFHIRConvertSubmissionQueueMessage(receiveReport, receivedReportContents, hl7SenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val externalReportRecord =
                ReportStreamTestDatabaseContainer.testDatabaseAccess.fetchReportFile(receiveReport.id, txn = txn)
            assertThat(externalReportRecord.sendingOrg).isEqualTo(hl7SenderWithNoTransform.organizationName)
            assertThat(externalReportRecord.sendingOrgClient).isEqualTo(hl7SenderWithNoTransform.name)
            val (routedReports, unroutedReports) = fetchChildReports(
                receiveReport, txn, 4, 4, parentIsRoot = true
            ).partition { it.nextAction != TaskAction.none }
            assertThat(routedReports).hasSize(2)
            routedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.destination_filter)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }
            assertThat(unroutedReports).hasSize(2)
            unroutedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.none)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }
            // Verify that the expected FHIR bundles were uploaded
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }

            assertThat(reportAndBundles).transform { pairs -> pairs.map { it.second } }.each {
                it.matchesPredicate { bytes ->
                    val invalidHL7Result = CompareData().compare(
                        cleanHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    invalidHL7Result.passed

                    val cleanHL7Result = CompareData().compare(
                        invalidHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
            }

            val expectedQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirDestinationFilterQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobUtils.digestToString(BlobUtils.sha256Digest(fhirBundle)),
                    hl7SenderWithNoTransform.fullName,
                    hl7SenderWithNoTransform.topic
                )
            }.map { it.serialize() }

            verify(exactly = 2) {
                QueueAccess.sendMessage(
                    QueueMessage.elrDestinationFilterQueueName,
                    match { expectedQueueMessages.contains(it) }
                )
            }

            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk()).from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(receiveReport.id))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(2)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs).transform { logs -> logs.map { it.detail.message } }
                .containsOnly(
                    "Item 3 in the report was not parseable. Reason: exception while parsing HL7: Determine encoding for message. The following is the first 50 chars of the message for reference, although this may not be where the issue is: MSH^~\\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16",
                    "Item 4 in the report was not parseable. Reason: exception while parsing HL7: Invalid or incomplete encoding characters - MSH-2 is ^~\\&#!"
                )
            assertThat(actionLogs).transform {
                it.map { log ->
                    log.trackingId
                }
            }.containsOnly(
                "",
                ""
            )

            assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.ITEM_ACCEPTED]!!).hasSize(2)
            val event =
                azureEventService
                    .reportStreamEvents[ReportStreamEventName.ITEM_ACCEPTED]!!.last() as ReportStreamItemEvent
            assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                ReportEventData(
                    routedReports[1].reportId,
                    receiveReport.id,
                    listOf(receiveReport.id),
                    Topic.FULL_ELR,
                    routedReports[1].bodyUrl,
                    TaskAction.convert,
                    OffsetDateTime.now(),
                    Version.commitId
                ),
                ReportEventData::timestamp
            )
            assertThat(event.itemEventData).isEqualToIgnoringGivenProperties(
                ItemEventData(
                    1,
                    2,
                    2,
                    "371784",
                    "phd.hl7-elr-no-transform"
                )
            )
            assertThat(event.params).isEqualTo(
                mapOf(
                    ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.HL7,
                    ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                        observationSummaries = AzureEventUtils
                            .getObservationSummaries(
                                FhirTranscoder.decode(
                                    reportAndBundles[1].second.toString(Charset.defaultCharset())
                                )
                            ),
                        patientState = listOf("TX"),
                        orderingFacilitySummaries = listOf(OrderingFacilitySummary(orderingFacilityState = "FL")),
                        performerSummaries = emptyList(),
                        eventType = "ORU^R01^ORU_R01"
                    ),
                    ReportStreamEventProperties.ENRICHMENTS to ""
                )
            )
        }
    }

    @Test
    fun `should successfully convert HL7 messages`() {
        val receivedReportContents =
            listOf(cleanHL7Record, invalidHL7Record, unparseableHL7Record, badEncodingHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(MimeFormat.HL7, hl7SenderWithNoTransform, receiveBlobUrl, 4)
        val queueMessage =
            generateFHIRConvertQueueMessage(receiveReport, receivedReportContents, hl7SenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val (routedReports, unroutedReports) = fetchChildReports(
                receiveReport, txn, 4, 4
            ).partition { it.nextAction != TaskAction.none }
            assertThat(routedReports).hasSize(2)
            routedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.destination_filter)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }
            assertThat(unroutedReports).hasSize(2)
            unroutedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.none)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }
            // Verify that the expected FHIR bundles were uploaded
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }

            assertThat(reportAndBundles).transform { pairs -> pairs.map { it.second } }.each {
                it.matchesPredicate { bytes ->
                    val invalidHL7Result = CompareData().compare(
                        cleanHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    invalidHL7Result.passed

                    val cleanHL7Result = CompareData().compare(
                        invalidHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
            }

            val expectedQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirDestinationFilterQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobUtils.digestToString(BlobUtils.sha256Digest(fhirBundle)),
                    hl7SenderWithNoTransform.fullName,
                    hl7SenderWithNoTransform.topic
                )
            }.map { it.serialize() }

            verify(exactly = 2) {
                QueueAccess.sendMessage(
                    QueueMessage.elrDestinationFilterQueueName,
                    match { expectedQueueMessages.contains(it) }
                )
            }

            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk()).from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(receiveReport.id))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(2)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs).transform { logs -> logs.map { it.detail.message } }
                .containsOnly(
                    "Item 3 in the report was not parseable. Reason: exception while parsing HL7: Determine encoding for message. The following is the first 50 chars of the message for reference, although this may not be where the issue is: MSH^~\\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16",
                    "Item 4 in the report was not parseable. Reason: exception while parsing HL7: Invalid or incomplete encoding characters - MSH-2 is ^~\\&#!"
                )
            assertThat(actionLogs).transform {
                it.map { log ->
                    log.trackingId
                }
            }.containsOnly(
                "",
                ""
            )

            assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.ITEM_ACCEPTED]!!).hasSize(2)
            val event =
                azureEventService
                    .reportStreamEvents[ReportStreamEventName.ITEM_ACCEPTED]!!.last() as ReportStreamItemEvent
            assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                ReportEventData(
                    routedReports[1].reportId,
                    receiveReport.id,
                    listOf(receiveReport.id),
                    Topic.FULL_ELR,
                    routedReports[1].bodyUrl,
                    TaskAction.convert,
                    OffsetDateTime.now(),
                    Version.commitId
                ),
                ReportEventData::timestamp
            )
            assertThat(event.itemEventData).isEqualToIgnoringGivenProperties(
                ItemEventData(
                    1,
                    2,
                    2,
                    "371784",
                    "phd.hl7-elr-no-transform"
                )
            )
            assertThat(event.params).isEqualTo(
                mapOf(
                    ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.HL7,
                    ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                        observationSummaries = AzureEventUtils
                            .getObservationSummaries(
                                FhirTranscoder.decode(
                                    reportAndBundles[1].second.toString(Charset.defaultCharset())
                                )
                            ),
                        patientState = listOf("TX"),
                        orderingFacilitySummaries = listOf(OrderingFacilitySummary(orderingFacilityState = "FL")),
                        performerSummaries = emptyList(),
                        eventType = "ORU^R01^ORU_R01"
                    ),
                    ReportStreamEventProperties.ENRICHMENTS to ""
                )
            )
        }
    }

    @Test
    fun `should successfully convert FHIR messages`() {
        val observationMappingTable = Table.create(
            "observation-mapping",
            StringColumn.create(ObservationMappingConstants.TEST_CODE_KEY, "80382-5"),
            StringColumn.create(ObservationMappingConstants.CONDITION_CODE_KEY, "6142004"),
            StringColumn.create(ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY, "SNOMEDCT"),
            StringColumn.create(ObservationMappingConstants.CONDITION_NAME_KEY, "Influenza (disorder)")
        )
        val observationMappingLookupTable = LookupTable(
            name = "observation-mapping",
            table = observationMappingTable
        )

        mockkConstructor(Metadata::class)
        every {
            anyConstructed<Metadata>().findLookupTable("observation-mapping")
        } returns observationMappingLookupTable

        val receivedReportContents =
            listOf(
                validFHIRRecord1,
                invalidEmptyFHIRRecord,
                validFHIRRecord2,
                invalidMalformedFHIRRecord
            ).joinToString(
                "\n"
            )

        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path-.fhir",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(
            MimeFormat.FHIR,
            fhirSenderWithNoTransform, receiveBlobUrl, 4
        )
        val queueMessage = generateFHIRConvertQueueMessage(
            receiveReport, receivedReportContents,
            fhirSenderWithNoTransform
        )
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val (routedReports, unroutedReports) = fetchChildReports(
                receiveReport, txn, 4, 4
            ).partition { it.nextAction != TaskAction.none }
            assertThat(routedReports).hasSize(2)
            routedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.destination_filter)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }
            assertThat(unroutedReports).hasSize(2)
            unroutedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.none)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }
            // Verify that the expected FHIR bundles were uploaded
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }
                    .map { Pair(it.first, it.second.toString(Charset.defaultCharset())) }
            assertThat(reportAndBundles).transform { pairs -> pairs.map { it.second } }
                .containsOnly(conditionCodedValidFHIRRecord1, validFHIRRecord2)
            val expectedQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirDestinationFilterQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobUtils.digestToString(BlobUtils.sha256Digest(fhirBundle.toByteArray())),
                    fhirSenderWithNoTransform.fullName,
                    fhirSenderWithNoTransform.topic
                )
            }.map { it.serialize() }
            verify(exactly = 2) {
                QueueAccess.sendMessage(
                    QueueMessage.elrDestinationFilterQueueName,
                    match { expectedQueueMessages.contains(it) }
                )
            }

            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk()).from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(receiveReport.id))
                .and(Tables.ACTION_LOG.TYPE.`in`(ActionLogType.error, ActionLogType.warning))
                .orderBy(Tables.ACTION_LOG.ACTION_LOG_ID.asc())
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(4)
            @Suppress("ktlint:standard:max-line-length")
            val expectedDetailedActions = listOf(
                2 to
                    "Item 2 in the report was not parseable. Reason: exception while parsing FHIR: HAPI-1838: Invalid JSON content detected, missing required element: 'resourceType'",
                3 to "Missing mapping for code(s): 41458-1",
                3 to "Missing mapping for code(s): 260373001",
                4 to
                    "Item 4 in the report was not parseable. Reason: exception while parsing FHIR: HAPI-1861: Failed to parse JSON encoded FHIR content: Unexpected end-of-input: was expecting closing quote for a string value\n" +
                    " at [line: 1, column: 23]"
            )

            val actualDetailedActions = actionLogs.map { log -> log.index to log.detail.message }

            assertThat(actualDetailedActions).isEqualTo(expectedDetailedActions)
            assertThat(actionLogs).transform {
                it.map { log ->
                    log.trackingId
                }
            }.containsOnly(
                "",
                "Observation/d683b42a-bf50-45e8-9fce-6c0531994f09",
                "Observation/d683b42a-bf50-45e8-9fce-6c0531994f09",
                ""
            )

            assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.ITEM_ACCEPTED]!!).hasSize(2)
            var event = azureEventService
                .reportStreamEvents[ReportStreamEventName.ITEM_ACCEPTED]!!.last() as ReportStreamItemEvent
            assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                ReportEventData(
                    routedReports[1].reportId,
                    receiveReport.id,
                    listOf(receiveReport.id),
                    Topic.FULL_ELR,
                    routedReports[1].bodyUrl,
                    TaskAction.convert,
                    OffsetDateTime.now(),
                    Version.commitId
                ),
                ReportEventData::timestamp
            )
            assertThat(event.itemEventData).isEqualToIgnoringGivenProperties(
                ItemEventData(
                    1,
                    3,
                    3,
                    "1234d1d1-95fe-462c-8ac6-46728dbau8cd",
                    "phd.fhir-elr-no-transform"
                )
            )
            assertThat(event.params).isEqualTo(
                mapOf(
                    ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.FHIR,
                    ReportStreamEventProperties.BUNDLE_DIGEST to BundleDigestLabResult(
                        observationSummaries = AzureEventUtils
                            .getObservationSummaries(
                                FhirTranscoder.decode(
                                    reportAndBundles[1].second
                                )
                            ),
                        patientState = emptyList(),
                        orderingFacilitySummaries = emptyList(),
                        performerSummaries = emptyList(),
                        eventType = "ORU^R01^ORU_R01"
                    ),
                    ReportStreamEventProperties.ENRICHMENTS to ""
                )
            )
        }
    }

    @Test
    fun `should successfully convert messages for a topic with validation`() {
        listOf(validRadxMarsHL7Message, invalidRadxMarsHL7Message).joinToString("\n")
        val receivedReportContents = listOf(validRadxMarsHL7Message, invalidRadxMarsHL7Message)
            .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(MimeFormat.HL7, senderWithValidation, receiveBlobUrl, 2)
        val queueMessage = generateFHIRConvertQueueMessage(receiveReport, receivedReportContents, senderWithValidation)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val (routedReports, notRouted) = fetchChildReports(
                receiveReport, txn, 2, 2
            ).partition { it.nextAction != TaskAction.none }

            with(routedReports.single()) {
                assertThat(this.nextAction).isEqualTo(TaskAction.destination_filter)
                assertThat(this.receivingOrg).isEqualTo(null)
                assertThat(this.receivingOrgSvc).isEqualTo(null)
                assertThat(this.schemaName).isEqualTo("None")
                assertThat(this.schemaTopic).isEqualTo(Topic.MARS_OTC_ELR)
                assertThat(this.bodyFormat).isEqualTo("FHIR")
            }
            with(notRouted.single()) {
                assertThat(this.nextAction).isEqualTo(TaskAction.none)
                assertThat(this.receivingOrg).isEqualTo(null)
                assertThat(this.receivingOrgSvc).isEqualTo(null)
                assertThat(this.schemaName).isEqualTo("None")
                assertThat(this.schemaTopic).isEqualTo(Topic.MARS_OTC_ELR)
                assertThat(this.bodyFormat).isEqualTo("FHIR")
            }

            // Verify that the expected FHIR bundles were uploaded
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }

            assertThat(reportAndBundles).transform { pairs -> pairs.map { it.second } }.each {
                it.matchesPredicate { bytes ->
                    CompareData().compare(
                        validRadxMarsHL7MessageConverted.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    ).passed
                }
            }

            val expectedQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirDestinationFilterQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobUtils.digestToString(BlobUtils.sha256Digest(fhirBundle)),
                    senderWithValidation.fullName,
                    senderWithValidation.topic
                )
            }.map { it.serialize() }
            verify(exactly = 1) {
                QueueAccess.sendMessage(
                    QueueMessage.elrDestinationFilterQueueName,
                    match { expectedQueueMessages.contains(it) }
                )
            }

            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk()).from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(receiveReport.id))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(1)
            @Suppress("ktlint:standard:max-line-length")
            assertThat(actionLogs.first()).transform { it.detail.message }
                .isEqualTo(
                    "Item 2 in the report was not valid. Reason: HL7 was not valid at MSH[1]-21[1].3 for validator: RADx MARS"
                )
            assertThat(actionLogs.first()).transform { it.trackingId }
                .isEqualTo("20240403205305_dba7572cc6334f1ea0744c5f235c823e")

            assertThat(azureEventService.reportStreamEvents[ReportStreamEventName.ITEM_FAILED_VALIDATION]!!).hasSize(1)
            val event = azureEventService
                .reportStreamEvents[ReportStreamEventName.ITEM_FAILED_VALIDATION]!!.last() as ReportStreamItemEvent
            assertThat(event.reportEventData).isEqualToIgnoringGivenProperties(
                ReportEventData(
                    notRouted.first().reportId,
                    receiveReport.id,
                    listOf(receiveReport.id),
                    Topic.MARS_OTC_ELR,
                    "",
                    TaskAction.convert,
                    OffsetDateTime.now(),
                    Version.commitId
                ),
                ReportEventData::timestamp
            )
            assertThat(event.itemEventData).isEqualToIgnoringGivenProperties(
                ItemEventData(
                    1,
                    2,
                    2,
                    null,
                    "phd.marsotc-hl7-sender"
                )
            )
            @Suppress("ktlint:standard:max-line-length")
            assertThat(event.params).isEqualTo(
                mapOf(
                    ReportStreamEventProperties.ITEM_FORMAT to MimeFormat.HL7,
                    ReportStreamEventProperties.VALIDATION_PROFILE to Topic.MARS_OTC_ELR.validator.validatorProfileName,
                    @Suppress("ktlint:standard:max-line-length")
                    ReportStreamEventProperties.PROCESSING_ERROR
                        to
                            "Item 2 in the report was not valid. Reason: HL7 was not valid at MSH[1]-21[1].3 for validator: RADx MARS"
                )
            )
        }
    }

    @Test
    fun `should successfully convert HL7 reports for a sender with a transform`() {
        val receivedReportContents = listOf(cleanHL7Record, invalidHL7Record)
            .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(MimeFormat.HL7, hl7Sender, receiveBlobUrl, 2)
        val queueMessage = generateFHIRConvertQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = fetchChildReports(receiveReport, txn, 2, 2)
            routedReports.forEach {
                assertThat(it.nextAction).isEqualTo(TaskAction.destination_filter)
                assertThat(it.receivingOrg).isEqualTo(null)
                assertThat(it.receivingOrgSvc).isEqualTo(null)
                assertThat(it.schemaName).isEqualTo("None")
                assertThat(it.schemaTopic).isEqualTo(Topic.FULL_ELR)
                assertThat(it.bodyFormat).isEqualTo("FHIR")
            }

            // Verify that the expected FHIR bundles were uploaded
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata())
                    )
                }

            assertThat(reportAndBundles).transform { pairs -> pairs.map { it.second } }.each {
                it.matchesPredicate { bytes ->
                    val invalidHL7Result = CompareData().compare(
                        cleanHL7RecordConvertedAndTransformed.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    val cleanHL7Result = CompareData().compare(
                        invalidHL7RecordConvertedAndTransformed.byteInputStream(),
                        bytes.inputStream(),
                        MimeFormat.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
            }

            val expectedQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirDestinationFilterQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobUtils.digestToString(BlobUtils.sha256Digest(fhirBundle)),
                    hl7Sender.fullName,
                    hl7Sender.topic
                )
            }.map { it.serialize() }

            verify(exactly = 2) {
                QueueAccess.sendMessage(
                    QueueMessage.elrDestinationFilterQueueName,
                    match { expectedQueueMessages.contains(it) }
                )
            }

            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk()).from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(receiveReport.id))
                .and(Tables.ACTION_LOG.TYPE.eq(ActionLogType.error))
                .fetchInto(
                    DetailedActionLog::class.java
                )

            assertThat(actionLogs).hasSize(0)
        }
    }

    @Test
    fun `test should gracefully handle a case where no items get converted`() {
        val receivedReportContents = unparseableHL7Record
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(MimeFormat.HL7, hl7Sender, receiveBlobUrl, 1)
        val queueMessage = generateFHIRConvertQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val report = fetchChildReports(receiveReport, txn, 1).single()
            assertThat(report.nextAction).isEqualTo(TaskAction.none)
            assertThat(report.receivingOrg).isEqualTo(null)
            assertThat(report.receivingOrgSvc).isEqualTo(null)
            assertThat(report.schemaName).isEqualTo("None")
            assertThat(report.schemaTopic).isEqualTo(Topic.FULL_ELR)
            assertThat(report.bodyFormat).isEqualTo("FHIR")
        }
    }

    @Test
    fun `test should gracefully handle a case where number of items is unknown`() {
        val receivedReportContents = garbledHL7Record
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(MimeFormat.HL7, hl7Sender, receiveBlobUrl, 1)
        val queueMessage = generateFHIRConvertQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val report = fetchChildReports(receiveReport, txn, 0).single()
            assertThat(report.nextAction).isEqualTo(TaskAction.none)
            assertThat(report.receivingOrg).isEqualTo(null)
            assertThat(report.receivingOrgSvc).isEqualTo(null)
            assertThat(report.schemaName).isEqualTo("None")
            assertThat(report.schemaTopic).isEqualTo(Topic.FULL_ELR)
            assertThat(report.bodyFormat).isEqualTo("HL7")
        }
    }

    @Test
    fun `test should gracefully handle a case with an empty contents`() {
        val receivedReportContents = "   "
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(MimeFormat.HL7, hl7Sender, receiveBlobUrl, 1)
        val queueMessage = generateFHIRConvertQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.process(queueMessage, 1, createFHIRConverter(), ActionHistory(TaskAction.convert))

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val report = fetchChildReports(receiveReport, txn, 0, 1).single()
            assertThat(report.nextAction).isEqualTo(TaskAction.none)
            assertThat(report.receivingOrg).isEqualTo(null)
            assertThat(report.receivingOrgSvc).isEqualTo(null)
            assertThat(report.schemaName).isEqualTo("None")
            assertThat(report.schemaTopic).isEqualTo(Topic.FULL_ELR)
            assertThat(report.bodyFormat).isEqualTo("HL7")
        }
    }
}