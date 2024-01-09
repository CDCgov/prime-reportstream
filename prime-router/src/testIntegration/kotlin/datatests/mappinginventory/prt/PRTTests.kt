package gov.cdc.prime.router.datatests.mappinginventory.prt

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

// This test asserts against a sample HL7v2 message that has an PRT segment associated with a PID and verifies
// that no data is lost between each step.
class PRTToRelatedPersonTests {
    @Test
    fun `can accurately map from HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("prt/PRT-to-RelatedPerson").passed)
    }
}