package gov.cdc.prime.router.datatests.mappinginventory.catchall.ce

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CEtoCodeableConceptTests {
    @Test
    fun `test values in CE`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ce/ce-test-values").passed)
    }
}