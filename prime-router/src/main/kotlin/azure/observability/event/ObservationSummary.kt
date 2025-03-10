package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Observation

/**
 * An observation can include multiple mapped conditions to be queried
 */
data class ObservationSummary(
    val testSummary: List<TestSummary> = emptyList(),
    val interpretations: List<CodeSummary> = emptyList(),
) {

    companion object {
        val EMPTY = ObservationSummary(emptyList())
        fun fromObservation(observation: Observation): ObservationSummary {
            val summaries = mutableListOf<TestSummary>()
            val interpretations = mutableListOf<CodeSummary>()
            observation.interpretation.forEach { concept ->
                (
                    concept.coding.forEach { interpretations.add(CodeSummary.fromCoding(it)) }
                    )
            }
            observation.code.coding.forEach { codingCode ->
                summaries.add(TestSummary.fromCoding(codingCode))
            }
            return ObservationSummary(summaries, interpretations)
        }
    }
}