package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
import org.apache.logging.log4j.kotlin.Logging

/*
 * Organizations API
 */

class GetOrganizations(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOrganizations")
    fun run(
        @HttpTrigger(
            name = "getOrganizations",
            methods = [HttpMethod.GET, HttpMethod.HEAD],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        return when (request.httpMethod) {
            HttpMethod.HEAD -> getHead(request)
            HttpMethod.GET -> getList(request, OrganizationAPI::class.java)
            else -> error("Unsupported method")
        }
    }
}

class GetOneOrganization(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication()
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOneOrganization")
    fun run(
        @HttpTrigger(
            name = "getOneOrganization",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        // Counter-intuitive:  this fails if you pass the organizationName as the organizationName. ;)
        return getOne(request, organizationName, OrganizationAPI::class.java, null)
    }
}

class UpdateOrganization(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("updateOneOrganization")
    fun run(
        @HttpTrigger(
            name = "updateOneOrganization",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return updateOne(
            request,
            organizationName,
            OrganizationAPI::class.java
        )
    }
}

/**
 * Sender APIs
 */
class GetSenders(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication()
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getSenders")
    fun run(
        @HttpTrigger(
            name = "getSenders",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return getList(request, organizationName, Sender::class.java)
    }
}

class GetOneSender(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication()
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOneSender")
    fun run(
        @HttpTrigger(
            name = "getOneSender",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders/{senderName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("senderName") senderName: String,
    ): HttpResponseMessage {
        return getOne(request, senderName, Sender::class.java, organizationName)
    }
}

class UpdateSender(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.ORGANIZATION_ADMIN)
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("updateOneSender")
    fun run(
        @HttpTrigger(
            name = "updateOneSender",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders/{senderName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("senderName") senderName: String,
    ): HttpResponseMessage {
        return updateOne(
            request,
            senderName,
            Sender::class.java,
            organizationName
        )
    }
}

/**
 * Receiver APIS
 */

class GetReceiver(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication()
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getReceivers")
    fun run(
        @HttpTrigger(
            name = "getReceivers",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return getList(request, organizationName, ReceiverAPI::class.java)
    }
}

class GetOneReceiver(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication()
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOneReceiver")
    fun run(
        @HttpTrigger(
            name = "getOneReceiver",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers/{receiverName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage {
        return getOne(request, receiverName, ReceiverAPI::class.java, organizationName)
    }
}

class UpdateReceiver(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.ORGANIZATION_ADMIN)
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("updateOneReceiver")
    fun run(
        @HttpTrigger(
            name = "updateOneReceiver",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers/{receiverName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage {
        return updateOne(
            request,
            receiverName,
            ReceiverAPI::class.java,
            organizationName
        )
    }
}

/**
 * Get a history of revisions for an Org's settings (by type).
 * It includes all the Setting data for the full history to enable
 * quick client diffs across revisions.
 *
 * Type returned depends on the request settingSelector parameter.
 * ALL named settings are return and the caller must group accordingly.
 * Return ALL names solves the problem where knowing a deleted name become impossible
 *
 * From the OpenAPI view, this is just 3 different api calls
 *   `settings/revision/organizations/{organizationName}/sender`
 *   `settings/revision/organizations/{organizationName}/receiver`
 *   `settings/revision/organizations/{organizationName}/organization`

 *   @param settingsFacade Same pattern as the rest of the funs in this module
 *   @param oktaAuthentication Default to require org admin, caller can override
 *   @return Spring HttpTrigger call
 */
class GetSettingRevisionHistory(
    settingsFacade: SettingsFacade = SettingsFacade.common,
    oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.ORGANIZATION_ADMIN)
) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getSettingRevisionHistory")
    fun run(
        @HttpTrigger(
            name = "getSettingRevisionHistory",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/org/{organizationName}/settings/revs/{settingSelector}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("settingSelector") settingSelector: String
    ): HttpResponseMessage {
        try {
            // verify the settingsTypeString is in the allowed setting enumeration.
            val settingType = SettingType.valueOf(settingSelector.uppercase())
            return getListHistory(request, organizationName, settingType)
        } catch (e: EnumConstantNotPresentException) {
            return HttpUtilities.badRequestResponse(request, "Invalid setting selector parameter")
        }
    }
}

/**
 * Common Settings API
 */

open class BaseFunction(
    private val facade: SettingsFacade,
    private val oktaAuthentication: OktaAuthentication
) : Logging {
    private val missingAuthorizationHeader = HttpUtilities.errorJson("Missing Authorization Header")
    private val invalidClaim = HttpUtilities.errorJson("Invalid Authorization Header")

    fun <T : SettingAPI> getList(
        request: HttpRequestMessage<String?>,
        clazz: Class<T>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            val settings = facade.findSettingsAsJson(clazz)
            val lastModified = facade.getLastModified()
            HttpUtilities.okResponse(request, settings, lastModified)
        }
    }

    fun <T : SettingAPI> getList(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        clazz: Class<T>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organizationName) {
            val (result, outputBody) = facade.findSettingsAsJson(organizationName, clazz)
            facadeResultToResponse(request, result, outputBody)
        }
    }

    /**
     * Returns all revisions of a settings (version) for an org/settingName
     * Handles Auth
     * @param request Incoming http request
     * @param organizationName Org name to auth again and use for query
     * @param settingType SettingType
     * @result HttpResponseMessage resulting json or HTTP error response
     */
    fun getListHistory(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        settingType: SettingType
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organizationName) {
            val settings = facade.findSettingHistoryAsJson(organizationName, settingType)
            HttpUtilities.okResponse(request, settings, facade.getLastModified())
        }
    }

    fun getHead(
        request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            val lastModified = facade.getLastModified()
            HttpUtilities.okResponse(request, lastModified = lastModified)
        }
    }

    /**
     * Return a single setting. Separated from http request for testability reasons
     * @param request Http request
     * @param settingName Name column in Setting table to match
     * @param clazz The class used to convert to Json
     * @param organizationName Mapped to organzationId then used to select the organization_id column
     * @return HttpResponseMessage The resulting json or HTTP error response
     */
    fun <T : SettingAPI> getOne(
        request: HttpRequestMessage<String?>,
        settingName: String,
        clazz: Class<T>,
        organizationName: String? = null,
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organizationName ?: settingName) {
            val setting = facade.findSettingAsJson(settingName, clazz, organizationName)
                ?: return@checkAccess HttpUtilities.notFoundResponse(request)
            HttpUtilities.okResponse(request, setting)
        }
    }

    fun <T : SettingAPI> updateOne(
        request: HttpRequestMessage<String?>,
        settingName: String,
        clazz: Class<T>,
        organizationName: String? = null
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organizationName ?: settingName) { claims ->
            val (result, outputBody) = when (request.httpMethod) {
                HttpMethod.PUT -> {
                    if (request.headers[HttpHeaders.CONTENT_TYPE.lowercase()] != HttpUtilities.jsonMediaType)
                        return@checkAccess HttpUtilities.badRequestResponse(request, errorJson("invalid media type"))
                    val body = request.body
                        ?: return@checkAccess HttpUtilities.badRequestResponse(request, errorJson("missing payload"))
                    facade.putSetting(settingName, body, claims, clazz, organizationName)
                }
                HttpMethod.DELETE ->
                    facade.deleteSetting(settingName, claims, clazz, organizationName)
                else ->
                    return@checkAccess HttpUtilities.badRequestResponse(request, errorJson("unsupported method"))
            }
            facadeResultToResponse(request, result, outputBody)
        }
    }

    private fun facadeResultToResponse(
        request: HttpRequestMessage<String?>,
        result: SettingsFacade.AccessResult,
        outputBody: String
    ): HttpResponseMessage {
        return when (result) {
            SettingsFacade.AccessResult.SUCCESS -> HttpUtilities.okResponse(request, outputBody)
            SettingsFacade.AccessResult.CREATED -> HttpUtilities.createdResponse(request, outputBody)
            SettingsFacade.AccessResult.NOT_FOUND -> HttpUtilities.notFoundResponse(request)
            SettingsFacade.AccessResult.BAD_REQUEST -> HttpUtilities.badRequestResponse(request, outputBody)
        }
    }

    private fun errorJson(message: String): String = HttpUtilities.errorJson(message)
}