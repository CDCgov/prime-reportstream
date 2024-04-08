package gov.cdc.prime.router.datatests.mappinginventory.nk1

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

// This test asserts against a sample HL7v2 message that has an NK1 segment associated with a PID and verifies
// that no data is lost between each step.
class NK1ToRelatedPersonTests {
    @Test
    fun `can accurately map from HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/nk1/NK1-to-RelatedPerson").passed)
    }

    @Test
    fun `can accurately map from mosty empty HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/nk1/NK1-to-RelatedPerson-mostly-empty").passed)
    }

    @Test
    fun `can accurately map from HL7 with repeats to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/nk1/NK1-to-RelatedPerson-with-repeats").passed)
    }
}