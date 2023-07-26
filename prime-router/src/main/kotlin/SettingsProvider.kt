package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import gov.cdc.prime.router.CustomerStatus.ACTIVE
import gov.cdc.prime.router.CustomerStatus.INACTIVE
import gov.cdc.prime.router.CustomerStatus.TESTING

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
enum class Topic(@JsonValue val jsonVal: String, val isUniversalPipeline: Boolean = true) {
    FULL_ELR("full-elr", true),
    ETOR_TI("etor-ti", true),
    ELR_ELIMS("elr-elims", true),
    COVID_19("covid-19", false),
    MONKEYPOX("monkeypox", false),
    TEST("test", false),
}