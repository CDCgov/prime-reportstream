package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.JwkSet
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.SenderUtils
import gov.cdc.prime.router.tokens.authenticationFailure
import org.apache.logging.log4j.kotlin.Logging

class ApiKeysFunctions(private val settingsFacade: SettingsFacade = SettingsFacade.common) : Logging {

    data class ApiKeysResponse(val orgName: String, val keys: List<JwkSet>)

    @FunctionName("getApiKeys")
    fun get(
        @HttpTrigger(
            name = "getApiKeys",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/public-keys"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") orgName: String
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf("*.*.primeadmin", "$orgName.*.admin"))) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        val organization = BaseEngine.settingsProviderSingleton.findOrganization(orgName)
            ?: return HttpUtilities.notFoundResponse(request, "No such organization: $orgName")

        val keys = organization.keys ?: emptyList()
        return HttpUtilities.okJSONResponse(request, ApiKeysResponse(orgName, keys))
    }

    @FunctionName("postApiKey")
    fun post(
        @HttpTrigger(
            name = "postApiKey",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/public-keys"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") orgName: String
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf("*.*.primeadmin", "$orgName.*.admin"))) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        val organization =
            BaseEngine.settingsProviderSingleton.findOrganization(orgName) ?: return HttpUtilities.notFoundResponse(
                request,
                "No such organization $orgName"
            )

        try {
            val scope = request.queryParameters["scope"] ?: return HttpUtilities.bad(request, "Scope must be provided")
            if (!Scope.isValidScope(scope, organization)) {
                return HttpUtilities.bad(
                    request,
                    "Organization name in scope must match $orgName.  Instead got: $scope"
                )
            }

            if (scope != "$orgName.*.report") {
                return HttpUtilities.bad(request, "Request scope must be $orgName.*.report")
            }

            val pemFileContents = request.body ?: return HttpUtilities.bad(request, "Body must be provided")
            val jwk = SenderUtils.readPublicKeyPem(pemFileContents)
            val kid = request.queryParameters["kid"] ?: return HttpUtilities.bad(request, "kid must be provided")
            jwk.kid = kid

            val updatedKeys = JwkSet.addKeyToScope(organization.keys, scope, jwk)

            val (result, error) = settingsFacade.putSetting(
                organization.name,
                JacksonMapperUtilities.defaultMapper.writeValueAsString(Organization(organization, updatedKeys)),
                claims,
                OrganizationAPI::class.java
            )

            return if (result == SettingsFacade.AccessResult.SUCCESS) {
                HttpUtilities.okJSONResponse(request, ApiKeysResponse(orgName, updatedKeys))
            } else {
                HttpUtilities.bad(request, error)
            }
        } catch (e: Exception) {
            return HttpUtilities.bad(request, "Unable to parse public key: ${e.localizedMessage}")
        }
    }
}