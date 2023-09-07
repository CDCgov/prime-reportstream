package gov.cdc.prime.router.fhirengine.translation.hl7

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
class HL7Truncator {

    /**
     * Trim and truncate the [value] according to the rules in [hl7Config] for [hl7Field].
     * [terser] provides hl7 standards
     */
    fun trimAndTruncateValue(
        value: String,
        hl7Field: String,
        terser: Terser,
        truncationConfig: TruncationConfig
    ): String {
        val maxLength = getMaxLength(
            hl7Field,
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
        hl7Field: String,
        value: String,
        terser: Terser,
        truncationConfig: TruncationConfig
    ): Int? {
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
                getTruncationLimitWithEncoding(value, getHl7MaxLength(hl7Field, terser))
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
     * Given the internal field or component specified in [hl7Field], return the maximum string length
     * according to the HL7 specification. The [terser] provides the HL7 specifications
     */
    fun getHl7MaxLength(hl7Field: String, terser: Terser): Int? {
        // Dev Note: this function is work in progress.
        // It is meant to be a general function for all fields and components,
        // but only has support for the cases of current COVID-19 schema.
        val segmentName = hl7Field.substring(0, 3)
        val segmentSpec = HL7Utils.formSegSpec(segmentName)
        val segment = terser.getSegment(segmentSpec)
        val parts = hl7Field.substring(4).split("-").map { it.toInt() }
        val field = segment.getField(parts[0], 0)
        return when (parts.size) {
            // In general, use the values found in the HAPI library for fields
            1 -> segment.getLength(parts[0])
            // use our max-length tables when field and component is specified
            2 -> getMaxLengthForCompositeType(field, parts[1])
            // Add cases for sub-components here
            else -> null
        }
    }

    private fun getMaxLengthForCompositeType(type: Type, component: Int): Int? {
        val table = HL7Constants.getHL7ComponentMaxLengthList(type.name)
        return table?.let {
            if (component <= it.size) {
                it[component - 1]
            } else null
        }
    }
}