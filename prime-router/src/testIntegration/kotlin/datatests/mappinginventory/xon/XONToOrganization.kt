package gov.cdc.prime.router.datatests.mappinginventory.xon

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

/*
This test class covers the mapping of XON to Organization and implicitly also tests to other mappings that are internal
to XON
 */
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

    @Test
    fun `test translate to HL7 to FHIR to HL7 when XON-10 is populated`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xon/xon-to-organization-xon10-populated.hl7",
                "mappinginventory/xon/xon-to-organization-xon10-populated.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xon/xon-to-organization-xon10-populated.fhir",
                "mappinginventory/xon/xon-to-organization-xon10-populated.hl7",
            ).passed
        )
    }

    @Test
    fun `test translate to HL7 to FHIR to HL7 when XON-10 is populated and XON-3 is empty`() {
        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xon/xon-to-organization-xon10-populated-xon3-empty.hl7",
                "mappinginventory/xon/xon-to-organization-xon10-populated-xon3-empty.fhir",
            ).passed
        )

        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xon/xon-to-organization-xon10-populated-xon3-empty.fhir",
                "mappinginventory/xon/xon-to-organization-xon10-populated-xon3-empty.hl7",
            ).passed
        )
    }
}