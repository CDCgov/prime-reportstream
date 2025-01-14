package gov.cdc.prime.router.datatests.mappinginventory.catchall.prt

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class PRTToDeviceTests {
    @Test
    fun `test translate from PRT to Device to PRT`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/prt/prt-to-device").passed)
    }
}