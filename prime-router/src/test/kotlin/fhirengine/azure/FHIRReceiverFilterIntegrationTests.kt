package gov.cdc.prime.router.fhirengine.azure

import assertk.assertThat
import assertk.assertions.hasSameSizeAs
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.matchesPredicate
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.ActionLogScope
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilterType
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.TestcontainersUtils
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.common.validFHIRRecord1
import gov.cdc.prime.router.common.validFHIRRecord1Identifier
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.fhirengine.engine.FHIRReceiverFilter
import gov.cdc.prime.router.fhirengine.engine.FhirTranslateQueueMessage
import gov.cdc.prime.router.fhirengine.engine.elrTranslationQueueName
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.getObservations
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

private const val MULTIPLE_OBSERVATIONS_FHIR_URL =
    "src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir"

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class FHIRReceiverFilterIntegrationTests : Logging {

    // Must have message ID, patient last name, patient first name, DOB, specimen type
    // At least one of patient street, patient zip code, patient phone number, patient email
    // At least one of order test date, specimen collection date/time, test result date
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

    // requires only an id exists in the message header
    val simpleElrQualifyFilter: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).id.exists()"
    )

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
    fun `should send valid FHIR report filtered by condition code 94558-5`() {
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = observationFilter
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val receiverFilter = UniversalPipelineTestUtils.createReceiverFilter(org)

        // set up
        val reportContents =
            listOf(
                File(MULTIPLE_OBSERVATIONS_FHIR_URL).readText()
            ).joinToString()

        val reports = UniversalPipelineTestUtils.createReportsWithLineage(
            reportContents,
            TaskAction.receiver_filter,
            azuriteContainer
        )
        val receiveReport = reports.first()
        val destinationReport = reports.last()
        val queueMessage = UniversalPipelineTestUtils.generateReceiverQueueMessage(
            destinationReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            "phd.x"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter,
            )
        )

        // execute
        fhirFunctions.doReceiverFilter(queueMessage, 1, receiverFilter)

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // did the report get pushed to blob store correctly and intact?
            val routedReports = UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles(
                destinationReport, receiveReport, txn, 1
            )
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

            val fhirBundlesAsObjectsOnly = reportAndBundles.map { it.second.toString(Charsets.UTF_8) }
                .map { FhirTranscoder.decode(it) }

            val fhirBundleReceiverX = fhirBundlesAsObjectsOnly[0]

            // there should only be one observation of five remaining, and the code of that observation
            // should be 94558-5
            assertThat(fhirBundleReceiverX.getObservations()).hasSize(1)
            assertThat(fhirBundleReceiverX.getObservations().first().code.coding).hasSize(1)
            assertThat(fhirBundleReceiverX.getObservations().first().code.coding.first().code).isEqualTo("94558-5")

            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.flatMap { (report, fhirBundle) ->
                listOf(
                    FhirTranslateQueueMessage(
                        report.reportId,
                        report.bodyUrl,
                        BlobAccess.digestToString(BlobAccess.sha256Digest(fhirBundle)),
                        "phd.fhir-elr-no-transform",
                        UniversalPipelineTestUtils.fhirSenderWithNoTransform.topic,
                        "phd.x"
                    )
                )
            }.map {
                it.serialize()
            }

            // TODO: clean this
            verify(exactly = 1) {
                QueueAccess.sendMessage(elrTranslationQueueName, expectedRouteQueueMessages.single())
            }

            // make sure action table has a new entry
            UniversalPipelineTestUtils.checkActionTable(
                listOf(
                    TaskAction.receive,
                    TaskAction.convert,
                    TaskAction.destination_filter,
                    TaskAction.receiver_filter,
                )
            )
        }
    }

    @Test
    fun `should send valid FHIR report with no condition related filtering`() {
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "y",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                conditionFilter = listOf("true")
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val receiverFilter = UniversalPipelineTestUtils.createReceiverFilter(org)

        // set up
        val reportContents =
            listOf(
                File(MULTIPLE_OBSERVATIONS_FHIR_URL).readText()
            ).joinToString()

        val reports = UniversalPipelineTestUtils.createReportsWithLineage(
            reportContents,
            TaskAction.receiver_filter,
            azuriteContainer
        )
        val receiveReport = reports.first()
        val destinationReport = reports.last()
        val queueMessage = UniversalPipelineTestUtils.generateReceiverQueueMessage(
            destinationReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            "phd.y"
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter,
            )
        )

        // execute
        fhirFunctions.doReceiverFilter(queueMessage, 1, receiverFilter)

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            // did the report get pushed to blob store correctly and intact?
            val routedReports = UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles(
                destinationReport, receiveReport, txn, 1
            )
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

            val fhirBundlesAsObjectsOnly = reportAndBundles.map { it.second.toString(Charsets.UTF_8) }
                .map { FhirTranscoder.decode(it) }

            val fhirBundleReceiverY = fhirBundlesAsObjectsOnly[0]

            // for receiver Y all five observations should be intact
            assertThat(fhirBundleReceiverY.getObservations()).hasSize(5)
            val expectedCodes = listOf("94558-5", "95418-0", "95417-2", "95421-4", "95419-8")
            for (i in 0..<fhirBundleReceiverY.getObservations().size) {
                // in this bundle the array "coding" in every "Observation" only ever has one element
                assertThat(fhirBundleReceiverY.getObservations()[i].code.coding).hasSize(1)
                assertThat(fhirBundleReceiverY.getObservations()[i].code.coding[0].code).isEqualTo(expectedCodes[i])
            }
            assertThat(fhirBundleReceiverY.getObservations()[0].code.coding[0].code).isEqualTo("94558-5")

            // is the queue messaging what we expect it to be?
            val expectedRouteQueueMessages = reportAndBundles.flatMap { (report, fhirBundle) ->
                listOf(
                    FhirTranslateQueueMessage(
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

            // TODO: clean this
            verify(exactly = 1) {
                QueueAccess.sendMessage(
                    elrTranslationQueueName,
                    expectedRouteQueueMessages.single()
                )
            }

            // make sure action table has a new entry
            UniversalPipelineTestUtils.checkActionTable(
                listOf(
                    TaskAction.receive,
                    TaskAction.convert,
                    TaskAction.destination_filter,
                    TaskAction.receiver_filter,
                )
            )
        }
    }

    // TODO: condition filter full prune

    // TODO mapped condition filter for all three cases above

    @Test
    fun `should respect full quality filter and not send message`() {
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                routingFilter = listOf("true"),
                qualityFilter = fullElrQualityFilterSample
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val receiver = receivers.single()
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val receiverFilter = UniversalPipelineTestUtils.createReceiverFilter(org)

        // set up
        val reportContents =
            listOf(
                validFHIRRecord1
            ).joinToString()

        val reports = UniversalPipelineTestUtils.createReportsWithLineage(
            reportContents,
            TaskAction.receiver_filter,
            azuriteContainer
        )
        val destinationReport = reports.last()
        val queueMessage = UniversalPipelineTestUtils.generateReceiverQueueMessage(
            destinationReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            receiver.fullName
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter
            )
        )

        // execute
        fhirFunctions.doReceiverFilter(queueMessage, 1, receiverFilter)

        // no messages should have been routed due to filter
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter,
                TaskAction.receiver_filter
            )
        )

        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)

            assertThat(actionLogRecords).hasSameSizeAs(fullElrQualityFilterSample)

            actionLogRecords.forEachIndexed { index, actionLog ->
                assertThat(actionLog.trackingId).isEqualTo(validFHIRRecord1Identifier)
                assertThat(actionLog.detail).isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                    .matchesPredicate {
                        it.filterType == ReportStreamFilterType.QUALITY_FILTER &&
                            it.filter == fullElrQualityFilterSample[index] &&
                            it.receiverName == receiver.name &&
                            it.receiverOrg == receiver.organizationName
                    }
            }
        }
    }

    @Test
    fun `should respect simple quality filter and send message`() {
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                routingFilter = listOf("true"),
                qualityFilter = simpleElrQualifyFilter
            )
        )

        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val receiver = receivers.first()
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val receiverFilter = UniversalPipelineTestUtils.createReceiverFilter(org)

        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reports = UniversalPipelineTestUtils.createReportsWithLineage(
            reportContents,
            TaskAction.receiver_filter,
            azuriteContainer
        )
        val receiveReport = reports.first()
        val destinationReport = reports.last()
        val queueMessage = UniversalPipelineTestUtils.generateReceiverQueueMessage(
            destinationReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            receiver.fullName
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter
            )
        )

        // execute
        fhirFunctions.doReceiverFilter(queueMessage, 1, receiverFilter)

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->

            // did the report get pushed to blob store correctly and intact?
            val routedReports =
                UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles(
                    destinationReport,
                    receiveReport,
                    txn,
                    1
                )
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
                FhirTranslateQueueMessage(
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
                    elrTranslationQueueName,
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
                    TaskAction.destination_filter,
                    TaskAction.receiver_filter
                )
            )
        }
    }

    @Test
    fun `should respect processing mode filter and send message`() {
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                processingModeFilter = processingModeFilterProduction
            )
        )
        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val receiver = receivers.single()
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val receiverFilter = UniversalPipelineTestUtils.createReceiverFilter(org)

        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reports = UniversalPipelineTestUtils.createReportsWithLineage(
            reportContents,
            TaskAction.receiver_filter,
            azuriteContainer
        )
        val receiveReport = reports.first()
        val destinationReport = reports.last()
        val queueMessage = UniversalPipelineTestUtils.generateReceiverQueueMessage(
            destinationReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            receiver.fullName
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter
            )
        )

        // execute
        fhirFunctions.doReceiverFilter(queueMessage, 1, receiverFilter)

        // check results
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->

            // did the report get pushed to blob store correctly and intact?
            val routedReports =
                UniversalPipelineTestUtils.verifyLineageAndFetchCreatedReportFiles(
                    destinationReport,
                    receiveReport,
                    txn,
                    1
                )
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
                FhirTranslateQueueMessage(
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
                    elrTranslationQueueName,
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
                    TaskAction.destination_filter,
                    TaskAction.receiver_filter
                )
            )
        }
    }

    @Test
    fun `should respect processing mode filter and not send message`() {
        val receiverSetupData = listOf(
            UniversalPipelineTestUtils.ReceiverSetupData(
                "x",
                jurisdictionalFilter = listOf("true"),
                qualityFilter = listOf("true"),
                routingFilter = listOf("true"),
                processingModeFilter = processingModeFilterDebugging,
            )
        )

        val receivers = UniversalPipelineTestUtils.createReceivers(receiverSetupData)
        val receiver = receivers.single()
        val org = UniversalPipelineTestUtils.createOrganizationWithReceivers(receivers)
        val receiverFilter = UniversalPipelineTestUtils.createReceiverFilter(org)

        // set up
        val reportContents =
            listOf(
                File(VALID_FHIR_URL).readText()
            ).joinToString()

        val reports = UniversalPipelineTestUtils.createReportsWithLineage(
            reportContents,
            TaskAction.receiver_filter,
            azuriteContainer
        )
        val destinationReport = reports.last()
        val queueMessage = UniversalPipelineTestUtils.generateReceiverQueueMessage(
            destinationReport,
            reportContents,
            UniversalPipelineTestUtils.fhirSenderWithNoTransform,
            receiver.fullName
        )
        val fhirFunctions = UniversalPipelineTestUtils.createFHIRFunctionsInstance()

        // make sure action table has only what we put in there
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter
            )
        )

        // execute
        fhirFunctions.doReceiverFilter(queueMessage, 1, receiverFilter)

        // check results
        verify(exactly = 0) {
            QueueAccess.sendMessage(elrTranslationQueueName, any())
        }

        // make sure action table has a new entry
        UniversalPipelineTestUtils.checkActionTable(
            listOf(
                TaskAction.receive,
                TaskAction.convert,
                TaskAction.destination_filter,
                TaskAction.receiver_filter
            )
        )

        // ACTION_LOG should have an entry for the filter action
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val actionLogRecords = DSL.using(txn)
                .select(Tables.ACTION_LOG.asterisk())
                .from(Tables.ACTION_LOG)
                .fetchInto(ActionLog::class.java)

            assertThat(actionLogRecords).hasSize(1)

            with(actionLogRecords.single()) {
                assertThat(this.trackingId).isEqualTo("MT_COCNB_ORU_NBPHELR.1.5348467")
                assertThat(this.type).isEqualTo(ActionLogLevel.warning)
                assertThat(this.scope).isEqualTo(ActionLogScope.item)
                assertThat(this.index).isEqualTo(1)
                assertThat(this.detail).isInstanceOf<FHIRReceiverFilter.ReceiverItemFilteredActionLogDetail>()
                    .matchesPredicate {
                        it.filter == processingModeFilterDebugging.single() &&
                        it.filterType == ReportStreamFilterType.PROCESSING_MODE_FILTER &&
                        it.receiverName == receiver.name &&
                        it.receiverOrg == receiver.organizationName
                    }
            }
        }
    }
}