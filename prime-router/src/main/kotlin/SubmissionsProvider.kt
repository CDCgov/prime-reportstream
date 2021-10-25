package gov.cdc.prime.router

/**
 * Used by the engine to find submissions and reports
 */
interface SubmissionsProvider {
    val submissions: Collection<Submission>
}