package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import io.mockk.every
import io.mockk.mockkConstructor
import kotlin.test.Test

class ValidateTranslationSchemaCommandTests {

    @Test
    fun `test some invalid`() {
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(
            TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true),
            TranslationSchemaManager.Companion.ValidationResult("foo/baz/transform.yml", false),
            TranslationSchemaManager.Companion.ValidationResult("foo/gar", false, true),
            TranslationSchemaManager.Companion.ValidationResult("foo/ban/transform.yml", true),
        )
        val command = ValidateTranslationSchemaCommand()

        val result = command.test("-s", "FHIR", "-c", "localhost", "-b", "container")
        assertThat(result.stdout).isEqualTo(
            """
            -----
            Results from validating FHIR, schemas were invalid
            
            4 schemas were validated
            -----
            
            Individual Results:
            
            Path validated: foo/bar/transform.yml
            Valid: true
            Errored while validating: false
            
            Path validated: foo/baz/transform.yml
            Valid: false
            Errored while validating: false
            
            Path validated: foo/gar
            Valid: false
            Errored while validating: true
            
            Path validated: foo/ban/transform.yml
            Valid: true
            Errored while validating: false
            
        """.trimIndent()
        )
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `test all valid`() {
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(
            TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true),
            TranslationSchemaManager.Companion.ValidationResult("foo/baz/transform.yml", true),
            TranslationSchemaManager.Companion.ValidationResult("foo/ban/transform.yml", true),
        )
        val command = ValidateTranslationSchemaCommand()

        val result = command.test("-s", "FHIR", "-c", "localhost", "-b", "container")
        assertThat(result.stdout).isEqualTo(
            """
            -----
            Results from validating FHIR, schemas were valid
            
            3 schemas were validated
            -----
            
            Individual Results:
            
            Path validated: foo/bar/transform.yml
            Valid: true
            Errored while validating: false
            
            Path validated: foo/baz/transform.yml
            Valid: true
            Errored while validating: false
            
            Path validated: foo/ban/transform.yml
            Valid: true
            Errored while validating: false
            
        """.trimIndent()
        )
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `test no results`() {
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns emptyList()
        val command = ValidateTranslationSchemaCommand()

        val result = command.test("-s", "FHIR", "-c", "localhost", "-b", "container")
        assertThat(result.stderr)
            .isEqualTo(
                "No schemas were found in fhir_transforms," +
                    " please make sure that you have the correct configuration\n"
            )
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `test invalid schema type`() {
        val command = ValidateTranslationSchemaCommand()

        val result = command.test("-s", "CSV", "-c", "localhost", "-b", "container")
        assertThat(result.stderr)
            .isEqualTo(
                """Usage: validateSchemas [<options>]

Error: invalid value for -s: invalid choice: CSV. (choose from FHIR, HL7)
"""
            )
        assertThat(result.statusCode).isEqualTo(1)
    }
}