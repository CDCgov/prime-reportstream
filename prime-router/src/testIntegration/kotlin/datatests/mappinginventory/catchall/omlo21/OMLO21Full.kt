package gov.cdc.prime.router.datatests.mappinginventory.omlo21

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class OMLO21Full {

    @Test
    fun `test OML_O21 all segments`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/omlo21/oml_o21-full",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}