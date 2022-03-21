package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.AuthenticatedClaims
import gov.cdc.prime.router.azure.AuthenticationVerifier
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.OktaAuthenticationVerifier
import gov.cdc.prime.router.azure.PrincipalLevel
import gov.cdc.prime.router.azure.TestAuthenticationVerifier
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.Environment
import org.apache.logging.log4j.kotlin.Logging

class OktaAuthentication(private val minimumLevel: PrincipalLevel = PrincipalLevel.USER) : Logging {
    private val missingAuthorizationHeader = HttpUtilities.errorJson("Missing Authorization Header")
    private val invalidClaim = HttpUtilities.errorJson("Invalid Authorization Header")

    fun getAccessToken(request: HttpRequestMessage<String?>): String? {
        // RFC6750 defines the access token
        val authorization = request.headers[HttpHeaders.AUTHORIZATION.lowercase()] ?: return null
        return authorization.substringAfter("Bearer ", "")
    }

    companion object : Logging {
        private var httpRequestMessage: HttpRequestMessage<String?>? = null

        fun setRequest(request: HttpRequestMessage<String?>) {
            httpRequestMessage = request
        }

        fun authenticationVerifier(accessToken: String): AuthenticationVerifier {
            // If we are running this locally, use the TestAuthenticationVerifier
            // To test locally _with_ auth, add a 'localauth=true' header to your POST.
            val localAuth = httpRequestMessage?.headers?.get("localauth")

            return if (!Environment.isLocal()) {
                OktaAuthenticationVerifier()
            } else if (localAuth != null && localAuth == "true") {
                OktaAuthenticationVerifier()
            } else if (accessToken.split(".").size == 3) {
                // Running local, but test using the real production parser.
                // The above test is purposefully lame so that we can test all kinds of error conditions
                // further downstream.
                logger.info("Running locally, but will use the OktaAuthenticationVerifier")
                OktaAuthenticationVerifier()
            } else {
                logger.info("No auth needed - running locally")
                TestAuthenticationVerifier()
            }
        }
    }

    fun checkAccess(
        request: HttpRequestMessage<String?>,
        organizationName: String = "",
        oktaSender: Boolean = false,
        actionHistory: ActionHistory? = null,
        block: (AuthenticatedClaims) -> HttpResponseMessage
    ): HttpResponseMessage {
        try {
            val accessToken = getAccessToken(request)
            if (accessToken == null) {
                logger.info("Missing Authorization Header: ${request.httpMethod}:${request.uri.path}")
                return HttpUtilities.unauthorizedResponse(request, missingAuthorizationHeader)
            }
            val host = request.uri.toURL().host
            setRequest(request)
            val claimVerifier = authenticationVerifier(accessToken)
            if (claimVerifier.requiredHosts.isNotEmpty() && !claimVerifier.requiredHosts.contains(host)) {
                logger.error("Wrong Authentication Verifier being used: ${claimVerifier::class} for $host")
                return HttpUtilities.unauthorizedResponse(request)
            }
            val claims = claimVerifier.checkClaims(accessToken, minimumLevel, organizationName, oktaSender)
            if (claims == null) {
                logger.info("Invalid Authorization Header: ${request.httpMethod}:${request.uri.path}")
                return HttpUtilities.unauthorizedResponse(request, invalidClaim)
            }

            logger.info("Request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")
            actionHistory?.trackUsername(claims.userName)
            return block(claims)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
    }

    /**
     * For endpoints that need to check if the organization is in the database
     */
    fun checkOrganizationExists(context: ExecutionContext, userName: String, orgName: String?): Organization? {
        var organization: Organization? = null
        if (orgName != null) {
            organization = WorkflowEngine().settings.findOrganization(orgName.replace('_', '-'))
            if (organization != null) {
                return organization
            } else {
                context.logger.info("User $userName failed auth: Organization $orgName is unknown to the system.")
            }
        }
        return organization
    }
}