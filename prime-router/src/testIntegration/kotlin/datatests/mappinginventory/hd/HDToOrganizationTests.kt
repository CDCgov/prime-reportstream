package gov.cdc.prime.router.datatests.mappinginventory.hd

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class HDToOrganizationTests {

    @Test
    fun `test that an ISO HD HL7 datatype can be mapped to a FHIR Organization`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Organization-iso.hl7",
                "mappinginventory/hd/HD-to-Organization-iso.fhir"
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Organization-iso.fhir",
                "mappinginventory/hd/HD-to-Organization-iso.hl7"
            ).passed
        )
    }

    @Test
    fun `test that an UUID HD HL7 datatype can be mapped to a FHIR Organization`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Organization-uuid.hl7",
                "mappinginventory/hd/HD-to-Organization-uuid.fhir"
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Organization-uuid.fhir",
                "mappinginventory/hd/HD-to-Organization-uuid.hl7"
            ).passed
        )
    }

    @Test
    fun `test that identifier2 system is not set for unknown system type`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/hd/HD-to-Organization-clia.hl7",
                "mappinginventory/hd/HD-to-Organization-clia.fhir"
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/hd/HD-to-Organization-clia.fhir",
                "mappinginventory/hd/HD-to-Organization-clia.hl7"
            ).passed
        )
    }
}