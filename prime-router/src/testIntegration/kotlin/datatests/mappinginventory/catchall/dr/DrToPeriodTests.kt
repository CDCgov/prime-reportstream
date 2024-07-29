package gov.cdc.prime.router.datatests.mappinginventory.dr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class DrToPeriodTests {
    @Test
    fun `test correctly handles DR datatype`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/dr/dr-to-period").passed)
    }
}