package gov.cdc.prime.router.fhirengine.engine.fhirRouterTests

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.fhirengine.engine.RawSubmission
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.deleteChildlessResource
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.deleteResource
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.getResourceProperties
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.getResourceReferences
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.spyk
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.lang.IllegalStateException
import java.util.UUID
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FHIRBundleHelpersTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val metadata = Metadata(schema = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList()))
    val bodyUrl = "http://anyblob.com"
    private val defaultReceivers = listOf(
        Receiver(
            "full-elr-hl7",
            "co-phd",
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            "one"
        ),
        Receiver(
            "full-elr-hl7-2",
            "co-phd",
            Topic.FULL_ELR,
            CustomerStatus.INACTIVE,
            "one"
        )
    )
    val oneOrganization = DeepOrganization(
        "co-phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = defaultReceivers
    )

    val message = spyk(RawSubmission(UUID.randomUUID(), "http://blob.url", "test", "test-sender"))

    private val validFhirWithProvenance = """
    {
        "resourceType": "Bundle",
        "id": "1666038428133786000.94addcb6-835c-4883-a095-0c50cf113744",
        "meta": {
        "lastUpdated": "2022-10-17T20:27:08.149+00:00",
        "security": [
            {
                "code": "SECURITY",
                "display": "SECURITY"
            }
        ]
    },
        "identifier": {
        "value": "MT_COCAA_ORU_AAPHELR.1.6214638"
    },
        "type": "message",
        "timestamp": "2028-08-08T15:28:05.000+00:00",
        "entry": [
            {
                "fullUrl": "Provenance/1666038430962443000.9671377b-8f2b-4f5c-951c-b43ca8fd1a25",
                "resource": {
                    "resourceType": "Provenance",
                    "id": "1666038430962443000.9671377b-8f2b-4f5c-951c-b43ca8fd1a25",
                    "recorded": "2028-08-08T09:28:05-06:00",
                    "activity": {
                        "coding": [
                            {
                                "system": "http://terminology.hl7.org/CodeSystem/v2-0003",
                                "code": "R01",
                                "display": "ORU_R01"
                            }
                        ]
                    }
                }
            }
        ]
    }
    """

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test adding receivers to bundle`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = listOf(oneOrganization.receivers[0])

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "co-phd.full-elr-hl7")
    }

    @Test
    fun `test skipping inactive receivers (only inactive)`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = listOf(oneOrganization.receivers[1])

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isEmpty())
    }

    @Test
    fun `test skipping inactive receivers (mixed)`() {
        // set up
        val bundle = FhirTranscoder.decode(validFhirWithProvenance)
        val receiversIn = oneOrganization.receivers

        // act
        FHIRBundleHelpers.addReceivers(bundle, receiversIn)

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val receiversOut = outs.map { (it.resource as Endpoint).identifier[0].value }
        assert(receiversOut.isNotEmpty())
        assert(receiversOut[0] == "co-phd.full-elr-hl7")
    }

    @Test
    fun `Test retrieving references for a Bundle resource`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        val diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        )[0]

        assertThat(diagnosticReport).isNotNull()
        val diagnosticReportReferences = diagnosticReport.getResourceReferences()

        assertThat(diagnosticReportReferences.count()).isEqualTo(5)
    }

    @Test
    fun `Test retrieving properties for a Bundle resource`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()

        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        val diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        )[0]

        assertThat(diagnosticReport).isNotNull()
        assertThat(diagnosticReport.getResourceProperties()).isNotEmpty()
    }

    @Test
    fun `Test find Diagnostic report no observation`() {
        val actionLogger = ActionLogger()
        val fhirBundle =
            File("src/test/resources/fhirengine/engine/bundle_diagnostic_report_no_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        assertThat(FHIRBundleHelpers.getDiagnosticReportNoObservations(bundle).count()).isEqualTo(1)
    }

    @Test
    fun `Test removing Diagnostic report no observation`() {
        val actionLogger = ActionLogger()
        val fhirBundle =
            File("src/test/resources/fhirengine/engine/bundle_diagnostic_report_no_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        assertThat(FHIRBundleHelpers.getDiagnosticReportNoObservations(bundle).count()).isEqualTo(1)
        messages[0].deleteChildlessResource(Observation())
        assertThat(FHIRBundleHelpers.getDiagnosticReportNoObservations(bundle)).isEmpty()
    }

    @Test
    fun `Test Removing observation from diagnostic report with single observation`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        val diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        )[0]

        val observation = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            diagnosticReport,
            bundle,
            "%resource.result[0].resolve()"
        )[0]

        assertThat(diagnosticReport).isNotNull()
        assertThat(observation).isNotNull()

        val observationReferences = observation.getResourceReferences()

        assertThat(observationReferences.count()).isEqualTo(4)

        // Verify that the bundle contains the Diagnostic Report and observation being removed
        assertThat(
            bundle.entry.find {
                it.fullUrl == diagnosticReport.idBase
            }
        ).isNotNull()
        assertThat(
            bundle.entry.find {
                it.fullUrl == observation.idBase
            }
        ).isNotNull()

        bundle.deleteResource(observation)

        // Verify that the bundle no longer contains the Diagnostic Report that contained the observation
        assertThat(
            bundle.entry.find {
                it.fullUrl == diagnosticReport.idBase
            }
        ).isNull()

        // Verify that the bundle no longer contains the observation deleted
        assertThat(
            bundle.entry.find {
                it.fullUrl == observation.idBase
            }
        ).isNull()

        // Verify that the resources referenced in this observation got removed
        // The observation being deleted has four references.
        // Two are referenced in other resources(Patient and Encounter) and should not be removed.
        // The other two (Practitioner and Organization) should be removed
        assertThat(
            bundle.entry.find {
                it.fullUrl == observationReferences.first { it.contains("Encounter") }
            }
        ).isNotNull()

        assertThat(
            bundle.entry.find {
                it.fullUrl == observationReferences.first { it.contains("Patient") }
            }
        ).isNotNull()

        assertThat(
            bundle.entry.find {
                it.fullUrl == observationReferences.first { it.contains("Organization") }
            }
        ).isNull()

        assertThat(
            bundle.entry.find {
                it.fullUrl == observationReferences.first { it.contains("Practitioner") }
            }
        ).isNull()
    }

    @Test
    fun `Test Removing observation from diagnostic report with multiple observations`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val diagnosticReport = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
        )[0]

        val observation = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            diagnosticReport,
            bundle,
            "%resource.result[0].resolve()"
        )[0]

        assertThat(observation).isNotNull()
        assertThat(diagnosticReport).isNotNull()

        // Check that diagnostic report has multiple observations
        // and contains the observation being removed
        assertThat(diagnosticReport.getResourceReferences().count() > 1).isTrue()
        assertThat(diagnosticReport.getResourceReferences().contains(observation.idBase)).isTrue()
        messages[0].deleteResource(observation)
        // Check that the diagnostic report still has multiple observations and
        // no longer contains the observation removed
        assertThat(diagnosticReport.getResourceReferences().count() > 1).isTrue()
        assertThat(diagnosticReport.getResourceReferences().contains(observation.idBase)).isFalse()
    }

    @Test
    fun `Test removing Provenance from Bundle`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val provenance = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Provenance)"
        )[0]
        assertThat(provenance).isNotNull()

        val organizationReference = (provenance as Provenance).agent[0].who.reference
        val deviceReference = provenance.entity[0].what.reference

        assertThat(organizationReference).isNotNull()
        assertThat(deviceReference).isNotNull()
        // Verify that Provenance and its organization and device are present in the bundle
        assertThat(
            bundle.entry.find {
                it.fullUrl == organizationReference
            }
        ).isNotNull()
        assertThat(
            bundle.entry.find {
                it.fullUrl == deviceReference
            }
        ).isNotNull()
        assertThat(
            bundle.entry.find {
                it.fullUrl == provenance.id
            }
        ).isNotNull()

        bundle.deleteResource(provenance)

        // Verify that  Provenance and its organization and device are removed from the bundle
        assertThat(
            bundle.entry.find {
                it.fullUrl == provenance.id
            }
        ).isNull()

        assertThat(
            bundle.entry.find {
                it.fullUrl == organizationReference
            }
        ).isNull()
        assertThat(
            bundle.entry.find {
                it.fullUrl == deviceReference
            }
        ).isNull()
    }

    @Test
    fun `Test Removing an observation that doesn't exist`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        val observation = Observation()
        observation.id = "test"

        assertFailsWith<IllegalStateException> { messages[0].deleteResource(observation) }
    }

    @Test
    fun `Test getChildProperties from a Property`() {

        val diagnosticReport = DiagnosticReport()
        val observation = Observation()
        observation.id = "Observation/123"
        diagnosticReport.id = "DiagnosticReport/123"
        val reference = Reference(observation)
        diagnosticReport.result.add(reference)
        val property = Property("Diagnostic Report", null, null, 0, 0, diagnosticReport)

        assertThat(FHIRBundleHelpers.getChildProperties(property).collect(Collectors.toList())).isNotEmpty()
    }

    @Test
    fun `Test getting nested references from a Resource`() {
        val observation = Observation()
        observation.id = "Observation/123"

        val practitioner = PractitionerRole()
        practitioner.id = "Practitioner/123"
        val practitionerReference = Reference(practitioner)
        practitionerReference.reference = practitioner.id
        observation.performer.add(practitionerReference)

        val organization = org.hl7.fhir.r4.model.Organization()
        organization.id = "Organization/123"
        val organizationReference = Reference(organization)
        organizationReference.reference = organization.id
        observation.extension.add(Extension("", organizationReference))

        val references = FHIRBundleHelpers.filterReferenceProperties(observation.getResourceProperties())
        assertThat(references).isNotEmpty()
        assertThat(references.count()).isEqualTo(2)
    }
}