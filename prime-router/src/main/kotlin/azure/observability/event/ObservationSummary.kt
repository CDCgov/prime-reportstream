package gov.cdc.prime.router.azure.observability.event

/**
 * An observation can include multiple mapped conditions to be queried
 */
data class ObservationSummary(
    val conditions: List<ConditionSummary> = emptyList(),
    val code: String = "Unknown",
    val display: String = "Unknown",
) {
    constructor(condition: ConditionSummary, code: String, display: String) : this(listOf(condition), code, display)
    constructor(condition: ConditionSummary, code: String) : this(listOf(condition), code, "Unknown")
    constructor(conditions: List<ConditionSummary>, code: String) : this(conditions, code, "Unknown")

    companion object {
        val EMPTY = ObservationSummary(emptyList(), "", "")
    }
}