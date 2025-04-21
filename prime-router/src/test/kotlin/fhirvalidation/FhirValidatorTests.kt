package gov.cdc.prime.router.fhirvalidation

import ca.uhn.fhir.parser.IParser
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.OperationOutcome
import org.junit.jupiter.api.Test
import kotlin.time.TimeSource

class FhirValidatorTests : Logging {
    val validator = RSFhirValidator()
    val timeSource = TimeSource.Monotonic
    var ctx = RSFhirValidator.ctx
    var parser: IParser = ctx.newJsonParser()

    @Test
    fun `test FHIR validation SR sample`() {
        validateAndPrintResults("/fhirsamples/SR-bundle-original.fhir.json", false)
        validateAndPrintResults("/fhirsamples/SR-bundle-original.fhir.json")
    }

    // path is in resources folder starting with /
    fun validateAndPrintResults(path: String, addProfiles: Boolean = true) {
        println("\n\n\n\nValidating resource: $path addProfiles: $addProfiles")
        var mark1 = timeSource.markNow()
        val result = validator.validateFhirInResourcesDir(path, addProfiles)
        var mark2 = timeSource.markNow()
        println("Done validating resource. Time: ${mark2 - mark1}")

        val outcome: OperationOutcome = result?.toOperationOutcome() as OperationOutcome
        val issues = outcome.getIssue()
        val errors = issues.filter { it.severity == OperationOutcome.IssueSeverity.ERROR }
        val warnings = issues.filter { it.severity == OperationOutcome.IssueSeverity.WARNING }
        val infos = issues.filter { it.severity == OperationOutcome.IssueSeverity.INFORMATION }
        println("There are ${errors.size} errors, ${warnings.size} warnings, and ${infos.size} information notes")
        println("\nErrors")
        printErrorReports(errors)
        println("\n\nWarnings")
        printWarningReports(warnings)
//        println("\n\nInformation Notes")
//        printIssueReports(infos)
    }
}

fun printErrorReports(issues: List<OperationOutcome.OperationOutcomeIssueComponent>) {
    if (issues.isEmpty()) {
        println("None")
    } else {
        val issuesByCode = issues.groupBy { it.details.coding[0].code }
        for ((key, value) in issuesByCode) {
            println("    $key:${value.size}")
            for (issue in value) {
                println("        ${issue.getDiagnostics()}")
            }
        }
    }
}

fun printWarningReports(issues: List<OperationOutcome.OperationOutcomeIssueComponent>) {
    if (issues.isEmpty()) {
        println("None")
    } else {
        val warningGroups = issues.groupBy { it.diagnostics.take(10) }
        for ((key, value) in warningGroups) {
            println("    $key:${value.size}")
            for (issue in value) {
                println("        ${issue.getDiagnostics()}")
            }
        }
    }
}

// Not really a test. This is used to add RS profiles to a fhir.json file
//    @Test
//    fun `test addFhirProfiles`() {
//        validator.addFhirProfiles("/Users/jaj/repos/cdc/prime-reportstream/prime-router/src/test/resources/fhirsamples/SR-bundle.fhir.json")
// //            validator.addFhirProfiles("/Users/jaj/repos/cdc/prime-reportstream/prime-router/src/test/resources/fhirsamples/simple-patient.fhir.json")
//        }