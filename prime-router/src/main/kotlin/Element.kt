package gov.cdc.prime.router

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*


/**
 * An element is represents a data element (ie. a single logical value) that is contained in single row
 * of a report. A set of Elements form the main content of a Schema.
 *
 * In some sense the element is like the data element in other data schemas that engineers are familiar with.
 * For the data-hub, the data element contains information specific to public health. For a given topic,
 * there is a "standard" schema with elements. The logically the mapping process is:
 *
 *    Schema 1 -> Standard Standard -> schema 2
 *
 * To describe the intent of a element there are references to the national standards.
 */
data class Element(
    // A element can either be a new element or one based on previously defined element
    // - A name of form [A-Za-z0-9_]+ is a new element
    // - A name of form [A-Za-z0-9_]+.[A-Za-z0-9_]+ is an element based on an previously defined element
    //
    val name: String,

    /**
     * Type of the
     */
    val type: Type? = null,
    val valueSet: String? = null,
    val altValues: List<ValueSet.Value>? = null, 
    val required: Boolean? = null,
    val pii: Boolean? = null,
    val phi: Boolean? = null,
    val default: String? = null,
    val mapper: String? = null,

    // Information about the elements definition
    val hhsGuidanceField: String? = null,
    val uscdiField: String? = null,
    val natFlatFileField: String? = null,

    // Format specific information used to format the table

    // HL7 specific information
    val hl7Field: String? = null,
    val hl7OutputFields: List<String>? = null,

    /**
     * The header fields that correspond to an element.
     * A element can output to multiple CSV fields.
     * The first field is considered the primary field. It is used
     * on input define the element
     */
    val csvFields: List<CsvField>? = null,

    // FHIR specific information
    val fhirField: String? = null,
) {
    enum class Type {
        TEXT,
        NUMBER,
        DATE,
        DATETIME,
        DURATION,
        CODE,
        HD,    // Hierarchic Designator
        ID,
        ID_CLIA,
        ID_DLN,
        ID_SSN,
        ID_NPI,
        STREET,
        CITY,
        STATE,
        COUNTY,
        POSTAL_CODE,
        PERSON_NAME,
        TELEPHONE,
        EMAIL,
    }

    data class CsvField(
        val name: String,
        val format: String?,
    )

    val isCodeType get() = this.type == Type.CODE

    fun nameContains(substring: String): Boolean {
        return name.contains(substring, ignoreCase = true)
    }

    fun extendFrom(baseElement: Element): Element {
        return Element(
            name = this.name,
            type = this.type ?: baseElement.type,
            valueSet = this.valueSet ?: baseElement.valueSet,
            required = this.required ?: baseElement.required,
            pii = this.pii ?: baseElement.pii,
            phi = this.phi ?: baseElement.phi,
            mapper = this.mapper ?: baseElement.mapper,
            default = this.default ?: baseElement.default,
            hhsGuidanceField = this.hhsGuidanceField ?: baseElement.hhsGuidanceField,
            uscdiField = this.uscdiField ?: baseElement.uscdiField,
            natFlatFileField = this.natFlatFileField ?: baseElement.natFlatFileField,
            hl7Field = this.hl7Field ?: baseElement.hl7Field,
            hl7OutputFields = this.hl7OutputFields ?: baseElement.hl7OutputFields,
            csvFields = this.csvFields ?: this.csvFields,
        )
    }

    fun toFormatted(field: CsvField, normalizedValue: String): String {
        if (normalizedValue.isEmpty()) return ""
        return when (type) {
            Type.DATE -> {
                if (field.format != null) {
                    val formatter = DateTimeFormatter.ofPattern(field.format)
                    LocalDate.parse(normalizedValue, dateFormatter).format(formatter)
                } else {
                    normalizedValue
                }
            }
            Type.DATETIME -> {
                if (field.format != null) {
                    val formatter = DateTimeFormatter.ofPattern(field.format)
                    LocalDateTime.parse(normalizedValue, datetimeFormatter).format(formatter)
                } else {
                    normalizedValue
                }
            }
            Type.CODE -> {
                if (valueSet == null) error("Schema Error: missing value set for '$name'")
                val set = Metadata.findValueSet(valueSet)
                    ?: error("Schema Error: invalid valueSet name: $valueSet")
                when (field.format) {
                    displayFormat -> {
                        set.toDisplay(normalizedValue)
                            ?: error("Internal Error: '$normalizedValue' cannot be formatted for '$name'")
                    }
                    systemFormat -> set.systemCode
                    else -> normalizedValue
                }
            }
            else -> normalizedValue
        }
    }

    fun toNormalized(field: CsvField, formattedValue: String): String {
        if (formattedValue.isEmpty()) return ""
        return when (type) {
            Type.DATE -> {
                var normalDate = try {
                    LocalDate.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val formatter = DateTimeFormatter.ofPattern(field.format ?: datePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: $formattedValue")
                }
                normalDate.format(dateFormatter)
            }
            Type.DATETIME -> {
                var normalDateTime = try {
                    LocalDateTime.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val formatter = DateTimeFormatter.ofPattern(field.format ?: datetimePattern, Locale.ENGLISH)
                    LocalDateTime.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: $formattedValue for element $name")
                }
                normalDateTime.format(dateFormatter)
            }
            Type.CODE -> {
                if (valueSet == null) error("Schema Error: missing value set for $name")
                val set = Metadata.findValueSet(valueSet) ?: error("Schema Error: invalid valueSet name: $valueSet")
                when (field.format) {
                    displayFormat -> set.toCode(formattedValue)
                        ?: error("Invalid code: '$formattedValue' not a display value for element '$name'")
                    else -> {
                        val display = set.toDisplay(formattedValue)
                            ?: error("Invalid code: '$formattedValue' not a code for element '$name'")
                        set.toCode(display)
                            ?: error("Internal Error: valueSet code error")
                    }
                }
            }
            else -> formattedValue
        }
    }

    companion object {
        const val datePattern = "yyyyMMdd"
        const val datetimePattern = "yyyyMMddHHmm"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH)
        val datetimeFormatter = DateTimeFormatter.ofPattern(datetimePattern, Locale.ENGLISH)
        const val displayFormat = "\$text"
        const val codeFormat = "\$text"
        const val systemFormat = "\$system"

        fun csvFields(name: String, format: String? = null): List<CsvField> {
            return listOf(CsvField(name, format))
        }
    }
}
