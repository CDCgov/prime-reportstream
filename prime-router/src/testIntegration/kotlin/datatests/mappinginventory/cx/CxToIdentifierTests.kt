package gov.cdc.prime.router.datatests.mappinginventory.cx

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CxToIdentifierTests {
    @Test
    fun `test correctly handles CX datatype`() {
        assert(verifyHL7ToFHIRToHL7Mapping("cx/cx-identifier").passed)
    }
}