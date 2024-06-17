package gov.cdc.prime.router.azure.observability.event

/**
 * An observation can include multiple mapped conditions to be queried
 */
data class ObservationSummary(
    val conditions: List<ConditionSummary>,
) {
    constructor(condition: ConditionSummary) : this(listOf(condition))

    companion object {
        val EMPTY = ObservationSummary(emptyList())
    }
}