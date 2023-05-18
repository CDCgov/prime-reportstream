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
import gov.cdc.prime.router.tokens.AuthUtils
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.JwkSet
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.authenticationFailure
import org.apache.logging.log4j.kotlin.Logging

class ApiKeysFunctions(private val settingsFacade: SettingsFacade = SettingsFacade.common) : Logging {

    data class ApiKeysResponse(val orgName: String, val keys: List<JwkSet>)

    /**
     * Reusable function for fetching API keys.  The useNewApiResponse dictates whether or not to use the consistent
     * API response format
     */
    private fun getApiKeysForOrg(
        request: HttpRequestMessage<String?>,
        orgName: String,
        useNewApiResponse: Boolean = false
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf("*.*.primeadmin", "$orgName.*.admin"))) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        val organization = BaseEngine.settingsProviderSingleton.findOrganization(orgName)
            ?: return HttpUtilities.notFoundResponse(request, "No such organization: $orgName")

        val keys = organization.keys ?: emptyList()
        return if (useNewApiResponse) {
            HttpUtilities.okJSONResponse(request, ApiResponse(keys, MetaApiResponse("PublicKey", keys.size)))
        } else {
            HttpUtilities.okJSONResponse(request, ApiKeysResponse(orgName, keys))
        }
    }

    @Deprecated("The v1 version should be used")
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
        return getApiKeysForOrg(request, orgName)
    }

    @FunctionName("getApiKeysV1")
    fun getV1(
        @HttpTrigger(
            name = "getApiKeysV1",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v1/settings/organizations/{organizationName}/public-keys"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") orgName: String
    ): HttpResponseMessage {
        return getApiKeysForOrg(request, orgName, true)
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
            val jwk = AuthUtils.readPublicKeyPem(pemFileContents)
            val kid = request.queryParameters["kid"] ?: return HttpUtilities.bad(request, "kid must be provided")

            if (!JwkSet.isValidKidForScope(organization.keys, scope, kid)) {
                return HttpUtilities.bad(request, "kid must be unique for the requested scope")
            }

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

    /**
     * Removes a key from an organization's JwkSet
     * If either the scope or kid is missing, returns a 404
     *
     * @param request
     * @param scope - The scope that the key should be removed from
     * @param kid - the kid for the key to be removed
     *
     */
    @FunctionName("deleteApiKey")
    fun delete(
        @HttpTrigger(
            name = "deleteApiKey",
            methods = [HttpMethod.DELETE],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/public-keys/{scope}/{kid}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") orgName: String,
        @BindingName("scope") scope: String,
        @BindingName("kid") kid: String
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
        if (!Scope.isValidScope(scope, organization)) {
            return HttpUtilities.bad(request, "Request scope must be $orgName.*.report")
        }
        val jwkSetForScope = organization.keys?.find { jwkSet -> jwkSet.scope == scope }
            ?: return HttpUtilities.notFoundResponse(request, "Scope: $scope not found for organization")
        val key = jwkSetForScope.keys.find { key -> key.kid == kid } ?: return HttpUtilities.notFoundResponse(
            request,
            "KID: $kid not found for scope $scope"
        )

        val updatedKeys = JwkSet.removeKeyFromScope(organization.keys, scope, key)
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
    }
}