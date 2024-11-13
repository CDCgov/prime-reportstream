package gov.cdc.prime.router.datatests.mappinginventory.cnn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class AUIExtensionTests {
    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cnn/cnn-to-Practitioner").passed)
    }
}