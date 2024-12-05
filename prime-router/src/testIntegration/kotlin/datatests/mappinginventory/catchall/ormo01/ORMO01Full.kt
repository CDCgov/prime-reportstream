package gov.cdc.prime.router.datatests.mappinginventory.ormo01

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class ORMO01Full {

    // There are no ORM mappings for FHIR -> HL7. The HL7 -> FHIR -> HL7 and FHIR -> HL7 scenarios here verify,
    // with the exception of MessageHeader.eventCoding, that FHIR crafted through ORM is fully mapped with OML mappings.
    @Test
    fun `test ORM_O01 all segments`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/ormo01/orm_o01-full",
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }
}