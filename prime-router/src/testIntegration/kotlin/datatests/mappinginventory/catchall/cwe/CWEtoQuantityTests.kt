package gov.cdc.prime.router.datatests.mappinginventory.cwe

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class CWEtoQuantityTests {
    @Ignore
    @Test
    fun `test values in OBX-6`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cwe/quantity/cwe-quantity").passed)
    }
}