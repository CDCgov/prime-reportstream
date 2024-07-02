package gov.cdc.prime.router.datatests.mappinginventory.msg

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class MSGToCodingTests {

    @Test
    fun `test translate to MSG to Coding to MSG`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msg/MSG-to-Coding").passed)
    }

    @Test
    fun `test translate to MSG to Coding to MSG missing MSG2`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/msg/MSG-to-Coding-missing-msg2").passed)
    }
}