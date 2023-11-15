package gov.cdc.prime.router.datatests.mappinginventory.cx

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CxToIdentifierTests {
    @Test
    fun `test correctly handles CX datatype for organization`() {
        assert(verifyHL7ToFHIRToHL7Mapping("cx/cx-identifier-organization").passed)
    }

    @Test
    fun `test correctly handles CX datatype for system `() {
        assert(verifyHL7ToFHIRToHL7Mapping("cx/cx-identifier-system").passed)
    }
}