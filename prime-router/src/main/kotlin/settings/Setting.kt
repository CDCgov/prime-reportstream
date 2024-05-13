package gov.cdc.prime.router.settings

import com.fasterxml.jackson.annotation.JsonProperty
import gov.cdc.prime.router.metadata.Metadata
import gov.cdc.prime.router.settings.CustomerStatus.ACTIVE
import gov.cdc.prime.router.settings.CustomerStatus.INACTIVE
import gov.cdc.prime.router.settings.CustomerStatus.TESTING
import java.time.OffsetDateTime

interface SettingAPI {
    val name: String
    val organizationName: String?
    var version: Int?
    var createdBy: String?
    var createdAt: OffsetDateTime?
    fun consistencyErrorMessage(metadata: Metadata): String?
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
    ACTIVE,
}