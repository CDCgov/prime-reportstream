package gov.cdc.prime.router.datatests.mappinginventory.pl

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PLToLocationTests {

    @Test
    fun `verify HL7 to FHIR to HL7 all leaves populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-all-leaves").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 missing PL1`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-missing-pl1").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 missing PL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-missing-pl7").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 missing PL1 and PL3`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-missing-pl1-pl3").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 only PL4`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-only-pl4").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 only PL3 and PL4`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-only-pl3-pl4").passed)
    }

    @Test
    fun `verify HL7 to FHIR to HL7 only PL3, PL1 and PL4`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/pl/PL-to-Location-only-pl3-pl1-pl4").passed)
    }
}