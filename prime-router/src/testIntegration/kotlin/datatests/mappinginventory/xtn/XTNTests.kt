package gov.cdc.prime.router.datatests.mappinginventory.xtn

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class XTNTests {
    // Tests use (line 2)
    @Test
    fun `XTN use code home`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_use_code_home.fhir",
                "mappinginventory/xtn/xtn_2_use_code_home.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_use_code_home.hl7",
                "mappinginventory/xtn/xtn_2_use_code_home.fhir",
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code mobile`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_use_code_mobile.fhir",
                "mappinginventory/xtn/xtn_2_use_code_mobile.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_use_code_mobile.hl7",
                "mappinginventory/xtn/xtn_2_use_code_mobile.fhir"
            ).passed
        )
    }

    // Tests use (line 2)
//    @Test
    fun `XTN use code temp`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_use_code_temp.fhir",
                "mappinginventory/xtn/xtn_2_use_code_temp.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_use_code_temp.hl7",
                "mappinginventory/xtn/xtn_2_use_code_temp.fhir"

            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code work`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_use_code_work.fhir",
                "mappinginventory/xtn/xtn_2_use_code_work.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_use_code_work.hl7",
                "mappinginventory/xtn/xtn_2_use_code_work.fhir"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
//    @Test
    fun `XTN use code no use system email`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.fhir",
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.hl7",
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.fhir"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
//    @Test
    fun `XTN use code no use system pager`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.fhir",
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.hl7",
                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.fhir"
            ).passed
        )
    }
}