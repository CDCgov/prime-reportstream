package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// Schemas used
const val HL7_SCHEMA = "covid-19"
const val GAEN_SCHEMA = "covid-19-gaen"

/**
 * The Translator properties are common properties used to
 */
interface TranslatorProperties {
    /**
     * [format] is the format used for translation
     */
    val format: Report.Format

    /**
     * [schemaName] is a the full name of the schema used in the translation
     */
    val schemaName: String

    /**
     * [defaults] are a dictionary of element names and values
     */
    val defaults: Map<String, String>

    /**
     * [nameFormat] is the name of the format used or the translation
     */
    val nameFormat: String

    /**
     * [receivingOrganization] is the full receiver name
     */
    val receivingOrganization: String?
}

// Base JSON Type
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(Hl7Configuration::class, name = "HL7"),
    JsonSubTypes.Type(GAENConfiguration::class, name = "GAEN"),
    JsonSubTypes.Type(CustomConfiguration::class, name = "CUSTOM"),
)
abstract class TranslatorConfiguration(val type: String) : TranslatorProperties

/**
 * Standard HL7 report configuration
 */
data class Hl7Configuration
@JsonCreator constructor(
    // SchemaName is added here so that - by default covid 19 schema is used and
    // if provided the input value is consumed.
    override val schemaName: String = HL7_SCHEMA,
    val useTestProcessingMode: Boolean = false,
    val useBatchHeaders: Boolean = true,
    val receivingApplicationName: String?,
    val receivingApplicationOID: String?,
    val receivingFacilityName: String?,
    val receivingFacilityOID: String?,
    val messageProfileId: String?,
    val replaceValue: Map<String, String>? = emptyMap(),
    val replaceValueAwithB: Map<String, Any>? = emptyMap(),
    val reportingFacilityName: String? = null,
    val reportingFacilityId: String? = null,
    val reportingFacilityIdType: String? = null,
    val suppressQstForAoe: Boolean = false,
    val suppressHl7Fields: String? = null,
    val suppressAoe: Boolean = false,
    val defaultAoeToUnknown: Boolean = false,
    val replaceUnicodeWithAscii: Boolean = false,
    val useBlankInsteadOfUnknown: String? = null,
    val truncateHDNamespaceIds: Boolean = false,
    // Specify a list of HL7 fields that will be truncated at their HL7 max lengths
    val truncateHl7Fields: String? = null,
    val usePid14ForPatientEmail: Boolean = false,
    val convertTimestampToDateTime: String? = null,
    val cliaForOutOfStateTesting: String? = null,
    val cliaForSender: Map<String, String>? = emptyMap(),
    val phoneNumberFormatting: PhoneNumberFormatting = PhoneNumberFormatting.STANDARD,
    val suppressNonNPI: Boolean = false,
    // pass this around as a property now
    val processingModeCode: String? = null,
    val replaceDiiWithOid: Boolean? = null,
    val applyOTCDefault: Boolean = false,
    // Specify how
    val useOrderingFacilityName: OrderingFacilityName = OrderingFacilityName.STANDARD,
    // we will now play that funky music that will drive us til the dawn
    //
    // is this hidden magic? I don't think so. I've moved the ability to override a valueset
    // to the translation config, and therefore we can say that for a specific receiver, use
    // the following alternate values instead of the regular ones
    val valueSetOverrides: Map<String, ValueSet>? = emptyMap(),
    override val nameFormat: String = "standard",
    override val receivingOrganization: String?,
    val convertPositiveDateTimeOffsetToNegative: Boolean? = false,
    // lets us strip chars we don't want showing up in the outbound message
    // this should really be done on the sender side, but it lives here for now
    val stripInvalidCharsRegex: String? = null,
    // A flag to control flipping a receiver to get the dates in their HL7 message converted
    // to their local time zone. If this is true, then we will convert all date time values
    // we have to the local date time for the receiver. If we do not have the time, and it's not
    // clear if we are dealing with a timezone offset, then we will not convert it.
    val convertDateTimesToReceiverLocalTime: Boolean? = false,
    /**
     * Some receivers need a higher precision batch and file header date time
     * value, so I am adding the option here for those who need it
     */
    val useHighPrecisionHeaderDateTimeFormat: Boolean? = false,
) : TranslatorConfiguration("HL7") {
    /**
     * Formatting for XTN fields
     */
    enum class PhoneNumberFormatting {
        /**
         * Standard formatting
         */
        STANDARD,

        /**
         * Component 1 formatted with only digits
         */
        ONLY_DIGITS_IN_COMPONENT_ONE,

        /**
         * (area)local format in component 1. Backward compatibility to an earlier format.
         */
        AREA_LOCAL_IN_COMPONENT_ONE
    }

    /**
     * Ordering facility name formatting
     */
    enum class OrderingFacilityName {
        /**
         * Use the value sent by the sender
         */
        STANDARD,

        /**
         * Override with the NCES enrichment
         */
        NCES,

        /**
         * Override with the organization_name field
         */
        ORGANIZATION_NAME
    }

    @get:JsonIgnore
    override val format: Report.Format get() = if (useBatchHeaders) Report.Format.HL7_BATCH else Report.Format.HL7

    @get:JsonIgnore
    override val defaults: Map<String, String> get() {
        val receivingApplication = when {
            receivingApplicationName != null && receivingApplicationOID != null ->
                "$receivingApplicationName^$receivingApplicationOID^ISO"
            receivingApplicationName != null && receivingApplicationOID == null ->
                receivingApplicationName
            else -> ""
        }
        val receivingFacility = when {
            receivingFacilityName != null && receivingFacilityOID != null ->
                "$receivingFacilityName^$receivingFacilityOID^ISO"
            receivingFacilityName != null && receivingFacilityOID == null ->
                receivingFacilityName
            else -> ""
        }
        val reportingFacility = when {
            reportingFacilityName != null && reportingFacilityId != null && reportingFacilityIdType == null ->
                "$reportingFacilityName^$reportingFacilityId^CLIA"
            reportingFacilityName != null && reportingFacilityId != null && reportingFacilityIdType != null ->
                "$reportingFacilityName^$reportingFacilityId^$reportingFacilityIdType"
            reportingFacilityName != null && reportingFacilityId == null ->
                reportingFacilityName
            else -> ""
        }
        return mapOf(
            "processing_mode_code" to (processingModeCode ?: "P"),
            "receiving_application" to receivingApplication,
            "receiving_facility" to receivingFacility,
            "message_profile_id" to (messageProfileId ?: ""),
            "reporting_facility" to reportingFacility
        )
    }
}

/**
 * A translation for a Google/Apple Exposure Notification. This translation does not have any options.
 */
data class GAENConfiguration
@JsonCreator constructor(
    val dummy: String? = null
) : TranslatorConfiguration("GAEN") {
    @get:JsonIgnore
    override val format: Report.Format get() = Report.Format.CSV_SINGLE // Single item CSV

    @get:JsonIgnore
    override val schemaName: String get() = GAEN_SCHEMA

    @get:JsonIgnore
    override val defaults: Map<String, String> = emptyMap()

    @get:JsonIgnore
    override val nameFormat: String = "standard"

    @get:JsonIgnore
    override val receivingOrganization: String? = null
}

/**
 * Custom report configuration
 */
data class CustomConfiguration
@JsonCreator constructor(
    override val schemaName: String,
    override val format: Report.Format,
    override val defaults: Map<String, String> = emptyMap(),
    override val nameFormat: String = "standard",
    override val receivingOrganization: String?,
) : TranslatorConfiguration("CUSTOM")