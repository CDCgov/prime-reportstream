package gov.cdc.prime.reportstream.auth

/**
 * File used for application-wide constants
 */
object AuthApplicationConstants {

    /**
     * All Auth service endpoints defined here
     */
    object Endpoints {
        const val HEALTHCHECK_ENDPOINT_V1 = "/api/v1/healthcheck"
    }

    /**
     * All Submissions service endpoints defined here
     */
    object SubmissionsEndpoints {
        const val REPORTS_ENDPOINT_V1 = "/api/v1/reports"
    }
}