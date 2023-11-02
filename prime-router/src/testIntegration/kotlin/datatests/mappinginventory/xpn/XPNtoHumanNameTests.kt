package gov.cdc.prime.router.datatests.mappinginventory.xpn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XPNtoHumanNameTests {
    @Test
    fun `test translate to HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xpn/xpn-to-humanname-xpn12-13-populated-xpn10-empty").passed)
    }
}