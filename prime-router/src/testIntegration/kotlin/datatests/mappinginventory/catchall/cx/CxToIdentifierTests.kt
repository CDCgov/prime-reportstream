package gov.cdc.prime.router.datatests.mappinginventory.cx

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CxToIdentifierTests {
    @Test
    fun `test correctly handles CX datatype for organization`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-organization").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-organization-null-org-type").passed)
    }

    @Test
    fun `test correctly handles CX datatype for system `() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-system-iso").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-system-iso-no-cx-4-1").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-system-iso-no-cx-4-2").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-system-uuid").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-system-uuid-no-cx-4-1").passed)
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-identifier-system-uuid-no-cx-4-2").passed)
    }

    @Test
    fun `test correctly handles CX 5 not being PLAC`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cx/cx-5").passed)
    }
}