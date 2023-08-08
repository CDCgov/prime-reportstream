package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilters
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.TranslatorConfiguration
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.JwkSet
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.JSONB
import java.time.OffsetDateTime

/**
 * Settings for Organization, Receivers, and Senders from the Azure Database.
 * Contains all business logic regarding settings as well as JSON serialization.
 */
class SettingsFacade(
    private val metadata: Metadata,
    private val db: DatabaseAccess = DatabaseAccess()
) : SettingsProvider, Logging {
    enum class AccessResult {
        SUCCESS,
        CREATED,
        NOT_FOUND,
        BAD_REQUEST
    }

    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override val organizations: Collection<Organization>
        get() = findSettings(OrganizationAPI::class.java)

    override val senders: Collection<Sender>
        get() = findSettings(Sender::class.java)

    override val receivers: Collection<Receiver>
        get() = findSettings(ReceiverAPI::class.java)

    override fun findOrganization(name: String): Organization? {
        return findSetting(name, OrganizationAPI::class.java)
    }

    override fun findReceiver(fullName: String): Receiver? {
        try {
            val pair = Receiver.parseFullName(fullName)
            return findSetting(pair.second, ReceiverAPI::class.java, pair.first)
        } catch (e: RuntimeException) {
            logger.warn("Cannot find receiver: ${e.localizedMessage} ${e.stackTraceToString()}")
            return null
        }
    }

    override fun findSender(fullName: String): Sender? {
        try {
            val pair = Sender.parseFullName(fullName)
            return findSetting(pair.second, Sender::class.java, pair.first)
        } catch (e: RuntimeException) {
            logger.warn("Cannot find sender: ${e.localizedMessage} ${e.stackTraceToString()}")
            return null
        }
    }

    override fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>? {
        return findOrganizationAndReceiver(fullName, null)
    }

    fun <T : SettingAPI> findSettingAsJson(
        name: String,
        clazz: Class<T>,
        organizationName: String? = null,
    ): String? {
        val result = findSetting(name, clazz, organizationName) ?: return null
        return mapper.writeValueAsString(result)
    }

    fun getLastModified(): OffsetDateTime? {
        return db.fetchLastModified()
    }

    private fun <T : SettingAPI> findSetting(
        name: String,
        clazz: Class<T>,
        organizationName: String? = null
    ): T? {
        val setting = db.transactReturning { txn ->
            val settingType = settingTypeFromClass(clazz.name)
            // When getting the organization setting (settingType == Organization), organizationName has to be null
            // and name has to be the organizationName, due to how the database and query is structured. An Organization
            // cannot have a parent, whereas Senders and Receivers do have a parent (their org)
            if (organizationName != null && settingType != SettingType.ORGANIZATION)
                db.fetchSetting(settingType, name, organizationName, txn)
            else
                db.fetchSetting(settingType, name, parentId = null, txn)
        } ?: return null
        val result = mapper.readValue(setting.values.data(), clazz)
        // Add the metadata
        result.createdAt = setting.createdAt
        result.createdBy = setting.createdBy
        result.version = setting.version
        return result
    }

    fun <T : SettingAPI> findSettingsAsJson(clazz: Class<T>): String {
        val list = findSettings(clazz)
        return mapper.writeValueAsString(list)
    }

    private fun <T : SettingAPI> findSettings(clazz: Class<T>): List<T> {
        val settingType = settingTypeFromClass(clazz.name)
        val settings = db.transactReturning { txn ->
            db.fetchSettings(settingType, txn)
        }
        return settings.map {
            val result = mapper.readValue(it.values.data(), clazz)
            result
        }
    }

    fun <T : SettingAPI> findSettingsAsJson(organizationName: String, clazz: Class<T>): Pair<AccessResult, String> {
        val (result, settings, errorMessage) = db.transactReturning { txn ->
            val organization = db.fetchSetting(SettingType.ORGANIZATION, organizationName, null, txn)
                ?: return@transactReturning Triple(
                    AccessResult.NOT_FOUND, emptyList(), errorJson("Organization not found")
                )
            val settingType = settingTypeFromClass(clazz.name)
            val settings = db.fetchSettings(settingType, organization.settingId, txn)
            Triple(AccessResult.SUCCESS, settings, "")
        }
        return if (result == AccessResult.SUCCESS) {
            val settingsWithMeta = settings.map {
                val setting = mapper.readValue(it.values.data(), clazz)
                setting
            }
            val json = mapper.writeValueAsString(settingsWithMeta)
            Pair(result, json)
        } else {
            Pair(result, errorMessage)
        }
    }

    /**
     * Queries for the settings history for an org based on the type (RECEIVER, SENDER, ORG)
     * The whole history (incl inactive and deleted) returned across all names for a given type.
     * @param organizationName Restrict query to this org
     * @param settingsType Type of setting. column in db and used to select
     * @return json result serialized to a string
     */
    fun findSettingHistoryAsJson(organizationName: String, settingsType: SettingType): String {
        val settings = db.transactReturning { txn ->
            db.fetchSettingRevisionHistory(organizationName, settingsType, txn)
        }
        return mapper.writeValueAsString(settings)
    }

    fun findOrganizationAndReceiver(fullName: String, txn: DataAccessTransaction?): Pair<Organization, Receiver>? {
        val (organizationName, receiverName) = Receiver.parseFullName(fullName)
        val (organizationSetting, receiverSetting) = db.fetchOrganizationAndSetting(
            SettingType.RECEIVER, receiverName, organizationName, txn
        ) ?: return null
        val receiver = mapper.readValue(receiverSetting.values.data(), ReceiverAPI::class.java)
        val organization = mapper.readValue(organizationSetting.values.data(), OrganizationAPI::class.java)
        return Pair(organization, receiver)
    }

    fun <T : SettingAPI> putSetting(
        name: String,
        json: String,
        claims: AuthenticatedClaims,
        clazz: Class<T>,
        organizationName: String? = null
    ): Pair<AccessResult, String> {
        return db.transactReturning { txn ->
            // Check that the orgName is valid (or null)
            val organizationId = organizationName?.let {
                val organization = db.fetchSetting(SettingType.ORGANIZATION, organizationName, null, txn)
                    ?: return@transactReturning Pair(AccessResult.BAD_REQUEST, errorJson("No organization match"))
                organization.settingId
            }
            // Check the payload
            val (valid, error, normalizedJson) = validateAndNormalize(json, clazz, name, organizationName)
            if (!valid)
                return@transactReturning Pair(AccessResult.BAD_REQUEST, errorJson(error ?: "validation error"))
            if (normalizedJson == null) error("Internal Error: validation error")

            // Find the current setting to see if this is a create or an update operation
            val settingType = settingTypeFromClass(clazz.name)
            val current = db.fetchSetting(settingType, name, organizationId, txn)
            val currentVersion = current?.version ?: db.findSettingVersion(settingType, name, organizationId, txn)

            // Form the new setting
            val setting = Setting(
                null, settingType, name, organizationId,
                normalizedJson, false, true,
                currentVersion + 1, claims.userName, OffsetDateTime.now()
            )

            // Now insert
            val accessResult = when {
                current == null -> {
                    // No existing setting, just add to the new setting to the table
                    db.insertSetting(setting, txn)
                    AccessResult.CREATED
                }
                current.values == normalizedJson -> {
                    // Don't create a new version if the payload matches the current version
                    AccessResult.SUCCESS
                }
                else -> {
                    // Update existing setting by deactivate the current setting and inserting a new version
                    db.deactivateSetting(current.settingId, txn)
                    val newId = db.insertSetting(setting, txn)
                    // If inserting an org, update all children settings to point to the new org
                    if (settingType == SettingType.ORGANIZATION)
                        db.updateOrganizationId(current.settingId, newId, txn)
                    AccessResult.SUCCESS
                }
            }

            val settingResult = mapper.readValue(setting.values.data(), clazz)
            if (settingResult is SettingAPI) {
                settingResult.version = setting.version
                settingResult.createdAt = setting.createdAt
                settingResult.createdBy = setting.createdBy
            }

            val outputJson = mapper.writeValueAsString(settingResult)
            Pair(accessResult, outputJson)
        }
    }

    /**
     * Make sure the input json is valid, consistent and normalized
     */
    private fun <T : SettingAPI> validateAndNormalize(
        json: String,
        clazz: Class<T>,
        name: String,
        organizationName: String? = null,
    ): Triple<Boolean, String?, JSONB?> {
        val input = try {
            mapper.readValue(json, clazz)
        } catch (ex: Exception) {
            return Triple(false, "Could not parse JSON payload", null)
        }
        if (input.name != name)
            return Triple(false, "Payload and path name do not match", null)
        if (input.organizationName != organizationName)
            return Triple(false, "Payload and path organization name do not match", null)
        input.consistencyErrorMessage(metadata)?.let { return Triple(false, it, null) }
        val normalizedJson = JSONB.valueOf(mapper.writeValueAsString(input))
        return Triple(true, null, normalizedJson)
    }

    fun <T : SettingAPI> deleteSetting(
        name: String,
        claims: AuthenticatedClaims,
        clazz: Class<T>,
        organizationName: String? = null
    ): Pair<AccessResult, String> {
        return db.transactReturning { txn ->
            val settingType = settingTypeFromClass(clazz.name)
            val current = if (organizationName != null)
                db.fetchSetting(settingType, name, organizationName, txn)
            else
                db.fetchSetting(settingType, name, parentId = null, txn)
            if (current == null) return@transactReturning Pair(AccessResult.NOT_FOUND, errorJson("Item not found"))

            db.insertDeletedSettingAndChildren(current.settingId, claims.userName, OffsetDateTime.now(), txn)
            db.deactivateSettingAndChildren(current.settingId, txn)
            // returned content-type is json/application, so return empty json not empty string
            Pair(AccessResult.SUCCESS, "{}")
        }
    }

    companion object {
        // The SettingAccess is heavy-weight object (because it contains a Jackson Mapper) so reuse it when possible
        val common: SettingsFacade by lazy {
            SettingsFacade(Metadata.getInstance(), DatabaseAccess())
        }

        private fun settingTypeFromClass(className: String): SettingType {
            return when (className) {
                OrganizationAPI::class.qualifiedName -> SettingType.ORGANIZATION
                ReceiverAPI::class.qualifiedName -> SettingType.RECEIVER
                Sender::class.qualifiedName -> SettingType.SENDER
                else -> error("Internal Error: Unknown classname: $className")
            }
        }

        private fun errorJson(message: String): String {
            return """{"error": "$message"}"""
        }
    }
}

interface SettingAPI {
    val name: String
    val organizationName: String?
    var version: Int?
    var createdBy: String?
    var createdAt: OffsetDateTime?
    fun consistencyErrorMessage(metadata: Metadata): String?
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class OrganizationAPI
@JsonCreator constructor(
    name: String,
    description: String,
    jurisdiction: Jurisdiction,
    stateCode: String?,
    countyName: String?,
    filters: List<ReportStreamFilters>?,
    featureFlags: List<String>?,
    keys: List<JwkSet>?,
    override var version: Int? = null,
    override var createdBy: String? = null,
    override var createdAt: OffsetDateTime? = null,
) : Organization(
    name,
    description,
    jurisdiction,
    stateCode.trimToNull(),
    countyName.trimToNull(),
    filters,
    featureFlags,
    keys
),

    SettingAPI {
    @get:JsonIgnore
    override val organizationName: String? = null
    override fun consistencyErrorMessage(metadata: Metadata): String? {
        return this.consistencyErrorMessage()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ReceiverAPI
@JsonCreator constructor(
    name: String,
    organizationName: String,
    topic: Topic,
    customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
    translation: TranslatorConfiguration,
    jurisdictionalFilter: ReportStreamFilter = emptyList(),
    qualityFilter: ReportStreamFilter = emptyList(),
    routingFilter: ReportStreamFilter = emptyList(),
    processingModeFilter: ReportStreamFilter = emptyList(),
    reverseTheQualityFilter: Boolean = false,
    conditionalFilter: ReportStreamFilter = emptyList(),
    deidentify: Boolean = false,
    deidentifiedValue: String = "",
    timing: Timing? = null,
    description: String = "",
    transport: TransportType? = null,
    override var version: Int? = null,
    override var createdBy: String? = null,
    override var createdAt: OffsetDateTime? = null,
) : Receiver(
    name,
    organizationName,
    topic,
    customerStatus,
    translation,
    jurisdictionalFilter,
    qualityFilter,
    routingFilter,
    processingModeFilter,
    reverseTheQualityFilter,
    conditionalFilter,
    deidentify,
    deidentifiedValue,
    timing,
    description,
    transport
),
    SettingAPI