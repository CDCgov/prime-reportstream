package gov.cdc.prime.router.datatests

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.cli.tests.CompareData
import org.junit.jupiter.api.Test

class MappingInventoryTests {
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

    @Test
    fun `XTN use code home`() {
        assert(
            shouldTranslateLosslessly(
                "mapping-inventory/xtn/xtn_use_code_home.fhir",
                "mapping-inventory/xtn/xtn_use_code_home.hl7"
            ).passed
        )
    }

    @Test
    fun `XTN use code mobile`() {
        assert(
            shouldTranslateLosslessly(
                "mapping-inventory/xtn/xtn_use_code_mobile.fhir",
                "mapping-inventory/xtn/xtn_use_code_mobile.hl7"
            ).passed
        )
    }

    @Test
    fun `XTN use code temp`() {
        assert(
            shouldTranslateLosslessly(
                "mapping-inventory/xtn/xtn_use_code_temp.fhir",
                "mapping-inventory/xtn/xtn_use_code_temp.hl7"
            ).passed
        )
    }

    @Test
    fun `XTN use code work`() {
        assert(
            shouldTranslateLosslessly(
                "mapping-inventory/xtn/xtn_use_code_work.fhir",
                "mapping-inventory/xtn/xtn_use_code_work.hl7"
            ).passed
        )
    }

    @Test
    fun `XTN use code no use system email`() {
        assert(
            shouldTranslateLosslessly(
                "mapping-inventory/xtn/xtn_use_code_no_use_system_email.fhir",
                "mapping-inventory/xtn/xtn_use_code_no_use_system_email.hl7"
            ).passed
        )
    }

    @Test
    fun `XTN use code no use system pager`() {
        assert(
            shouldTranslateLosslessly(
                "mapping-inventory/xtn/xtn_use_code_no_use_system_pager.fhir",
                "mapping-inventory/xtn/xtn_use_code_no_use_system_pager.hl7"
            ).passed
        )
    }
}