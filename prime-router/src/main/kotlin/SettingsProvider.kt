package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.CustomerStatus.ACTIVE
import gov.cdc.prime.router.CustomerStatus.INACTIVE
import gov.cdc.prime.router.CustomerStatus.TESTING
import gov.cdc.prime.router.tokens.JwkSet

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

    fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>?

    // TODO: https://github.com/CDCgov/prime-reportstream/issues/8659
    // this should be removed after all setting keys have been migrated to the organization
    // it is just a temporary helper function for consolidating the keys between organizations and senders
    /**
     *
     * Accepts a sender name and returns all the keys associated with that sender as well as the
     * organization the sender belongs to.
     *
     * @param senderName - the name of the sender
     *
     * @return List of the keys associated with the sender and the sender's organization
     */
    fun getKeys(senderName: String): List<JwkSet>? {
        val sender = this.findSender(senderName) ?: return null
        val organization = this.findOrganization(sender.organizationName) ?: return null
        val organizationKeys = organization.keys ?: emptyList<JwkSet>()
        val senderKeys = sender.keys ?: emptyList<JwkSet>()
        return organizationKeys + senderKeys
    }
}

/**
 * @property INACTIVE Sender or receiver is not using ReportStream
 * @property TESTING Sender or receiver is onboarding, but is not yet fully set up
 * @property ACTIVE Sender or receiver is onboarded and sending/receiving data (either automatically or via manual Download, for receiver)
 */
enum class CustomerStatus {
    @JsonProperty("inactive")
    INACTIVE,

    @JsonProperty("testing")
    TESTING,

    @JsonProperty("active")
    ACTIVE
}

/**
 * A submission with topic FULL_ELR will be processed using the full ELR pipeline (fhir engine), submissions
 * from a sender with topic COVID_19 will be processed using the covid-19 pipeline.
 */
enum class Topic(val json_val: String) {
    @JsonProperty("full-elr")
    FULL_ELR("full-elr"),

    @JsonProperty("covid-19")
    COVID_19("covid-19"),

    @JsonProperty("monkeypox")
    MONKEYPOX("monkeypox"),

    @JsonProperty("CsvFileTests-topic")
    CSV_TESTS("CsvFileTests-topic"),

    @JsonProperty("test")
    TEST("test"),
}