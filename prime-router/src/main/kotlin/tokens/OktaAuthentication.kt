package gov.cdc.prime.router.tokens

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.AuthenticatedClaims
import gov.cdc.prime.router.azure.AuthenticatedClaimsResult
import gov.cdc.prime.router.azure.AuthenticationResult
import gov.cdc.prime.router.azure.AuthenticationVerifier
import gov.cdc.prime.router.azure.Authenticator
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.OktaAuthenticationVerifier
import gov.cdc.prime.router.azure.PrincipalLevel
import gov.cdc.prime.router.azure.TestAuthenticationVerifier
import gov.cdc.prime.router.azure.WorkflowEngine
import org.apache.logging.log4j.kotlin.Logging

class OktaAuthentication(private val minimumLevel: PrincipalLevel = PrincipalLevel.USER) : Authenticator, Logging {
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

        fun authenticationVerifier(): AuthenticationVerifier {
            // If we are running this locally, use the TestAuthenticationVerifier
            // To test locally _with_ auth, add a 'localauth=true' header to your POST.
            val primeEnv = System.getenv("PRIME_ENVIRONMENT")
            val localAuth = httpRequestMessage?.headers?.get("localauth")

            return if (primeEnv != "local") {
                OktaAuthenticationVerifier()
            } else if (localAuth != null && localAuth == "true") {
                OktaAuthenticationVerifier()
            } else {
                logger.info("No auth needed - running locally")
                TestAuthenticationVerifier()
            }
        }
    }

    fun checkClaims(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        oktaSender: Boolean = false,
    ): AuthenticatedClaimsResult {
        val accessToken = getAccessToken(request)
        if (accessToken == null) {
            logger.info("Missing Authorization Header: ${request.httpMethod}:${request.uri.path}")
            return AuthenticatedClaimsResult(null, missingAuthorizationHeader)
        }
        val host = request.uri.toURL().host
        setRequest(request)
        val claimVerifier = authenticationVerifier()
        if (claimVerifier.requiredHosts.isNotEmpty() && !claimVerifier.requiredHosts.contains(host)) {
            logger.error("Wrong Authentication Verifier being used: ${claimVerifier::class} for $host")
            return AuthenticatedClaimsResult(null, "")
        }
        val claims = claimVerifier.checkClaims(accessToken, minimumLevel, organizationName, oktaSender)
        if (claims == null) {
            logger.info("Invalid Authorization Header: ${request.httpMethod}:${request.uri.path}")
            return AuthenticatedClaimsResult(null, invalidClaim)
        }

        logger.info("Request by ${claims.userName}: ${request.httpMethod}:${request.uri.path}")
        return AuthenticatedClaimsResult(claims, null)
    }

    override fun checkAccess(request: HttpRequestMessage<String?>, sender: String): AuthenticationResult {
        val oktaSender = true
        val (claims, status) = checkClaims(request, sender, oktaSender)
        if (claims != null) {
            return AuthenticationResult(true, null)
        }
        return AuthenticationResult(false, status)
    }

    fun checkAccess(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        oktaSender: Boolean = false,
        block: (AuthenticatedClaims) -> HttpResponseMessage
    ): HttpResponseMessage {
        val (claims, status) = checkClaims(request, organizationName, oktaSender)
        if (claims != null) {
            return block(claims)
        }
        return HttpUtilities.unauthorizedResponse(request, status ?: "")
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