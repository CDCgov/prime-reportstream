package gov.cdc.prime.router.api.azure

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.prime.router.api.ApiResponse
import gov.cdc.prime.router.api.HttpUtilities
import gov.cdc.prime.router.api.MetaApiResponse
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.settings.db.SettingsFacade
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.JwkSet
import gov.cdc.prime.router.tokens.authenticationFailure
import org.apache.logging.log4j.kotlin.Logging

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

abstract class ApiKeysFunctionsBase(private val settingsFacade: SettingsFacade = SettingsFacade.common) : Logging {
    data class ApiKeysResponse(val orgName: String, val keys: List<JwkSet>)

    /**
     * Reusable function for fetching API keys.  The useNewApiResponse dictates whether or not to use the consistent
     * API response format
     */
    protected fun getApiKeysForOrg(
        request: HttpRequestMessage<String?>,
        orgName: String,
        useNewApiResponse: Boolean = false,
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
            HttpUtilities.okJSONResponse(
                request,
                ApiResponse(keys, MetaApiResponse("PublicKey", keys.size))
            )
        } else {
            HttpUtilities.okJSONResponse(request, ApiKeysResponse(orgName, keys))
        }
    }
}