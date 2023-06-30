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
import io.swagger.v3.oas.annotations.Parameter
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
import javax.ws.rs.PathParam

const val KEY_MGMT_TAG = "Public Key Management"
const val HTTP_401_ERR_MSG = "Unauthorized operation"
const val HTTP_404_ERR_MSG = "API key not found"
const val HTTP_400_ERR_MSG = "Bad request"
const val HTTP_200_GET_MSG = "API keys returned"
const val HTTP_200_DELETE_MSG = "API key deleted"
const val HTTP_200_POST_MSG = "API key created"
const val HTTP_404_DELETE_MSG = "Organization, scope, or key id not found"

const val PRIME_ADMIN_PATTERN = "*.*.primeadmin"
const val ORG_NAME_DESC = "the organization whose keys are retrieved"
const val OPERATION_GET_KEYS_DESC = "Retrieve API key(s) for the given organization"
const val ORG_NAME_STR = "organizationName"
const val SCOPE_STR = "scope"
const val KID_STR = "kid"
const val HTTP_200_OK = "200"
const val HTTP_400_BAD_REQ = "400"
const val HTTP_401_UNAUTHORIZED = "401"
const val HTTP_404_NOT_FOUND = "404"

@OpenAPIDefinition(
    info = Info(
        title = "Prime ReportStream",
        description = "A router of public health data from senders to receivers",
        contact = Contact(
            name = "USDS at Centers for Disease Control and Prevention",
            url = "https://reportstream.cdc.gov",
            email = "reportstream@cdc.gov"
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
        if (claims == null || !claims.authorized(setOf(PRIME_ADMIN_PATTERN, "$orgName.*.admin"))) {
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
    @SuppressWarnings("all")
    @Operation(
        summary = "Retrieve API keys (deprecated use v1 version)",
        description = OPERATION_GET_KEYS_DESC, // NOSONAR
        tags = [KEY_MGMT_TAG], // NOSONAR
        parameters = [
            Parameter(
                name = ORG_NAME_STR, // NOSONAR
                required = true, // NOSONAR
                description = ORG_NAME_DESC // NOSONAR
            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_200_OK, // NOSONAR
                description = HTTP_200_GET_MSG, // NOSONAR
                content = [ // NOSONAR
                    Content( // NOSONAR
                        mediaType = "application/json", // NOSONAR
                        schema = Schema(implementation = ApiKeysResponse::class) // NOSONAR
                    )
                ]
            ),
            ApiResponse(responseCode = HTTP_404_NOT_FOUND, description = HTTP_404_ERR_MSG), // NOSONAR
            ApiResponse(responseCode = HTTP_400_BAD_REQ, description = HTTP_400_ERR_MSG) // NOSONAR
        ],
    )
    @GET
    @Path("settings/organizations/{organizationName}/public-keys")
    fun get(
        @HttpTrigger(
            name = "getApiKeys",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/public-keys"
        ) @Parameter(hidden = true) request: HttpRequestMessage<String?>,
        @Parameter(
            name = ORG_NAME_STR,
            description = ORG_NAME_DESC
        )
        @PathParam(ORG_NAME_STR)
        @BindingName(ORG_NAME_STR) orgName: String
    ): HttpResponseMessage {
        return getApiKeysForOrg(request, orgName)
    }

    @FunctionName("getApiKeysV1")
    @SuppressWarnings("all")
    @Operation(
        summary = "Retrieve API keys for the organization (v1), return API keys when successful",
        description = OPERATION_GET_KEYS_DESC, // NOSONAR
        tags = [KEY_MGMT_TAG], // NOSONAR
        parameters = [
            Parameter(
                name = ORG_NAME_STR, // NOSONAR
                required = true, // NOSONAR
                description = ORG_NAME_DESC // NOSONAR
            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_200_OK, // NOSONAR
                description = HTTP_200_GET_MSG, // NOSONAR
                content = [
                    Content( // NOSONAR
                        mediaType = "application/json", // NOSONAR
                        schema = Schema(implementation = ApiKeysResponse::class) // NOSONAR
                    )
                ]
            ),
            ApiResponse(responseCode = HTTP_404_NOT_FOUND, description = HTTP_404_ERR_MSG), // NOSONAR
            ApiResponse(responseCode = HTTP_400_BAD_REQ, description = HTTP_400_ERR_MSG) // NOSONAR
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
        ) @Parameter(hidden = true) request: HttpRequestMessage<String?>,
        @Parameter(
            name = ORG_NAME_STR,
            description = ORG_NAME_DESC
        )
        @PathParam(ORG_NAME_STR)
        @BindingName(ORG_NAME_STR) orgName: String
    ): HttpResponseMessage {
        return getApiKeysForOrg(request, orgName, true)
    }

    @FunctionName("postApiKey")
    @POST
    @Path("settings/organizations/{organizationName}/public-keys")
    @SuppressWarnings("all")
    @Operation(
        summary = "Create (post) an API key for the organization",
        description = "Create API key for the given organization",
        tags = [KEY_MGMT_TAG],
        responses = [
            ApiResponse(responseCode = HTTP_200_OK, description = HTTP_200_POST_MSG), // NOSONAR
            ApiResponse(responseCode = HTTP_401_UNAUTHORIZED, description = HTTP_401_ERR_MSG) // NOSONAR
        ]
    )
    fun post(
        @HttpTrigger(
            name = "postApiKey",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/public-keys"
        ) request: HttpRequestMessage<String?>,
        @Parameter(
            name = ORG_NAME_STR,
            description = "the organization where a key is to be created."
        )
        @PathParam(ORG_NAME_STR)
        @BindingName(ORG_NAME_STR) orgName: String
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf(PRIME_ADMIN_PATTERN, "$orgName.*.admin"))) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        }
        val organization =
            BaseEngine.settingsProviderSingleton.findOrganization(orgName) ?: return HttpUtilities.notFoundResponse(
                request,
                "No such organization $orgName"
            )

        try {
            val scope = request.queryParameters[SCOPE_STR]
                ?: return HttpUtilities.bad(request, "Scope must be provided")
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
            val kid = request.queryParameters[KID_STR] ?: return HttpUtilities.bad(request, "kid must be provided")

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
    @SuppressWarnings("all")
    @Operation(
        summary = "Delete API key",
        description = "Delete API key given organization name, scope, and kid",
        tags = [KEY_MGMT_TAG], // NOSONAR
        security = [
            SecurityRequirement(
                name = "primeSecurity"
            )
        ],
        responses = [
            ApiResponse(responseCode = HTTP_401_UNAUTHORIZED, description = HTTP_401_ERR_MSG), // NOSONAR
            ApiResponse(responseCode = HTTP_404_NOT_FOUND, description = HTTP_404_DELETE_MSG), // NOSONAR

            ApiResponse(
                responseCode = HTTP_200_OK, // NOSONAR
                description = HTTP_200_DELETE_MSG, // NOSONAR
                content = [
                    Content( // NOSONAR
                        mediaType = "application/json", // NOSONAR
                        schema = Schema(implementation = ApiKeysResponse::class) // NOSONAR
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
        ) @Parameter(hidden = true) request: HttpRequestMessage<String?>,
        @Parameter(
            name = ORG_NAME_STR,
            description = "the organization whose key is to be deleted."
        )
        @PathParam(ORG_NAME_STR) @BindingName(ORG_NAME_STR) orgName: String,
        @Parameter(
            name = SCOPE_STR,
            description = "scope of the key to be deleted."
        )
        @PathParam(SCOPE_STR) @BindingName(SCOPE_STR) scope: String,
        @Parameter(
            name = KID_STR,
            description = "id of the key to be deleted."
        )
        @PathParam(KID_STR) @BindingName(KID_STR) kid: String
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null || !claims.authorized(setOf(PRIME_ADMIN_PATTERN, "$orgName.*.admin"))) {
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