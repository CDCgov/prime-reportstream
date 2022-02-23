package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream

class FileSettings : SettingsProvider {
    private var organizationStore: Map<String, Organization> = mapOf()
    private var receiverStore: Map<String, Receiver> = mapOf()
    private var senderStore: Map<String, Sender> = mapOf()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )

    /**
     * Empty settings
     */
    constructor()

    /**
     * Load the from a directory and its sub-directories
     */
    constructor(
        settingsPath: String,
        orgExt: String? = null
    ) {
        val organizationsFilename = "${organizationsBaseName}${orgExt ?: ""}.yml"
        val settingsDirectory = File(settingsPath)
        if (!settingsDirectory.isDirectory) error("Expected settings directory")
        loadOrganizations(settingsDirectory.toPath().resolve(organizationsFilename).toString())
    }

    fun loadOrganizations(filePath: String): FileSettings {
        val organizationsFile = File(filePath)
        try {
            return loadOrganizations(organizationsFile.inputStream())
        } catch (e: Exception) {
            throw Exception("Error loading: ${organizationsFile.name}", e)
        }
    }

    fun loadOrganizations(organizationStream: InputStream): FileSettings {
        val list = mapper.readValue<List<DeepOrganization>>(organizationStream)
        return loadOrganizationList(list)
    }

    fun loadOrganizations(vararg organizations: DeepOrganization): FileSettings {
        return loadOrganizationList(organizations.toList())
    }

    fun loadOrganizationList(organizations: List<DeepOrganization>): FileSettings {
        organizations.forEach { org ->
            if (org.receivers.find { it.organizationName != org.name } != null)
                error("Metadata Error: receiver organizationName does not match in ${org.name}")
            if (org.receivers.associateBy { it.fullName }.size != org.receivers.size)
                error("Metadata Error: duplicate receiver name in ${org.name}")
            if (org.senders.find { it.organizationName != org.name } != null)
                error("Metadata Error: sender organizationName does not match in ${org.name}")
            if (org.senders.associateBy { it.fullName }.size != org.senders.size)
                error("Metadata Error: duplicate sender name in ${org.name}")
        }
        organizationStore = organizations.associateBy { it.name }
        senderStore = organizations.flatMap { it.senders }.associateBy { it.fullName }
        receiverStore = organizations.flatMap { it.receivers }.associateBy { it.fullName }

        receiverStore.forEach { (_, receiver) ->
            receiver.timing?.let {
                if (!it.isValid())
                    error("Metadata Error: improper batch value for ${receiver.fullName}")
            }
        }
        return this
    }

    /*
     * Settings Provider
     */
    override val organizations get() = this.organizationStore.values
    override val senders get() = this.senderStore.values
    override val receivers get() = this.receiverStore.values

    override fun findOrganization(name: String): Organization? {
        return organizationStore[name]
    }

    override fun findReceiver(fullName: String): Receiver? {
        return receiverStore[fullName]
    }

    override fun findSender(fullName: String): Sender? {
        return senderStore[Sender.canonicalizeFullName(fullName)]
    }

    override fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>? {
        val (organizationName, _) = Receiver.parseFullName(fullName)
        val organization = organizationStore[organizationName] ?: return null
        val receiver = receiverStore[fullName] ?: return null
        return Pair(organization, receiver)
    }

    companion object {
        const val defaultSettingsDirectory = "./settings"
        const val organizationsBaseName = "organizations"
    }
}