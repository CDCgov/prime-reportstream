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
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticatedClaims.Companion.authenticateAdmin
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging

/*
 * Settings API
 */
class SettingsFunction(settingsFacade: SettingsFacade = SettingsFacade.common) : BaseFunction(settingsFacade) {
    /**
     * Get a full list of organizations and their settings.
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @return HttpResponseMessage List of all organizations and settings
     */
    @FunctionName("getOrganizations")
    fun getOrganizations(
        @HttpTrigger(
            name = "getOrganizations",
            methods = [HttpMethod.GET, HttpMethod.HEAD],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage = when (request.httpMethod) {
            HttpMethod.HEAD -> getHead(request)
            HttpMethod.GET -> getList(request, OrganizationAPI::class.java)
            else -> error("Unsupported method")
        }

    /**
     * Get settings for the given organization.
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param organizationName Organization to get settings for
     * @return HttpResponseMessage List of settings for the organization
     */
    @FunctionName("getOneOrganization")
    fun getOneOrganization(
        @HttpTrigger(
            name = "getOneOrganization",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage = getOne(request, organizationName, OrganizationAPI::class.java, organizationName)

    /**
     * Update settings for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     *      Expected PUT Body: JSON (see OrganizationAPI for structure)
     * @param organizationName Organization to update settings for
     * @return HttpResponseMessage Result of update attempt and new value if successful
     */
    @FunctionName("updateOneOrganization")
    fun updateOneOrganization(
        @HttpTrigger(
            name = "updateOneOrganization",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage = updateOne(
            request,
            organizationName,
            OrganizationAPI::class.java
        )

    /**
     * Get senders for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param organizationName Organization to get senders for
     * @return HttpResponseMessage List of senders for the organization
     */
    @FunctionName("getSenders")
    fun getSenders(
        @HttpTrigger(
            name = "getSenders",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage = getList(request, Sender::class.java, organizationName)

    /**
     * Get a single sender for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param organizationName Organization in which to look for the sender
     * @param senderName Name of the sender we're looking for
     * @return HttpResponseMessage Sender data if found
     */
    @FunctionName("getOneSender")
    fun getOneSender(
        @HttpTrigger(
            name = "getOneSender",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders/{senderName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("senderName") senderName: String,
    ): HttpResponseMessage = getOne(request, senderName, Sender::class.java, organizationName)

    /**
     * Update a single sender for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     *      Expected PUT Body: JSON (see OrganizationAPI for structure)
     * @param organizationName Organization in which to look for the sender
     * @param senderName Name of the sender we're updating
     * @return HttpResponseMessage Result of update attempt and new value if successful
     */
    @FunctionName("updateOneSender")
    fun updateOneSender(
        @HttpTrigger(
            name = "updateOneSender",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders/{senderName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("senderName") senderName: String,
    ): HttpResponseMessage = updateOne(
            request,
            senderName,
            Sender::class.java,
            organizationName
        )

    /**
     * Get receiver for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     *      Expected PUT Body: JSON (see OrganizationAPI for structure)
     * @param organizationName Organization to get receivers for
     * @return HttpResponseMessage List of receivers for the organization
     */
    @FunctionName("getReceivers")
    fun getReceivers(
        @HttpTrigger(
            name = "getReceivers",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage = getList(request, ReceiverAPI::class.java, organizationName)

    /**
     * Get a single receiver for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param organizationName Organization in which to look for the receiver
     * @param receiverName Name of the receiver we're looking for
     * @return HttpResponseMessage Receiver if found
     */
    @FunctionName("getOneReceiver")
    fun getOneReceiver(
        @HttpTrigger(
            name = "getOneReceiver",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers/{receiverName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage = getOne(request, receiverName, ReceiverAPI::class.java, organizationName)

    /**
     * Update one receiver for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     *      Expected PUT Body: JSON (see OrganizationAPI for structure)
     * @param organizationName Organization in which to look for the receiver
     * @param receiverName Name of the receiver we're updating
     * @return HttpResponseMessage Result of update attempt and new value if successful
     */
    @FunctionName("updateOneReceiver")
    fun updateOneReceiver(
        @HttpTrigger(
            name = "updateOneReceiver",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers/{receiverName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage = updateOne(
            request,
            receiverName,
            ReceiverAPI::class.java,
            organizationName
        )

    /**
     * Get a history of revisions for an organization's settings (by type).
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
     * @param organizationName Organization in which to look for the receiver
     * @param settingSelector Name of setting type we're looking for. See SettingType for options
     * @return HttpResponseMessage List of settings changes based on the parameters
     */
    @FunctionName("getSettingRevisionHistory")
    fun getSettingRevisionHistory(
        @HttpTrigger(
            name = "getSettingRevisionHistory",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "waters/org/{organizationName}/settings/revs/{settingSelector}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("settingSelector") settingSelector: String,
    ): HttpResponseMessage = try {
            // verify the settingsTypeString is in the allowed setting enumeration.
            val settingType = SettingType.valueOf(settingSelector.uppercase())
            getListHistory(request, organizationName, settingType)
        } catch (e: EnumConstantNotPresentException) {
            HttpUtilities.badRequestResponse(request, "Invalid setting selector parameter")
        }
}

/**
 * Common Settings API
 */
open class BaseFunction(private val facade: SettingsFacade) : Logging {
    /**
     * Gets the list of settings for a given organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param clazz The class used to convert to Json
     * @param organizationName Organization to get settings for
     * @return HttpResponseMessage List of settings for the organization
     */
    fun <T : SettingAPI> getList(
        request: HttpRequestMessage<String?>,
        clazz: Class<T>,
        organizationName: String? = null,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null ||
            !claims.authorized(setOf(PRIME_ADMIN_PATTERN, "$organizationName.*.admin", "$organizationName.*.user"))
        ) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
        }
        return if (organizationName != null) {
            val (result, outputBody) = facade.findSettingsAsJson(organizationName, clazz)
            facadeResultToResponse(request, result, outputBody)
        } else {
            val settings = facade.findSettingsAsJson(clazz)
            val lastModified = facade.getLastModified()
            HttpUtilities.okResponse(request, settings, lastModified)
        }
    }

    /**
     * Returns all revisions of a settings (version) for an org/settingName
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param organizationName Org name to auth again and use for query
     * @param settingType Name of setting type we're looking for
     * @result HttpResponseMessage History of revisions based on the parameters
     */
    fun getListHistory(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        settingType: SettingType,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null ||
            !claims.authorized(
                setOf(PRIME_ADMIN_PATTERN, "$organizationName.*.admin", "$organizationName.*.user")
            )
        ) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
        }
        val settings = facade.findSettingHistoryAsJson(organizationName, settingType)
        return HttpUtilities.okResponse(request, settings, facade.getLastModified())
    }

    /**
     * Get header data
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @return HttpResponseMessage Last modified date for the settings
     */
    fun getHead(
        request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        authenticateAdmin(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)
        val lastModified = facade.getLastModified()
        return HttpUtilities.okResponse(request, lastModified = lastModified)
    }

    /**
     * Return a single setting
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param settingName Name column in Setting table to match
     * @param clazz The class used to convert to Json
     * @return HttpResponseMessage Setting data if found
     */
    fun <T : SettingAPI> getOne(
        request: HttpRequestMessage<String?>,
        settingName: String,
        clazz: Class<T>,
        organizationName: String? = null,
    ): HttpResponseMessage {
        val claims = AuthenticatedClaims.authenticate(request)
        if (claims == null ||
            !claims.authorized(
                setOf(PRIME_ADMIN_PATTERN, "$organizationName.*.admin", "$organizationName.*.user")
            )
        ) {
            logger.warn("User '${claims?.userName}' FAILED authorized for endpoint ${request.uri}")
            return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
        }
        val setting = facade.findSettingAsJson(settingName, clazz, organizationName)
        return if (setting == null) {
            HttpUtilities.notFoundResponse(request)
        } else {
            HttpUtilities.okResponse(request, setting)
        }
    }

    /**
     * Update a single setting for an organization
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     *      Expected PUT Body: JSON (see OrganizationAPI for structure)
     * @param settingName Name of the setting being updated
     * @param clazz The SettingType used to structure the response JSON
     * @param organizationName Name of the organization we are looking for the setting in
     * @return HttpResponseMessage Result of update attempt and new value if successful
     */
    fun <T : SettingAPI> updateOne(
        request: HttpRequestMessage<String?>,
        settingName: String,
        clazz: Class<T>,
        organizationName: String? = null,
    ): HttpResponseMessage {
        val claims = authenticateAdmin(request)
            ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

        val (result, outputBody) = when (request.httpMethod) {
            HttpMethod.PUT -> {
                if (request.headers[HttpHeaders.CONTENT_TYPE.lowercase()] != HttpUtilities.jsonMediaType) {
                    return HttpUtilities.badRequestResponse(request, errorJson("invalid media type"))
                }
                val body = request.body
                    ?: return HttpUtilities.badRequestResponse(request, errorJson("missing payload"))
                facade.putSetting(settingName, body, claims, clazz, organizationName)
            }

            HttpMethod.DELETE ->
                facade.deleteSetting(settingName, claims, clazz, organizationName)

            else ->
                return HttpUtilities.badRequestResponse(request, errorJson("unsupported method"))
        }
        return facadeResultToResponse(request, result, outputBody)
    }

    /**
     * Convert the output of the facade into a Http response
     * @param request Incoming http request
     *      Expected Headers: authorization, content-type
     * @param result Output of the facade
     * @param outputBody Body of the HTTP response
     * @return HttpResponseMessage Facade output converted to a Http response
     */
    private fun facadeResultToResponse(
        request: HttpRequestMessage<String?>,
        result: SettingsFacade.AccessResult,
        outputBody: String,
    ): HttpResponseMessage = when (result) {
            SettingsFacade.AccessResult.SUCCESS -> HttpUtilities.okResponse(request, outputBody)
            SettingsFacade.AccessResult.CREATED -> HttpUtilities.createdResponse(request, outputBody)
            SettingsFacade.AccessResult.NOT_FOUND -> HttpUtilities.notFoundResponse(request)
            SettingsFacade.AccessResult.BAD_REQUEST -> HttpUtilities.badRequestResponse(request, outputBody)
        }

    /**
     * Convert a message to a JSON error
     * @param message Input string
     * @return HTTP error response
     */
    private fun errorJson(message: String): String = HttpUtilities.errorJson(message)
}