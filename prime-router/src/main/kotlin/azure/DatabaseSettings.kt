package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.db.enums.SettingType

/**
 * Settings for Organization, Receivers, and Senders from the Azure Database
 */
class DatabaseSettings(
    val databaseAccess: DatabaseAccess = DatabaseAccess(DatabaseAccess.dataSource)
) : SettingsProvider {
    private val mapper = jacksonObjectMapper()

    override val organizations: Collection<Organization>
        get() = findSettings(SettingType.organization, Organization::class.java)
    override val senders: Collection<Sender>
        get() = findSettings(SettingType.sender, Sender::class.java)
    override val receivers: Collection<Receiver>
        get() = findSettings(SettingType.receiver, Receiver::class.java)

    override fun findOrganization(name: String): Organization? {
        return findSetting(name, name, SettingType.organization, Organization::class.java)
    }

    override fun findReceiver(fullName: String): Receiver? {
        val pair = Receiver.parseFullName(fullName)
        return findSetting(pair.first, pair.second, SettingType.receiver, Receiver::class.java)
    }

    override fun findSender(fullName: String): Sender? {
        val pair = Sender.parseFullName(fullName)
        return findSetting(pair.first, pair.second, SettingType.sender, Sender::class.java)
    }

    private fun <T> findSetting(name: String, organizationName: String, settingType: SettingType, clazz: Class<T>): T? {
        val setting = databaseAccess.transactReturning { txn ->
            databaseAccess.fetchSetting(name, organizationName, settingType, txn)
        } ?: return null
        return mapper.readValue(setting.values.data(), clazz)
    }

    private fun <T> findSettings(settingType: SettingType, clazz: Class<T>): List<T> {
        val settings = databaseAccess.transactReturning { txn ->
            databaseAccess.fetchSettings(settingType, txn)
        }
        return settings.map {
            mapper.readValue(it.values.data(), clazz)
        }
    }
}