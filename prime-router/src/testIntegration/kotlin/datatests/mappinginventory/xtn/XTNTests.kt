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

    @Test
    fun `XTN test value of 1 in XTN-5`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_9_10_xtn5_value_of_1.fhir",
                "mappinginventory/xtn/xtn_9_10_xtn5_value_of_1.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_9_10_xtn5_value_of_1.hl7",
                "mappinginventory/xtn/xtn_9_10_xtn5_value_of_1.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value XTN-6`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_11_xtn6_has_value.fhir",
                "mappinginventory/xtn/xtn_11_xtn6_has_value.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_11_xtn6_has_value.hl7",
                "mappinginventory/xtn/xtn_11_xtn6_has_value.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-7 and XTN-3 is Internet`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_Internet.fhir",
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_Internet.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_Internet.hl7",
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_Internet.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-8`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_13_xtn8_has_value.fhir",
                "mappinginventory/xtn/xtn_13_xtn8_has_value.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_13_xtn8_has_value.hl7",
                "mappinginventory/xtn/xtn_13_xtn8_has_value.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-9`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_14_xtn9_has_value.fhir",
                "mappinginventory/xtn/xtn_14_xtn9_has_value.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_14_xtn9_has_value.hl7",
                "mappinginventory/xtn/xtn_14_xtn9_has_value.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-7 and XTN-3 is PH`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_PH.fhir",
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_PH.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_PH.hl7",
                "mappinginventory/xtn/xtn_12_xtn7_has_value_xtn3_is_PH.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-12 and XTN-3 is Internet`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_Internet.fhir",
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_Internet.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_Internet.hl7",
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_Internet.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-12 and XTN-3 is PH`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_PH.fhir",
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_PH.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_PH.hl7",
                "mappinginventory/xtn/xtn_17_xtn12_has_value_xtn3_is_PH.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-13`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_18_xtn13_has_value.fhir",
                "mappinginventory/xtn/xtn_18_xtn13_has_value.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_18_xtn13_has_value.hl7",
                "mappinginventory/xtn/xtn_18_xtn13_has_value.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-14`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_19_xtn14_has_value.fhir",
                "mappinginventory/xtn/xtn_19_xtn14_has_value.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_19_xtn14_has_value.hl7",
                "mappinginventory/xtn/xtn_19_xtn14_has_value.fhir"
            ).passed
        )
    }

    @Test
    fun `XTN test positive value in XTN-18`() {
        assert(
            translateAndCompareFHIRToHL7(
                "mappinginventory/xtn/xtn_23_xtn18_has_positive_value.fhir",
                "mappinginventory/xtn/xtn_23_xtn18_has_positive_value.hl7"
            ).passed
        )

        assert(
            translateAndCompareHL7ToFHIR(
                "mappinginventory/xtn/xtn_23_xtn18_has_positive_value.hl7",
                "mappinginventory/xtn/xtn_23_xtn18_has_positive_value.fhir"
            ).passed
        )
    }

//    @Test
//    fun `XTN test negative value in XTN-18`() {
//        assert(
//            !translateAndCompareFHIRToHL7(
//                "mappinginventory/xtn/xtn_23_xtn18_has_negative_value.fhir",
//                "mappinginventory/xtn/xtn_23_xtn18_has_negative_value.hl7"
//            ).passed
//        )
//
//        assert(
//            !translateAndCompareHL7ToFHIR(
//                "mappinginventory/xtn/xtn_23_xtn18_has_negative_value.hl7",
//                "mappinginventory/xtn/xtn_23_xtn18_has_negative_value.fhir"
//            ).passed
//        )
//    }
}