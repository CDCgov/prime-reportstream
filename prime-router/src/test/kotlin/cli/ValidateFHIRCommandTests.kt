package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import kotlin.test.Test

class ValidateFHIRCommandTests {
    private val command = ValidateFHIRCommand()

    @Test
    fun `validate FHIR file`() {
        val result = command.test("--file src/test/resources/fhirsamples/SR-bundle-original.fhir.json")
        assertThat(result.stdout).contains("There are 64 errors")
        assertThat(result.statusCode).isEqualTo(0)
    }
}