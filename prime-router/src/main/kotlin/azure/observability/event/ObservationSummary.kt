package gov.cdc.prime.router.azure.observability.event

/**
 * An observation can include multiple mapped conditions to be queried
 */
data class ObservationSummary(val testSummary: List<TestSummary> = emptyList()) {

    companion object {
        val EMPTY = ObservationSummary(emptyList())
    }
}