package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Coding

/**
 * The important fields from a mapped condition to be queried in Azure
 *
 * @param code condition code
 * @param display human-readable condition
 */
data class ConditionSummary(
    val code: String,
    val display: String,
) {
    companion object {
        /**
         * Create an instance of [ConditionSummary] from a [Coding]
         */
        fun fromCoding(coding: Coding) = ConditionSummary(
            coding.code,
            coding.display
        )
    }
}