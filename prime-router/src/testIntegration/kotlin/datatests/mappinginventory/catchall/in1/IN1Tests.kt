package gov.cdc.prime.router.datatests.mappinginventory.in1

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class IN1Tests {
    @Test
    fun `test IN1 mapped to Coverage`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/in1/IN1-to-Coverage",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}