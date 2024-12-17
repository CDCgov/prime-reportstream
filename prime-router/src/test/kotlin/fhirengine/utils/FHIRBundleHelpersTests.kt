package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.each
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.v251.segment.MSH
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.CodeStringConditionFilter
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.ConditionStamper
import gov.cdc.prime.router.azure.ConditionStamper.Companion.conditionCodeExtensionURL
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.LookupTableConditionMapper
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.spyk
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.Date
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val ORGANIZATION_NAME = "co-phd"
private const val RECEIVER_NAME = "full-elr-hl7"
private const val VALID_ROUTING_DATA_URL = "src/test/resources/fhirengine/engine/routing/valid.fhir"
private const val VALID_DATA_URL = "src/test/resources/fhirengine/engine/valid_data.fhir"
private const val DIAGNOSTIC_REPORT_EXPRESSION = "Bundle.entry.resource.ofType(DiagnosticReport)[0]"
private const val MULTIPLE_OBSERVATIONS_URL = "src/test/resources/fhirengine/engine/bundle_multiple_observations.fhir"
private const val OBSERVATIONS_FILTER = "%resource.code.coding.code.intersect('94558-5').exists()"

class FHIRBundleHelpersTests {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val blobMock = mockkClass(BlobAccess::class)
    val metadata = Metadata(schema = Schema(name = "None", topic = Topic.FULL_ELR, elements = emptyList()))
    private val shorthandLookupTable = emptyMap<String, String>().toMutableMap()

    private val defaultReceivers = listOf(
        Receiver(
            RECEIVER_NAME,
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            "one"
        ),
        Receiver(
            "$RECEIVER_NAME-2",
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.INACTIVE,
            "one"
        )
    )

    val oneOrganization = DeepOrganization(
        ORGANIZATION_NAME,
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = defaultReceivers
    )

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test adding diagnosticreport references to bundle`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File(VALID_DATA_URL).readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // act
        bundle.addProvenanceReference()

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val references = outs.filterNot { it.resource is Endpoint }
            .map { it.reference }
            .filter { it.substringBefore(delimiter = "/", missingDelimiterValue = "none") == "DiagnosticReport" }
        assertThat(references).isNotEmpty()
    }

    @Test
    fun `test adding diagnosticreport references to bundle when no original provenance`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data_no_provenance.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // act
        bundle.addProvenanceReference()

        // assert
        val provenance = bundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        val outs = provenance.target
        val references = outs.filterNot { it.resource is Endpoint }
            .map { it.reference }
            .filter { it.substringBefore(delimiter = "/", missingDelimiterValue = "none") == "DiagnosticReport" }
        assertThat(references).isNotEmpty()
    }

    @Test
    fun `Test retrieving references for a Bundle resource`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(VALID_DATA_URL).readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        val diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            DIAGNOSTIC_REPORT_EXPRESSION
        )[0]

        assertThat(diagnosticReport).isNotNull()
        val diagnosticReportReferences = diagnosticReport.getResourceReferences()

        assertThat(diagnosticReportReferences.count()).isEqualTo(5)
    }

    @Test
    fun `Test retrieving properties for a Bundle resource`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(VALID_DATA_URL).readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()

        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        val diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            DIAGNOSTIC_REPORT_EXPRESSION
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
        assertThat(bundle.getDiagnosticReportNoObservations().count()).isEqualTo(1)
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
        assertThat(bundle.getDiagnosticReportNoObservations().count()).isEqualTo(1)
        messages[0].deleteChildlessResource(Observation())
        assertThat(bundle.getDiagnosticReportNoObservations()).isEmpty()
    }

    @Test
    fun `Test Removing observation from diagnostic report with single observation`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(VALID_DATA_URL).readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        val diagnosticReport = FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
            DIAGNOSTIC_REPORT_EXPRESSION
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
            bundle.entry.find { bundleEntry ->
                bundleEntry.fullUrl == observationReferences.first { it.contains("Encounter") }
            }
        ).isNotNull()

        assertThat(
            bundle.entry.find { bundleEntryComponent ->
                bundleEntryComponent.fullUrl == observationReferences.first { it.contains("Patient") }
            }
        ).isNotNull()

        assertThat(
            bundle.entry.find { bundleEntryComponent ->
                bundleEntryComponent.fullUrl == observationReferences.first { it.contains("Organization") }
            }
        ).isNull()

        assertThat(
            bundle.entry.find { bundleEntryComponent ->
                bundleEntryComponent.fullUrl == observationReferences.first { it.contains("Practitioner") }
            }
        ).isNull()
    }

    @Test
    fun `Test Removing observation from diagnostic report with multiple observations`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(MULTIPLE_OBSERVATIONS_URL).readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        val diagnosticReport = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            DIAGNOSTIC_REPORT_EXPRESSION
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
        val fhirBundle = File(VALID_DATA_URL).readText()
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
        val fhirBundle = File(MULTIPLE_OBSERVATIONS_URL).readText()
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

    @Test
    fun `test getObservationExtensions`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(MULTIPLE_OBSERVATIONS_URL)
            .readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)

        val receiver = Receiver(
            "$RECEIVER_NAME-2",
            ORGANIZATION_NAME,
            Topic.FULL_ELR,
            CustomerStatus.ACTIVE,
            "one",
            conditionFilter = listOf("%resource.code.coding.code.intersect('94558-5').exists()")

        )

        shorthandLookupTable["obsPerformedCodes"] = "%resource.code.coding.code"

        val extensions = getObservationExtensions(messages[0], receiver, shorthandLookupTable)
        assertThat(extensions.size).isEqualTo(1)
        assertThat((extensions[0].value as Reference).reference)
            .isEqualTo("Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f")
    }

    @Test
    fun `test filterObservations`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File(MULTIPLE_OBSERVATIONS_URL)
            .readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)

        val bundle = messages[0].filterObservations(
            listOf(OBSERVATIONS_FILTER),
            emptyMap<String, String>().toMutableMap()
        )

        val observations = bundle.getObservations()

        assertThat(observations.size).isEqualTo(1)
        assertThat(observations[0].id).isEqualTo("Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f")
    }

    @Test
    fun `test filterMappedObservations`() {
        val fhirRecord = File(VALID_ROUTING_DATA_URL).readText()
        val bundle = FhirContext.forR4().newJsonParser().parseResource(Bundle::class.java, fhirRecord)
        bundle.getObservations()[0].code.coding[0].addExtension(
            conditionCodeExtensionURL, Coding("SOMESYSTEM", "840539006", "SOMECONDITION")
        )

        val filteredBundle = bundle.filterMappedObservations(
            listOf(CodeStringConditionFilter("840539006"))
        ).second

        val filteredObservations = filteredBundle.getObservations()
        assertThat(filteredObservations.size).isEqualTo(1)
        assertThat(filteredObservations[0].id)
            .isEqualTo("Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f")
    }

    @Test
    fun `test batchMessages`() {
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/bundle_multiple_bundles.fhir")
            .readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger).map { FhirTranscoder.encode(it) }
        assertThat(messages.size).isGreaterThan(1)

        val batchedMessages = FHIRBundleHelpers.batchMessages(messages)
        assertThat(batchedMessages.split('\n')).isEqualTo(messages)

        val emptyBatch = FHIRBundleHelpers.batchMessages(listOf())
        assertThat(emptyBatch).isEqualTo("")
    }

    @Test
    fun `Test removing PHI data from bundle`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/valid_data.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()
        var patient = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Patient)"
        )[0] as Patient

        assertThat(patient).isNotNull()
        assertThat(patient.address[0].city).isNotNull()
        assertThat(patient.address[0].line).isNotEmpty()
        assertThat(patient.telecom).isNotEmpty()
        assertThat(patient.birthDate).isNotNull()
        assertThat(patient.deceased).isNotNull()
        assertThat(patient.identifier).isNotEmpty()
        assertThat(patient.contact).isNotEmpty()

        bundle.removePHI()

        patient = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Patient)"
        )[0] as Patient

        assertThat(patient).isNotNull()
        assertThat(patient.address[0].city).isNull()
        assertThat(patient.address[0].line).isEmpty()
        assertThat(patient.telecom).isEmpty()
        assertThat(patient.birthDate).isNull()
        assertThat(patient.deceased).isNull()
        assertThat(patient.identifier).isEmpty()
        assertThat(patient.contact).isEmpty()
    }

    @Test
    fun `Test birth date time`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/fhir_without_birth_time.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // create the hl7 message
        val hl7Message = File("src/test/resources/fhirengine/engine/hl7_with_birth_time.hl7").readText()
        val parsedHl7Message = Hl7InputStreamMessageIterator(hl7Message.byteInputStream()).next()

        bundle.handleBirthTime(parsedHl7Message)

        val patient = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Patient)"
        )[0] as Patient

        assertThat(patient).isNotNull()
        val birthDateTimeValue = patient.birthDateElement.extension[0].value.primitiveValue()
        assertThat(
            birthDateTimeValue
        ).isEqualTo("2023-05-04T13:10:23-05:00")
    }

    @Test
    fun `Test birth date time no patient`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/fhir_without_patient.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // create the hl7 message
        val hl7Message = File("src/test/resources/fhirengine/engine/hl7_with_birth_time.hl7").readText()
        val parsedHl7Message = Hl7InputStreamMessageIterator(hl7Message.byteInputStream()).next()

        bundle.handleBirthTime(parsedHl7Message)

        val patient = FhirPathUtils.evaluate(
            CustomContext(bundle, bundle),
            bundle,
            bundle,
            "Bundle.entry.resource.ofType(Patient)"
        )[0] as Patient

        assertThat(patient).isNotNull()
        val birthDateTimeValue = patient.birthDateElement.extension[0].value.primitiveValue()
        assertThat(
            birthDateTimeValue
        ).isEqualTo("2023-05-04T13:10:23-05:00")
    }

    @Test
    fun `Test enhance bundle metadata 2-5-1`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/fhir_without_patient.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // create the hl7 message
        val hl7Message = File("src/test/resources/fhirengine/engine/hl7_with_birth_time.hl7").readText()
        val parsedHl7Message = Hl7InputStreamMessageIterator(hl7Message.byteInputStream()).next()

        assertThat(parsedHl7Message["MSH"] is MSH).isTrue()

        bundle.enhanceBundleMetadata(parsedHl7Message)

        val expectedDate = Date(1612994857000) // Wednesday, February 10, 2021 10:07:37 PM GMT
        assertThat(bundle.timestamp).isEqualTo(expectedDate)
        assertThat(bundle.identifier.value).isEqualTo("371784")
        assertThat(bundle.identifier.system).isEqualTo("https://reportstream.cdc.gov/prime-router")
    }

    @Test
    fun `Test enhance bundle metadata 2-7`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/fhir_without_patient.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // create the hl7 message
        val hl7Message = File("src/test/resources/fhirengine/engine/hl7_2.7.hl7").readText()
        val parsedHl7Message = Hl7InputStreamMessageIterator(hl7Message.byteInputStream()).next()

        assertThat(parsedHl7Message["MSH"] is ca.uhn.hl7v2.model.v27.segment.MSH).isTrue()

        bundle.enhanceBundleMetadata(parsedHl7Message)

        val expectedDate = Date(1612994857000) // Wednesday, February 10, 2021 10:07:37 PM GMT
        assertThat(bundle.timestamp).isEqualTo(expectedDate)
        assertThat(bundle.identifier.value).isEqualTo("371785")
        assertThat(bundle.identifier.system).isEqualTo("https://reportstream.cdc.gov/prime-router")
    }

    @Test
    fun `Test enhance bundle metadata - unexpected message version`() {
        // set up
        val actionLogger = ActionLogger()
        val fhirBundle = File("src/test/resources/fhirengine/engine/fhir_without_patient.fhir").readText()
        val messages = FhirTranscoder.getBundles(fhirBundle, actionLogger)
        assertThat(messages).isNotEmpty()
        val bundle = messages[0]
        assertThat(bundle).isNotNull()

        // create the hl7 message
        val hl7Message = File("src/test/resources/fhirengine/engine/hl7_2.6.hl7").readText()
        val parsedHl7Message = Hl7InputStreamMessageIterator(hl7Message.byteInputStream()).next()

        assertThat(parsedHl7Message["MSH"] is MSH).isFalse()
        assertThat(parsedHl7Message["MSH"] is ca.uhn.hl7v2.model.v27.segment.MSH).isFalse()

        bundle.enhanceBundleMetadata(parsedHl7Message)

        assertThat(bundle.timestamp).isNull()
        assertThat(bundle.identifier.value).isNull()
        assertThat(bundle.identifier.system).isEqualTo("https://reportstream.cdc.gov/prime-router")
    }

    @Test
    fun `Ensure a fully unmappable observation logs the unmapped code`() {
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                    listOf(
                        ObservationMappingConstants.TEST_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                        ObservationMappingConstants.CONDITION_NAME_KEY
                    ),
                    listOf(
                        "80382-5",
                        "6142004",
                        "SNOMEDCT",
                        "Influenza (disorder)"
                    ),
                    listOf(
                        "260373001",
                        "Some Condition Code",
                        "Condition Code System",
                        "Condition Name"
                    )
                )
            )
        )
        val stamper = ConditionStamper(LookupTableConditionMapper(metadata))

        val entry = Observation()
        val code = CodeableConcept()
        val coding = Coding("system", "some-unmapped-code", "display")
        code.addCoding(coding)
        entry.setCode(code)

        val result = stamper.stampObservation(entry)
        assertThat(result.success).isFalse()
        assertThat(result.failures).hasSize(1)

        val failure = result.failures.first()
        assertThat(failure.source).isEqualTo("observation.code.coding.code")
        assertThat(failure.failures).hasSize(1)
        assertThat(failure.failures.first()).isEqualTo(coding)
    }

    @Test
    fun `Ensure a partially mapped observation is stamped and does not log an unmapped code`() {
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                    listOf(
                        ObservationMappingConstants.TEST_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                        ObservationMappingConstants.CONDITION_NAME_KEY
                    ),
                    listOf(
                        "80382-5",
                        "6142004",
                        "SNOMEDCT",
                        "Influenza (disorder)"
                    ),
                    listOf(
                        "260373001",
                        "Some Condition Code",
                        "Condition Code System",
                        "Condition Name"
                    )
                )
            )
        )
        val stamper = ConditionStamper(LookupTableConditionMapper(metadata))

        val entry = Observation()
        val code = CodeableConcept()
        code.addCoding(Coding("system", "80382-5", "display"))
        code.addCoding(Coding("system", "some-unmapped-code", "display"))
        entry.setCode(code)

        val result = stamper.stampObservation(entry)
        assertThat(result.success).isTrue()
        assertThat(result.failures).hasSize(1)

        val failure = result.failures.first()
        assertThat(failure.source).isEqualTo("observation.code.coding.code")
        assertThat(failure.failures).hasSize(1)
        assertThat(failure.failures.first().code).isEqualTo("some-unmapped-code")

        val extension = code.coding.first().extension.first()
        assertThat(extension.url).isEqualTo(conditionCodeExtensionURL)
        assertThat((extension.value as? Coding)?.code).isEqualTo("6142004")
    }

    @Test
    fun `Ensure a mapped observation is stamped with all condition codes if there are multiple`() {
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                    listOf(
                        ObservationMappingConstants.TEST_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                        ObservationMappingConstants.CONDITION_NAME_KEY
                    ),
                    listOf(
                        "80382-5",
                        "6142004",
                        "SNOMEDCT",
                        "Influenza (disorder)"
                    ),
                    listOf(
                        "80382-5",
                        "Some Condition Code",
                        "Condition Code System",
                        "Condition Name"
                    )
                )
            )
        )
        val stamper = ConditionStamper(LookupTableConditionMapper(metadata))

        val entry = Observation()
        val code = CodeableConcept()
        code.addCoding(Coding("system", "80382-5", "display"))
        entry.setCode(code)

        val result = stamper.stampObservation(entry)
        assertThat(result.success).isTrue()
        assertThat(result.failures).isEmpty()

        val conditions = entry.getMappedConditions()
        assertThat(conditions).hasSize(2)
        assertThat(conditions)
            .extracting { it.code }
            .containsExactlyInAnyOrder("6142004", "Some Condition Code")

        val extensions = entry.getMappedConditionExtensions()
        assertThat(extensions)
            .extracting { it.url }
            .each { it.isEqualTo(conditionCodeExtensionURL) }
    }

    @Test
    fun `addMappedCondition supports processing values other than codeable concept`() {
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                    listOf(
                        ObservationMappingConstants.TEST_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                        ObservationMappingConstants.CONDITION_NAME_KEY
                    ),
                    listOf(
                        "80382-5",
                        "6142004",
                        "SNOMEDCT",
                        "Influenza (disorder)"
                    ),
                    listOf(
                        "260373001",
                        "Some Condition Code",
                        "Condition Code System",
                        "Condition Name"
                    )
                )
            )
        )
        val stamper = ConditionStamper(LookupTableConditionMapper(metadata))

        val entry = Observation()
        val code = CodeableConcept()
        code.addCoding(Coding("system", "80382-5", "display"))
        entry.setCode(code)

        entry.setValue(StringType("A string value"))

        val result = stamper.stampObservation(entry)
        assertThat(result.success).isTrue()
        assertThat(result.failures).isEmpty()

        val extension = code.coding.first().extension.first()
        assertThat(extension.url).isEqualTo(conditionCodeExtensionURL)
        assertThat(extension.value)
            .isInstanceOf<Coding>()
            .transform { it.code }
            .isEqualTo("6142004")
    }

    @Test
    fun `addMappedCondition supports adding a condition when code is null`() {
        val metadata = Metadata(UnitTestUtils.simpleSchema)

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                    listOf(
                        ObservationMappingConstants.TEST_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                        ObservationMappingConstants.CONDITION_NAME_KEY
                    ),
                    listOf(
                        "80382-5",
                        "6142004",
                        "SNOMEDCT",
                        "Influenza (disorder)"
                    ),
                    listOf(
                        "260373001",
                        "Some Condition Code",
                        "Condition Code System",
                        "Condition Name"
                    )
                )
            )
        )
        val stamper = ConditionStamper(LookupTableConditionMapper(metadata))

        val entry = Observation()
        val code = CodeableConcept()
        code.addCoding(Coding("system", "80382-5", "display"))
        entry.setValue(code)

        val result = stamper.stampObservation(entry)
        assertThat(result.success).isTrue()
        assertThat(result.failures).isEmpty()

        val extension = code.coding.first().extension.first()
        assertThat(extension.url).isEqualTo(conditionCodeExtensionURL)
        assertThat(extension.value)
            .isInstanceOf<Coding>()
            .transform { it.code }
            .isEqualTo("6142004")
    }
}