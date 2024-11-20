package gov.cdc.prime.router.datatests.mappinginventory.cnn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class AUIExtensionTests {
    @Test
    fun `test correctly handles ISO universal id type`() {
        assert(verifyHL7ToFHIRToHL7Mapping(
            "catchall/aui/aui",
            false,
            false,
            false,
            outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-base.yml"
        ).passed)
    }
}