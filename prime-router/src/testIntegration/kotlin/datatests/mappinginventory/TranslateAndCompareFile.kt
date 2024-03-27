package gov.cdc.prime.router.datatests.mappinginventory

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.datatests.TranslationTests

fun translateAndCompareFHIRToHL7(inputFile: String, expectedOutputFile: String): CompareData.Result {
    val outputSchemaPath = "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-test.yml"

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

fun verifyHL7ToFHIRToHL7Mapping(
    testFileName: String,
    skipHl7ToFhir: Boolean = false,
    skipFhirToHl7: Boolean = false,
    skipHl7ToHl7: Boolean = false,
    additionalProfiles: List<String> = emptyList(),
): CompareData.Result {
    if (!skipHl7ToFhir) {
        val hl7ToFhirConfig = TranslationTests.TestConfig(
            "mappinginventory/$testFileName.hl7",
            Report.Format.HL7,
            "",
            "mappinginventory/$testFileName.fhir",
            Report.Format.FHIR,
            null,
            true,
            null,
            null,
            null,
            additionalProfiles = additionalProfiles
        )
        val hl7ToFhirResult = TranslationTests().FileConversionTest(hl7ToFhirConfig).runTest()
        if (!hl7ToFhirResult.passed) {
            return hl7ToFhirResult
        }
    }

    if (!skipFhirToHl7) {
        val fhirToHl7Config = TranslationTests.TestConfig(
            "mappinginventory/$testFileName.fhir",
            Report.Format.FHIR,
            "",
            "mappinginventory/$testFileName.hl7",
            Report.Format.HL7,
            "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-test.yml",
            true,
            null,
            null,
            null
        )
        val fhirToHl7Result = TranslationTests().FileConversionTest(fhirToHl7Config).runTest()
        if (!fhirToHl7Result.passed) {
            return fhirToHl7Result
        }
    }
    if (!skipHl7ToHl7) {
        val hl7toFhirToHl7Config = TranslationTests.TestConfig(
            "mappinginventory/$testFileName.hl7",
            Report.Format.HL7,
            "",
            "mappinginventory/$testFileName.hl7",
            Report.Format.HL7,
            "classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-test.yml",
            true,
            null,
            null,
            null
        )
        return TranslationTests().FileConversionTest(hl7toFhirToHl7Config).runTest()
    }
    return CompareData.Result()
}