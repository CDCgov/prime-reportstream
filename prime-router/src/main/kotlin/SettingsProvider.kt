package gov.cdc.prime.router

/**
 * Used by the engine to find orgs, senders and receivers
 */
interface SettingsProvider {
    val organizations: Collection<Organization>

    val senders: Collection<Sender>

    val receivers: Collection<Receiver>

    fun findOrganization(name: String): Organization?

    fun findReceiver(fullName: String): Receiver?

    fun findSender(fullName: String): Sender?
}