package gov.cdc.prime.router.datatests.mappinginventory.nte_note

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class NTETests {

    @Test
    fun `can accurately map NTE segment to annotation resource`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/nte_note/nte_segment_test_file.hl7",
                "mappinginventory/nte_note/annotation_resource_test_file.fhir"
            ).passed
        )
    }

    @Test
    fun `can accurately map Annotation resource to NTE segment`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/nte_note/annotation_resource_test_file.fhir",
                "mappinginventory/nte_note/nte_segment_test_file.hl7"
            ).passed
        )
    }
}