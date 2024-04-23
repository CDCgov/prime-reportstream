package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.matchesPredicate
import gov.cdc.prime.router.FileSettings
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
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.hl7Sender
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.universalPipelineOrganization
import gov.cdc.prime.router.common.UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles
import gov.cdc.prime.router.common.cleanHL7Record
import gov.cdc.prime.router.common.cleanHL7RecordConvertedAndTransformed
import gov.cdc.prime.router.common.invalidHL7Record
import gov.cdc.prime.router.common.invalidHL7RecordConvertedAndTransformed
import gov.cdc.prime.router.common.unparseableHL7Record
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRConverter
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

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

    private fun generateQueueMessage(report: Report, blobContents: String, sender: Sender): String {
        return """
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
        unmockkObject(BlobAccess)
        unmockkObject(QueueAccess)
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

    // TODO convert between sender format and report form
    private fun setupConvertStep(
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
    }

    @Test
    fun `should successfully convert HL7 messages`() {
        val receivedReportContents =
            """$cleanHL7Record
$invalidHL7Record
$unparseableHL7Record
""".trimIndent()
        val receiveBlobUrl = BlobAccess.uploadBlob(
            "receive/happy-path-.hl7",
            receivedReportContents.toByteArray(),
            getBlobContainerMetadata()
        )

        val receiveReport = setupConvertStep(Report.Format.HL7, hl7Sender, receiveBlobUrl, 3)
        val queueMessage = generateQueueMessage(receiveReport, receivedReportContents, hl7Sender)
        val fhirFunctions = createFHIRFunctionsInstance()

        fhirFunctions.doConvert(queueMessage, 1, createFHIRConverter())

        verify(exactly = 2) {
            QueueAccess.sendMessage(any(), any())
        }
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val routedReports = verifyLineageAndFetchCreatedReportFiles(receiveReport, txn, 2)
            // Verify that the expected FHIR bundles were uploaded
            val fhirBundles =
                routedReports.map { BlobAccess.downloadBlobAsByteArray(it.bodyUrl, getBlobContainerMetadata()) }
            // TODO figure out how to make this assertion work
            // The issue is that the SR transform sets a value on specimen to %diagnosticReport.id
            // and %diagnosticReport.id is a random value generated when converting rom HL7 -> FHIR
            assertThat(fhirBundles).each {
                it.matchesPredicate { bytes ->
                    val invalidHL7Result = CompareData().compare(
                        cleanHL7RecordConvertedAndTransformed.byteInputStream(),
                        bytes.inputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed

                    val cleanHL7Result = CompareData().compare(
                        invalidHL7RecordConvertedAndTransformed.byteInputStream(),
                        bytes.inputStream(),
                        Report.Format.FHIR,
                        null
                    )
                    invalidHL7Result.passed || cleanHL7Result.passed
                }
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
                .isEqualTo("Item 3 in the report was not parseable. Reason: exception while parsing HL7: Determine encoding for message. The following is the first 50 chars of the message for reference, although this may not be where the issue is: MSH^~\\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16")
        }
    }

    @Test
    fun `should successfully convert FHIR messages`() {
    }

    @Test
    fun `should successfully convert messages for a topic without validation`() {
    }

    @Test
    fun `should successfully convert messages for a sender without a transform`() {
    }

    @Test
    fun `test should gracefully handle a case where no items get converted`() {
    }

    @Test
    fun `test should successfully convert an HL7 message with a custom message type configured`() {
    }
}