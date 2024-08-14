package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import gov.cdc.prime.router.config.validation.models.HL7ToFHIRMappingResourceTemplate
import io.github.linuxforhealth.hl7.expression.ExpressionAttributes
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class ValidationsTest {

    @Nested
    inner class HL7ToFHIRMappingResourceTemplateValidationTest {

        @Test
        fun `valid conditions`() {
            // each type of condition
            listOf(
                "\$var NOT_NULL",
                "\$var NULL",
                "\$var > 5",
                "\$var1 NOT_NULL && \$var2 NULL",
                "\$var1 NOT_NULL || \$var2 NULL",
                "\$var1 NOT_NULL && (\$var2 NULL || \$var3 < 5)",
                "true"
            )
                .map(::generateResourceTemplate)
                .map(HL7ToFHIRMappingResourceTemplateValidation.validation::validate)
                .forEach { assertThat(it.errors).isEmpty() }
        }

        @Test
        fun `invalid conditions`() {
            // all conditions missing '$' on variable
            listOf(
                "var NOT_NULL",
                "var NULL",
                "var > 5",
                "var1 NOT_NULL && \$var2 NULL",
                "var1 NOT_NULL || \$var2 NULL",
                "var1 NOT_NULL && (\$var2 NULL || \$var3 < 5)",
            )
                .map(::generateResourceTemplate)
                .map(HL7ToFHIRMappingResourceTemplateValidation.validation::validate)
                .forEach {
                    assertThat(it.errors)
                        .transform { errors -> errors.first().message }
                        .contains("Invalid format for condition variable")
                }
        }

        private fun generateResourceTemplate(condition: String): HL7ToFHIRMappingResourceTemplate {
            val resourceType = "resource"
            val expression = ExpressionAttributes.Builder()
                .withCondition(condition)
                .build()
            val flatExpressions = listOf(expression.filter)
            val expressionsMap = mapOf(
                "fake" to expression
            )
            return HL7ToFHIRMappingResourceTemplate(
                resourceType,
                expressionsMap,
                flatExpressions
            )
        }
    }
}