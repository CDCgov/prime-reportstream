package gov.cdc.prime.router.datatests.mappinginventory.id

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class IDToEncounterTests {
    @Test
    fun `test hl7 ID type to FHIR Encounter`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/id/ID-to-Encounter").passed)
    }

    @Test
    fun `test hl7 ID type to FHIR Encounter with value F`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/id/ID-to-Encounter-pv2-22-F").passed)
    }
}