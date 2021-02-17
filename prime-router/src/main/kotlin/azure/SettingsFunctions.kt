package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import org.apache.logging.log4j.kotlin.Logging

class GetOrganizations(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.USER) {
    @FunctionName("getOrganizations")
    fun run(
        @HttpTrigger(
            name = "getOrganizations",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        return getList(
            request,
            APIOrganization::class.java
        )
    }
}

class GetOneOrganization(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.USER) {
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
        return getOne(
            request,
            organizationName,
            organizationName,
            APIOrganization::class.java
        )
    }
}

class UpdateOrganization(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.SYSTEM_ADMIN) {
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
            organizationName,
            APIOrganization::class.java
        )
    }
}

/**
 * Sender APIs
 */
class GetSenders(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.USER) {
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
        return getList(request, organizationName, APISender::class.java)
    }
}

class GetOneSender(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.USER) {
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
        return getOne(request, organizationName, senderName, APISender::class.java)
    }
}

class UpdateSender(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.ORGANIZATION_ADMIN) {
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
            organizationName,
            senderName,
            APISender::class.java
        )
    }
}

/**
 * Receiver APIS
 */

class GetReceiver(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.USER) {
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
        return getList(request, organizationName, APIReceiver::class.java)
    }
}

class GetOneReceiver(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.USER) {
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
        return getOne(request, organizationName, receiverName, APIReceiver::class.java)
    }
}

class UpdateReceiver(settingsAccess: SettingsAccess = SettingsAccess.singleton) :
    BaseFunction(settingsAccess, minimumLevel = PrincipalLevel.ORGANIZATION_ADMIN) {
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
            organizationName,
            receiverName,
            APIReceiver::class.java
        )
    }
}

/**
 * Common Settings API
 */

open class BaseFunction(
    private val facade: SettingsAccess,
    private val minimumLevel: PrincipalLevel
) : Logging {

    private val verifier: AuthenticationVerifier = TestAuthenticationVerifier()

    fun <T : APISetting> getList(
        request: HttpRequestMessage<String?>,
        clazz: Class<T>
    ): HttpResponseMessage {
        return handleRequest(request, "") {
            val settings = facade.findSettingsAsJson(clazz)
            HttpUtilities.okResponse(request, settings)
        }
    }

    fun <T : APISetting> getList(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        clazz: Class<T>
    ): HttpResponseMessage {
        return handleRequest(request, "") {
            val settings = facade.findSettingsAsJson(organizationName, clazz)
            HttpUtilities.okResponse(request, settings)
        }
    }

    fun <T : APISetting> getOne(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        settingName: String,
        clazz: Class<T>
    ): HttpResponseMessage {
        return handleRequest(request, organizationName) {
            val setting = facade.findSettingAsJson(settingName, organizationName, clazz)
                ?: return@handleRequest HttpUtilities.notFoundResponse(request)
            HttpUtilities.okResponse(request, setting)
        }
    }

    fun <T : APISetting> updateOne(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        settingName: String,
        clazz: Class<T>
    ): HttpResponseMessage {
        return handleRequest(request, organizationName) { claims ->
            val (result, outputBody) = when (request.httpMethod) {
                HttpMethod.PUT -> {
                    val body = request.body ?: return@handleRequest HttpUtilities.badRequestResponse(request)
                    facade.putSetting(organizationName, settingName, body, claims, clazz)
                }
                HttpMethod.DELETE -> {
                    facade.deleteSetting(organizationName, settingName, claims, clazz)
                }
                else ->
                    return@handleRequest HttpUtilities.badRequestResponse(request)
            }
            when (result) {
                SettingsAccess.AccessResult.SUCCESS -> HttpUtilities.okResponse(request, outputBody)
                SettingsAccess.AccessResult.CREATED -> HttpUtilities.createdResponse(request, outputBody)
                SettingsAccess.AccessResult.NOT_FOUND -> HttpUtilities.notFoundResponse(request)
                SettingsAccess.AccessResult.BAD_REQUEST -> HttpUtilities.badRequestResponse(request)
            }
        }
    }

    private fun handleRequest(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        block: (claims: AuthenticationClaims) -> HttpResponseMessage
    ): HttpResponseMessage {
        try {
            logger.info("Settings request: ${request.httpMethod}:${request.uri.path}")

            val accessToken = getAccessToken(request)
                ?: return HttpUtilities.unauthorizedResponse(request)
            val claims = verifier.checkClaims(accessToken, minimumLevel, organizationName)
                ?: return HttpUtilities.unauthorizedResponse(request)

            return block(claims)
        } catch (ex: Exception) {
            if (ex.message != null)
                logger.error(ex.message!!, ex)
            else
                logger.error(ex)
            return HttpUtilities.internalErrorResponse(request)
        }
    }

    private fun getAccessToken(request: HttpRequestMessage<String?>): String? {
        // TODO
        return ""
    }
}