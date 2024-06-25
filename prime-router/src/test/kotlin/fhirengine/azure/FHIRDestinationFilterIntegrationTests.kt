package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.common.validFHIRRecord1
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FhirReceiverFilterQueueMessage
import gov.cdc.prime.router.fhirengine.engine.elrReceiverFilterQueueName
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

private const val VALID_FHIR_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRDestinationFilterIntegrationTests : Logging {
    // patient must reside in Colorado
    val jurisdictionalFilterCo: ReportStreamFilter =
        listOf("Bundle.entry.resource.ofType(Patient).address.state='CO'")

    // patient must reside in Illinois
    val jurisdictionalFilterIl: ReportStreamFilter =
        listOf("Bundle.entry.resource.ofType(Patient).address.state='IL'")

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
        every { QueueAccess.sendMessage(any(), any()) } returns Unit
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

        val reportPair = UniversalPipelineTestUtils.createReportsWithLineage(reportContents, azuriteContainer)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = UniversalPipelineTestUtils.generateQueueMessage(
            TaskAction.destination_filter,
            convertReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.convert))

        // execute
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
                )
            )
        )

        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receiverList)

        val destinationFilter = UniversalPipelineTestUtils.createDestinationFilter(org)
        fhirFunctions.doDestinationFilter(queueMessage, 1, destinationFilter)

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // did the report get pushed to blob store correctly and intact?
            val routedReports =
                UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 2)
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(
                            it.bodyUrl,
                            UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
                        )
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
                    FhirReceiverFilterQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                        "phd.fhir-elr-no-transform",
                        UniversalPipelineTestUtils.fhirSenderWithNoTransform.topic,
                        "phd.x"
                    ),
                    FhirReceiverFilterQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
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
                    elrReceiverFilterQueueName,
                    match {
                        expectedRouteQueueMessages.contains(it)
                    }
                )
            }

            // make sure action table has a new entry
            UniversalPipelineTestUtils.checkActionTable(
                listOf(
                    TaskAction.receive,
                    TaskAction.convert,
                    TaskAction.destination_filter
                )
            )
        }
    }

    @Test
    fun `should respect jurisdictional filter and send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = UniversalPipelineTestUtils.createReportsWithLineage(reportContents, azuriteContainer)
        val receiveReport = reportPair.first
        val convertReport = reportPair.second
        val queueMessage = UniversalPipelineTestUtils.generateQueueMessage(
            TaskAction.destination_filter,
            convertReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.convert))

        // execute
        val receivers = UniversalPipelineTestUtils.createReceivers(
            listOf(
                UniversalPipelineTestUtils.ReceiverSetupData(
                    "x",
                    jurisdictionalFilter = jurisdictionalFilterCo
                )
            )
        )
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val destinationFilter = UniversalPipelineTestUtils.createDestinationFilter(org)
        fhirFunctions.doDestinationFilter(queueMessage, 1, destinationFilter)

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->

            // did the report get pushed to blob store correctly and intact?
            val routedReports =
                UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles(convertReport, receiveReport, txn, 1)
            val reportAndBundles =
                routedReports.map {
                    Pair(
                        it,
                        BlobAccess.downloadBlobAsByteArray(
                            it.bodyUrl,
                            UniversalPipelineTestUtils.getBlobContainerMetadata(azuriteContainer)
                        )
                    )
                }
            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.map { (report, fhirBundle) ->
                FhirReceiverFilterQueueMessage(
                    report.reportId,
                    report.bodyUrl,
                    BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                    "phd.fhir-elr-no-transform",
                    UniversalPipelineTestUtils.fhirSenderWithNoTransform.topic,
                    "phd.x"
                )
            }.map {
                it.serialize()
            }

            // filter should permit message and should not mangle message
            verify(exactly = 1) {
                QueueAccess.sendMessage(
                    elrReceiverFilterQueueName,
                    match {
                        expectedRouteQueueMessages.contains(it)
                    }
                )
            }

            // make sure action table has a new entry
            UniversalPipelineTestUtils.checkActionTable(
                listOf(
                    TaskAction.receive,
                    TaskAction.convert,
                    TaskAction.destination_filter
                )
            )
        }
    }

    @Test
    fun `should respect jurisdictional filter and not send message`() {
        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reportPair = UniversalPipelineTestUtils.createReportsWithLineage(reportContents, azuriteContainer)
        val convertReport = reportPair.second
        val queueMessage = UniversalPipelineTestUtils.generateQueueMessage(
            TaskAction.destination_filter,
            convertReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(listOf(TaskAction.receive, TaskAction.convert))

        // execute
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = jurisdictionalFilterIl
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val destinationFilter = UniversalPipelineTestUtils.createDestinationFilter(org)
        fhirFunctions.doDestinationFilter(queueMessage, 1, destinationFilter)

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrReceiverFilterQueueName, any())
        }

        // make sure action table has a new entry
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter
            )
        )

        // we don't log applications of jurisdictional filter to ACTION_LOG at this time
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)

            assertThat(actionLogRecords).isEmpty()
        }

        // we don't log jurisdictional filter actions
        // TODO: hm
        // checkActionLogTable(listOf())
    }
}