package gov.cdc.prime.router.fhirengine.translation.hl7

import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.common.StringUtilities.trimAndTruncate
import gov.cdc.prime.router.fhirengine.translation.hl7.config.TruncationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Constants
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Constants.HD_FIELDS_LOCAL
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Constants.HD_TRUNCATION_LIMIT
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import kotlin.math.min

/**
 * The shared HL7 truncation logic used in both the Covid Pipeline and the Universal Pipeline
 */
sealed interface HL7Truncator {

    fun getHl7MaxLength(hl7FieldOrPath: String, terser: Terser): Int?

    /**
     * Trim and truncate the [value] according to the rules in [hl7Config] for [hl7FieldOrPath].
     * [terser] provides hl7 standards
     */
    fun trimAndTruncateValue(
        value: String,
        hl7FieldOrPath: String,
        terser: Terser,
        truncationConfig: TruncationConfig
    ): String {
        val maxLength = getMaxLength(
            hl7FieldOrPath,
            value,
            terser,
            truncationConfig
        )
        return value.trimAndTruncate(maxLength)
    }

    /**
     * Calculate for [hl7Field] and [value] the length to truncate the value according to the
     * truncation rules in [hl7Config]. The [terser] is used to determine the HL7 specification length.
     */
    fun getMaxLength(
        hl7FieldOrPath: String,
        value: String,
        terser: Terser,
        truncationConfig: TruncationConfig
    ): Int? {
        val hl7Field = hl7FieldFromPath(hl7FieldOrPath)

        // The & character in HL7 is a sub sub field separator. A validly
        // produced HL7 message should escape & characters as \T\ so that
        // the HL7 parser doesn't interpret these as sub sub field separators.
        // Because of this reason, all string values should go through the getTruncationLimitWithEncoding
        // so that string values that contain sub sub field separators (^&~) will be properly truncated.
        return when {
            // This special case takes into account special rules needed by jurisdiction
            truncationConfig.truncateHDNamespaceIds && hl7Field in HD_FIELDS_LOCAL -> {
                getTruncationLimitWithEncoding(value, HD_TRUNCATION_LIMIT)
            }
            hl7Field in truncationConfig.customLengthHl7Fields -> {
                getTruncationLimitWithEncoding(value, truncationConfig.customLengthHl7Fields[hl7Field])
            }
            // For the fields listed here use the hl7 max length
            hl7Field in truncationConfig.truncateHl7Fields -> {
                getTruncationLimitWithEncoding(value, getHl7MaxLength(hl7FieldOrPath, terser))
            }
            // In general, don't truncate. The thinking is that
            // 1. the max length of the specification is "normative" not system specific.
            // 2. ReportStream is a conduit and truncation is a loss of information
            // 3. Much of the current HHS guidance implies lengths longer than the 2.5.1 minimums
            // 4. Later hl7 specifications, relax the minimum length requirements
            else -> null
        }
    }

    /**
     * Get a new truncation limit accounting for the encoding of HL7 special characters.
     * @param value string value to search for HL7 special characters
     * @param truncationLimit the starting limit
     * @return the new truncation limit or starting limit if no special characters are found
     */
    fun getTruncationLimitWithEncoding(value: String, truncationLimit: Int?): Int? {
        return truncationLimit?.let { limit ->
            val regex = "[&^~|]".toRegex()
            val endIndex = min(value.length, limit)
            val matchCount = regex.findAll(value.substring(0, endIndex)).count()

            limit - (matchCount * 2)
        }
    }

    /**
     * Attempts to lookup a subcomponents max length on our internal table
     *
     * ex:
     * PID-5-1 ("family name") -> PID-5 type = XPN - Extended Person Name Type
     * getMaxLengthForCompositeType(XPN, 1) -> 194 max length
     */
    fun HL7Truncator.getMaxLengthForCompositeType(type: Type, component: Int): Int? {
        val table = HL7Constants.getHL7ComponentMaxLengthList(type.name)
        return table?.let {
            if (component <= it.size) {
                it[component - 1]
            } else null
        }
    }

    /**
     * Grabs the last segment of the path and removes the index if present
     *
     * This will handle a single field passed to it without modifying it
     */
    fun HL7Truncator.hl7FieldFromPath(path: String): String {
        val rawHL7Field = path.substringAfterLast("/")
        return HL7Utils.removeIndexFromHL7Field(rawHL7Field).trim()
    }

    /**
     * Given the internal field or component specified in [hl7Field], return the maximum string length
     * according to the HL7 specification. The [terser] provides the HL7 specifications
     */
    fun getHl7MaxLength(
        segment: Segment,
        field: Type,
        parts: HL7FieldComponents
    ): Int? {
        return if (parts.third != null) {
            null // Add cases for sub-components here
        } else if (parts.second != null) {
            getMaxLengthForCompositeType(field, parts.second)
        } else {
            segment.getLength(parts.first)
        }
    }

    /**
     * Container for an HL7 field's components
     */
    data class HL7FieldComponents(
        val first: Int,
        val second: Int?,
        val third: Int?
    ) {
        companion object {
            /**
             * This will blow up if a malformed string is passed
             *
             * "MSH-5-1" -> HL7FieldComponents(5, 1, null)
             */
            fun parse(hl7Field: String): HL7FieldComponents {
                val rawParts = hl7Field
                    .substring(4)
                    .split("-")
                    .map { it.toInt() }

                return HL7FieldComponents(
                    rawParts[0],
                    rawParts.getOrNull(1),
                    rawParts.getOrNull(2)
                )
            }
        }
    }
}

class CovidPipelineHL7Truncator : HL7Truncator {

    /**
     * Given the internal field or component specified in [hl7Field], return the maximum string length
     * according to the HL7 specification. The [terser] provides the HL7 specifications
     */
    override fun getHl7MaxLength(hl7FieldOrPath: String, terser: Terser): Int? {
        // Dev Note: this function is work in progress.
        // It is meant to be a general function for all fields and components,
        // but only has support for the cases of current COVID-19 schema.
        // always a field in COVID pipeline
        val segmentName = hl7FieldOrPath.take(HL7Constants.SEGMENT_NAME_LENGTH)
        val segmentSpec = HL7Utils.formSegSpec(segmentName)
        val segment = terser.getSegment(segmentSpec)
        val parts = HL7Truncator.HL7FieldComponents.parse(hl7FieldOrPath)
        val field = segment.getField(parts.first, 0)
        return getHl7MaxLength(segment, field, parts)
    }
}

class UniversalPipelineHL7Truncator : HL7Truncator {

    /**
     * In the UP we pass an entire field's path. We want to use the existing COVID
     * pipeline logic while avoiding the call to HL7Utils.formSegSpec to find
     * the path (since we already have it)
     */
    override fun getHl7MaxLength(hl7FieldOrPath: String, terser: Terser): Int? {
        val hl7Field = hl7FieldFromPath(hl7FieldOrPath)
        val segmentName = hl7FieldOrPath.replace(hl7Field, hl7Field.take(HL7Constants.SEGMENT_NAME_LENGTH))
        val segment = terser.getSegment(segmentName)
        val parts = HL7Truncator.HL7FieldComponents.parse(hl7Field)
        val field = segment.getField(parts.first, 0)
        return getHl7MaxLength(segment, field, parts)
    }
}