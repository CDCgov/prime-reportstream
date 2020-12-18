package gov.cdc.prime.router

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

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
     * Type of the element
     */
    val type: Type? = null,

    // Either valueSet or altValues must be defined for a CODE type
    val valueSet: String? = null,
    val valueSetRef: ValueSet? = null, // set during fixup
    val altValues: List<ValueSet.Value>? = null,

    // table and tableColumn must be defined for a TABLE type
    val table: String? = null,
    val tableRef: LookupTable? = null, // set during fixup
    val tableColumn: String? = null, // set during fixup

    /**
     * Usage can either be required, requested, requiredIfPresent, requiredIfNotPresent, optional (default)
     */
    val usage: String? = null,
    val usageRequirement: UsageRequirement? = null, // set during fixup
    val pii: Boolean? = null,
    val phi: Boolean? = null,
    val default: String? = null,
    val mapper: String? = null,
    val mapperRef: Mapper? = null, // set during fixup
    val mapperArgs: List<String>? = null, // set during fixup

    // Information about the elements definition.
    val reference: String? = null,
    val referenceUrl: String? = null,
    val hhsGuidanceField: String? = null,
    val uscdiField: String? = null,
    val natFlatFileField: String? = null,

    // Format specific information used to format the table

    // HL7 specific information
    val hl7Field: String? = null,
    val hl7OutputFields: List<String>? = null,
    val hl7AOEQuestion: String? = null,

    /**
     * The header fields that correspond to an element.
     * A element can output to multiple CSV fields.
     * The first field is considered the primary field. It is used
     * on input define the element
     */
    val csvFields: List<CsvField>? = null,

    // FHIR specific information
    val fhirField: String? = null,

    // a field to let us incorporate documentation data (markdown)
    // in the schema files so we can generate documentation off of
    // the file
    val documentation: String? = null,
) {
    enum class Type {
        TEXT,
        NUMBER,
        DATE,
        DATETIME,
        DURATION,
        CODE, // CODED with a HL7, SNOMED-CT, LONIC valueSet
        TABLE, // A table column value
        HD, // ISO Hierarchic Designator
        ID, // Generic ID
        ID_CLIA,
        ID_DLN,
        ID_SSN,
        ID_NPI,
        STREET,
        CITY,
        POSTAL_CODE,
        PERSON_NAME,
        TELEPHONE,
        EMAIL,
    }

    data class CsvField(
        val name: String,
        val format: String?,
    )

    data class HDFields(
        val name: String,
        val universalId: String?,
        val universalIdSystem: String?
    )

    enum class Usage {
        REQUIRED,
        REQUESTED,
        REQUIRED_IF_PRESENT,
        REQUIRED_IF_NOT_PRESENT,
        OPTIONAL,
    }

    data class UsageRequirement(val usage: Usage, val elementName: String? = null)

    val isCodeType get() = this.type == Type.CODE

    fun inheritFrom(baseElement: Element): Element {
        return Element(
            name = this.name,
            type = this.type ?: baseElement.type,
            valueSet = this.valueSet ?: baseElement.valueSet,
            altValues = this.altValues ?: baseElement.altValues,
            table = this.table ?: baseElement.table,
            tableColumn = this.tableColumn ?: baseElement.tableColumn,
            usage = this.usage ?: baseElement.usage,
            pii = this.pii ?: baseElement.pii,
            phi = this.phi ?: baseElement.phi,
            mapper = this.mapper ?: baseElement.mapper,
            default = this.default ?: baseElement.default,
            reference = this.reference ?: baseElement.reference,
            referenceUrl = this.referenceUrl ?: baseElement.referenceUrl,
            hhsGuidanceField = this.hhsGuidanceField ?: baseElement.hhsGuidanceField,
            uscdiField = this.uscdiField ?: baseElement.uscdiField,
            natFlatFileField = this.natFlatFileField ?: baseElement.natFlatFileField,
            hl7Field = this.hl7Field ?: baseElement.hl7Field,
            hl7OutputFields = this.hl7OutputFields ?: baseElement.hl7OutputFields,
            hl7AOEQuestion = this.hl7AOEQuestion ?: baseElement.hl7AOEQuestion,
            documentation = this.documentation ?: baseElement.documentation,
            csvFields = this.csvFields ?: baseElement.csvFields,
        )
    }

    fun nameContains(substring: String): Boolean {
        return name.contains(substring, ignoreCase = true)
    }

    /**
     * Is there a default value for this element?
     *
     * @param defaultValues a dynamic set of default values to use
     */
    fun hasDefaultValue(defaultValues: DefaultValues): Boolean {
        return defaultValues.containsKey(name) || default?.isNotBlank() == true
    }

    fun defaultValue(defaultValues: DefaultValues): String {
        return defaultValues.getOrDefault(name, default ?: "")
    }

    /**
     * A formatted string is the Element's normalized value formatted using the format string passed in
     * The format string's value is specific to the type of the element.
     */
    fun toFormatted(
        normalizedValue: String,
        format: String? = null
    ): String {
        if (normalizedValue.isEmpty()) return ""
        return when (type) {
            Type.DATE -> {
                if (format != null) {
                    val formatter = DateTimeFormatter.ofPattern(format)
                    LocalDate.parse(normalizedValue, dateFormatter).format(formatter)
                } else {
                    normalizedValue
                }
            }
            Type.DATETIME -> {
                if (format != null) {
                    val formatter = DateTimeFormatter.ofPattern(format)
                    LocalDateTime.parse(normalizedValue, datetimeFormatter).format(formatter)
                } else {
                    normalizedValue
                }
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                if (format == altDisplayFormat) {
                    toAltDisplay(normalizedValue)
                        // TODO Revisit: there may be times that normalizedValue is not an altValue
                        ?: error("Schema Error: '$normalizedValue' is not in altValues set for '$name")
                } else {
                    if (valueSetRef == null)
                        error("Schema Error: missing value set for '$name'")
                    when (format) {
                        displayFormat ->
                            valueSetRef.toDisplayFromCode(normalizedValue)
                                ?: error("Internal Error: '$normalizedValue' cannot be formatted for '$name'")
                        systemFormat ->
                            // Very confusing, but this special case is in the HHS Guidance Confluence page
                            if (valueSetRef.name == "hl70136" && normalizedValue == "UNK")
                                "NULLFL"
                            else
                                valueSetRef.systemCode
                        else ->
                            normalizedValue
                    }
                }
            }
            Type.TELEPHONE -> {
                // normalized telephone always has 3 values national:country:extension
                val parts = normalizedValue.split(phoneDelimiter)
                (format ?: defaultPhoneFormat)
                    .replace(countryCodeToken, parts[1])
                    .replace(areaCodeToken, parts[0].substring(0, 3))
                    .replace(exchangeToken, parts[0].substring(3, 6))
                    .replace(subscriberToken, parts[0].substring(6))
                    .replace(extensionToken, parts[2])
            }
            Type.POSTAL_CODE -> {
                when (format) {
                    zipFiveToken -> {
                        // If this is US zip, return the first 5 digits
                        val matchResult = Regex(usZipFormat).matchEntire(normalizedValue)
                        matchResult?.groupValues?.get(1)
                            ?: normalizedValue
                    }
                    zipFivePlusFourToken -> {
                        // If this a US zip, either 5 or 9 digits depending on the value
                        val matchResult = Regex(usZipFormat).matchEntire(normalizedValue)
                        if (matchResult != null && matchResult.groups[2] == null) {
                            matchResult.groups[1]?.value ?: ""
                        } else if (matchResult != null && matchResult.groups[2] != null) {
                            "${matchResult.groups[1]?.value}-${matchResult.groups[2]?.value}"
                        } else {
                            normalizedValue
                        }
                    }
                    else -> normalizedValue
                }
            }
            Type.HD -> {
                val hdFields = parseHD(normalizedValue)
                when (format) {
                    null,
                    hdNameToken -> hdFields.name
                    hdUniversalIdToken -> hdFields.universalId ?: ""
                    hdSystemToken -> hdFields.universalIdSystem ?: ""
                    else -> error("Schema Error: unsupported format for output: '$format' in '$name'")
                }
            }
            else -> normalizedValue
        }
    }

    /**
     * Take a formatted value and check to see if can be stored in a report.
     */
    fun checkForError(formattedValue: String, format: String? = null): String? {
        if (formattedValue.isBlank()) return "Blank value for element '$name'"
        return when (type) {
            Type.DATE -> {
                try {
                    LocalDate.parse(formattedValue)
                    return null
                } catch (e: DateTimeParseException) {
                }
                return try {
                    val formatter = DateTimeFormatter.ofPattern(format ?: datePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                    null
                } catch (e: DateTimeParseException) {
                    "Invalid date: '$formattedValue' for element '$name'"
                }
            }
            Type.DATETIME -> {
                try {
                    // Try an ISO pattern
                    OffsetDateTime.parse(formattedValue)
                    return null
                } catch (e: DateTimeParseException) {
                }
                try {
                    // Try a HL7 pattern
                    val formatter = DateTimeFormatter.ofPattern(format ?: datetimePattern, Locale.ENGLISH)
                    OffsetDateTime.parse(formattedValue, formatter)
                    return null
                } catch (e: DateTimeParseException) {
                }
                try {
                    // Try to parse using a LocalDate pattern assuming it is in our canonical dateFormatter. Central timezone.
                    val date = LocalDate.parse(formattedValue, dateFormatter)
                    val zoneOffset = ZoneId.of(USTimeZone.CENTRAL.zoneId).rules.getOffset(Instant.now())
                    OffsetDateTime.of(date, LocalTime.of(0, 0), zoneOffset)
                    return null
                } catch (e: DateTimeParseException) {
                }
                return try {
                    // Try to parse using a LocalDate pattern, assuming it follows a non-canonical format value.
                    // Example: 'yyyy-mm-dd' - the incoming data is a Date, but not our canonical date format.
                    val formatter = DateTimeFormatter.ofPattern(format ?: datetimePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                    null
                } catch (e: DateTimeParseException) {
                    "Invalid date: '$formattedValue' for element '$name'"
                }
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                return if (format == altDisplayFormat) {
                    if (toAltCode(formattedValue) != null) null else
                        "Invalid code: '$formattedValue' is not a display value in altValues set for '$name'"
                } else {
                    if (valueSetRef == null) error("Schema Error: missing value set for $name")
                    when (format) {
                        displayFormat ->
                            if (valueSetRef.toCodeFromDisplay(formattedValue) != null) null else
                                "Invalid code: '$formattedValue' not a display value for element '$name'"
                        else ->
                            if (valueSetRef.toNormalizedCode(formattedValue) != null) null else
                                "Invalid Code: '$formattedValue' does not match any codes for '$name'"
                    }
                }
            }
            Type.TELEPHONE -> {
                val number = phoneNumberUtil.parse(formattedValue, "US")
                return if (!number.hasNationalNumber() || number.nationalNumber > 9999999999L)
                    "Invalid phone number '$formattedValue' for '$name'"
                else
                    null
            }
            Type.POSTAL_CODE -> {
                // Let in all formats defined by http://www.dhl.com.tw/content/dam/downloads/tw/express/forms/postcode_formats.pdf
                return if (!Regex("^[A-Za-z\\d\\- ]{3,12}\$").matches(formattedValue))
                    "Input Error: invalid postal code '$formattedValue'"
                else
                    null
            }
            Type.HD -> {
                when (format) {
                    null,
                    hdNameToken -> null
                    else -> "Schema Error: unsupported format for input: '$format' in '$name'"
                }
            }
            else -> null
        }
    }

    /**
     * Take a formatted value and turn into a normalized value stored in a report
     */
    fun toNormalized(formattedValue: String, format: String? = null): String {
        if (formattedValue.isEmpty()) return ""
        return when (type) {
            Type.DATE -> {
                val normalDate = try {
                    LocalDate.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val formatter = DateTimeFormatter.ofPattern(format ?: datePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: '$formattedValue' for element '$name'")
                }
                normalDate.format(dateFormatter)
            }
            Type.DATETIME -> {
                val normalDateTime = try {
                    // Try an ISO pattern
                    OffsetDateTime.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    // Try a HL7 pattern
                    val formatter = DateTimeFormatter.ofPattern(format ?: datetimePattern, Locale.ENGLISH)
                    OffsetDateTime.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    // Try to parse using a LocalDate pattern assuming it is in our canonical dateFormatter. Central timezone.
                    val date = LocalDate.parse(formattedValue, dateFormatter)
                    val zoneOffset = ZoneId.of(USTimeZone.CENTRAL.zoneId).rules.getOffset(Instant.now())
                    OffsetDateTime.of(date, LocalTime.of(0, 0), zoneOffset)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    // Try to parse using a LocalDate pattern, assuming it follows a non-canonical format value.
                    // Example: 'yyyy-mm-dd' - the incoming data is a Date, but not our canonical date format.
                    val formatter = DateTimeFormatter.ofPattern(format ?: datetimePattern, Locale.ENGLISH)
                    val date = LocalDate.parse(formattedValue, formatter)
                    val zoneOffset = ZoneId.of(USTimeZone.CENTRAL.zoneId).rules.getOffset(Instant.now())
                    OffsetDateTime.of(date, LocalTime.of(0, 0), zoneOffset)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: '$formattedValue' for element '$name'")
                }
                normalDateTime.format(datetimeFormatter)
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                if (format == altDisplayFormat) {
                    toAltCode(formattedValue)
                        ?: error("Invalid code: '$formattedValue' is not a display value in altValues set for '$name'")
                } else {
                    if (valueSetRef == null) error("Schema Error: missing value set for $name")
                    when (format) {
                        displayFormat ->
                            valueSetRef.toCodeFromDisplay(formattedValue)
                                ?: error("Invalid code: '$formattedValue' not a display value for element '$name'")
                        else ->
                            valueSetRef.toNormalizedCode(formattedValue)
                                ?: error("Invalid Code: '$formattedValue' does not match any codes for '$name'")
                    }
                }
            }
            Type.TELEPHONE -> {
                val number = phoneNumberUtil.parse(formattedValue, "US")
                if (!number.hasNationalNumber() || number.nationalNumber > 9999999999L)
                    error("Invalid phone number '$formattedValue' for '$name'")
                val nationalNumber = DecimalFormat("0000000000").format(number.nationalNumber)
                "${nationalNumber}$phoneDelimiter${number.countryCode}$phoneDelimiter${number.extension}"
            }
            Type.POSTAL_CODE -> {
                // Let in all formats defined by http://www.dhl.com.tw/content/dam/downloads/tw/express/forms/postcode_formats.pdf
                if (!Regex("^[A-Za-z\\d\\- ]{3,12}\$").matches(formattedValue))
                    error("Input Error: invalid postal code '$formattedValue'")
                formattedValue.replace(" ", "")
            }
            Type.HD -> {
                // No matter what data value is, overwrite with our hardcoded default.
                // By definition, we're the sending_application!
                //
                // Note:  This hack 'fixes' a bug in the Send function where data is read back in, and the
                // incoming value is split between two fields: sending_application and sending_application_id
                // There's currently no function to combine these in toNormalized(),
                // so that it gets properly split apart again later in toFormatted.
                this.default ?: ""
            }
            else -> formattedValue
        }
    }

    fun toAltDisplay(code: String): String? {
        if (!isCodeType) error("Internal Error: asking for an altDisplay for a non-code type")
        if (altValues == null) error("Schema Error: missing alt values for '$name'")
        return altValues.find { code.equals(it.code, ignoreCase = true) }?.display
    }

    fun toAltCode(altDisplay: String): String? {
        if (!isCodeType) error("Internal Error: asking for an altDisplay for a non-code type")
        if (altValues == null) error("Schema Error: missing alt values for '$name'")
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
        const val hdDelimiter = "&"
        const val hdNameToken = "\$name"
        const val hdUniversalIdToken = "\$universalId"
        const val hdSystemToken = "\$system"
        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
        const val zipFiveToken = "\$zipFive"
        const val zipFivePlusFourToken = "\$zipFivePlusFour"
        const val usZipFormat = """^(\d{5})[- ]?(\d{4})?$"""
        const val zipDefaultFormat = zipFiveToken

        fun csvFields(name: String, format: String? = null): List<CsvField> {
            return listOf(CsvField(name, format))
        }

        fun parseHD(value: String): HDFields {
            val parts = value.split(hdDelimiter)
            return when (parts.size) {
                3 -> HDFields(parts[0], parts[1], parts[2])
                1 -> HDFields(parts[0], universalId = null, universalIdSystem = null)
                else -> error("Internal Error: Invalid HD value '$value'")
            }
        }
    }
}