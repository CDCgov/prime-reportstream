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
import java.time.OffsetDateTime

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
    fun getSendFailures(
        @HttpTrigger(
            name = "getsendfailures",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "adm/getsendfailures" // this can NOT be "admin/" or it fails.
        )
        request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        logger.info("Entering adm/getsendfailures api")
        return getOktaAuthenticator(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) {
            try {
                val daysToShow = request.queryParameters[daysBackSpanParameter]
                    ?.toIntOrDefault(30) ?: 30

                val results = db.fetchSendFailures(daysToShow)
                val jsonb = mapper.writeValueAsString(results)
                HttpUtilities.okResponse(request, jsonb ?: "[]")
            } catch (e: Exception) {
                logger.error("Unable to fetch send failures", e)
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Fetch the list of send_errors. Spans orgs, so should ONLY be done
     * with admin permissions this.getOktaAuthenticator() defaults to SYSTEM_ADMIN above
     */
    @FunctionName("listreceiversconnstatus")
    fun listReceiversConnStatus(
        @HttpTrigger(
            name = "listreceiversconnstatus",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "adm/listreceiversconnstatus" // this can NOT be "admin/" or it fails.
        )
        request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        logger.info("Entering adm/listreceiversconnstatus api")
        return getOktaAuthenticator(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) {
            try {
                val startDateTime = OffsetDateTime.parse(request.queryParameters[startDateParam])
                // endDateParam may be missing and that's ok.
                val endDateTimeStr = request.queryParameters[endDateParam]
                val endDateTime =
                    if (endDateTimeStr != null) OffsetDateTime.parse(endDateTimeStr) else null
                val results = db.fetchReceiverConnectionCheckResults(startDateTime, endDateTime)
                val jsonb = mapper.writeValueAsString(results)
                HttpUtilities.okResponse(request, jsonb ?: "[]")
            } catch (e: Exception) {
                logger.error(e)
                // admin calling so, it's ok to reveal more info in error
                HttpUtilities.badRequestResponse(
                    request,
                    "Invalid/missing cgi parameter. Check date formats."
                )
            }
        }
    }

    /**
     * Fetch the list of "resent". Spans orgs, so should ONLY be done
     * with admin permissions this.getOktaAuthenticator() defaults to SYSTEM_ADMIN above
     */
    @FunctionName("getresend")
    fun getSendRetries(
        @HttpTrigger(
            name = "getresend",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "adm/getresend" // this can NOT be "admin/" or it fails.
        )
        request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        logger.info("Entering adm/getresend api")
        return getOktaAuthenticator(PrincipalLevel.SYSTEM_ADMIN).checkAccess(request) {
            try {
                val daysToShow = request.queryParameters[daysBackSpanParameter]
                    ?.toIntOrDefault(30) ?: 30
                val results = db.fetchResends(OffsetDateTime.now().minusDays(daysToShow.toLong()))
                val jsonb = mapper.writeValueAsString(results)
                HttpUtilities.okResponse(request, jsonb ?: "[]")
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

        /**
         * Days back is not very flexible. Moving calls to start and end dated.
         */
        const val startDateParam = "start_date"
        /**
         * End date *may* be optional. If missing assumes current date.
         */
        const val endDateParam = "end_date"
    }
}