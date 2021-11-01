package gov.cdc.prime.router

import gov.cdc.prime.router.azure.SubmissionAPI

/**
 * Used by the engine to find submissions and reports
 */
interface SubmissionsProvider {
    fun findSubmissionsAsJson(
        organizationName: String,
    ): String
}