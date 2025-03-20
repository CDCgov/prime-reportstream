package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.azure.ConditionStamper.Companion.CONDITION_CODE_EXTENSION_URL
import org.hl7.fhir.r4.model.Coding

data class TestSummary(
    val conditions: List<CodeSummary> = emptyList(),
    val testPerformedSystem: String = UNKNOWN,
    val testPerformedCode: String = UNKNOWN,
    val testPerformedDisplay: String = UNKNOWN,
) {
    companion object {
        const val UNKNOWN = "Unknown"

        /**
         * Create an instance of [TestSummary] from a [Coding]
         */
        fun fromCoding(coding: Coding): TestSummary {
            val conditions = coding.extension
                .filter { it.url == CONDITION_CODE_EXTENSION_URL }
                .map { it.castToCoding(it.value) }
                .map(CodeSummary::fromCoding)
            return TestSummary(
                conditions,
                coding.system ?: UNKNOWN,
                coding.code ?: UNKNOWN,
                coding.display ?: UNKNOWN
            )
        }
    }
}