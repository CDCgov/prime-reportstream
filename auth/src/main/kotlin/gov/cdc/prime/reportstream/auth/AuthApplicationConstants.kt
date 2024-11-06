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

    object Scopes {
        const val ORGANIZATION_SCOPE = "organization"
        const val SUBJECT_SCOPE = "sub"
    }
}