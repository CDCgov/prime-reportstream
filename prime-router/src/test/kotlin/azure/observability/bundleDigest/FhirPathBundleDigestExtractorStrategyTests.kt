package gov.cdc.prime.router.azure.observability.bundleDigest

import assertk.assertThat
import assertk.assertions.isDataClassEqualTo
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.azure.ConditionStamper.Companion.CONDITION_CODE_EXTENSION_URL
import gov.cdc.prime.router.azure.observability.event.CodeSummary
import gov.cdc.prime.router.azure.observability.event.ObservationSummary
import gov.cdc.prime.router.azure.observability.event.OrderingFacilitySummary
import gov.cdc.prime.router.azure.observability.event.PerformerSummary
import gov.cdc.prime.router.azure.observability.event.TestSummary
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PractitionerRole
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ServiceRequest
import org.junit.jupiter.api.Test

class FhirPathBundleDigestExtractorStrategyTests {

    @Test
    fun `test extracts data from bundle correctly when all data is populated`() {
        val bundle = Bundle()
        addPatient(bundle)
        addObservation(bundle)
        val performer = createPerformer(bundle)
        addMessageHeader(bundle, performer)
        val orderingFacility = createOrganization(bundle)
        val orderingPractitionerRole = createPractiionerRole(orderingFacility, bundle)
        addServiceRequest(performer, orderingPractitionerRole, bundle)

        val bundleExtractor = BundleDigestExtractor(
            FhirPathBundleDigestLabResultExtractorStrategy(
                CustomContext(
                    bundle, bundle, mutableMapOf(), CustomFhirPathFunctions()
                )
            )
        )
        val digest = bundleExtractor.generateDigest(bundle)

        assertThat(digest)
            .isDataClassEqualTo(
                BundleDigestLabResult(
                    listOf(
                        ObservationSummary(
                            testSummary = listOf(
                                TestSummary(
                                    conditions = listOf(
                                        CodeSummary(system = "Unknown", code = "Unknown", display = "Unknown", memberOid = "Unknown")
                                    )
                                )
                            ),
                            interpretations = listOf(
                                CodeSummary(
                                    system = "http://snomed.info/sct",
                                    code = "260385009",
                                    display = "Negative",
                                    memberOid = "Unknown"
                                )
                            )
                        )
                    ),
                                listOf("VA"),
                                listOf(
                                    PerformerSummary(
                                        performerName = "Jane Roe",
                                        performerState = "MD",
                                        performerCLIA = "999999"
                                    )
                                ),
                                listOf(
                                    OrderingFacilitySummary(orderingFacilityState = "DC")
                    ),
                        "ORU_R01"
                )
            )
    }

    @Test
    fun `test extracts data from bundle correctly when data is missing`() {
        val bundle = Bundle()
        val performer = createPerformer(bundle)
        addMessageHeader(bundle, performer)
        val orderingFacility = createOrganization(bundle)
        val orderingPractitionerRole = createPractiionerRole(orderingFacility, bundle)
        addServiceRequest(performer, orderingPractitionerRole, bundle)

        val bundleExtractor = BundleDigestExtractor(
            FhirPathBundleDigestLabResultExtractorStrategy(
                CustomContext(
                    bundle, bundle, mutableMapOf(), CustomFhirPathFunctions()
                )
            )
        )
        val digest = bundleExtractor.generateDigest(bundle)

        assertThat(digest)
            .isDataClassEqualTo(
                BundleDigestLabResult(
                    emptyList(),
                    emptyList(),
                    listOf(
                        PerformerSummary(
                            performerName = "Jane Roe",
                            performerState = "MD",
                            performerCLIA = "999999"
                        )
                    ),
                    listOf(OrderingFacilitySummary(orderingFacilityState = "DC")),
                    "ORU_R01"
                )
            )
    }

    private fun addPatient(bundle: Bundle) {
        val patient = Patient()
        val patientAddress = Address()
        patientAddress.state = "VA"
        patient.address.add(patientAddress)
        bundle.addEntry().resource = patient
    }

    private fun addServiceRequest(performer: Practitioner, orderingPractitionerRole: PractitionerRole, bundle: Bundle) {
        val serviceRequest = ServiceRequest()
        val performerReference = Reference()
        performerReference.reference = performer.id
        serviceRequest.addPerformer(performerReference)
        val orderingFacilityReference = Reference()
        orderingFacilityReference.reference = orderingPractitionerRole.id
        serviceRequest.requester = orderingFacilityReference
        bundle.addEntry().resource = serviceRequest
    }

    private fun createPractiionerRole(orderingFacility: Organization, bundle: Bundle): PractitionerRole {
        val orderingPractitionerRole = PractitionerRole()
        orderingPractitionerRole.id = "PractitionerRole/1"
        val organizationReference = Reference()
        organizationReference.reference = orderingFacility.id
        orderingPractitionerRole.organization = organizationReference
        val orderingPractionerRoleEntry = bundle.addEntry()
        orderingPractionerRoleEntry.fullUrl = orderingPractitionerRole.id
        orderingPractionerRoleEntry.resource = orderingPractitionerRole
        return orderingPractitionerRole
    }

    private fun createOrganization(bundle: Bundle): Organization {
        val orderingFacility = Organization()
        orderingFacility.id = "Organization/1"
        val orderingFacilityAddress = Address()
        orderingFacilityAddress.state = "DC"
        orderingFacility.address = listOf(orderingFacilityAddress)
        val orderingFacilityEntry = bundle.addEntry()
        orderingFacilityEntry.resource = orderingFacility
        orderingFacilityEntry.fullUrl = orderingFacility.id
        return orderingFacility
    }

    private fun createPerformer(bundle: Bundle): Practitioner {
        val performer = Practitioner()
        performer.id = "Performer/1"
        performer.apply {
            addName().apply {
                family = "Roe"
                given = listOf(org.hl7.fhir.r4.model.StringType("Jane"))
            }
            addAddress().apply {
                state = "MD"
            }
            addIdentifier().apply {
                type.addCoding().apply {
                    code = "CLIA"
                }
                value = "999999"
            }
        }
        val performerEntry = bundle.addEntry()
        performerEntry.resource = performer
        performerEntry.fullUrl = performer.id
        return performer
    }

    private fun addMessageHeader(bundle: Bundle, performer: Practitioner) {
        val messageHeader = MessageHeader()
        messageHeader.event = Coding("ORU", "R01", "ORU_R01")

        val senderReference = Reference()
        senderReference.reference = performer.id
        messageHeader.sender = senderReference
        bundle.addEntry().resource = messageHeader
    }

    private fun addObservation(bundle: Bundle) {
        val observation = Observation()
        val coding = Coding()
        val extension = Extension()
        extension.url = CONDITION_CODE_EXTENSION_URL
        extension.setValue(Coding())
        coding.extension = listOf(extension)
        observation.code.coding = listOf(coding)

        val interpretation = CodeableConcept()
        val interpretationCoding = Coding()
        interpretationCoding.system = "http://snomed.info/sct"
        interpretationCoding.code = "260385009"
        interpretationCoding.display = "Negative"
        interpretation.addCoding(interpretationCoding)
        observation.interpretation = listOf(interpretation)

        bundle.addEntry().resource = observation
    }
}