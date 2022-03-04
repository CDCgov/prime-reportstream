package gov.cdc.prime.router.messages

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.ReportStreamFilters
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.TranslatorConfiguration
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.common.StringUtilities.Companion.trimToNull
import java.time.OffsetDateTime

/**
 * Classes for JSON serialization of payloads to Settings API end-points.
 * These classes should match the ./docs/openapi.yml file
 */

/**
 * Version information for a particular setting. Used in all settings.
 */
data class SettingMetadata(
    /**
     * Version number, 0-based and increasing
     */
    val version: Int,

    /**
     * User id (email name) for the actor
     */
    val createdBy: String,

    /**
     * Time of creation. Note: settings are immutable, so modification time is not applicable.
     */
    val createdAt: OffsetDateTime
)

interface SettingMessage {
    val name: String
    val organizationName: String?
    var meta: SettingMetadata?
    fun consistencyErrorMessage(metadata: Metadata): String?
}

class OrganizationMessage
@JsonCreator constructor(
    name: String,
    description: String,
    jurisdiction: Jurisdiction,
    stateCode: String?,
    countyName: String?,
    filters: List<ReportStreamFilters>?,
    override var meta: SettingMetadata?,
) : Organization(name, description, jurisdiction, stateCode.trimToNull(), countyName.trimToNull(), filters),
    SettingMessage {
    @get:JsonIgnore
    override val organizationName: String? = null
    override fun consistencyErrorMessage(metadata: Metadata): String? { return this.consistencyErrorMessage() }
}

class SenderMessage
@JsonCreator constructor(
    name: String,
    organizationName: String,
    format: Format,
    topic: String,
    customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
    schemaName: String,
    override var meta: SettingMetadata?,
) : Sender(
    name,
    organizationName,
    format,
    topic,
    customerStatus,
    schemaName,
),
    SettingMessage

class ReceiverMessage
@JsonCreator constructor(
    name: String,
    organizationName: String,
    topic: String,
    customerStatus: CustomerStatus = CustomerStatus.INACTIVE,
    translation: TranslatorConfiguration,
    jurisdictionalFilter: ReportStreamFilter = emptyList(),
    qualityFilter: ReportStreamFilter = emptyList(),
    routingFilter: ReportStreamFilter = emptyList(),
    processingModeFilter: ReportStreamFilter = emptyList(),
    reverseTheQualityFilter: Boolean = false,
    deidentify: Boolean = false,
    timing: Timing? = null,
    description: String = "",
    transport: TransportType? = null,
    override var meta: SettingMetadata?,
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
    deidentify,
    timing,
    description,
    transport
),
    SettingMessage