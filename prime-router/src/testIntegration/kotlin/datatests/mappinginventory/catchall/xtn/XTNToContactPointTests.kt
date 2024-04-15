package gov.cdc.prime.router.datatests.mappinginventory.xtn

import gov.cdc.prime.router.datatests.mappinginventory.verifyHL7ToFHIRToHL7Mapping
import org.junit.jupiter.api.Test

class XTNToContactPointTests {
    @Test
    fun `XTN no value in XTN-3, XTN-7, XTN-12`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_3_no_value_in_xtn3_xtn7_xtn12").passed
        )
    }

    @Test
    fun `XTN test value in XTN-3, XTN-7 and no value in XTN-12`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/xtn/contact-point/xtn_3_test_value_in_xtn3_xtn7_no_value_in_xtn12"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-3, XTN-12 and no value in XTN-7`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/xtn/contact-point/xtn_3_test_value_in_xtn3_xtn12_no_value_in_xtn7"
            ).passed
        )
    }

    @Test
    fun `XTN test value in XTN-3 and no value in XTN-7, XTN-12`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping(
                "catchall/xtn/contact-point/xtn_3_5_test_value_in_xtn3_no_value_in_xtn7_xtn12"
            ).passed
        )
    }

    @Test
    fun `XTN test ORN in XTN-2`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_4_ORN_in_xtn2").passed
        )
    }

    @Test
    fun `XTN test PRN in XTN-2`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_4_PRN_in_xtn2").passed
        )
    }

    @Test
    fun `XTN test PRS in XTN-2`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_4_PRS_in_xtn2").passed
        )
    }

    @Test
    fun `XTN test WPN in XTN-2`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_4_WPN_in_xtn2").passed
        )
    }

    @Test
    fun `XTN test XTN-3 not valued but XTN-4 valued`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_6_xtn3_no_value_xtn4_valued").passed
        )
    }

    @Test
    fun `XTN test XTN-3 not valued and XTN-4 has a value`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_7_xtn3_no_value_xtn4_no_value").passed
        )
    }

    @Test
    fun `XTN test XTN-3 is Internet and XTN-4 has a value`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_8_xtn3_internet_xtn4_test_email").passed
        )
    }

    @Test
    fun `XTN test XTN-3 is X400 and XTN-4 has a value`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_8_xtn3_x400_xtn4_test_email").passed
        )
    }

    @Test
    fun `XTN test value of 1 in XTN-5`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_9_10_xtn5_value_of_1").passed
        )
    }

    @Test
    fun `XTN test value XTN-6`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_11_xtn6_has_value").passed
        )
    }

    @Test
    fun `XTN test value in XTN-7 and XTN-3 is Internet`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_12_xtn7_has_value_xtn3_is_Internet").passed
        )
    }

    @Test
    fun `XTN test value in XTN-7 and XTN-3 not valued`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_12_xtn7_has_value_xtn3_no_value").passed
        )
    }

    @Test
    fun `XTN test value in XTN-8`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_13_xtn8_has_value").passed
        )
    }

    @Test
    fun `XTN test value in XTN-9`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_14_xtn9_has_value").passed
        )
    }

    @Test
    fun `XTN test value in XTN-7 and XTN-3 is PH`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_12_xtn7_has_value_xtn3_is_PH").passed
        )
    }

    @Test
    fun `XTN test value in XTN-12 and XTN-3 is Internet`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_17_xtn12_has_value_xtn3_is_Internet").passed
        )
    }

    @Test
    fun `XTN test value in XTN-12 and XTN-3 is PH`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_17_xtn12_has_value_xtn3_is_PH").passed
        )
    }

    @Test
    fun `XTN test value in XTN-12 and XTN-3 not valued`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_17_xtn12_has_value_xtn3_no_value").passed
        )
    }

    @Test
    fun `XTN test value in XTN-13`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_18_xtn13_has_value").passed
        )
    }

    @Test
    fun `XTN test value in XTN-14`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_19_xtn14_has_value").passed
        )
    }

    @Test
    fun `XTN test positive value in XTN-18`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_23_xtn18_has_positive_value").passed
        )
    }

    //    @Test
    fun `XTN test negative value in XTN-18`() {
        assert(
            verifyHL7ToFHIRToHL7Mapping("catchall/xtn/contact-point/xtn_23_xtn18_has_negative_value").passed
        )
    }
}