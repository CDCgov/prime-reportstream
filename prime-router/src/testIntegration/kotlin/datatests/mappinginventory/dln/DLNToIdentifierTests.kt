package gov.cdc.prime.router.datatests.mappinginventory.dln

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class DLNToIdentifierTests {

    @Test
    fun `test converting HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("dln/DLN-to-Identifier-cwe-4-populated").passed)
    }
}