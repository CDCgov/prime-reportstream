package gov.cdc.prime.router.fhirvalidation

import org.apache.logging.log4j.kotlin.Logging
import org.junit.jupiter.api.Test
import kotlin.time.TimeSource

class FhirValidatorTests : Logging {
    val validator = RSFhirValidator()
    val timeSource = TimeSource.Monotonic

    fun validateAndPrintResults(path: String, addProfiles: Boolean = true, level: Int = 1) {
        println(
                "\n\n\n\nValidating resource: " +
                        "$path ${if (addProfiles) "with Public Health profiles" else "with base R4 profiles"}"
        )
        var mark1 = timeSource.markNow()
        val result = validator.validateFhirInResourcesDir(path, addProfiles)
        var mark2 = timeSource.markNow()
        println("Done validating resource. Time: ${mark2 - mark1}")
        validator.printResults(result, level)
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
    fun `validate json5 sample`() {
        // SR-bundle-fixed-full.fhir.json5 is intended to pass all recommended profiles, ultimately
        validateAndPrintResults("/fhirsamples/SR-bundle-fixed-full.fhir.json5", false)
    }
}