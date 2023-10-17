package gov.cdc.prime.router.datatests.mappinginventory.xtn

import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareFHIRToHL7
import gov.cdc.prime.router.datatests.mappinginventory.translateAndCompareHL7ToFHIR
import org.junit.jupiter.api.Test

class XTNTests {
    // Tests use (line 2)
//    @Test
//    fun `XTN use code home`() {
//        assert(
//            translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_2_use_code_home.fhir",
//                "mappinginventory/xtn/xtn_2_use_code_home.hl7"
//            ).passed
//        )
//
//        assert(
//            translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_2_use_code_home.hl7",
//                "mappinginventory/xtn/xtn_2_use_code_home.fhir",
//            ).passed
//        )
//    }
//
//    // Tests use (line 2)
//    @Test
//    fun `XTN use code mobile`() {
//        assert(
//            translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_2_use_code_mobile.fhir",
//                "mappinginventory/xtn/xtn_2_use_code_mobile.hl7"
//            ).passed
//        )
//
//        assert(
//            translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_2_use_code_mobile.hl7",
//                "mappinginventory/xtn/xtn_2_use_code_mobile.fhir"
//            ).passed
//        )
//    }
//
//    // Tests use (line 2)
//    @Test
//    fun `XTN use code temp`() {
//        assert(
//            translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_2_use_code_temp.fhir",
//                "mappinginventory/xtn/xtn_2_use_code_temp.hl7"
//            ).passed
//        )
//
//        assert(
//            translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_2_use_code_temp.hl7",
//                "mappinginventory/xtn/xtn_2_use_code_temp.fhir"
//
//            ).passed
//        )
//    }
//
//    // Tests use (line 2)
//    @Test
//    fun `XTN use code work`() {
//        assert(
//            translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_2_use_code_work.fhir",
//                "mappinginventory/xtn/xtn_2_use_code_work.hl7"
//            ).passed
//        )
//
//        assert(
//            translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_2_use_code_work.hl7",
//                "mappinginventory/xtn/xtn_2_use_code_work.fhir"
//            ).passed
//        )
//    }
//
//    // Tests use (line 2) as well as system (line 3)
//    @Test
//    fun `XTN use code no use system email`() {
//        assert(
//            translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.fhir",
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.hl7"
//            ).passed
//        )
//
//        assert(
//            translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.hl7",
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_email.fhir"
//            ).passed
//        )
//    }
//
//    // Tests use (line 2) as well as system (line 3)
//    @Test
//    fun `XTN use code no use system pager`() {
//        assert(
//            translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.fhir",
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.hl7"
//            ).passed
//        )
//
//        assert(
//            translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.hl7",
//                "mappinginventory/xtn/xtn_2_3_use_code_no_use_system_pager.fhir"
//            ).passed
//        )
//    }

    @Test
    fun `XTN no value in XTN-3, XTN-7, XTN-12`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_1_no_value_in_xtn3_xtn7_xtn12.fhir",
                "mappinginventory/xtn/xtn_1_no_value_in_xtn3_xtn7_xtn12.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_1_no_value_in_xtn3_xtn7_xtn12.hl7",
                "mappinginventory/xtn/xtn_1_no_value_in_xtn3_xtn7_xtn12.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-3 and no value in XTN-7, XTN-12`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_1_3_test_value_in_xtn3_no_value_in_xtn7_xtn12.fhir",
                "mappinginventory/xtn/xtn_1_3_test_value_in_xtn3_no_value_in_xtn7_xtn12.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_1_3_test_value_in_xtn3_no_value_in_xtn7_xtn12.hl7",
                "mappinginventory/xtn/xtn_1_3_test_value_in_xtn3_no_value_in_xtn7_xtn12.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test ORN in xtn2`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_ORN_in_xtn2.fhir",
                "mappinginventory/xtn/xtn_2_ORN_in_xtn2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_ORN_in_xtn2.hl7",
                "mappinginventory/xtn/xtn_2_ORN_in_xtn2.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test PRN in xtn2`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_PRN_in_xtn2.fhir",
                "mappinginventory/xtn/xtn_2_PRN_in_xtn2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_PRN_in_xtn2.hl7",
                "mappinginventory/xtn/xtn_2_PRN_in_xtn2.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test PRS in xtn2`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_PRS_in_xtn2.fhir",
                "mappinginventory/xtn/xtn_2_PRS_in_xtn2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_PRS_in_xtn2.hl7",
                "mappinginventory/xtn/xtn_2_PRS_in_xtn2.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test WPN in xtn2`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_2_WPN_in_xtn2.fhir",
                "mappinginventory/xtn/xtn_2_WPN_in_xtn2.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_2_WPN_in_xtn2.hl7",
                "mappinginventory/xtn/xtn_2_WPN_in_xtn2.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test XTN-3 not valued but XTN-4 valued`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_contact_point_6_xtn3_no_value_xtn4_valued.fhir",
                "mappinginventory/xtn/xtn_contact_point_6_xtn3_no_value_xtn4_valued.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_contact_point_6_xtn3_no_value_xtn4_valued.hl7",
                "mappinginventory/xtn/xtn_contact_point_6_xtn3_no_value_xtn4_valued.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test XTN-3 is Internet and XTN-4 has a value`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_contact_point_8_xtn3_internet_xtn4_test_email.fhir",
                "mappinginventory/xtn/xtn_contact_point_8_xtn3_internet_xtn4_test_email.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_contact_point_8_xtn3_internet_xtn4_test_email.hl7",
                "mappinginventory/xtn/xtn_contact_point_8_xtn3_internet_xtn4_test_email.fhir"
            ).passed
        )
    }
}