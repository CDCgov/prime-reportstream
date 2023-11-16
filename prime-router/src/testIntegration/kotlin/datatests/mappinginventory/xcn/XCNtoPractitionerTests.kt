package gov.cdc.prime.router.datatests.mappinginventory.xcn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XCNtoPractitionerTests {
    @Test
    fun `test translate to HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("xcn/xcn-to-practitioner-xcn19-20-populated-xcn17-empty").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("xcn/xcn-to-practitioner-xcn17-populated-xcn19-20-empty").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("xcn/xcn-to-practitioner-uuid-assigning-authority").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("xcn/xcn-to-practitioner-uuid-assigning-authority-no-namespace").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("xcn/xcn-to-practitioner-iso-assigning-authority-no-namespace").passed)
    }
}