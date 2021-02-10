package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(Hl7Configuration::class, name = "HL7"),
    JsonSubTypes.Type(RedoxConfiguration::class, name = "REDOX"),
    JsonSubTypes.Type(CustomConfiguration::class, name = "CUSTOM"),
)
abstract class TranslatorConfiguration(val type: String) {
    open fun buildFormat(): Report.Format { error("override") }
    open fun buildSchemaName(): String { error("override") }
    open fun buildDefaults(): Map<String, String> { error("override") }
}

const val HL7_SCHEMA = "covid-19"
const val REDOX_SCHEMA = "covid-19-redox"

/**
 * Standard HL7 report configuration
 */
data class Hl7Configuration
@JsonCreator constructor(
    val useTestProcessingMode: Boolean = false,
    val useBatchHeaders: Boolean = true,
    val receivingApplicationName: String?,
    val receivingApplicationOID: String?,
    val receivingFacilityName: String?,
    val receivingFacilityOID: String?,
    val messageProfileId: String?,
) : TranslatorConfiguration("HL7") {

    override fun buildFormat(): Report.Format {
        return if (useBatchHeaders) Report.Format.HL7_BATCH else Report.Format.HL7
    }

    override fun buildSchemaName(): String {
        // TODO do the HL7 without AOE work
        return HL7_SCHEMA
    }

    override fun buildDefaults(): Map<String, String> {
        val receivingApplication = when {
            receivingApplicationName != null && receivingApplicationOID != null ->
                "$receivingApplicationName^$receivingApplicationOID^ISO"
            receivingApplicationName != null && receivingApplicationOID == null -> receivingApplicationName
            else -> ""
        }
        val receivingFacility = when {
            receivingFacilityName != null && receivingFacilityOID != null ->
                "$receivingFacilityName^$receivingFacilityOID^ISO"
            receivingFacilityName != null && receivingFacilityOID == null -> receivingFacilityName
            else -> ""
        }
        return mapOf(
            "processing_mode_code" to (if (useTestProcessingMode) "T" else "P"),
            "receiving_application" to receivingApplication,
            "receiving_facility" to receivingFacility,
            "message_profile_id" to (messageProfileId ?: ""),
        )
    }
}

data class RedoxConfiguration
@JsonCreator constructor(
    val useTestProcessingMode: Boolean = false,
    val destinationId: String,
    val destinationName: String,
    val sourceId: String,
    val sourceName: String,
) : TranslatorConfiguration("REDOX") {
    override fun buildFormat(): Report.Format {
        return Report.Format.REDOX
    }

    override fun buildSchemaName(): String {
        return REDOX_SCHEMA
    }

    override fun buildDefaults(): Map<String, String> {
        return mapOf(
            "processing_mode_code" to (if (useTestProcessingMode) "T" else "P"),
            "redox_destination_id" to destinationId,
            "redox_destination_name" to destinationName,
            "redox_source_id" to sourceId,
            "redox_source_name" to sourceName,
        )
    }
}

data class CustomConfiguration
@JsonCreator constructor(
    val schemaName: String,
    val format: Report.Format,
    val defaults: Map<String, String> = emptyMap()
) : TranslatorConfiguration("CUSTOM") {
    override fun buildFormat(): Report.Format {
        return format
    }

    override fun buildSchemaName(): String {
        return schemaName
    }

    override fun buildDefaults(): Map<String, String> {
        return defaults
    }
}