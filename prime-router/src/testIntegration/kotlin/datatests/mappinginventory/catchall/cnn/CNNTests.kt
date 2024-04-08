package gov.cdc.prime.router.datatests.mappinginventory.catchall.cnn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CNNTests {
    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/cnn/cnn-to-Practitioner").passed)
    }
}