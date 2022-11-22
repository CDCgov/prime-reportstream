package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.hl7v2.util.Terser
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
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.UUID
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FhirTranslatorTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "full-elr-hl7",
                "co-phd",
                Topic.FULL_ELR,
                CustomerStatus.ACTIVE,
                "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
            )
        )
    )
    private val colorado = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr.secondary",
                "co-phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "metadata/hl7_mapping/STLTs/CO/CO"
            ),
            Receiver(
                "elr",
                "co-phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "metadata/hl7_mapping/STLTs/CO/CO",
                Report.Format.CSV,
                null,
                null,
                null
            )
        )
    )

    private fun makeFhirEngine(metadata: Metadata, settings: SettingsProvider, taskAction: TaskAction): FHIREngine {
        return FHIREngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).build(taskAction)
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
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File("src/test/resources/fhirengine/engine/valid_data.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)

        val engine = makeFhirEngine(metadata, settings, TaskAction.translate)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            queueMock.sendMessage(any(), any())
        }
        verify(exactly = 1) {
            actionHistory.trackExistingInputReport(any())
            actionHistory.trackCreatedReport(any(), any(), any())
            BlobAccess.Companion.uploadBlob(any(), any())
            accessSpy.insertTask(any(), any(), any(), any())
        }
    }

    // happy path, with a receiver that has a custom schema
    @Test
    fun `test full elr translation happy path, custom schema`() {
        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(colorado)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = makeFhirEngine(metadata, settings, TaskAction.translate)
        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File("src/test/resources/fhirengine/engine/valid_data.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }.returns(Unit)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 0) {
            queueMock.sendMessage(any(), any())
        }
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
        val schemaName = "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
        val receiver = Receiver(
            "full-elr-hl7",
            "co-phd",
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = true, schemaName = schemaName)
        )

        val testOrg = DeepOrganization(
            "co-phd", "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assert(terser.get("MSH-11-1") == "T")
    }

    /**
     * When the receiver is in active mode but the override boolean is set to true,
     * all output HL7 should have processingId = T
     */
    @Test
    fun `test when customerStatus = active, useTestProcessingMode = true`() {
        val schemaName = "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
        // set up
        val receiver = Receiver(
            "full-elr-hl7",
            "co-phd",
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = true, schemaName = schemaName)
        )
        val testOrg = DeepOrganization(
            "co-phd", "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assert(terser.get("MSH-11-1") == "T")
    }

    /**
     * When the receiver is in production mode and sender is in production mode, output HL7 should be 'P'
     */
    @Test
    fun `test when customerStatus = active, useTestProcessingMode = false, P from sender`() {
        // set up
        val schemaName = "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
        val receiver = Receiver(
            "full-elr-hl7",
            "co-phd",
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = false, schemaName = schemaName)
        )

        val testOrg = DeepOrganization(
            "co-phd", "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        val code = terser.get("MSH-11-1")
        assert(code == "P")
    }

    /**
     * When the receiver is in production mode and sender is in testing mode, output HL7 should be 'T'
     */
    @Test
    fun `test when customerStatus = active, useTestProcessingMode = false, T from sender`() {
        // set up
        val schemaName = "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
        val receiver = Receiver(
            "full-elr-hl7",
            "co-phd",
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            schemaName,
            translation = UnitTestUtils.createConfig(useTestProcessingMode = false, schemaName = schemaName)
        )

        val testOrg = DeepOrganization(
            "co-phd", "test", Organization.Jurisdiction.FEDERAL,
            receivers = listOf(receiver)
        )

        val settings = FileSettings().loadOrganizations(testOrg)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)

        // dodo - make Testing sender fhir

        val fhirData = File("src/test/resources/fhirengine/engine/valid_data_testing_sender.fhir").readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)

        // act
        val hl7Message = engine.getHL7MessageFromBundle(bundle, receiver)
        val terser = Terser(hl7Message)

        // assert
        assert(terser.get("MSH-11-1") == "T")
    }
}