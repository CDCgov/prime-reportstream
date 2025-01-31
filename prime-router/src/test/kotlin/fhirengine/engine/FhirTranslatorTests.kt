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
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.JSONB
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
private const val ORU_R01_SCHEMA = "classpath:/metadata/hl7_mapping/receivers/STLTs/CA/CA-receiver-transform.yml"
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
                format = MimeFormat.HL7,
            )
        )
    )
    val reportServiceMock = mockk<ReportService>()
    val reportStreamEventService = mockk<ReportStreamEventService>()

    private fun makeFhirEngine(
        metadata: Metadata = Metadata(
            schema = Schema(
                name = "None",
                topic = Topic.FULL_ELR,
                elements = emptyList()
            )
        ),
        settings: SettingsProvider = FileSettings().loadOrganizations(oneOrganization),
    ): FHIRTranslator = FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).reportService(reportServiceMock).reportEventService(reportStreamEventService)
            .build(TaskAction.translate) as FHIRTranslator

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    // valid fhir, read file, one destination (hard coded for phase 1), generate output file, no message on queue
    @Test
    fun `test full elr translation happy path, one receiver`() {
        mockkObject(BlobAccess)
        mockkObject(BlobAccess.BlobContainerMetadata)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val engine = makeFhirEngine()

        val reportId = UUID.randomUUID()
        val message =
            spyk(
                FhirTranslateQueueMessage(
                    reportId,
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER,
                    topic = Topic.FULL_ELR,
                    oneOrganization.receivers[0].fullName
                )
            )

        val bodyFormat = MimeFormat.FHIR
        val bodyUrl = BODY_URL
        val rootReport = mockk<ReportFile>()

        every { actionLogger.hasErrors() } returns false
        every { BlobAccess.downloadBlob(any(), any()) }
            .returns(File(VALID_DATA_URL).readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every {
            BlobAccess.BlobContainerMetadata.build(
                "metadata",
                any()
            )
        } returns mockk<BlobAccess.BlobContainerMetadata>()
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }.returns(Unit)
        every { actionHistory.action }.returns(
            Action(
                1,
                TaskAction.receive,
                "",
                "",
                OffsetDateTime.now(),
                JSONB.valueOf(""),
                1,
                1,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            )
        )
        every { rootReport.reportId } returns reportId
        every { rootReport.sendingOrg } returns oneOrganization.name
        every { rootReport.sendingOrgClient } returns oneOrganization.receivers[0].fullName
        every { rootReport.bodyFormat } returns bodyFormat.toString()
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getRootReports(any()) } returns listOf(rootReport)
        every { reportServiceMock.getRootItemIndex(any(), any()) } returns 1
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns "1".toByteArray(Charsets.UTF_8)
        every { reportStreamEventService.sendItemEvent(any(), any<Report>(), any(), any(), any()) } returns Unit

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
            reportStreamEventService.sendItemEvent(any(), any<Report>(), any(), any(), any())
        }
    }

    @Test
    fun `test translation happy path with file digest exception`() {
        mockkObject(BlobAccess)
        mockkObject(BlobAccess.BlobContainerMetadata)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()
        val engine = makeFhirEngine()

        val reportId = UUID.randomUUID()
        val message =
            spyk(
                FhirTranslateQueueMessage(
                    reportId,
                    BLOB_URL,
                    "test",
                    BLOB_SUB_FOLDER,
                    topic = Topic.ELR_ELIMS,
                    oneOrganization.receivers[0].fullName
                )
            )

        val bodyFormat = MimeFormat.FHIR
        val bodyUrl = BODY_URL
        val rootReport = mockk<ReportFile>()

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.error(any<ActionLogDetail>()) } returns Unit
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every {
            BlobAccess.BlobContainerMetadata.build(
                "metadata",
                any()
            )
        } returns mockk<BlobAccess.BlobContainerMetadata>()
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }.returns(Unit)
        every { actionHistory.action }.returns(
            Action(
                1,
                TaskAction.receive,
                "",
                "",
                OffsetDateTime.now(),
                JSONB.valueOf(""),
                1,
                1,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            )
        )
        every { rootReport.reportId } returns reportId
        every { rootReport.sendingOrg } returns oneOrganization.name
        every { rootReport.sendingOrgClient } returns oneOrganization.receivers[0].fullName
        every { rootReport.bodyUrl } returns BLOB_URL
        every { rootReport.bodyFormat } returns "HL7"
        every { rootReport.blobDigest } returns reportId.toString().toByteArray(Charsets.UTF_8)
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getRootReports(any()) } returns listOf(rootReport)
        every { reportServiceMock.getRootItemIndex(any(), any()) } returns 1
        every { BlobAccess.downloadBlobAsByteArray(any()) } returns "1".toByteArray(Charsets.UTF_8)

        // act
        @Suppress("ktlint:standard:max-line-length")
        accessSpy.transact { txn ->
            assertFailsWith<IllegalStateException>(
                message = "Downloaded file does not match expected file\n" +
                    "test | 6bffffff86ffffffb273ffffffff34fffffffcffffffe1ffffff9d6bffffff804effffffff5a3f5747ffffffadffffffa4ffffffeaffffffa22f1d49ffffffc01e52ffffffddffffffb7ffffff875b4b",
                block = { engine.run(message, actionLogger, actionHistory, txn) }
            )
        }
    }

    // happy path, with a receiver that has a custom schema
    @Test
    fun `test full elr translation happy path, custom schema`() {
        mockkObject(BlobAccess)
        mockkObject(BlobAccess.BlobContainerMetadata)

        // set up
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = makeFhirEngine(settings = settings)
        val reportId = UUID.randomUUID()
        val message = spyk(
            FhirTranslateQueueMessage(
                reportId,
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER,
                Topic.FULL_ELR,
                oneOrganization.receivers[0].fullName
            )
        )

        val bodyFormat = MimeFormat.FHIR
        val bodyUrl = BODY_URL
        val rootReport = mockk<ReportFile>()
        every { actionLogger.hasErrors() } returns false
        every { BlobAccess.downloadBlob(any(), any()) }
            .returns(File(VALID_DATA_URL).readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every {
            BlobAccess.BlobContainerMetadata.build(
                "metadata",
                any()
            )
        } returns mockk<BlobAccess.BlobContainerMetadata>()
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) } returns Unit
        every { actionHistory.action }.returns(
            Action(
                1,
                TaskAction.receive,
                "",
                "",
                OffsetDateTime.now(),
                JSONB.valueOf(""),
                1,
                1,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            )
        )

        every { rootReport.reportId } returns reportId
        every { rootReport.bodyFormat } returns bodyFormat.toString()
        every { rootReport.sendingOrg } returns oneOrganization.name
        every { rootReport.sendingOrgClient } returns oneOrganization.receivers[0].fullName
        every { reportServiceMock.getRootReport(any()) } returns rootReport
        every { reportServiceMock.getRootReports(any()) } returns listOf(rootReport)
        every { reportServiceMock.getRootItemIndex(any(), any()) } returns 1
        every { reportStreamEventService.sendItemEvent(any(), any<Report>(), any(), any(), any()) } returns Unit

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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

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
                "classpath:/enrichments/testing.yml",
                "classpath:/enrichments/testing2.yml"
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

        val reportId = UUID.randomUUID()
        val message = spyk(
            FhirTranslateQueueMessage(
                reportId,
                BLOB_URL,
                "test",
                BLOB_SUB_FOLDER,
                Topic.FULL_ELR,
                badSchemaOrganization.receivers[0].fullName
            )
        )

        val bodyFormat = MimeFormat.FHIR
        val bodyUrl = BODY_URL
        val rootReport = mockk<ReportFile>()

        every { rootReport.reportId } returns reportId
        every { rootReport.sendingOrg } returns oneOrganization.name
        every { rootReport.sendingOrgClient } returns oneOrganization.receivers[0].fullName
        every { rootReport.bodyFormat } returns bodyFormat.toString()
        every { reportServiceMock.getRootReport(any()) } returns rootReport

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.error(any<ActionLogDetail>()) } returns Unit
        every { BlobAccess.downloadBlob(any(), any()) }
            .returns(File("src/test/resources/fhirengine/engine/valid_data_with_extensions.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), blobInfo = any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }.returns(Unit)
        every { actionHistory.action }.returns(
            Action(
                1,
                TaskAction.receive,
                "",
                "",
                OffsetDateTime.now(),
                JSONB.valueOf(""),
                1,
                1,
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            )
        )

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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        val fhirData = File(VALID_DATA_URL).readText()
        val fhirBundle = FhirTranscoder.decode(fhirData)

        val hl7v2Receiver = Receiver(
            RECEIVER_NAME, ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE,
            ORU_R01_SCHEMA, format = MimeFormat.HL7_BATCH,
        )
        val fhirReceiver = Receiver(
            "full-elr-fhir", ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE,
            "classpath:/metadata/fhir_transforms/receivers/fhir-transform-sample.yml", format = MimeFormat.FHIR,
        )
        val csvReceiver = Receiver(
            "full-elr-fhir", ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE, "", format = MimeFormat.CSV,
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
        mockkClass(BlobAccess::class)
        mockkObject(BlobAccess.Companion)
        every { BlobAccess.Companion.getBlobConnection(any()) } returns "testconnection"

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data_five_encoding_chars.fhir").readText()
        val fhirBundle = FhirTranscoder.decode(fhirData)

        val hl7v2Receiver = Receiver(
            RECEIVER_NAME, ORGANIZATION_NAME, Topic.FULL_ELR, CustomerStatus.ACTIVE,
            ORU_R01_SCHEMA, format = MimeFormat.HL7_BATCH,
        )
        val engine = makeFhirEngine()

        val hl7Message = engine.getHL7MessageFromBundle(fhirBundle, hl7v2Receiver)
        val strBody = hl7Message.encodePreserveEncodingChars()
        assertThat(strBody).isNotEmpty()
        assertThat(strBody.contains("MSH|^~\\&#")).isTrue()

        assertFailure { hl7Message.encode() }
    }
}