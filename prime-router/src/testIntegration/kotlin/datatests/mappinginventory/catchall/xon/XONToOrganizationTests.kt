package gov.cdc.prime.router.datatests.mappinginventory.xon

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

/**
 * This test class covers the mapping of XON to Organization and implicitly also tests to other mappings that are internal
 * to XON
 */
class XONToOrganizationTests {

    @Test
    fun `test translate to HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xon/xon-to-organization").passed)
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 when XON-10 is populated`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xon/xon-to-organization-xon10-populated").passed)
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 when XON-10 is populated and XON-3 is empty`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/xon/xon-to-organization-xon10-populated-xon3-empty").passed)
    }
}