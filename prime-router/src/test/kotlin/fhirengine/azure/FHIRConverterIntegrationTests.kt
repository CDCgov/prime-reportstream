package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.matchesPredicate
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.cli.ObservationMappingConstants
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.fhirSenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7Sender
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7SenderWithNoTransform
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.senderWithValidation
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles
import gov.cdc.prime.router.common.badEncodingHL7Record
import gov.cdc.prime.router.common.cleanHL7Record
import gov.cdc.prime.router.common.cleanHL7RecordConverted
import gov.cdc.prime.router.common.cleanHL7RecordConvertedAndTransformed
import gov.cdc.prime.router.common.conditionCodedValidFHIRRecord1
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
import gov.cdc.prime.router.fhirengine.engine.FhirRouteQueueMessage
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkConstructor
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
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import java.nio.charset.Charset

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
        )
    }

    private fun generateQueueMessage(report: Report, blobContents: String, sender: Sender): String = """
            {
                "type": "convert",
                "reportId": "${report.id}",
                "blobURL": "${report.bodyURL}",
                "digest": "${BlobAccess.digestToString(BlobAccess.sha256Digest(blobContents.toByteArray()))}",
                "blobSubFolderName": "${sender.fullName}",
                "topic": "${sender.topic.jsonVal}",
                "schemaName": "${sender.schemaName}" 
            }
        """.trimIndent()

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
        format: Report.Format,
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
    fun `should successfully convert HL7 messages`() {
        val receivedReportContents =
            listOf(cleanHL7Record, invalidHL7Record, unparseableHL7Record, badEncodingHL7Record)
                .joinToString("\n")
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(Report.Format.HL7, hl7SenderWithNoTransform, receiveBlobUrl, 4)
        val queueMessage = generateQueueMessage(receiveReport, receivedReportContents, hl7SenderWithNoTransform)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.doConvert(queueMessage, 1, createFHIRConverter())

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = verifyLineageAndFetchCreatedReportFiles(receiveReport, receiveReport, txn, 2)
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
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed

                    val cleanHL7Result = CompareData().compare(
                        invalidHL7RecordConverted.byteInputStream(),
                        bytes.inputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
            }

            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirRouteQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    hl7SenderWithNoTransform.fullName,
                    hl7SenderWithNoTransform.topic
                )
            }.map { it.serialize() }

            verify(exactly = 2) {
                QueueAccess.sendMessage("elr-fhir-route", match { expectedRouteQueueMessages.contains(it) })
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
            Report.Format.FHIR,
            fhirSenderWithNoTransform, receiveBlobUrl, 4
        )
        val queueMessage = generateQueueMessage(
            receiveReport, receivedReportContents,
            fhirSenderWithNoTransform
        )
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.doConvert(queueMessage, 1, createFHIRConverter())

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = verifyLineageAndFetchCreatedReportFiles(receiveReport, receiveReport, txn, 2)
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
            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirRouteQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle.toByteArray())),
                    fhirSenderWithNoTransform.fullName,
                    fhirSenderWithNoTransform.topic
                )
            }.map { it.serialize() }
            verify(exactly = 2) {
                QueueAccess.sendMessage("elr-fhir-route", match { expectedRouteQueueMessages.contains(it) })
            }

            val actionLogs = DSL.using(txn).select(Tables.ACTION_LOG.asterisk()).from(Tables.ACTION_LOG)
                .where(Tables.ACTION_LOG.REPORT_ID.eq(receiveReport.id))
                .and(Tables.ACTION_LOG.TYPE.`in`(ActionLogType.error, ActionLogType.warning))
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

        val receiveReport = setupConvertStep(Report.Format.HL7, senderWithValidation, receiveBlobUrl, 2)
        val queueMessage = generateQueueMessage(receiveReport, receivedReportContents, senderWithValidation)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.doConvert(queueMessage, 1, createFHIRConverter())

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = verifyLineageAndFetchCreatedReportFiles(receiveReport, receiveReport, txn, 1)
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
                        Report.Format.FHIR,
                        null
                    ).passed
                }
            }

            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirRouteQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    senderWithValidation.fullName,
                    senderWithValidation.topic
                )
            }.map { it.serialize() }
            verify(exactly = 1) {
                QueueAccess.sendMessage("elr-fhir-route", match { expectedRouteQueueMessages.contains(it) })
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

        val receiveReport = setupConvertStep(Report.Format.HL7, hl7Sender, receiveBlobUrl, 2)
        val queueMessage = generateQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.doConvert(queueMessage, 1, createFHIRConverter())

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = verifyLineageAndFetchCreatedReportFiles(receiveReport, receiveReport, txn, 2)
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
                        Report.Format.FHIR,
                        null
                    )
                    val cleanHL7Result = CompareData().compare(
                        invalidHL7RecordConvertedAndTransformed.byteInputStream(),
                        bytes.inputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
            }

            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirRouteQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    hl7Sender.fullName,
                    hl7Sender.topic
                )
            }.map { it.serialize() }

            verify(exactly = 2) {
                QueueAccess.sendMessage("elr-fhir-route", match { expectedRouteQueueMessages.contains(it) })
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

        val receiveReport = setupConvertStep(Report.Format.HL7, hl7Sender, receiveBlobUrl, 1)
        val queueMessage = generateQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.doConvert(queueMessage, 1, createFHIRConverter())

        verify(exactly = 0) {
            QueueAccess.sendMessage(any(), any())
        }
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            verifyLineageAndFetchCreatedReportFiles(receiveReport, receiveReport, txn, 1)
        }
    }
}