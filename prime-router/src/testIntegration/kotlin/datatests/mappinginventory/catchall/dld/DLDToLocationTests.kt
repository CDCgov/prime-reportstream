package gov.cdc.prime.router.datatests.mappinginventory.dld

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class DLDToLocationTests {

    @Test
    fun `verify HL7 to FHIR to HL7 all leaves populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/dld/DLD-to-Location").passed)
    }
}