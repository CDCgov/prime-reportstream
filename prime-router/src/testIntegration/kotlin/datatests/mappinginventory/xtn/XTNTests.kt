package gov.cdc.prime.router.datatests.mappinginventory.xtn

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.datatests.TranslationTests
import org.junit.jupiter.api.Test

class XTNTests {
    fun shouldTranslateLosslessly(inputFile: String, outputFile: String): CompareData.Result {
        val outputSchemaPath = "metadata/hl7_mapping/ORU_R01/ORU_R01-test"

        val testConfig = TranslationTests.TestConfig(
            inputFile, Report.Format.FHIR, "", outputFile,
            Report.Format.HL7, outputSchemaPath, true, null, null, null
        )
        return TranslationTests().FileConversionTest(
            testConfig
        ).runTest()
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code home`() {
        assert(
            shouldTranslateLosslessly(
                "mappinginventory/xtn/xtn_use_code_home_2.fhir",
                "mappinginventory/xtn/xtn_use_code_home_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code mobile`() {
        assert(
            shouldTranslateLosslessly(
                "mappinginventory/xtn/xtn_use_code_mobile_2.fhir",
                "mappinginventory/xtn/xtn_use_code_mobile_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code temp`() {
        assert(
            shouldTranslateLosslessly(
                "mappinginventory/xtn/xtn_use_code_temp_2.fhir",
                "mappinginventory/xtn/xtn_use_code_temp_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2)
    @Test
    fun `XTN use code work`() {
        assert(
            shouldTranslateLosslessly(
                "mappinginventory/xtn/xtn_use_code_work_2.fhir",
                "mappinginventory/xtn/xtn_use_code_work_2.hl7"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
    @Test
    fun `XTN use code no use system email`() {
        assert(
            shouldTranslateLosslessly(
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.fhir",
                "mappinginventory/xtn/xtn_use_code_no_use_system_email_2_3.hl7"
            ).passed
        )
    }

    // Tests use (line 2) as well as system (line 3)
    @Test
    fun `XTN use code no use system pager`() {
        assert(
            shouldTranslateLosslessly(
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.fhir",
                "mappinginventory/xtn/xtn_use_code_no_use_system_pager_2_3.hl7"
            ).passed
        )
    }
}