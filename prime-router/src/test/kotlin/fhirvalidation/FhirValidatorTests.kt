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
    // path is in resources folder starting with /
    // level 1 = errors only, level 2 = errors and warnings, level 3 = errors, warnings, and information notes

    fun validateAndPrintResults(path: String, addProfiles: Boolean = true, level: Int = 1) {
        println(
            "\n\n\n\nValidating resource: " +
            "$path ${if (addProfiles) "with Public Health profiles" else "with base R4 profiles"}"
        )
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
        if (level >= 1) {
            println("\nErrors")
            printErrorReports(errors)
        }
        if (level >= 2) {
            println("\n\nWarnings")
            printWarningReports(warnings)
        }
        if (level >= 3) {
            println("\n\nInformation Notes")
            printInfoReports(infos)
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
//                println("            location: ${issue.location}")
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

    fun printInfoReports(issues: List<OperationOutcome.OperationOutcomeIssueComponent>) {
        if (issues.isEmpty()) {
            println("None")
        } else {
            val infoGroups = issues.groupBy { it.diagnostics.take(10) }
            for ((key, value) in infoGroups) {
                println("    ${value.size} ${value[0].diagnostics}")
            }
        }
    }

    @Test
    fun `validate original SR sample`() {
        // This tests base FHIR R4
        validateAndPrintResults("/fhirsamples/SR-bundle-original.fhir.json", false)
        // This tests using all recommended profiles
        validateAndPrintResults("/fhirsamples/SR-bundle-original.fhir.json", true)
    }

    @Test
    fun `validate fixed SR sample`() {
        // This tests base FHIR R4
        validateAndPrintResults("/fhirsamples/SR-bundle-fixed.fhir.json", false)
    }

    @Test
    fun `validate SR fixed with profiles sample`() {
        // SR-bundle-fixed-full.fhir.json is intended to pass all recommended profiles
        validateAndPrintResults("/fhirsamples/SR-bundle-fixed-full.fhir.json", true)
    }

//    @Test
//    fun `validate random sample`() {
//        // SR-bundle-fixed-full.fhir.json is intended to pass all recommended profiles
//        validateAndPrintResults("/fhirsamples/SR-bundle-fixed-full.fhir.json5", false)
//    }
}