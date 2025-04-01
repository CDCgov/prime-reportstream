package gov.cdc.prime.router.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.uhn.fhir.validation.ValidationResult
import gov.nist.validation.report.impl.EntryImpl
import hl7.v2.validation.report.Report
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val radxmarsProfileLocation = "metadata/hl7_validation/v251/radxmars/production"

class MessageValidatorTests {

    @Nested
    inner class MessageValidationResultTests {

        @Test
        fun `test HL7ValidationResult is valid`() {
            val errors = listOf(
                EntryImpl(
                    0,
                    0,
                    "",
                    "",
                    "",
                    AbstractItemValidator.ERROR_CLASSIFICATION
                )
            )

            val warnings = listOf(
                EntryImpl(
                    0,
                    0,
                    "",
                    "",
                    "",
                    "Warning"
                )
            )

            val warningReport = mockk<Report>()
            every { warningReport.entries } returns mapOf("1" to warnings)
            val errorReport = mockk<Report>()
            every { errorReport.entries } returns mapOf("1" to errors)
            val errorsAndWarningsReport = mockk<Report>()
            every { errorsAndWarningsReport.entries } returns mapOf("1" to errors + warnings)

            assertThat(HL7ValidationResult(warningReport).isValid()).isTrue()
            assertThat(HL7ValidationResult(errorReport).isValid()).isFalse()
            assertThat(HL7ValidationResult(errorsAndWarningsReport).isValid()).isFalse()
        }

        @Test
        fun `test FHIRValidationResult is valid`() {
            val successFhirResult = mockk<ValidationResult>()
            every { successFhirResult.isSuccessful } returns true
            val failedFhirResult = mockk<ValidationResult>()
            every { failedFhirResult.isSuccessful } returns false

            assertThat(FHIRValidationResult(successFhirResult).isValid()).isTrue()
            assertThat(FHIRValidationResult(failedFhirResult).isValid()).isFalse()
        }
    }

    @Test
    fun `test get HL7 validator`() {
        val validator = AbstractItemValidator.getHL7Validator(radxmarsProfileLocation)
        assertThat(validator).isNotNull()
    }

    @Test
    fun `test get HL7 validator reuses existing one if loaded`() {
        val validator1 = AbstractItemValidator.getHL7Validator(radxmarsProfileLocation)
        val validator2 = AbstractItemValidator.getHL7Validator(radxmarsProfileLocation)
        assertThat(validator1).isEqualTo(validator2)
    }

    @Test
    fun `test get HL7 validator without a profile throws an error`() {
        assertThrows<RuntimeException> {
            AbstractItemValidator.getHL7Validator("validation/badProfile")
        }
    }

    @Test
    fun `test loading HL7 validator with only a profile`() {
        val validator = AbstractItemValidator.getHL7Validator("validation/sparseProfile")
        assertThat(validator).isNotNull()
    }

    @Test
    fun `test only validates an HL7 message or FHIR bundle`() {
        class TestValidator : AbstractItemValidator() {
            override val validatorProfileName: String = "Test"
        }

        val validator = TestValidator()

        assertThrows<RuntimeException> {
            validator.validate("")
        }
    }

    @Test
    fun `test FHIR validation`() {
        class TestValidator : AbstractItemValidator() {
            override val validatorProfileName: String = "Test"
        }

        val validator = TestValidator()

        val bundle = Bundle()
        val result = validator.validate(bundle)
        assertThat(result.isValid()).isFalse()
    }
}