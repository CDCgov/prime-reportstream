package gov.cdc.prime.router.datatests.mappinginventory.cp

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class CPTests {
    @Test
    fun `test CP mapped to CPExtension`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/cp/cp-to-extension",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}