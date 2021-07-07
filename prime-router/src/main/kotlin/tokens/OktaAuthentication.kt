package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.prime.router.azure.*
import org.apache.logging.log4j.kotlin.Logging

class OktaAuthentication(private val minimumLevel: PrincipalLevel = PrincipalLevel.USER): Logging {
    private val missingAuthorizationHeader = HttpUtilities.errorJson("Missing Authorization Header")
    private val invalidClaim = HttpUtilities.errorJson("Invalid Authorization Header")

    fun getAccessToken(request: HttpRequestMessage<String?>): String? {
        // RFC6750 defines the access token
        val authorization = request.headers[HttpHeaders.AUTHORIZATION.lowercase()] ?: return null
        return authorization.substringAfter("Bearer ", "")
    }

    companion object: Logging {
        private var httpRequestMessage: HttpRequestMessage<String?>? = null

        fun setRequest(request: HttpRequestMessage<String?>) {
            httpRequestMessage = request
        }

        val claimVerifier: AuthenticationVerifier by lazy {

            // If we are running this locally and it is the initial setup from `prime-router/settings/put-local-settings.py`,
            // return the TestAuthenticationVerifier
            val primeEnv = System.getenv("PRIME_ENVIRONMENT")
            val settingsSetup = httpRequestMessage?.headers?.get("settingssetup")

            if (primeEnv == "local" && settingsSetup == "true")
                TestAuthenticationVerifier()
            else
                OktaAuthenticationVerifier()
        }
    }

    fun handleRequest(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        block: (claims: AuthenticatedClaims) -> HttpResponseMessage
    ): HttpResponseMessage {
        try {
            val accessToken = getAccessToken(request)
            if (accessToken == null) {
                logger.info("Missing Authorization Header: ${request.httpMethod}:${request.uri.path}")
                return HttpUtilities.unauthorizedResponse(request, missingAuthorizationHeader)
            }
            val host = request.uri.toURL().host
            setRequest(request)
            if (claimVerifier.requiredHosts.isNotEmpty() && !claimVerifier.requiredHosts.contains(host)) {
                logger.error("Wrong Authentication Verifier being used: ${claimVerifier::class} for $host")
                return HttpUtilities.unauthorizedResponse(request)
            }
            val claims = claimVerifier.checkClaims(accessToken, minimumLevel, organizationName)
            if (claims == null) {
                logger.info("Invalid Authorization Header: ${request.httpMethod}:${request.uri.path}")
                return HttpUtilities.unauthorizedResponse(request, invalidClaim)
            }

            logger.info("Settings request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")
            return block(claims)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
    }

}