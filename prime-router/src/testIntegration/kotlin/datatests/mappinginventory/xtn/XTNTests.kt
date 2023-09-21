package gov.cdc.prime.router.datatests.mappinginventory.xtn

import gov.cdc.prime.router.datatests.mappinginventory.TranslateAndCompareFile.Companion.translateAndCompareFile
import org.junit.jupiter.api.Test

class XTNTests {
    // Tests use (line 2)
    @Test
    fun `XTN use code home`() {
        assert(
            translateAndCompareFile(
                "mappinginventory/xtn/xtn_use_code_home_2.fhir",
                "mappinginventory/xtn/xtn_use_code_home_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code mobile`() {
        assert(
            translateAndCompareFile(
                "mappinginventory/xtn/xtn_use_code_mobile_2.fhir",
                "mappinginventory/xtn/xtn_use_code_mobile_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code temp`() {
        assert(
            translateAndCompareFile(
                "mappinginventory/xtn/xtn_use_code_temp_2.fhir",
                "mappinginventory/xtn/xtn_use_code_temp_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code work`() {
        assert(
            translateAndCompareFile(
                "mappinginventory/xtn/xtn_use_code_work_2.fhir",
                "mappinginventory/xtn/xtn_use_code_work_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
    @Test
    fun `XTN use code no use system email`() {
        assert(
            translateAndCompareFile(
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.fhir",
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.hl7"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
    @Test
    fun `XTN use code no use system pager`() {
        assert(
            translateAndCompareFile(
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.fhir",
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.hl7"
            ).passed
        )
    }
}