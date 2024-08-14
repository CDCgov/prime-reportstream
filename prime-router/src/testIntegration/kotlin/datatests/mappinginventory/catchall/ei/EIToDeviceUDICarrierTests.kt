package gov.cdc.prime.router.datatests.mappinginventory.ei

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import kotlin.test.Test

class EIToDeviceUDICarrierTests {

    @Test
    fun `EI to Device UDI Carrier`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/ei/EI-to-Device-UDI-Carrier").passed)
    }
}