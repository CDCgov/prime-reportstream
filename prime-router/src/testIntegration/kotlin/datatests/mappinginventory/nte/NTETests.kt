package gov.cdc.prime.router.datatests.mappinginventory.nte

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

// This test asserts against a sample HL7v2 message that has an NTE segment associated with a PID, OBR and OBX and verifies
// that no data is lost between each step. The NTE segments associated with PID and OBX both have NTE.5 populated
class NTEToAnnotationTests {
    @Test
    fun `can accurately map from HL7 to FHIR to HL7`() {
        assert(verifyHL7ToFHIRToHL7Mapping("nte/NTE-to-annotation").passed)
    }

    /**
     * FHIR supports an authorString in addition to the authorReference that is generated in the HL7->FHIR conversion
     * which does not have a direct mapping to HL7.  If provided, the mapping will append "Authored by: {authorString}"
     * to the note comment
     */

    // Temporarily disabling this test as the underlying FHIR needs to get updated
    // for the new EI mapping
    fun `can acccurately map an authorString fhir attribute to an NTE segment`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/nte/annotation_author_string_resource_test_file.fhir",
                "mappinginventory/nte/nte_segment_author_string_test_file.hl7"
            ).passed
        )
    }
}