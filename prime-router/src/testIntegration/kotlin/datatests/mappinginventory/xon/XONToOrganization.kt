package gov.cdc.prime.router.datatests.mappinginventory.xon

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class XONToOrganization {

    @Test
    fun `test translate to HL7 to FHIR to HL7`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xon/xon-to-organization.hl7",
                "mappinginventory/xon/xon-to-organization.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xon/xon-to-organization.fhir",
                "mappinginventory/xon/xon-to-organization.hl7",
            ).passed
        )
    }
}