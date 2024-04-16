package gov.cdc.prime.router.datatests.mappinginventory.ndl

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class NDLToPractitionerRoleTests {

    @Test
    fun `test translate to NDL to PractitionerRole to NDL`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ndl/NDL-to-PractitionerRole").passed)
    }
}