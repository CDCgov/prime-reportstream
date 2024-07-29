package gov.cdc.prime.router.datatests.mappinginventory.xcn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XCNtoPractitionerTests {
    @Test
    fun `test translate to HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xcn/xcn-to-practitioner-xcn19-20-populated-xcn17-empty").passed)
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 when XCN-17 is populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xcn/xcn-to-practitioner-xcn17-populated-xcn19-20-empty").passed)
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 with organization assigning authority`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xcn/xcn-to-practitioner-dns-assigning-authority").passed)
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 with system assigning authority`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xcn/xcn-to-practitioner-uuid-assigning-authority").passed)
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/xcn/xcn-to-practitioner-uuid-assigning-authority-no-namespace"
            ).passed
        )
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/xcn/xcn-to-practitioner-iso-assigning-authority-no-namespace"
            ).passed
        )
    }
}