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
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.OAuthFlow
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.security.OAuthScope
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.apache.logging.log4j.kotlin.Logging
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path

@OpenAPIDefinition(
    info = Info(
        title = "Prime ReportStream",
        description = "A router of public health data from multiple senders and receivers",
        contact = Contact(
            name = "USDS at Centers for Disease Control and Prevention",
            url = "https://reportstream.cdc.gov",
            email = "email: reportstream@cdc.gov"
        ),
        version = "0.2.0-oas3"
    )
)
@SecurityScheme(
    name = "primeSecurity",
    type = SecuritySchemeType.OAUTH2,
    flows = OAuthFlows(
        authorizationCode = OAuthFlow(
            authorizationUrl = "https://hhs-prime.okta.com/oauth/authorize",
            tokenUrl = "https://hhs-prime.okta.com/oauth/token",
            scopes = [
                OAuthScope(
                    name = "org_admin",
                    description = "Grants write access to single org"
                ),
                OAuthScope(name = "system_admin", description = "Grants access to admin operations"),
                OAuthScope(name = "user", description = "Grants read access")
            ]
        )
    )
)
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
    @Operation(
        summary = "Get Api Keys (deprecated)) use v1 version",
        responses = [
            ApiResponse(responseCode = "200", description = "Api key returned"),
            ApiResponse(responseCode = "404", description = "Api Key not found"),
            ApiResponse(responseCode = "400", description = "Bad request")
        ]
    )
    @GET
    @Path("settings/organizations/{organizationName}/public-keys")
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
    @Operation(
        summary = "Get Api Keys v1 version",
        responses = [
            ApiResponse(responseCode = "200", description = "Api key returned"),
            ApiResponse(responseCode = "404", description = "Api Key not found"),
            ApiResponse(responseCode = "400", description = "Bad request")
        ]
    )
    @GET
    @Path("v1/settings/organizations/{organizationName}/public-keys")
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
    @POST
    @Path("settings/organizations/{organizationName}/public-keys")
    @Operation(
        summary = "Post (update / override) to Api Keys",
        responses = [
            ApiResponse(responseCode = "403", description = "Operation forbidden")
        ]
    )
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
    @Operation(
        summary = "Delete key endpoint",
        description = "Test delete",
        tags = ["Public Key Management"],
        security = [
            SecurityRequirement(
                name = "primeSecurity"
            )
        ],
        responses = [
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "404", description = "Org, scope, or KID not found"),
            ApiResponse(
                responseCode = "200",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiKeysResponse::class)
                    )
                ]
            )
        ]
    )
    @DELETE
    @Path("settings/organizations/{organizationName}/public-keys/{scope}/{kid}")
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