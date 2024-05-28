package gov.cdc.prime.router.azure.observability.event

/**
 * An observation can include multiple mapped conditions to be queried
 */
data class ObservationSummary(
    val conditions: List<ConditionSummary>,
    val code: String,
    val display: String,
) {
    constructor(condition: ConditionSummary, code: String, display: String) : this(listOf(condition), code, display)

    companion object {
        val EMPTY = ObservationSummary(emptyList(), "", "")
    }
}