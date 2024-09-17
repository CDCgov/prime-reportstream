package gov.cdc.prime.router.datatests.mappinginventory.omlo21

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class OMLO21Full {

    @Test
    fun `test OML_O21 all segments N E W`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/omlo21/oml_o21-fullh2f2h_NEW",
                true,
                true,
                false,
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test_NEW.yml"
            ).passed
        )
    }

    @Test // todo delete
    fun `test OML_O21 all segments HL7 to FHIR to HL7 O L D`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/omlo21/oml_o21-fullh2f2h",
                true,
                true,
                false,
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test_NEW.yml"
            ).passed
        )
    }

    @Test // todo delete
    fun `test OML_O21 all segments FHIR to HL7 O L D`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/omlo21/oml_o21-fullf2h",
                true,
                false,
                true,
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}