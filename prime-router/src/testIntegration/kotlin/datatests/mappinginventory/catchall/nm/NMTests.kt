package gov.cdc.prime.router.datatests.mappinginventory.nm

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class NMTests {
    @Test
    fun `test NM to quantity`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/nm/nm-to-quantity").passed)
    }
}