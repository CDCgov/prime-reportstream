package gov.cdc.prime.router.datatests.mappinginventory.nte_note

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

// This test asserts against a sample HL7 that an NTE segment associated with a PID, OBR and OBX and verifies
// that the no data is loss between each step.  The NTE segments associated with PID and OBX both have NTE.5 populated
class HL7ToFhirToHL7 {
    @Test
    fun `can accurately map NTE segment to annotation resource`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/NTE/nte_segment_test_file.hl7",
                "mappinginventory/NTE/annotation_resource_test_file.fhir"
            ).passed
        )
    }

    @Test
    fun `can accurately map Annotation resource to NTE segment`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/NTE/annotation_resource_test_file.fhir",
                "mappinginventory/NTE/nte_segment_test_file.hl7"
            ).passed
        )
    }
}

// FHIR supports an authorString in addition to the authorReference that is generated in the HL7->FHIR conversion
// which does not have a direct mapping to HL7.  If provided, the mapping will append "Authored by: {authorString}"
// to the note comment

class FhirToHL7 {

    @Test
    fun `can acccurately map an authorString fhir attribute to an NTE segment`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/NTE/annotation_author_string_resource_test_file.fhir",
                "mappinginventory/NTE/nte_segment_author_string_test_file.hl7"
            ).passed
        )
    }
}