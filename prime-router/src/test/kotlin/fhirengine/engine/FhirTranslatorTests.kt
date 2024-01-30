package gov.cdc.prime.router.fhirengine.engine

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.BlobAccess.Companion.downloadBlobAsByteArray
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.ktor.utils.io.core.toByteArray
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import net.wussmann.kenneth.mockfuel.any
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val ORIGINAL_SENDER_RECEIVER_NAME = "send-original"
private const val ORU_R01_SCHEMA = "metadata/hl7_mapping/receivers/STLTs/CA/CA-receiver-transform"
private const val BLOB_SUB_FOLDER = "test-sender"
private const val BLOB_URL = "http://blob.url"
private const val BODY_URL = "http://anyblob.com"
private const val VALID_DATA_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"
private const val MSH_11_1 = "MSH-11-1"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirTranslatorTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val oneOrganization = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                RECEIVER_NAME,
                ORGANIZATION_NAME,
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                ORU_R01_SCHEMA,
                format = Report.Format.HL7,
            )
        )
    )

    private val originalSenderOrganization = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                ORIGINAL_SENDER_RECEIVER_NAME,
                ORGANIZATION_NAME,
                Topic.ELR_ELIMS,
                CustomerStatus.ACTIVE,
                ORU_R01_SCHEMA,
                format = Report.Format.HL7_BATCH,
            )
        )
    )

    private fun makeFhirEngine(
        metadata: Metadata = Metadata(
            schema = Schema(
                name = "None",
                topic = Topic.FULL_ELR,
                elements = emptyList()
            )
        ),
        settings: SettingsProvider = FileSettings().loadOrganizations(oneOrganization),
    ): FHIRTranslator {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).build(TaskAction.translate) as FHIRTranslator
    }

    private fun makeSendOriginalFhirEngine(
        metadata: Metadata = Metadata(
            schema = Schema(
                name = "None",
                topic = Topic.ELR_ELIMS,
                elements = emptyList()
            )
        ),
        settings: SettingsProvider = FileSettings().loadOrganizations(originalSenderOrganization),
    ): FHIRTranslator {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).build(TaskAction.translate) as FHIRTranslator
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // valid fhir, read file, one destination (hard coded for phase 1), generate output file, no message on queue
    @Test
    fun `test full elr translation happy path, one receiver`() {
        mockkObject(BlobAccess)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val engine = makeFhirEngine()

        val message =
            spyk(
                FhirTranslateQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER,
                    topic = Topic.FULL_ELR,
                    oneOrganization.receivers[0].fullName
                )
            )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }
            .returns(Unit)

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
            BlobAccess.Companion.uploadBlob(any(), any(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
            actionHistory.trackActionReceiverInfo(any(), any())
        }
    }

    @Test
    fun `test send original translation happy path, one receiver`() {
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val engine = makeSendOriginalFhirEngine()
        mockkObject(BlobAccess.Companion)

        val parentReportId = UUID.randomUUID()
        val childReportId = UUID.randomUUID()
        val rootItemLineage =
            ItemLineage(
                9000000125356546,
                null,
                0,
                parentReportId,
                0,
                "trackingId1",
                null,
                OffsetDateTime.now(),
                null
            )
        val childItemLineage =
            ItemLineage(
                9000000125356546,
                parentReportId,
                0,
                childReportId,
                0,
                "trackingId2",
                null,
                OffsetDateTime.now(),
                null
            )
        val report = ReportFile().setReportId(parentReportId).setBodyUrl("testingurl").setSchemaTopic(Topic.ELR_ELIMS)
        val reportContent = "reportContent"
        val message =
            spyk(
                FhirTranslateQueueMessage(
                    UUID.randomUUID(),
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER,
                    topic = Topic.ELR_ELIMS,
                    originalSenderOrganization.receivers[0].fullName
                )
            )
        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        // need a different mock for the one that is instantiated within the method
        mockkConstructor(WorkflowEngine::class)
        mockkConstructor(DatabaseAccess::class)

        val dbAccessMock = mockk<DatabaseAccess>()
        every {
            anyConstructed<WorkflowEngine>().db
        }.returns(dbAccessMock)
        every {
            dbAccessMock.fetchItemLineagesForReport(any(), any(), any())
        }.returnsMany(listOf(childItemLineage), listOf(rootItemLineage))
        every { downloadBlobAsByteArray(any()) }.returns(reportContent.toByteArray())
        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { dbAccessMock.fetchReportFile(any(), any(), any()) }.returns(report)
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }
            .returns(Unit)

        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), blobInfo = any())
            BlobAccess.Companion.uploadBlob(any(), any(), any())
            BlobAccess.Companion.downloadBlobAsByteArray(any(), any(), any())
            accessSpy.insertTask(any(), any(), any(), any(), any())
            actionHistory.trackActionReceiverInfo(any(), any())
        }
    }

    @Test
    fun `test getOriginalMessage`() {
        val mockWorkflowEngine = mockk<WorkflowEngine>()
        val mockDatabaseAccess = mockk<DatabaseAccess>()
        mockkObject(BlobAccess.Companion)
        val parentReportId = UUID.randomUUID()
        val childReportId = UUID.randomUUID()
        val rootItemLineage =
            ItemLineage(9000000125356546, null, 0, parentReportId, 0, "trackingId1", null, OffsetDateTime.now(), null)
        val childItemLineage =
            ItemLineage(
                9000000125356546,
                parentReportId,
                0,
                childReportId,
                0,
                "trackingId2",
                null,
                OffsetDateTime.now(),
                null
            )
        val report = ReportFile().setReportId(parentReportId).setBodyUrl("testingurl")
        val reportContent = "reportContent"

        every {
            mockWorkflowEngine.db
        }.returns(mockDatabaseAccess)
        every {
            mockWorkflowEngine.db.fetchItemLineagesForReport(any(), any(), any())
        }.returnsMany(listOf(childItemLineage), listOf(rootItemLineage))
        every { mockWorkflowEngine.db.fetchReportFile(any()) }.returns(report)
        every { downloadBlobAsByteArray(any()) }.returns(reportContent.toByteArray())

        val rootReport = FHIRTranslator().getOriginalMessage(childReportId, mockWorkflowEngine)

        assertThat(String(rootReport)).isEqualTo(reportContent)
    }

    // happy path, with a receiver that has a custom schema
    @Test
    fun `test full elr translation happy path, custom schema`() {
        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = makeFhirEngine(settings = settings)
        val message = spyk(
            FhirTranslateQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER,
                Topic.FULL_ELR,
                oneOrganization.receivers[0].fullName
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL
        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) } returns Unit

        // act
        accessSpy.transact { txn ->
            engine.run(message, actionLogger, actionHistory, txn)
        }

        // assert
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
        }
    }

    /**
     * When the receiver is in test mode, all output HL7 should have processingId = T
     */
    @Test
    fun `test when customerStatus = testing`() {
        // set up
        val schemaName = ORU_R01_SCHEMA
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = true, schemaName = schemaName)
        )

        val testOrg = DeepOrganization(
            ORGANIZATION_NAME, "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)

        val fhirData = File(VALID_DATA_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = makeFhirEngine(settings = settings)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assertThat(terser.get(MSH_11_1)).isEqualTo("T")
    }

    /**
     * When the receiver is in active mode but the override boolean is set to true,
     * all output HL7 should have processingId = T
     */
    @Test
    fun `test when customerStatus = active, useTestProcessingMode = true`() {
        val schemaName = ORU_R01_SCHEMA
        // set up
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = true, schemaName = schemaName)
        )
        val testOrg = DeepOrganization(
            ORGANIZATION_NAME, "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)

        val fhirData = File(VALID_DATA_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = makeFhirEngine(settings = settings)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assertThat(terser.get(MSH_11_1)).isEqualTo("T")
    }

    /**
     * When the receiver is in production mode and sender is in production mode, output HL7 should be 'P'
     */
    @Test
    fun `test when customerStatus = active, useTestProcessingMode = false, P from sender`() {
        // set up
        val schemaName = ORU_R01_SCHEMA
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = false, schemaName = schemaName)
        )

        val testOrg = DeepOrganization(
            ORGANIZATION_NAME, "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)

        val fhirData = File(VALID_DATA_URL).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = makeFhirEngine(settings = settings)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assertThat(terser.get(MSH_11_1)).isEqualTo("P")
    }

    /**
     * When the receiver is in production mode and sender is in testing mode, output HL7 should be 'T'
     */
    @Test
    fun `test when customerStatus = active, useTestProcessingMode = false, T from sender`() {
        // set up
        val schemaName = ORU_R01_SCHEMA
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = false, schemaName = schemaName)
        )

        val testOrg = DeepOrganization(
            ORGANIZATION_NAME, "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)

        // dodo - make Testing sender fhir

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data_testing_sender.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = makeFhirEngine(settings = settings)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assertThat(terser.get(MSH_11_1)).isEqualTo("T")
    }

    /**
     * When the receiver is in production mode and sender is in testing mode, output HL7 should be 'T'
     */
    @Test
    fun `test receiver enrichment`() {
        // set up
        val schemaName = ORU_R01_SCHEMA
        val receiver = Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = false, schemaName = schemaName),
            enrichmentSchemaNames = listOf(
                "/src/test/resources/enrichments/testing",
                "/src/test/resources/enrichments/testing2"
            )
        )

        val testOrg = DeepOrganization(
            ORGANIZATION_NAME, "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data_testing_sender.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = makeFhirEngine(settings = settings)

        // act
        val byteArray = engine.getByteArrayFromBundle(receiver, bundle)
        val messageIterator = Hl7InputStreamMessageIterator(byteArray.inputStream())
        val message = messageIterator.next()
        val terser = Terser(message)

        // assert
        assertThat(terser.get("SFT-1-1")).isEqualTo("Orange Software Vendor Name")
        assertThat(terser.get("SFT-2")).isEqualTo("0.2-YELLOW")
        // because while it will initially get set, it will then be overridden by the transform
        assertThat(terser.get("SFT-3")).isEqualTo("PRIME ReportStream")
    }

    @Test
    fun `test full elr translation hl7 translation exception`() {
        mockkObject(BlobAccess)

        val badSchemaOrganization = DeepOrganization(
            ORGANIZATION_NAME,
            "test",
            Organization.Jurisdiction.FEDERAL,
            receivers = listOf(
                Receiver(
                    RECEIVER_NAME,
                    ORGANIZATION_NAME,
                    Topic.FULL_ELR,
                    CustomerStatus.ACTIVE,
                    "THIS_PATH_IS_BAD"
                )
            )
        )

        // set up
        val settings = FileSettings().loadOrganizations(badSchemaOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val message = spyk(
            FhirTranslateQueueMessage(
                UUID.randomUUID(),
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER,
                Topic.FULL_ELR,
                badSchemaOrganization.receivers[0].fullName
            )
        )

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.error(any<ActionLogDetail>()) } returns Unit
        every { message.downloadContent() }
            .returns(File("src/test/resources/fhirengine/engine/valid_data_with_extensions.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }.returns(Unit)

        val engine = spyk(makeFhirEngine(settings = settings))

        // act
        accessSpy.transact { txn ->
            assertFailsWith<IllegalStateException>(
                message = "Receiver format CSV not supported.",
                block = { engine.run(message, actionLogger, actionHistory, txn) }
            )
        }
    }

    @Test
    fun `test getByteArrayFromBundle`() {
        val fhirData = File(VALID_DATA_URL).readText()
        val fhirBundle = FhirTranscoder.decode(fhirData)

        val hl7v2Receiver = Receiver(
            RECEIVER_NAME, ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE,
            ORU_R01_SCHEMA, format = Report.Format.HL7_BATCH,
        )
        val fhirReceiver = Receiver(
            "full-elr-fhir", ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE,
            "metadata/fhir_transforms/receivers/fhir-transform-sample", format = Report.Format.FHIR,
        )
        val csvReceiver = Receiver(
            "full-elr-fhir", ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE, "", format = Report.Format.CSV,
        )
        val engine = makeFhirEngine()

        var byteBody = engine.getByteArrayFromBundle(hl7v2Receiver, fhirBundle)
        assertThat(byteBody).isNotEmpty()

        byteBody = engine.getByteArrayFromBundle(fhirReceiver, fhirBundle)
        assertThat(byteBody).isNotEmpty()

        assertFailure { engine.getByteArrayFromBundle(csvReceiver, fhirBundle) }
    }

    @Test
    fun `test encodePreserveEncodingChars`() {
        val fhirData = File("src/test/resources/fhirengine/engine/valid_data_five_encoding_chars.fhir").readText()
        val fhirBundle = FhirTranscoder.decode(fhirData)

        val hl7v2Receiver = Receiver(
            RECEIVER_NAME, ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE,
            ORU_R01_SCHEMA, format = Report.Format.HL7_BATCH,
        )
        val engine = makeFhirEngine()

        val hl7Message = engine.getHL7MessageFromBundle(fhirBundle, hl7v2Receiver)
        val strBody = hl7Message.encodePreserveEncodingChars()
        assertThat(strBody).isNotEmpty()
        assertThat(strBody.contains("MSH|^~\\&#")).isTrue()

        assertFailure { hl7Message.encode() }
    }
}