package gov.cdc.prime.router.cli.tests

import com.github.ajalt.clikt.testing.test
import gov.cdc.prime.router.cli.PIIRemovalCommands
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.Specimen
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

class RemovePIITest : CoolTest() {
    /**
     * The name of the call
     */
    override val name: String
        get() = "removepiicheck"

    /**
     * Description of the call
     */
    override val description: String
        get() = "Tests that all pii is removed from a message"

    /**
     * Type of test
     */
    override val status: TestStatus
        get() = TestStatus.SMOKE

    /**
     * Function that is run when this command is called
     */
    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting remove PII test")
        val inputFilePath = Paths.get("").toAbsolutePath().toString() +
            "/src/main/resources/clitests/compare-test-files/fakePII.fhir"
        val outputFilePath = Paths.get("").toAbsolutePath().toString() +
            "/src/main/resources/clitests/compare-test-files/piiRemoved.fhir"

        PIIRemovalCommands().test(
            "-i $inputFilePath -o $outputFilePath"
        )

        val inputContent = File(inputFilePath).inputStream().readBytes().toString(Charsets.UTF_8)
        val inputBundle = FhirTranscoder.decode(inputContent)
        val outputContent = File(outputFilePath).inputStream().readBytes().toString(Charsets.UTF_8)
        val outputBundle = FhirTranscoder.decode(outputContent)

        if (!testIdsRemoved(inputBundle, outputContent)) {
            ugly("Not all IDs removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        if (!testPatientPIIRemoved(inputBundle, outputBundle)) {
            ugly("Not all patient PII removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        if (!testOrganizationPIIRemoved(inputBundle, outputBundle)) {
            ugly("Not all organization PII removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        if (!testPractitionerPIIRemoved(inputBundle, outputBundle)) {
            ugly("Not all practitioner PII removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        if (!testServiceRequestPIIRemoved(inputBundle, outputBundle)) {
            ugly("Not all service request PII removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        if (!testObservationPIIRemoved(inputBundle, outputBundle)) {
            ugly("Not all observation PII removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        if (!testSpecimenPIIRemoved(inputBundle, outputBundle)) {
            ugly("Not all specimen PII removed. Test failed.")
            Path(outputFilePath).deleteIfExists()
            return false
        }

        ugly("PII removal test passed")
        Path(outputFilePath).deleteIfExists()
        return true
    }

    /**
     * Tests patient PII is replaced
     */
    private fun testPatientPIIRemoved(inputBundle: Bundle, outputBundle: Bundle): Boolean {
        var patientIndex = 0
        val outputPatients = outputBundle.entry.map { it.resource }.filterIsInstance<Patient>()
        inputBundle.entry.map { it.resource }.filterIsInstance<Patient>()
            .forEach { inputPatient ->
                val outputPatient = outputPatients[patientIndex]
                var nameIndex = 0
                if (inputPatient.birthDate == outputPatient.birthDate) {
                    return false
                }
                inputPatient.name.forEach { name ->
                    if (!testName(name, outputPatient.name[nameIndex])) {
                        return false
                    }
                    nameIndex++
                }

                var addressIndex = 0
                inputPatient.address.forEach { address ->
                    testAddress(address, outputPatient.address[addressIndex])
                    addressIndex++
                }

                var telecomIndex = 0
                inputPatient.telecom.forEach { telecom ->
                    if (!telecom.value.isNullOrBlank() && telecom.value == outputPatient.telecom[telecomIndex].value) {
                        return false
                    }
                    telecomIndex++
                }

                var contactIndex = 0
                inputPatient.contact.forEach { contact ->
                    val outputContact = outputPatient.contact[contactIndex]
                    if (!testName(contact.name, outputContact.name)) {
                        return false
                    }
                    if (!testAddress(contact.address, outputContact.address)) {
                        return false
                    }

                    telecomIndex = 0
                    contact.telecom.forEach { telecom ->
                        if (telecom.value == outputPatient.telecom[telecomIndex].value) {
                            return false
                        }
                        telecomIndex++
                    }

                    contactIndex++
                }
                patientIndex++
            }
        return true
    }

    /**
     * Tests name PII is replaced
     */
    private fun testName(inputName: HumanName, outputName: HumanName): Boolean {
        var givenNameIndex = 0
        inputName.given.forEach { givenName ->
            if (!givenName.isEmpty && inputName.given == outputName.given[givenNameIndex]) {
                return false
            }
            givenNameIndex++
        }
        if (!inputName.family.isNullOrBlank() &&
            inputName.family == outputName.family
        ) {
            return false
        }
        return true
    }

    /**
     * Tests address PII is replaced
     */
    private fun testAddress(inputAddress: Address, outputAddress: Address): Boolean {
        var addressLineIndex = 0
        inputAddress.line.forEach { addressLine ->
            if (!addressLine.isEmpty && addressLine == outputAddress.line[addressLineIndex]) {
                return false
            }
            addressLineIndex++
        }

        if (
            (
                !inputAddress.city.isNullOrBlank() &&
                inputAddress.city == outputAddress.city
            ) ||
            (
                !inputAddress.postalCode.isNullOrBlank() &&
                inputAddress.postalCode == outputAddress.postalCode
            ) ||
            (
                !inputAddress.district.isNullOrBlank() &&
                inputAddress.district == outputAddress.district
                )

        ) {
            return false
        }
        return true
    }

    /**
     * Tests service request PII is replaced
     */
    private fun testServiceRequestPIIRemoved(inputBundle: Bundle, outputBundle: Bundle): Boolean {
        var serviceRequestIndex = 0
        val outputServiceRequests = outputBundle.entry.map { it.resource }.filterIsInstance<ServiceRequest>()
        inputBundle.entry.map { it.resource }.filterIsInstance<ServiceRequest>()
            .forEach { inputServiceRequest ->
                val outputServiceRequest = outputServiceRequests[serviceRequestIndex]
                var noteIndex = 0
                inputServiceRequest.note.forEach { inputNote ->
                    if (!inputNote.text.isNullOrBlank() &&
                        inputNote.text == outputServiceRequest.note[noteIndex].text
                    ) {
                        return false
                    }
                    noteIndex++
                }
                serviceRequestIndex++
            }
        return true
    }

    /**
     * Tests observation PII is replaced
     */
    private fun testObservationPIIRemoved(inputBundle: Bundle, outputBundle: Bundle): Boolean {
        var observationIndex = 0
        val outputObservations = outputBundle.entry.map { it.resource }.filterIsInstance<Observation>()
        inputBundle.entry.map { it.resource }.filterIsInstance<Observation>()
            .forEach { inputObservation ->
                val outputObservation = outputObservations[observationIndex]
                if (inputObservation.issued != null && inputObservation.issued == outputObservation.issued) {
                    return false
                }
                if (inputObservation.effective != null && inputObservation.effective == outputObservation.effective) {
                    return false
                }
                var noteIndex = 0
                inputObservation.note.forEach { inputNote ->
                    if (!inputNote.text.isNullOrBlank() && inputNote.text == outputObservation.note[noteIndex].text) {
                        return false
                    }
                    noteIndex++
                }

                observationIndex++
            }
        return true
    }

    /**
     * Tests specimen PII is replaced
     */
    private fun testSpecimenPIIRemoved(inputBundle: Bundle, outputBundle: Bundle): Boolean {
        var specimenIndex = 0
        val outputSpecimens = outputBundle.entry.map { it.resource }.filterIsInstance<Specimen>()
        inputBundle.entry.map { it.resource }.filterIsInstance<Specimen>()
            .forEach { inputSpecimen ->
                val outputSpecimen = outputSpecimens[specimenIndex]
                var noteIndex = 0
                inputSpecimen.note.forEach { inputNote ->
                    if (inputNote.text != null && inputNote.text == outputSpecimen.note[noteIndex].text) {
                        return false
                    }
                    noteIndex++
                }
                specimenIndex++
            }
        return true
    }

    /**
     * Tests organization PII is replaced
     */
    private fun testOrganizationPIIRemoved(inputBundle: Bundle, outputBundle: Bundle): Boolean {
        var organizationIndex = 0
        val outputOrganizations = outputBundle.entry.map { it.resource }.filterIsInstance<Organization>()
        inputBundle.entry.map { it.resource }.filterIsInstance<Organization>()
            .forEach { inputOrganization ->
                val outputOrganization = outputOrganizations[organizationIndex]
                var addressIndex = 0
                inputOrganization.address.forEach { address ->
                    if (
                        !testAddress(address, outputOrganization.address[addressIndex])
                    ) {
                        return false
                    }

                    addressIndex++
                }

                var telecomIndex = 0
                inputOrganization.telecom.forEach { telecom ->
                    if (!telecom.value.isNullOrBlank() &&
                        telecom.value == outputOrganization.telecom[telecomIndex].value
                    ) {
                        return false
                    }
                    telecomIndex++
                }

                var contactIndex = 0
                inputOrganization.contact.forEach { contact ->
                    val outputContact = outputOrganization.contact[contactIndex]
                    if (!testName(contact.name, outputContact.name) ||
                        !testAddress(contact.address, outputContact.address)
                    ) {
                        return false
                    }

                    telecomIndex = 0
                    contact.telecom.forEach { telecom ->
                        if (!telecom.value.isNullOrBlank() &&
                            telecom.value == outputOrganization.telecom[telecomIndex].value
                        ) {
                            return false
                        }
                        telecomIndex++
                    }

                    contactIndex++
                }
                organizationIndex++
            }
        return true
    }

    /**
     * Tests practitioner PII is replaced
     */
    private fun testPractitionerPIIRemoved(inputBundle: Bundle, outputBundle: Bundle): Boolean {
        var practitionerIndex = 0
        val outputPractitioners = outputBundle.entry.map { it.resource }.filterIsInstance<Practitioner>()
        inputBundle.entry.map { it.resource }.filterIsInstance<Practitioner>()
            .forEach { inputPractitioner ->
                val outputPractitioner = outputPractitioners[practitionerIndex]
                var nameIndex = 0
                inputPractitioner.name.forEach { name ->
                    if (!testName(name, outputPractitioner.name[nameIndex])) {
                        return false
                    }
                    nameIndex++
                }

                var addressIndex = 0
                inputPractitioner.address.forEach { address ->
                    if (!testAddress(address, outputPractitioner.address[addressIndex])) {
                        return false
                    }

                    addressIndex++
                }

                var telecomIndex = 0
                inputPractitioner.telecom.forEach { telecom ->
                    if (!telecom.value.isNullOrBlank() &&
                        telecom.value == outputPractitioner.telecom[telecomIndex].value
                    ) {
                        return false
                    }
                    telecomIndex++
                }
                practitionerIndex++
            }
        return true
    }

    /**
     * Tests PII IDs replaced
     */
    private fun testIdsRemoved(inputBundle: Bundle, outputContent: String): Boolean {
        PIIRemovalCommands().idPaths.forEach { path ->
            if (!testIdRemoved(path, inputBundle, outputContent)) {
                return false
            }
        }
        return true
    }

    /**
     * Tests specific PII id is replaced
     */
    fun testIdRemoved(path: String, inputBundle: Bundle, outputcontent: String): Boolean {
        FhirPathUtils.evaluate(
            null,
            inputBundle,
            inputBundle,
            path
        ).forEach { resourceId ->
            if (outputcontent.contains(resourceId.primitiveValue().toString())) {
                return false
            }
        }
        return true
    }
}