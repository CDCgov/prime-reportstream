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
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.OAuthFlow
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.security.OAuthScope
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.security.SecuritySchemes
import io.swagger.v3.oas.annotations.servers.Server
import org.apache.logging.log4j.kotlin.Logging
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam

const val KEY_MGMT_TAG = "Public Key Management"

const val HTTP_200_OK = "200"
const val HTTP_400_BAD_REQ = "400"
const val HTTP_401_UNAUTHORIZED = "401"
const val HTTP_404_NOT_FOUND = "404"

const val HTTP_401_ERR_MSG = "Unauthorized operation"
const val HTTP_404_ERR_MSG = "Not found, e.g. No such organization: simple_X_report"
const val HTTP_400_ERR_MSG = "Bad request"
const val HTTP_200_GET_MSG = "API keys returned"
const val HTTP_200_DELETE_MSG = "API key deleted"
const val HTTP_200_POST_MSG = "API key created"
const val HTTP_404_ERR_MSG_DELETE = "Not Found, e.g. no such organization, or no key id found"
const val HTTP_404_ERR_MSG_POST = "Not Found, e.g. No such organization simple_report_noop"
const val HTTP_400_ERR_MSG_DELETE = "Bad Request, e.g. request scope must be simple_report.*.report"
const val HTTP_400_ERR_MSG_POST = "Bad Request, e.g. kid must be unique for the requested scope"
const val PRIME_ADMIN_PATTERN = "*.*.primeadmin"
const val OPERATION_GET_KEYS_DESC = "Retrieve API key(s) for the given organization"

const val PARAM_NAME_ORGNAME = "organizationName"
const val PARAM_NAME_SCOPE = "scope"
const val PARAM_NAME_KID = "kid"

// GET
const val PARAM_DESC_ORGNAME_GET = "the organization whose keys are retrieved"

// DELETE
const val PARAM_DESC_SCOPE_DEL = "the scope of the key to be deleted."
const val PARAM_DESC_ORGNAME_DEL = "the organization whose key is to be deleted."
const val PARAM_DESC_KID_DEL = "the id of the key to be deleted."

// POST
const val PARAM_DESC_SCOPE_POST = "the scope of the key to be created."
const val PARAM_DESC_ORGNAME_POST = "the organization whose key is to be created."
const val PARAM_DESC_KID_POST = "the id of the key to be created."

// Examples for GET, POST, DELETE
const val EX_GET_APIKEYS_RESP = """
{
    "data": [
        {
            "scope": "simple_report.*.report",
            "keys": [
                {
                    "kty": "RSA",
                    "kid": "simple_report",
                    "n": "jgM1afKc5oBw_jq_B4C0oqKbqFTvAAt-FGnZZJ8hczsZmTIr4L2orV49zdaRQOve7Q0KwUOzWPAHpv9WYjDOyvf8ea-IgngM0EQAjcXuxlDaD7UvGurQmiATOTvpDQkjhaMQyTyfD8_6p8kjY3hcQw2dByoFziZ-ofRgYI5jGvtgSRDc_obIs2u5G0wrhlh2sGRUF0mI9pqE8P8bd7TCeUpLJU2E3wz4LSbkbmL-u_JMRfSRzxh0c2baLcwJT9CtzWufNWeto9hITrgVddX7xdjVNq3uyeQvypeq9ZX9IhfiHTQTt4uZ9FKQUF9VP2mk4GRsCjnkNCRpi6LhP_d0Sw",
                    "e": "AQAB"
                }
            ]
        }
    ],
    "meta": {
        "type": "PublicKey",
        "totalCount": 1
    }
}    
"""

const val EX_POST_APIKEY_RESP = """
{
    "orgName": "simple_report",
    "keys": [
        {
            "scope": "simple_report.*.report",
            "keys": [
                {
                    "kty": "RSA",
                    "kid": "simple_report",
                    "n": "jgM1afKc5oBw_jq_B4C0oqKbqFTvAAt-FGnZZJ8hczsZmTIr4L2orV49zdaRQOve7Q0KwUOzWPAHpv9WYjDOyvf8ea-IgngM0EQAjcXuxlDaD7UvGurQmiATOTvpDQkjhaMQyTyfD8_6p8kjY3hcQw2dByoFziZ-ofRgYI5jGvtgSRDc_obIs2u5G0wrhlh2sGRUF0mI9pqE8P8bd7TCeUpLJU2E3wz4LSbkbmL-u_JMRfSRzxh0c2baLcwJT9CtzWufNWeto9hITrgVddX7xdjVNq3uyeQvypeq9ZX9IhfiHTQTt4uZ9FKQUF9VP2mk4GRsCjnkNCRpi6LhP_d0Sw",
                    "e": "AQAB"
                }
            ]
        }
    ]
}
"""

const val EX_DELETE_APIKEY_RESP = """
{
    "orgName": "simple_report",
    "keys": [
        {
            "scope": "simple_report.*.report",
            "keys": []
        }
    ]
}
"""

const val EX_POST_APIKEY_REQUEST_BODY = """
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjgM1afKc5oBw/jq/B4C0
oqKbqFTvAAt+FGnZZJ8hczsZmTIr4L2orV49zdaRQOve7Q0KwUOzWPAHpv9WYjDO
yvf8ea+IgngM0EQAjcXuxlDaD7UvGurQmiATOTvpDQkjhaMQyTyfD8/6p8kjY3hc
Qw2dByoFziZ+ofRgYI5jGvtgSRDc/obIs2u5G0wrhlh2sGRUF0mI9pqE8P8bd7TC
eUpLJU2E3wz4LSbkbmL+u/JMRfSRzxh0c2baLcwJT9CtzWufNWeto9hITrgVddX7
xdjVNq3uyeQvypeq9ZX9IhfiHTQTt4uZ9FKQUF9VP2mk4GRsCjnkNCRpi6LhP/d0
SwIDAQAB
-----END PUBLIC KEY-----
"""

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
    ),
    servers = [
        Server(
            url = "http://localhost:7071/api/",
            description = "Local Server (Local Development Use)"
        ),
        Server(
            url = "https://staging.prime.cdc.gov/api/",
            description = "Staging Server"
        ),
        Server(
            url = "https://prime.cdc.gov/api/",
            description = "Production Server"
        )
    ]
)

@SecuritySchemes(
    value = [
        SecurityScheme(
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
                        OAuthScope(name = "prime_admin", description = "Grants access to admin operations"),
                        OAuthScope(name = "user", description = "Grants read access")
                    ]
                )
            ),
            description = "OAUTH2 Authorization for Report Stream API Access."
        ),
        SecurityScheme(
            name = "primeSecurityAPIKey",
            type = SecuritySchemeType.APIKEY,
            paramName = "x-functions-key",
            description = "Azure Function Key Authorization for Report Stream API Access."
        ),
        SecurityScheme(
            name = "primeSecurityServerToServer",
            type = SecuritySchemeType.HTTP,
            scheme = "Bearer",
            bearerFormat = "JWT",
            description = "HTTP Bearer Token Authorization for Report Stream API Access."
        )
    ]
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
        if (claims == null || !claims.authorized(setOf(PRIME_ADMIN_PATTERN, "$orgName.*.admin", "$orgName.*.user"))) {
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
        deprecated = true,
        summary = "Retrieve API keys (deprecated use v1 version)",
        description = OPERATION_GET_KEYS_DESC,
        tags = [KEY_MGMT_TAG],
        parameters = [
            Parameter(
                name = PARAM_NAME_ORGNAME,
                required = true,
                description = PARAM_DESC_ORGNAME_GET
            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_200_OK,
                description = HTTP_200_GET_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiKeysResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Example response for API key retrieval",
                                summary = "Example response when key retrieved successfully",
                                value = EX_GET_APIKEYS_RESP
                            ),
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_404_NOT_FOUND,
                description = HTTP_404_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_400_BAD_REQ,
                description = HTTP_400_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            )
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
            name = PARAM_NAME_ORGNAME,
            description = PARAM_DESC_ORGNAME_GET
        )
        @PathParam(PARAM_NAME_ORGNAME)
        @BindingName(PARAM_NAME_ORGNAME) orgName: String
    ): HttpResponseMessage {
        return getApiKeysForOrg(request, orgName)
    }

    @FunctionName("getApiKeysV1")
    @Operation(
        summary = "Retrieve API keys for the organization (v1), return API keys when successful",
        description = OPERATION_GET_KEYS_DESC,
        tags = [KEY_MGMT_TAG],
        parameters = [
            Parameter(
                name = PARAM_NAME_ORGNAME,
                required = true,
                description = PARAM_DESC_ORGNAME_GET,
            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_200_OK,
                description = HTTP_200_GET_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiKeysResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Example response for API key retrieval",
                                summary = "Example response when key retrieved successfully",
                                value = EX_GET_APIKEYS_RESP
                            ),
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_404_NOT_FOUND,
                description = HTTP_404_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_400_BAD_REQ,
                description = HTTP_400_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            )
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
            name = PARAM_NAME_ORGNAME,
            description = PARAM_DESC_ORGNAME_GET,
        )
        @PathParam(PARAM_NAME_ORGNAME)
        @BindingName(PARAM_NAME_ORGNAME) orgName: String
    ): HttpResponseMessage {
        return getApiKeysForOrg(request, orgName, true)
    }

    @FunctionName("postApiKey")
    @POST
    @Path("settings/organizations/{organizationName}/public-keys")
    @Operation(
        summary = "Create (POST) an API key for the organization",
        description = "Create API key for the given organization",
        tags = [KEY_MGMT_TAG],
        parameters = [
            Parameter(
                name = PARAM_NAME_ORGNAME,
                required = true,
                description = PARAM_DESC_ORGNAME_POST,

            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_200_OK,
                description = HTTP_200_POST_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = HttpResponseMessage::class),
                        examples = [
                            ExampleObject(
                                name = "Example response for API key creation",
                                summary = "Example response when key created successfully",
                                value = EX_POST_APIKEY_RESP
                            ),
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_400_BAD_REQ,
                description = HTTP_400_ERR_MSG_POST,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_404_NOT_FOUND,
                description = HTTP_404_ERR_MSG_POST,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_401_UNAUTHORIZED,
                description = HTTP_401_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            )
        ]
    )
    fun post(
        @HttpTrigger(
            name = "postApiKey",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/public-keys"
        ) @RequestBody(
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = String::class),
                    examples = [
                        ExampleObject(
                            name = "Example request body for API key creation",
                            summary = "Example request body for API key creation",
                            value = EX_POST_APIKEY_REQUEST_BODY
                        ),
                    ]
                )
            ]
        )
        request: HttpRequestMessage<String?>,
        @Parameter(
            name = PARAM_NAME_ORGNAME,
            description = "the organization where a key is to be created."
        )
        @PathParam(PARAM_NAME_ORGNAME)
        @BindingName(PARAM_NAME_ORGNAME) orgName: String,
        @Parameter(
            name = PARAM_NAME_SCOPE,
            description = PARAM_DESC_SCOPE_POST,
        )
        @QueryParam(PARAM_NAME_SCOPE)
        @BindingName(PARAM_NAME_SCOPE) scopeStr: String? = null,
        @Parameter(
            name = PARAM_NAME_KID,
            description = PARAM_DESC_KID_POST,
        )
        @QueryParam(PARAM_NAME_KID)
        @BindingName(PARAM_NAME_KID) kidStr: String? = null
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
            val scope = scopeStr ?: request.queryParameters[PARAM_NAME_SCOPE]
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
            val kid = kidStr
                ?: request.queryParameters[PARAM_NAME_KID]
                ?: return HttpUtilities.bad(request, "kid must be provided")

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
     * @param organizationName - The org name that the key should be removed from
     * @param scope - The scope that the key should be removed from
     * @param kid - the kid for the key to be removed
     *
     */

    @FunctionName("deleteApiKey")
    @Operation(
        summary = "Delete API key",
        description = "Delete API key given organization name, scope, and kid",
        tags = [KEY_MGMT_TAG],
        security = [
            SecurityRequirement(
                name = "primeSecurity"
            )
        ],
        responses = [
            ApiResponse(
                responseCode = HTTP_401_UNAUTHORIZED,
                description = HTTP_401_ERR_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_400_BAD_REQ,
                description = HTTP_400_ERR_MSG_DELETE,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_404_NOT_FOUND,
                description = HTTP_404_ERR_MSG_DELETE,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = String::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = HTTP_200_OK,
                description = HTTP_200_DELETE_MSG,
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = HttpResponseMessage::class),
                        examples = [
                            ExampleObject(
                                name = "Example response for API key deletion",
                                summary = "Example response when key deleted successfully",
                                value = EX_DELETE_APIKEY_RESP
                            ),
                        ]
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
            name = PARAM_NAME_ORGNAME,
            description = PARAM_DESC_ORGNAME_DEL
        )
        @PathParam(PARAM_NAME_ORGNAME) @BindingName(PARAM_NAME_ORGNAME) orgName: String,
        @Parameter(
            name = PARAM_NAME_SCOPE,
            description = PARAM_DESC_SCOPE_DEL
        )
        @PathParam(PARAM_NAME_SCOPE) @BindingName(PARAM_NAME_SCOPE) scope: String,
        @Parameter(
            name = PARAM_NAME_KID,
            description = PARAM_DESC_KID_DEL
        )
        @PathParam(PARAM_NAME_KID) @BindingName(PARAM_NAME_KID) kid: String
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