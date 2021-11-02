package gov.cdc.prime.router

/**
 * Used by the engine to find submissions and reports
 */
interface SubmissionsProvider {
    fun findSubmissionsAsJson(
        organizationName: String,
        limit: String,
    ): String
}