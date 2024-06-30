package gov.cdc.prime.router

import ca.uhn.fhir.rest.api.Constants.CT_TEXT
import ca.uhn.fhir.rest.api.Constants.CT_FHIR_NDJSON


enum class MimeFormat(val ext: String, val mimeType: String, val isSingleItemFormat: Boolean = false) {
    INTERNAL("internal.csv", CT_TEXT), // A format that serializes all elements of a Report.kt (in CSV)
    CSV("csv", CT_TEXT), // A CSV format the follows the csvFields
    CSV_SINGLE("csv", CT_TEXT, true),
    HL7("hl7", "application/hl7-v2", true), // HL7 with one result per file
    HL7_BATCH("hl7", "application/hl7-v2"), // HL7 with BHS and FHS headers
    FHIR("fhir", CT_FHIR_NDJSON),
    ;

    companion object {
        // Default to CSV if weird or unknown
        fun safeValueOf(formatStr: String?): MimeFormat = try {
            valueOf(formatStr ?: "CSV")
        } catch (e: IllegalArgumentException) {
            CSV
        }

        /**
         * Returns a Format based on the [ext] provided, ignoring case.
         */
        fun valueOfFromExt(ext: String): MimeFormat = when (ext.lowercase()) {
            HL7.ext.lowercase() -> HL7
            FHIR.ext.lowercase() -> FHIR
            CSV.ext.lowercase() -> CSV
            else -> throw IllegalArgumentException("Unexpected extension $ext.")
        }

        fun valueOfIgnoreCase(bodyFormat: String): MimeFormat = valueOf(bodyFormat.uppercase())
    }
}
