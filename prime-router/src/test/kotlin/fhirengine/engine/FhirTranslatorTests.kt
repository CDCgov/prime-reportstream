package gov.cdc.prime.router.fhirengine.engine

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
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
                "metadata/hl7_mapping/receivers/STLTs/CO/CO"
            ),
            Receiver(
                "elr",
                "co-phd",
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
        assertThat(terser.get("MSH-11-1")).isEqualTo("T")
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
        assertThat(terser.get("MSH-11-1")).isEqualTo("T")
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
        assertThat(terser.get("MSH-11-1")).isEqualTo("P")
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
        assertThat(terser.get("MSH-11-1")).isEqualTo("T")
    }

    @Test
    fun `test full elr translation happy path, receiver with condition filter so extensions`() {
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
            .returns(File("src/test/resources/fhirengine/engine/valid_data_with_extensions.fhir").readText())
        every { BlobAccess.Companion.uploadBlob(any(), any()) } returns "test"
        every { accessSpy.insertTask(any(), bodyFormat.toString(), bodyUrl, any()) }.returns(Unit)
        every { actionHistory.trackCreatedReport(any(), any(), any()) }.returns(Unit)
        every { actionHistory.trackExistingInputReport(any()) }.returns(Unit)
        every { queueMock.sendMessage(any(), any()) }
            .returns(Unit)

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)

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
            engine.removeUnwantedConditions(any(), any())
        }
    }

    @Test
    fun `test full elr translation hl7 translation exception`() {
        mockkObject(BlobAccess)

        val badSchemaOrganization = DeepOrganization(
            "co-phd",
            "test",
            Organization.Jurisdiction.FEDERAL,
            receivers = listOf(
                Receiver(
                    "full-elr-hl7",
                    "co-phd",
                    Topic.FULL_ELR,
                    CustomerStatus.ACTIVE,
                    "THIS_PATH_IS_BAD"
                )
            )
        )

        // set up
        val settings = FileSettings().loadOrganizations(badSchemaOrganization)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionHistory = mockk<ActionHistory>()
        val actionLogger = mockk<ActionLogger>()

        val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

        val bodyFormat = Report.Format.FHIR
        val bodyUrl = "http://anyblob.com"

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

        val engine = spyk(makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)

        // act
        engine.doWork(message, actionLogger, actionHistory)

        // assert
        verify(exactly = 1) {
            actionLogger.error(any<ActionLogDetail>())
        }
    }

    @Test
    fun `Test removing some filtered observations from a DiagnosticReport`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_some_filtered_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val endpoint = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()[0]

        var observations = getResource(bundle, "Observation")

        assertThat(observations.count()).isEqualTo(5)

        val updatedBundle = engine.removeUnwantedConditions(bundle, endpoint)

        observations = getResource(updatedBundle, "Observation")

        assertThat(observations.count()).isEqualTo(2)
    }

    @Test
    fun `Test removing all filtered observations from a DiagnosticReport`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_all_filtered_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        assertThat(provenance).isNotNull()
        val endpoint = provenance.target.map { it.resource }.filterIsInstance<Endpoint>()[0]
        assertThat(endpoint).isNotNull()
        var observations = getResource(bundle, "Observation")
        var diagnosticReport = getResource(bundle, "DiagnosticReport")

        assertThat(observations.count()).isEqualTo(3)
        assertThat(diagnosticReport.count()).isEqualTo(3)

        val updatedBundle = engine.removeUnwantedConditions(bundle, endpoint)

        observations = getResource(updatedBundle, "Observation")
        diagnosticReport = getResource(updatedBundle, "DiagnosticReport")
        assertThat(observations.count()).isEqualTo(1)
        assertThat(diagnosticReport.count()).isEqualTo(1)
    }

    @Test
    fun `Test observations are not removed if receiver Endpoint is not populated`() {
        val settings = FileSettings().loadOrganizations(oneOrganization)
        val one = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList())
        val metadata = Metadata(schema = one)
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val engine = (makeFhirEngine(metadata, settings, TaskAction.translate) as FHIRTranslator)
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

        val updatedBundle = engine.removeUnwantedConditions(bundle, endpoint)

        observations = getResource(updatedBundle, "Observation")
        diagnosticReport = getResource(updatedBundle, "DiagnosticReport")
        assertThat(observations.count()).isEqualTo(observationsCount)
        assertThat(diagnosticReport.count()).isEqualTo(diagnosticReportCount)
    }

    private fun getResource(bundle: Bundle, resource: String) =
        FhirPathUtils.evaluate(null, bundle, bundle, "Bundle.entry.resource.ofType($resource)")
}