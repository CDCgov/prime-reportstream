package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class HDToExtensionAssigningAuthorityTests {

    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-extensionAssigningAuthority-iso").passed)
    }

    @Test
    fun `test correctly handles UUID universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-extensionAssigningAuthority-uuid").passed)
    }

    @Test
    fun `test correctly handles unknown universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-extensionAssigningAuthority-clia").passed)
    }

    @Test
    fun `test correctly handles empty hd3`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/hd/HD-to-extensionAssigningAuthority-empty-hd3").passed)
    }
}