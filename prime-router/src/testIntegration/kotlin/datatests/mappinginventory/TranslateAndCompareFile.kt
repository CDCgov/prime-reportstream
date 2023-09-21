package gov.cdc.prime.router.datatests.mappinginventory

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.datatests.TranslationTests

class TranslateAndCompareFile {
    companion object {
        fun translateAndCompareFHIRToHL7(inputFile: String, expectedOutputFile: String): CompareData.Result {
            val outputSchemaPath = "metadata/hl7_mapping/ORU_R01/ORU_R01-test"

            val testConfig = TranslationTests.TestConfig(
                inputFile, Report.Format.FHIR, "", expectedOutputFile,
                Report.Format.HL7, outputSchemaPath, true, null, null, null
            )
            return TranslationTests().FileConversionTest(
                testConfig
            ).runTest()
        }

        fun translateAndCompareHL7ToFHIR(inputFile: String, expectedOutputFile: String): CompareData.Result {
            val testConfig = TranslationTests.TestConfig(
                inputFile, Report.Format.HL7, "", expectedOutputFile,
                Report.Format.FHIR, null, true, null, null, null
            )
            return TranslationTests().FileConversionTest(
                testConfig
            ).runTest()
        }
    }
}