package gov.cdc.prime.router

import ca.uhn.fhir.rest.api.Constants.CT_FHIR_NDJSON
import ca.uhn.fhir.rest.api.Constants.CT_TEXT_CSV

/**
 * Enum class representing different MIME formats used in the system.
 * Each format has an associated file extension, MIME type, and a flag indicating whether it's a single item format.
 *
 * @property ext The file extension associated with the format.
 * @property mimeType The MIME type associated with the format.
 * @property isSingleItemFormat A flag indicating whether the format is for single item serialization.
 */
enum class MimeFormat(val ext: String, val mimeType: String, val isSingleItemFormat: Boolean = false) {
    INTERNAL("internal.csv", CT_TEXT_CSV), // A format that serializes all elements of a Report.kt (in CSV)
    CSV("csv", CT_TEXT_CSV), // A CSV format that follows the csvFields
    CSV_SINGLE("csv", CT_TEXT_CSV, true), // A CSV format for single item serialization
    HL7("hl7", "application/hl7-v2", true), // HL7 format with one result per file
    HL7_BATCH("hl7", "application/hl7-v2"), // HL7 format with BHS and FHS headers
    FHIR("fhir", CT_FHIR_NDJSON), // FHIR format with NDJSON MIME type
    ;

    companion object {
        /**
         * Safely returns the MIME format corresponding to the provided string.
         * Defaults to CSV if the string is null or cannot be matched to a known format.
         *
         * @param formatStr The string representation of the format.
         * @return The corresponding MimeFormat, or CSV if the input is invalid.
         */
        fun safeValueOf(formatStr: String?): MimeFormat = try {
            valueOf(formatStr ?: "CSV")
        } catch (e: IllegalArgumentException) {
            CSV
        }

        /**
         * Returns the MIME format based on the provided file extension, ignoring case.
         *
         * @param ext The file extension.
         * @return The corresponding MimeFormat.
         * @throws IllegalArgumentException If the extension does not match any known format.
         */
        fun valueOfFromExt(ext: String): MimeFormat = when (ext.lowercase()) {
            HL7.ext.lowercase() -> HL7
            FHIR.ext.lowercase() -> FHIR
            CSV.ext.lowercase() -> CSV
            else -> throw IllegalArgumentException("Unexpected extension $ext.")
        }

        /**
         * Returns the MIME format corresponding to the provided string, ignoring case.
         *
         * @param bodyFormat The string representation of the format.
         * @return The corresponding MimeFormat.
         */
        fun valueOfIgnoreCase(bodyFormat: String): MimeFormat = valueOf(bodyFormat.uppercase())

        /**
         * Returns the MIME format based on the provided MIME type.
         *
         * @param mimeType The MIME type.
         * @return The corresponding MimeFormat.
         * @throws IllegalArgumentException If the MIME type does not match any known format.
         */
        fun valueOfFromMimeType(mimeType: String): MimeFormat = entries.find { it.mimeType == mimeType }
                ?: throw IllegalArgumentException("Unexpected MIME type $mimeType.")
    }
}