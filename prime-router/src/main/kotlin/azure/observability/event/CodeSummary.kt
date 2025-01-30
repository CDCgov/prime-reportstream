package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Coding

data class CodeSummary(
    val system: String = EMPTY_STRING,
    val code: String = EMPTY_STRING,
    val display: String = EMPTY_STRING,
) {
    companion object {
        const val UNKNOWN = "Unknown"
        const val EMPTY_STRING = ""

        /**
         * Create an instance of [CodeSummary] from a [Coding]
         */
        fun fromCoding(coding: Coding?) = CodeSummary(
            coding?.system ?: UNKNOWN,
            coding?.code ?: UNKNOWN,
            coding?.display ?: UNKNOWN,
        )
    }
}