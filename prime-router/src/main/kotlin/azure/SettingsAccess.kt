package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TranslatorConfiguration
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import gov.cdc.prime.router.azure.db.tables.pojos.SettingHistory
import org.jooq.JSON
import java.time.OffsetDateTime

/**
 * Settings for Organization, Receivers, and Senders from the Azure Database.
 * Contains all business logic regarding settings.
 */
class SettingsAccess(private val db: DatabaseAccess = DatabaseAccess()) : SettingsProvider {
    enum class AccessResult {
        SUCCESS,
        CREATED,
        NOT_FOUND,
        BAD_REQUEST
    }

    private val mapper = jacksonObjectMapper()

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override val organizations: Collection<Organization>
        get() = findSettings(APIOrganization::class.java)

    override val senders: Collection<Sender>
        get() = findSettings(APISender::class.java)

    override val receivers: Collection<Receiver>
        get() = findSettings(APIReceiver::class.java)

    override fun findOrganization(name: String): Organization? {
        return findSetting(name, name, APIOrganization::class.java)
    }

    override fun findReceiver(fullName: String): Receiver? {
        val pair = Receiver.parseFullName(fullName)
        return findSetting(pair.second, pair.first, APIReceiver::class.java)
    }

    override fun findSender(fullName: String): Sender? {
        val pair = Sender.parseFullName(fullName)
        return findSetting(pair.second, pair.first, APISender::class.java)
    }

    fun <T : APISetting> findSetting(
        organizationName: String,
        name: String,
        clazz: Class<T>
    ): T? {
        val settingType = settingTypeFromClass(clazz.name)
        val setting = db.transactReturning { txn ->
            db.fetchSetting(settingType, organizationName, name, txn)
        } ?: return null
        val result = mapper.readValue(setting.values.data(), clazz)
        result.meta = SettingMetadata(setting.version, setting.createdBy, setting.createdAt)
        return result
    }

    fun <T : APISetting> findSettingAsJson(
        organizationName: String,
        name: String,
        clazz: Class<T>
    ): String? {
        val result = findSetting(organizationName, name, clazz) ?: return null
        return mapper.writeValueAsString(result)
    }

    fun <T : APISetting> findSettings(clazz: Class<T>): List<T> {
        val settingType = settingTypeFromClass(clazz.name)
        val settings = db.transactReturning { txn ->
            db.fetchSettings(settingType, txn)
        }
        return settings.map {
            val result = mapper.readValue(it.values.data(), clazz)
            result.meta = SettingMetadata(it.version, it.createdBy, it.createdAt)
            result
        }
    }

    fun <T : APISetting> findSettingsAsJson(clazz: Class<T>): String {
        val list = findSettings(clazz)
        return mapper.writeValueAsString(list)
    }

    fun <T : APISetting> findSettings(organizationName: String, clazz: Class<T>): List<T> {
        val settingType = settingTypeFromClass(clazz.name)
        val settings = db.transactReturning { txn ->
            db.fetchSettings(settingType, organizationName, txn)
        }
        return settings.map {
            val result = mapper.readValue(it.values.data(), clazz)
            result.meta = SettingMetadata(it.version, it.createdBy, it.createdAt)
            result
        }
    }

    fun <T : APISetting> findSettingsAsJson(
        organizationName: String,
        clazz: Class<T>
    ): String {
        val list = findSettings(organizationName, clazz)
        return mapper.writeValueAsString(list)
    }

    fun <T : APISetting> putSetting(
        organizationName: String,
        settingName: String,
        json: String,
        claims: AuthenticationClaims,
        clazz: Class<T>
    ): Pair<AccessResult, String> {
        val settingType = settingTypeFromClass(clazz.name)
        return db.transactReturning { txn ->
            val version = db.findSettingVersion(settingName, organizationName, settingType, txn) + 1
            val settingMetadata = SettingMetadata(version, claims.userName, OffsetDateTime.now())
            val setting = buildSetting(organizationName, settingName, json, settingMetadata, clazz)
                ?: return@transactReturning Pair(AccessResult.BAD_REQUEST, "")
            val settingHistory = buildSettingHistory(setting, isDeleted = false)
            val accessResult = if (version == 0) {
                db.insertSetting(setting, txn)
                db.insertSettingHistory(settingHistory, txn)
                AccessResult.CREATED
            } else {
                db.updateSetting(setting, txn)
                db.insertSettingHistory(settingHistory, txn)
                AccessResult.SUCCESS
            }
            val outputJson = mapper.writeValueAsString(settingMetadata)
            Pair(accessResult, outputJson)
        }
    }

    private fun <T : APISetting> buildSetting(
        organizationName: String,
        settingName: String,
        json: String,
        settingMetadata: SettingMetadata,
        clazz: Class<T>
    ): Setting? {
        val input = try {
            mapper.readValue(json, clazz)
        } catch (ex: Exception) {
            null
        } ?: return null
        if (input.name != settingName) return null
        if (input.organizationName != organizationName) return null
        return Setting(
            null,
            settingTypeFromClass(clazz.name),
            organizationName,
            settingName,
            JSON.valueOf(json),
            settingMetadata.version,
            settingMetadata.createdBy,
            settingMetadata.createdAt
        )
    }

    private fun buildSettingHistory(setting: Setting, isDeleted: Boolean): SettingHistory {
        return SettingHistory(
            null,
            setting.type,
            setting.organizationName,
            setting.settingName,
            setting.values,
            setting.version,
            setting.createdBy,
            setting.createdAt,
            isDeleted
        )
    }

    fun <T : APISetting> deleteSetting(
        organizationName: String,
        settingName: String,
        claims: AuthenticationClaims,
        clazz: Class<T>
    ): Pair<AccessResult, String> {
        val settingType = settingTypeFromClass(clazz.name)
        return db.transactReturning { txn ->
            val setting = db.fetchSetting(settingType, organizationName, settingName, txn)
                ?: return@transactReturning Pair(AccessResult.NOT_FOUND, "")
            setting.version = setting.version + 1
            val settingHistory = buildSettingHistory(setting, isDeleted = true)
            db.deleteSetting(settingName, organizationName, settingType, txn)
            db.insertSettingHistory(settingHistory, txn)
            val settingMetadata = SettingMetadata(setting.version, claims.userName, OffsetDateTime.now())
            val outputJson = mapper.writeValueAsString(settingMetadata)
            Pair(AccessResult.SUCCESS, outputJson)
        }
    }

    private fun settingTypeFromClass(className: String): SettingType {
        return when (className) {
            "gov.cdc.prime.router.azure.APIOrganization" -> SettingType.ORGANIZATION
            "gov.cdc.prime.router.azure.APIReceiver" -> SettingType.RECEIVER
            "gov.cdc.prime.router.azure.APISender" -> SettingType.SENDER
            else -> error("Internal Error: Unknown classname: $className")
        }
    }

    companion object {
        // The SettingAccess is heavy-weight object (because it contains a Jackson Mapper) so reuse it when possible
        val singleton = SettingsAccess()
    }
}

data class SettingMetadata(
    val version: Int,
    val createdBy: String,
    val createdAt: OffsetDateTime
)

interface APISetting {
    val name: String
    val organizationName: String
    var meta: SettingMetadata?
}

class APIOrganization(
    name: String,
    description: String,
    jurisdiction: Jurisdiction,
    stateCode: String?,
    countyName: String?,
    override var meta: SettingMetadata?,
) : Organization(name, description, jurisdiction, stateCode, countyName), APISetting {
    override val organizationName: String get() = this.name
}

class APISender(
    name: String,
    organizationName: String,
    format: Format,
    topic: String,
    schemaName: String,
    override var meta: SettingMetadata?,
) : Sender(
    name,
    organizationName,
    format,
    topic,
    schemaName,
),
    APISetting

class APIReceiver(
    name: String,
    organizationName: String,
    topic: String,
    translation: TranslatorConfiguration,
    jurisdictionalFilter: List<String> = emptyList(),
    deidentify: Boolean = false,
    timing: Timing? = null,
    description: String = "",
    transport: TransportType? = null,
    override var meta: SettingMetadata?,
) : Receiver(
    name,
    organizationName,
    topic,
    translation,
    jurisdictionalFilter,
    deidentify,
    timing,
    description,
    transport
),
    APISetting