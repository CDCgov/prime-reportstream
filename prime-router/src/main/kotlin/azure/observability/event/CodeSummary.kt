package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Coding

data class CodeSummary(
    val system: String = "",
    val code: String = "",
    val display: String = "",
) {
    companion object {
        /**
         * Create an instance of [CodeSummary] from a [Coding]
         */
        fun fromCoding(coding: Coding) = CodeSummary(
            coding.system ?: "Unknown",
            coding.code ?: "Unknown",
            coding.display ?: "Unknown",
        )
    }
}