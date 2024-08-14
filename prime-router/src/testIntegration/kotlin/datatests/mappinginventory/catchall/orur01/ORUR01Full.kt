package gov.cdc.prime.router.datatests.mappinginventory.orur01

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class ORUR01Full {
    @Test
    fun `test ORU_R01 all segments`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/orur01/oru_r01-full").passed)
    }
}