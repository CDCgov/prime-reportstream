package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
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
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Provenance
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.UUID
import kotlin.test.Test

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val ORU_R01_SCHEMA = "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
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
                format = Report.Format.HL7_BATCH,
            )
        )
    )
    private val colorado = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr.secondary",
                ORGANIZATION_NAME,
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "metadata/hl7_mapping/receivers/STLTs/CO/CO"
            ),
            Receiver(
                "elr",
                ORGANIZATION_NAME,
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "metadata/hl7_mapping/receivers/STLTs/CO/CO",
                Report.Format.CSV,
                null,
                null,
                null
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
            .blobAccess(blobMock).queueAccess(queueMock).build(TaskAction.translate) as FHIRTranslator
    }

    private fun getResource(bundle: Bundle, resource: String) =
        FhirPathUtils.evaluate(null, bundle, bundle, "Bundle.entry.resource.ofType($resource)")

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
            spyk(RawSubmission(UUID.randomUUID(), BLOB_URL, "test", BLOB_SUB_FOLDER, topic = Topic.FULL_ELR))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }
            .returns(Unit)

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
            actionHistory.trackActionReceiverInfo(any(), any())
        }
    }

    // happy path, with a receiver that has a custom schema
    @Test
    fun `test full elr translation happy path, custom schema`() {
        mockkObject(BlobAccess)

        // set up
        val settings = FileSettings().loadOrganizations(colorado)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val engine = makeFhirEngine(settings = settings)
        val message = spyk(RawSubmission(UUID.randomUUID(), BLOB_URL, "test", BLOB_SUB_FOLDER, Topic.FULL_ELR))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File(VALID_DATA_URL).readText())
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

    @Test
    fun `test full elr translation happy path, receiver with condition filter so extensions`() {
        mockkObject(BlobAccess)

        // set up
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val message = spyk(RawSubmission(UUID.randomUUID(), BLOB_URL, "test", BLOB_SUB_FOLDER, Topic.FULL_ELR))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { message.downloadContent() }
            .returns(File("src/test/resources/fhirengine/engine/valid_data_with_extensions.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }.returns(Unit)

        val engine = spyk(makeFhirEngine())

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
            engine.pruneBundleForReceiver(any(), any())
        }
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

        val message = spyk(RawSubmission(UUID.randomUUID(), BLOB_URL, "test", BLOB_SUB_FOLDER, Topic.FULL_ELR))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = BODY_URL

        every { actionLogger.hasErrors() } returns false
        every { actionLogger.error(any<ActionLogDetail>()) } returns Unit
        every { message.downloadContent() }
            .returns(File("src/test/resources/fhirengine/engine/valid_data_with_extensions.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)
        every { actionHistory.trackActionReceiverInfo(any(), any()) }.returns(Unit)

        val engine = spyk(makeFhirEngine(settings = settings))

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionLogger.error(any<ActionLogDetail>())
        }
    }

    @Test
    fun `Test removing some filtered observations from a DiagnosticReport`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_some_filtered_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val engine = makeFhirEngine()
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val endpoint = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()[0]

        var observations = getResource(bundle, "Observation")

        assertThat(observations.count()).isEqualTo(5)

        val updatedBundle = with(engine) { bundle.removeUnwantedConditions(endpoint) }

        observations = getResource(updatedBundle, "Observation")

        assertThat(observations.count()).isEqualTo(2)
    }

    @Test
    fun `Test removing all filtered observations from a DiagnosticReport`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_all_filtered_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val engine = makeFhirEngine()
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(provenance).isNotNull()
        val endpoint = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()[0]
        assertThat(endpoint).isNotNull()
        var observations = getResource(bundle, "Observation")
        var diagnosticReport = getResource(bundle, "DiagnosticReport")

        assertThat(observations.count()).isEqualTo(3)
        assertThat(diagnosticReport.count()).isEqualTo(3)

        val updatedBundle = with(engine) { bundle.removeUnwantedConditions(endpoint) }

        observations = getResource(updatedBundle, "Observation")
        diagnosticReport = getResource(updatedBundle, "DiagnosticReport")
        assertThat(observations.count()).isEqualTo(1)
        assertThat(diagnosticReport.count()).isEqualTo(1)
    }

    @Test
    fun `Test observations are not removed if receiver Endpoint is not populated`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(VALID_DATA_URL).readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val engine = makeFhirEngine()
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(provenance).isNotNull()
        val endpoint = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()[0]
        assertThat(endpoint).isNotNull()
        var observations = getResource(bundle, "Observation")
        var diagnosticReport = getResource(bundle, "DiagnosticReport")
        val observationsCount = observations.count()
        val diagnosticReportCount = diagnosticReport.count()
        assertThat(observationsCount).isEqualTo(3)
        assertThat(diagnosticReportCount).isEqualTo(3)

        val updatedBundle = with(engine) { bundle.removeUnwantedConditions(endpoint) }

        observations = getResource(updatedBundle, "Observation")
        diagnosticReport = getResource(updatedBundle, "DiagnosticReport")
        assertThat(observations.count()).isEqualTo(observationsCount)
        assertThat(diagnosticReport.count()).isEqualTo(diagnosticReportCount)
    }

    @Test
    fun `Test remove provenance targets`() {
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data_multiple_targets.fhir").readText()
        val origBundle = FhirTranscoder.decode(fhirBundle)
        assertThat(origBundle).isNotNull()
        val engine = makeFhirEngine()
        val origProv = origBundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(origProv).isNotNull()
        assertThat(origProv.target.size).isEqualTo(4)
        val origEndpoints = origProv.target.map { it.resource }.filterIsInstance<Endpoint>()
        assertThat(origEndpoints.size).isEqualTo(3)

        var bundle = origBundle.copy()
        bundle = with(engine) { bundle.removeUnwantedProvenanceEndpoints(origEndpoints[0]) }
        var provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(provenance.target.size).isEqualTo(2) // Still has all non-endpoint items
        var endpoints = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()
        assertThat(endpoints.size).isEqualTo(1)
        assertThat(endpoints[0]).isEqualTo(origEndpoints[0])

        bundle = origBundle.copy()
        bundle = with(engine) { bundle.removeUnwantedProvenanceEndpoints(origEndpoints[2]) }
        provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(provenance.target.size).isEqualTo(2) // Still has all non-endpoint items
        endpoints = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()
        assertThat(endpoints.size).isEqualTo(1)
        assertThat(endpoints[0]).isEqualTo(origEndpoints[2])

        bundle = origBundle.copy()
        val otherEndpoint = origEndpoints[0].copy()
        otherEndpoint.name += "other"
        bundle = with(engine) { bundle.removeUnwantedProvenanceEndpoints(otherEndpoint) }
        provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(provenance.target.size).isEqualTo(1) // Still has all non-endpoint items
        endpoints = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()
        assertThat(endpoints.size).isEqualTo(0) // Removed all endpoints
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

        assertThat { engine.getByteArrayFromBundle(csvReceiver, fhirBundle) }.isFailure()
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

        assertThat { hl7Message.encode() }.isFailure()
    }
}