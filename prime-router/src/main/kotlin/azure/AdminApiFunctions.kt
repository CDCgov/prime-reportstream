package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.common.StringUtilities.toIntOrDefault
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
import org.apache.logging.log4j.kotlin.Logging

/**
 * Functions that are restricted to prime admins
 *
 * Sidequest: This was copy pasta from LookupTableFunctions It needs a refactor to
 * move common into some kind of base class.
 */
class AdminApiFunctions(
    private val db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    private var oktaAuthentication: OktaAuthentication? = null
) : Logging {
    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Maybe be specified via the constructor for unit tests.
     * level defaults to safe (admin)
     * @return the Okta authenticator
     */
    private fun getOktaAuthenticator(level: PrincipalLevel = PrincipalLevel.SYSTEM_ADMIN): OktaAuthentication {
        return oktaAuthentication ?: OktaAuthentication(level)
    }

    /**
     * Fetch the list of send_errors. Spans orgs, so should ONLY be done
     * with admin permissions this.getOktaAuthenticator() defaults to SYSTEM_ADMIN above
     */
    @FunctionName("getsendfailures")
    fun run(
        @HttpTrigger(
            name = "getsendfailures",
            methods = [HttpMethod.GET, HttpMethod.OPTIONS],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "adm/getsendfailures" // this can NOT be "admin/" or it fails.
        )
        request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        logger.info("Entering adm/getsendfailures api")
        return getOktaAuthenticator().checkAccess(request) {
            try {
                val daysToShow = request.queryParameters[daysBackSpanParameter]
                    ?.toIntOrDefault(30) ?: 30

                val results = db.fetchSendFailures(daysToShow)
                val jsonb = mapper.writeValueAsString(results)
                HttpUtilities.okResponse(request, jsonb ?: "{}")
            } catch (e: Exception) {
                logger.error("Unable to fetch send failures", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    companion object {
        /**
         * Name of the query parameter to specify how many days back to show.
         */
        const val daysBackSpanParameter = "days_to_show"
    }
}