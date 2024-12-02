package gov.cdc.prime.router.datatests.mappinginventory.orcobr

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class OBRToSpecimenTests {

    @Test
    fun `test OBR Specimen Source fields populated for ORU`() {
        assert(verifyHL7ToFHIRToHL7Mapping("catchall/orcobr/oru-obr-to-specimen").passed)
    }

    @Test
    fun `test OBR Specimen Source fields populated for OML`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/orcobr/oml-obr-to-specimen",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }

    @Test
    fun `test OBR Specimen Source fields populated for ORM`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/orcobr/orm-obr-to-specimen",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}