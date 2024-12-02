package gov.cdc.prime.router.datatests.mappinginventory.aui

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class AUIExtensionTests {
    @Test
    fun `test AUI mapped to AUIExtension`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/aui/AUI-to-Extension",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}