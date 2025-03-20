package gov.cdc.prime.router.datatests.mappinginventory.xpn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XPNtoHumanNameTests {
    @Test
    fun `test translate to HL7 to FHIR to HL7 when XPN-10 empty and XPN-12,13 populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xpn/xpn-to-humanname-xpn12-13-populated-xpn10-empty").passed)
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 when XPN-10 populated and XPN-12,13 empty`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xpn/xpn-to-humanname-xpn10-populated-xpn12-13-empty").passed)
    }
}