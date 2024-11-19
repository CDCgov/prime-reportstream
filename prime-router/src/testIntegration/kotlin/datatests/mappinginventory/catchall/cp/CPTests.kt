package gov.cdc.prime.router.datatests.mappinginventory.cp

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CPTests {
    @Test
    fun `test correctly maps cp`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/in1/in1-to-Coverage", true, true, true).passed)
    }
}