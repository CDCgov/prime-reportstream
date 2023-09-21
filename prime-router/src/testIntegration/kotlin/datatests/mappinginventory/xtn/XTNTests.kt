package gov.cdc.prime.router.datatests.mappinginventory.xtn

import gov.cdc.prime.router.datatests.mappinginventory.TranslateAndCompareFile.Companion.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.TranslateAndCompareFile.Companion.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class XTNTests {
    // Tests use (line 2)
    @Test
    fun `XTN use code home`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_use_code_home_2.fhir",
                "mappinginventory/xtn/xtn_use_code_home_2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_use_code_home_2.hl7",
                "mappinginventory/xtn/xtn_use_code_home_2.fhir",
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code mobile`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_use_code_mobile_2.fhir",
                "mappinginventory/xtn/xtn_use_code_mobile_2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_use_code_mobile_2.hl7",
                "mappinginventory/xtn/xtn_use_code_mobile_2.fhir"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code temp`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_use_code_temp_2.fhir",
                "mappinginventory/xtn/xtn_use_code_temp_2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_use_code_temp_2.hl7",
                "mappinginventory/xtn/xtn_use_code_temp_2.fhir"

            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code work`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_use_code_work_2.fhir",
                "mappinginventory/xtn/xtn_use_code_work_2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_use_code_work_2.hl7",
                "mappinginventory/xtn/xtn_use_code_work_2.fhir"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
    @Test
    fun `XTN use code no use system email`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.fhir",
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.hl7",
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.fhir"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
    @Test
    fun `XTN use code no use system pager`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.fhir",
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.hl7",
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.fhir"
            ).passed
        )
    }
}