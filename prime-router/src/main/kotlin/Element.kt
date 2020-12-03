package gov.cdc.prime.router

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
            altValues = this.altValues ?: baseElement.altValues,
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
            csvFields = this.csvFields ?: baseElement.csvFields,
        )
    }

    /**
     * A formatted string is the Element's value formatted for a specific csvField
     */
    fun toFormatted(normalizedValue: String, field: CsvField? = null): String {
        if (normalizedValue.isEmpty()) return ""
        return when (type) {
            Type.DATE -> {
                if (field?.format != null) {
                    val formatter = DateTimeFormatter.ofPattern(field.format)
                    LocalDate.parse(normalizedValue, dateFormatter).format(formatter)
                } else {
                    normalizedValue
                }
            }
            Type.DATETIME -> {
                if (field?.format != null) {
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
                // TODO: A more flexible form of the format field for codes is possible and necessary
                when (field?.format) {
                    displayFormat -> set.toDisplayFromCode(normalizedValue)
                        ?: error("Internal Error: '$normalizedValue' cannot be formatted for '$name'")
                    altDisplayFormat -> toAltDisplay(normalizedValue)
                        ?: error("Schema Error: '$normalizedValue' is not in altValues set for '$name")
                    systemFormat -> set.systemCode
                    else -> normalizedValue
                }
            }
            Type.TELEPHONE -> {
                // normalized telephone always has 3 values national:country:extension
                val parts = normalizedValue.split(phoneDelimiter)
                val format = field?.format ?: defaultPhoneFormat
                format
                    .replace(countryCodeToken, parts[1])
                    .replace(areaCodeToken, parts[0].substring(0, 3))
                    .replace(exchangeToken, parts[0].substring(3, 6))
                    .replace(subscriberToken, parts[0].substring(6))
                    .replace(extensionToken, parts[2])
            }
            else -> normalizedValue
        }
    }

    /**
     * Take a formatted CsvField value and turn into a normalized value stored in an element
     */
    fun toNormalized(formattedValue: String, field: CsvField? = null): String {
        if (formattedValue.isEmpty()) return ""
        return when (type) {
            Type.DATE -> {
                val normalDate = try {
                    LocalDate.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val formatter = DateTimeFormatter.ofPattern(field?.format ?: datePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: '$formattedValue' for element '$name'")
                }
                normalDate.format(dateFormatter)
            }
            Type.DATETIME -> {
                val normalDateTime = try {
                    OffsetDateTime.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val formatter = DateTimeFormatter.ofPattern(field?.format ?: datetimePattern, Locale.ENGLISH)
                    OffsetDateTime.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    // Finally, accept a date pattern assume it is in the UTC timezone
                    val date = LocalDate.parse(formattedValue, dateFormatter)
                    OffsetDateTime.of(date, LocalTime.of(0, 0), ZoneOffset.UTC)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: '$formattedValue' for element '$name'")
                }
                normalDateTime.format(datetimeFormatter)
            }
            Type.CODE -> {
                if (valueSet == null) error("Schema Error: missing value set for $name")
                val values = Metadata.findValueSet(valueSet) ?: error("Schema Error: invalid valueSet name: $valueSet")
                when (field?.format) {
                    displayFormat -> values.toCodeFromDisplay(formattedValue)
                        ?: error("Invalid code: '$formattedValue' not a display value for element '$name'")
                    altDisplayFormat -> toAltCode(formattedValue)
                        ?: error("Invalid code: '$formattedValue' not a alt display value for element '$name'")
                    else -> values.toNormalizedCode(formattedValue)
                        ?: error("Invalid Code: '$formattedValue' does not match any codes for '${name}'")
                }
            }
            Type.TELEPHONE -> {
                val number = phoneNumberUtil.parse(formattedValue, "US")
                if (!number.hasNationalNumber() || number.nationalNumber > 9999999999L)
                    error("Invalid phone number '$formattedValue' for '$name'")
                val nationalNumber = DecimalFormat("0000000000").format(number.nationalNumber)
                "${nationalNumber}$phoneDelimiter${number.countryCode}$phoneDelimiter${number.extension}"
            }
            else -> formattedValue
        }
    }

    fun toAltDisplay(code: String): String? {
        if (!isCodeType) return error("Internal Error: asking for an altDisplay for a non-code type")
        if (altValues == null) error("Schema Error: missing alt values for '${name}'")
        return altValues.find { code.equals(it.code, ignoreCase = true) }?.display
    }

    fun toAltCode(altDisplay: String): String? {
        if (!isCodeType) return error("Internal Error: asking for an altDisplay for a non-code type")
        if (altValues == null) error("Schema Error: missing alt values for '${name}'")
        return altValues.find { altDisplay.equals(it.display, ignoreCase = true) }?.code
    }

    companion object {
        const val datePattern = "yyyyMMdd"
        const val datetimePattern = "yyyyMMddHHmmZZZ"
        val dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH)
        val datetimeFormatter = DateTimeFormatter.ofPattern(datetimePattern, Locale.ENGLISH)
        const val displayFormat = "\$display"
        const val codeFormat = "\$code"
        const val systemFormat = "\$system"
        const val altDisplayFormat = "\$alt"
        const val areaCodeToken = "\$area"
        const val exchangeToken = "\$exchange"
        const val subscriberToken = "\$subscriber"
        const val countryCodeToken = "\$country"
        const val extensionToken = "\$extension"
        const val defaultPhoneFormat = "\$area\$exchange\$subscriber"
        const val phoneDelimiter = ":"
        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

        fun csvFields(name: String, format: String? = null): List<CsvField> {
            return listOf(CsvField(name, format))
        }
    }
}
