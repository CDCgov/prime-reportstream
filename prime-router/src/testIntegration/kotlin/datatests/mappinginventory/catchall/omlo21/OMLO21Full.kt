package gov.cdc.prime.router.datatests.mappinginventory.omlo21

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test
// import kotlin.test.Ignore

class OMLO21Full {

     // not working yet, now that it is switched over to OML output schema, lots of errors as far as the eye can see
    @Test
    fun `test OML_O21 all segments`() {
        // eventually these should all be true
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/omlo21/oml_o21-full",
                true,
                true,
                false,
                outputSchema = "classpath:/metadata/hl7_mapping/OML_O21/OML_O21-test.yml"
            ).passed
        )
    }

    // philosophy of this ticket should be to doc how the mappings currently work, not how they should work.
    // follow up ticket for how they should work

    // probably will have to split this test out into three
    // uses cases: FHIR -> OML; OML -> OML; ORM -> OML
    // each use case may need it's own test here because of what is/isn't mapped at different parts.
    // The approach that needs to be tried is to pare down fields in the hl7 to find what is actually possible in an
    // hl7 to fhir to hl7 scenario. This should be the first OML integration test and should reflect what is currently
    // mapped and not what *should* be mapped. Follow up tickets need to be logged for missing fields.

    // datatests/mappinginventory/catchall/omlo21/maximal_oml_preserved.hl7 represents a preserved fully mapped out
    // OML message. This was derived from starting at the original FHIR->HL7 OML mappings and creating a message
    // that had every field populated. Likely a lot of these fields aren't captured going from HL7 to FHIR and can be
    // removed.
}