package gov.cdc.prime.router

import com.google.i18n.phonenumbers.PhoneNumberUtil
import gov.cdc.prime.router.Element.Cardinality.ONE
import gov.cdc.prime.router.Element.Cardinality.ZERO_OR_ONE
import java.lang.Exception
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

    val cardinality: Cardinality? = null,
    val pii: Boolean? = null,
    val phi: Boolean? = null,
    val maxLength: Int? = null, // used to truncate outgoing formatted String fields.  null == no length limit.
    val default: String? = null,
    val mapper: String? = null,
    val mapperRef: Mapper? = null, // set during fixup
    val mapperArgs: List<String>? = null, // set during fixup

    // Information about the elements definition.
    val reference: String? = null,
    val referenceUrl: String? = null,
    val hhsGuidanceField: String? = null,
    val natFlatFileField: String? = null,

    // Format specific information used to format output

    // HL7 specific information
    val hl7Field: String? = null,
    val hl7OutputFields: List<String>? = null,
    val hl7AOEQuestion: String? = null,

    // Redox specific information
    val redoxOutputFields: List<String>? = null,

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

    // used for the concatenation mapper. the element carries this
    // value around and into the mapper itself so the interface for the
    // mapper remains as generic as possible
    val delimiter: String? = null,
) {
    /**
     * Types of elements. Types imply a specific format and fake generator.
     */
    enum class Type {
        TEXT,
        TEXT_OR_BLANK, // Blank values are valid (not null)
        NUMBER,
        DATE,
        DATETIME,
        DURATION,
        CODE, // CODED with a HL7, SNOMED-CT, LONIC valueSet
        TABLE, // A table column value
        TABLE_OR_BLANK,
        EI, // A HL7 Entity Identifier (4 parts)
        HD, // ISO Hierarchic Designator
        ID, // Generic ID
        ID_CLIA, // CMS CLIA number (must follow CLIA format rules)
        ID_DLN,
        ID_SSN,
        ID_NPI,
        STREET,
        STREET_OR_BLANK,
        CITY,
        POSTAL_CODE,
        PERSON_NAME,
        TELEPHONE,
        EMAIL,
        BLANK,
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

    data class EIFields(
        val name: String,
        val namespace: String?,
        val universalId: String?,
        val universalIdSystem: String?
    )

    data class SubValue(
        val name: String,
        val value: String,
        val format: String?
    )

    /**
     * @property ZERO_OR_ONE Can be null or present (default)
     * @property ONE Must be present, error if not present
     */
    enum class Cardinality {
        ZERO_OR_ONE,
        ONE;
        // ZERO is not a value, just remove the element to represent this concept
        // Other values including conditionals in the future.

        fun toFormatted(): String {
            return when (this) {
                ZERO_OR_ONE -> "[0..1]"
                ONE -> "[1..1]"
            }
        }
    }

    val isCodeType get() = this.type == Type.CODE

    val isOptional get() = this.cardinality == null ||
        this.cardinality == Cardinality.ZERO_OR_ONE || canBeBlank

    val canBeBlank
        get() = type == Type.TEXT_OR_BLANK ||
            type == Type.STREET_OR_BLANK ||
            type == Type.TABLE_OR_BLANK ||
            type == Type.BLANK

    // Creates a field mapping string showing the external CSV header name(s)
    // and the corresponding internal field name
    val fieldMapping get() = "'${this.csvFields?.joinToString { it -> it.name }}' ('${this.name}')"

    fun inheritFrom(baseElement: Element): Element {
        return Element(
            name = this.name,
            type = this.type ?: baseElement.type,
            valueSet = this.valueSet ?: baseElement.valueSet,
            altValues = this.altValues ?: baseElement.altValues,
            table = this.table ?: baseElement.table,
            tableColumn = this.tableColumn ?: baseElement.tableColumn,
            cardinality = this.cardinality ?: baseElement.cardinality,
            pii = this.pii ?: baseElement.pii,
            phi = this.phi ?: baseElement.phi,
            maxLength = this.maxLength ?: baseElement.maxLength,
            mapper = this.mapper ?: baseElement.mapper,
            default = this.default ?: baseElement.default,
            reference = this.reference ?: baseElement.reference,
            referenceUrl = this.referenceUrl ?: baseElement.referenceUrl,
            hhsGuidanceField = this.hhsGuidanceField ?: baseElement.hhsGuidanceField,
            natFlatFileField = this.natFlatFileField ?: baseElement.natFlatFileField,
            hl7Field = this.hl7Field ?: baseElement.hl7Field,
            hl7OutputFields = this.hl7OutputFields ?: baseElement.hl7OutputFields,
            hl7AOEQuestion = this.hl7AOEQuestion ?: baseElement.hl7AOEQuestion,
            redoxOutputFields = this.redoxOutputFields ?: baseElement.redoxOutputFields,
            documentation = this.documentation ?: baseElement.documentation,
            csvFields = this.csvFields ?: baseElement.csvFields,
            delimiter = this.delimiter ?: baseElement.delimiter,
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
        val formattedValue = when (type) {
            // sometimes you just need to send through an empty column
            Type.BLANK -> ""
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
                when (format) {
                    // TODO Revisit: there may be times that normalizedValue is not an altValue
                    altDisplayToken ->
                        toAltDisplay(normalizedValue)
                            ?: error("Schema Error: '$normalizedValue' is not in altValues set for $fieldMapping")
                    codeToken ->
                        toCode(normalizedValue)
                            ?: error(
                                "Schema Error: " +
                                    "'$normalizedValue' is not in valueSet '$valueSet' for $fieldMapping/'$format'. " +
                                    "\nAvailable values are " +
                                    "${valueSetRef?.values?.joinToString { "${it.code} -> ${it.display}" }}" +
                                    "\nAlt values (${altValues?.count()}) are " +
                                    "${altValues?.joinToString { "${it.code} -> ${it.display}" }}"
                            )
                    caretToken -> {
                        val display = valueSetRef?.toDisplayFromCode(normalizedValue)
                            ?: error("Internal Error: '$normalizedValue' cannot be formatted for $fieldMapping")
                        "$normalizedValue^$display^${valueSetRef.systemCode}"
                    }
                    displayToken -> {
                        valueSetRef?.toDisplayFromCode(normalizedValue)
                            ?: error("Internal Error: '$normalizedValue' cannot be formatted for $fieldMapping")
                    }
                    systemToken -> {
                        // Very confusing, but this special case is in the HHS Guidance Confluence page
                        if (valueSetRef?.name == "hl70136" && normalizedValue == "UNK")
                            "NULLFL"
                        else
                            valueSetRef?.systemCode ?: error("valueSetRef for $valueSet is null!")
                    }
                    else -> normalizedValue
                }
            }
            Type.TELEPHONE -> {
                // normalized telephone always has 3 values national:country:extension
                val parts = if (normalizedValue.contains(phoneDelimiter)) {
                    normalizedValue.split(phoneDelimiter)
                } else {
                    // remove parens from HL7 formatting
                    listOf(
                        normalizedValue
                            .replace("(", "")
                            .replace(")", ""),
                        "1", // country code
                        "" // extension
                    )
                }

                (format ?: defaultPhoneFormat)
                    .replace(countryCodeToken, parts[1])
                    .replace(areaCodeToken, parts[0].substring(0, 3))
                    .replace(exchangeToken, parts[0].substring(3, 6))
                    .replace(subscriberToken, parts[0].substring(6))
                    .replace(extensionToken, parts[2])
                    .replace(e164Token, "+${parts[1]}${parts[0]}")
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
                    else -> error("Schema Error: unsupported HD format for output: '$format' in $fieldMapping")
                }
            }
            Type.EI -> {
                val eiFields = parseEI(normalizedValue)
                when (format) {
                    null,
                    eiNameToken -> eiFields.name
                    eiNamespaceIdToken -> eiFields.namespace ?: ""
                    eiUniversalIdToken -> eiFields.universalId ?: ""
                    eiSystemToken -> eiFields.universalIdSystem ?: ""
                    else -> error("Schema Error: unsupported EI format for output: '$format' in $fieldMapping")
                }
            }
            else -> normalizedValue
        }
        return truncateIfNeeded(formattedValue)
    }

    fun truncateIfNeeded(str: String): String {
        if (maxLength == null) return str // no maxLength is very common
        if (str.isEmpty()) return str
        // Only TEXTy fields can be truncated, and only if a valid maxLength is set in the schema
        return when (type) {
            Type.TEXT,
            Type.TEXT_OR_BLANK,
            Type.STREET,
            Type.STREET_OR_BLANK,
            Type.CITY,
            Type.PERSON_NAME,
            Type.EMAIL -> {
                if (str.length <= maxLength)
                    str
                else
                    str.substring(0, maxLength)
            }
            else -> str
        }
    }

    /**
     * Take a formatted value and check to see if can be stored in a report.
     */
    fun checkForError(formattedValue: String, format: String? = null): String? {
        if (formattedValue.isBlank() && !isOptional && !canBeBlank) return "Blank value for element $fieldMapping"
        return when (type) {
            Type.DATE -> {
                try {
                    LocalDate.parse(formattedValue)
                    return null
                } catch (e: DateTimeParseException) {
                }
                try {
                    val formatter = DateTimeFormatter.ofPattern(format ?: datePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                    return null
                } catch (e: DateTimeParseException) {
                }
                try {
                    val optionalDateTime = variableDateTimePattern
                    val df = DateTimeFormatter.ofPattern(optionalDateTime)
                    val ta = df.parseBest(formattedValue, OffsetDateTime::from, LocalDateTime::from, Instant::from)
                    LocalDate.from(ta)
                    return null
                } catch (e: DateTimeParseException) {
                    "Invalid date: '$formattedValue' for element $fieldMapping"
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
                try {
                    // this is a saving throw
                    val optionalDateTime = variableDateTimePattern
                    val df = DateTimeFormatter.ofPattern(optionalDateTime)
                    val ta = df.parseBest(formattedValue, OffsetDateTime::from, LocalDateTime::from, Instant::from)
                    LocalDateTime.from(ta).atZone(ZoneId.of(USTimeZone.CENTRAL.zoneId)).toOffsetDateTime()
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
                    "Invalid date time: '$formattedValue' for element $fieldMapping"
                }
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                return if (format == altDisplayToken) {
                    if (toAltCode(formattedValue) != null) null else
                        "Invalid code: '$formattedValue' is not a display value in altValues set for $fieldMapping"
                } else {
                    if (valueSetRef == null) error("Schema Error: missing value set for $fieldMapping")
                    when (format) {
                        displayToken ->
                            if (valueSetRef.toCodeFromDisplay(formattedValue) != null) null else
                                "Invalid code: '$formattedValue' not a display value for element $fieldMapping"
                        codeToken -> {
                            val values = altValues ?: valueSetRef.values
                            if (values.find { it.code == formattedValue } != null) null else
                                "Invalid code: '$formattedValue' is not a code value for element $fieldMapping"
                        }
                        else ->
                            if (valueSetRef.toNormalizedCode(formattedValue) != null) null else
                                "Invalid code: '$formattedValue' does not match any codes for $fieldMapping"
                    }
                }
            }
            Type.TELEPHONE -> {
                return try {
                    // parse can fail if the phone number is not correct, which feels like bad behavior
                    // this then causes a report level failure, not an element level failure
                    val number = phoneNumberUtil.parse(formattedValue, "US")
                    if (!number.hasNationalNumber() || number.nationalNumber > 9999999999L)
                        "Invalid phone number '$formattedValue' for $fieldMapping"
                    else
                        null
                } catch (ex: Exception) {
                    "Invalid phone number '$formattedValue' for $fieldMapping"
                }
            }
            Type.POSTAL_CODE -> {
                // Let in all formats defined by http://www.dhl.com.tw/content/dam/downloads/tw/express/forms/postcode_formats.pdf
                return if (!Regex("^[A-Za-z\\d\\- ]{3,12}\$").matches(formattedValue))
                    "Invalid postal code '$formattedValue' for $fieldMapping"
                else
                    null
            }
            Type.HD -> {
                when (format) {
                    null,
                    hdNameToken -> null
                    hdUniversalIdToken -> null
                    hdSystemToken -> null
                    hdCompleteFormat -> {
                        val parts = formattedValue.split(hdDelimiter)
                        if (parts.size == 1 || parts.size == 3) null else "Invalid HD format"
                    }
                    else -> "Unsupported HD format for input: '$format' in $fieldMapping"
                }
            }
            Type.EI -> {
                when (format) {
                    null,
                    eiNameToken -> null
                    eiNamespaceIdToken -> null
                    eiSystemToken -> null
                    eiCompleteFormat -> {
                        val parts = formattedValue.split(eiDelimiter)
                        if (parts.size == 1 || parts.size == 4) null else "Invalid EI format"
                    }
                    else -> "Unsupported EI format for input: '$format' in $fieldMapping"
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
            Type.BLANK -> ""
            Type.DATE -> {
                val normalDate = try {
                    LocalDate.parse(formattedValue)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val formatter = DateTimeFormatter.ofPattern(format ?: datePattern, Locale.ENGLISH)
                    LocalDate.parse(formattedValue, formatter)
                } catch (e: DateTimeParseException) {
                    null
                } ?: try {
                    val optionalDateTime = variableDateTimePattern
                    val df = DateTimeFormatter.ofPattern(optionalDateTime)
                    val ta = df.parseBest(formattedValue, OffsetDateTime::from, LocalDateTime::from, Instant::from)
                    LocalDate.from(ta)
                } catch (e: DateTimeParseException) {
                    error("Invalid date: '$formattedValue' for element $fieldMapping")
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
                    null
                } ?: try {
                    // this is a saving throw
                    val optionalDateTime = variableDateTimePattern
                    val df = DateTimeFormatter.ofPattern(optionalDateTime)
                    val ta = df.parseBest(formattedValue, OffsetDateTime::from, LocalDateTime::from, Instant::from)
                    LocalDateTime.from(ta).atZone(ZoneId.of(USTimeZone.CENTRAL.zoneId)).toOffsetDateTime()
                } catch (e: DateTimeParseException) {
                    error("Invalid date: '$formattedValue' for element $fieldMapping")
                }
                normalDateTime.format(datetimeFormatter)
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                when (format) {
                    altDisplayToken ->
                        toAltCode(formattedValue)
                            ?: error(
                                "Invalid code: '$formattedValue' is not a display value in altValues set " +
                                    "for $fieldMapping"
                            )
                    codeToken ->
                        toCode(formattedValue)
                            ?: error(
                                "Invalid code '$formattedValue' is not a display value in valueSet " +
                                    "for $fieldMapping"
                            )
                    displayToken ->
                        valueSetRef?.toCodeFromDisplay(formattedValue)
                            ?: error(
                                "Invalid code: '$formattedValue' is not a display value " +
                                    "for element $fieldMapping"
                            )
                    else ->
                        valueSetRef?.toNormalizedCode(formattedValue)
                            ?: error(
                                "Invalid code: '$formattedValue' does not match any codes " +
                                    "for $fieldMapping"
                            )
                }
            }
            Type.TELEPHONE -> {
                val number = phoneNumberUtil.parse(formattedValue, "US")
                if (!number.hasNationalNumber() || number.nationalNumber > 9999999999L)
                    error("Invalid phone number '$formattedValue' for $fieldMapping")
                val nationalNumber = DecimalFormat("0000000000").format(number.nationalNumber)
                "${nationalNumber}$phoneDelimiter${number.countryCode}$phoneDelimiter${number.extension}"
            }
            Type.POSTAL_CODE -> {
                // Let in all formats defined by http://www.dhl.com.tw/content/dam/downloads/tw/express/forms/postcode_formats.pdf
                if (!Regex("^[A-Za-z\\d\\- ]{3,12}\$").matches(formattedValue))
                    error("Input Error: invalid postal code '$formattedValue' for $fieldMapping")
                formattedValue.replace(" ", "")
            }
            Type.HD -> {
                when (format) {
                    null,
                    hdCompleteFormat -> {
                        parseHD(formattedValue) // to check
                        formattedValue
                    }
                    hdNameToken -> {
                        val hd = parseHD(formattedValue)
                        hd.name
                    }
                    else -> error("Schema Error: invalid format value")
                }
            }
            Type.EI -> {
                when (format) {
                    null,
                    eiCompleteFormat -> {
                        parseEI(formattedValue) // to check
                        formattedValue
                    }
                    eiNameToken -> {
                        val ei = parseEI(formattedValue)
                        ei.name
                    }
                    else -> error("Schema Error: invalid format value")
                }
            }
            else -> formattedValue
        }
    }

    fun toNormalized(subValues: List<SubValue>): String {
        if (subValues.isEmpty()) return ""
        return when (type) {
            Type.HD -> {
                var name = ""
                var universalId = ""
                var universalIdSystem = "ISO"
                for (subValue in subValues) {
                    when (subValue.format) {
                        null,
                        hdCompleteFormat -> {
                            val hdFields = parseHD(subValue.value)
                            name = hdFields.name
                            if (hdFields.universalId != null) universalId = hdFields.universalId
                            if (hdFields.universalIdSystem != null) universalIdSystem = hdFields.universalIdSystem
                        }
                        hdNameToken -> {
                            name = subValue.value
                        }
                        hdUniversalIdToken -> {
                            universalId = subValue.value
                        }
                        hdSystemToken -> {
                            universalIdSystem = subValue.value
                        }
                    }
                }
                "$name$hdDelimiter$universalId$hdDelimiter$universalIdSystem"
            }
            Type.EI -> {
                var name = ""
                var namespace = ""
                var universalId = ""
                var universalIdSystem = "ISO"
                for (subValue in subValues) {
                    when (subValue.format) {
                        null,
                        eiCompleteFormat -> {
                            val eiFields = parseEI(subValue.value)
                            name = eiFields.name
                            if (eiFields.namespace != null) namespace = eiFields.namespace
                            if (eiFields.universalId != null) universalId = eiFields.universalId
                            if (eiFields.universalIdSystem != null) universalIdSystem = eiFields.universalIdSystem
                        }
                        eiNameToken -> {
                            name = subValue.value
                        }
                        eiNamespaceIdToken -> {
                            namespace = subValue.value
                        }
                        eiUniversalIdToken -> {
                            universalId = subValue.value
                        }
                        eiSystemToken -> {
                            universalIdSystem = subValue.value
                        }
                    }
                }
                "$name$eiDelimiter$namespace$eiDelimiter$universalId$eiDelimiter$universalIdSystem"
            }
            else -> TODO("unsupported type")
        }
    }

    fun toAltDisplay(code: String): String? {
        if (!isCodeType) error("Internal Error: asking for an altDisplay for a non-code type")
        if (altValues == null) error("Schema Error: missing alt values for $fieldMapping")
        val altValue = altValues.find { code.equals(it.code, ignoreCase = true) }
            ?: altValues.find { "*" == it.code }
        return altValue?.display
    }

    fun toAltCode(altDisplay: String): String? {
        if (!isCodeType) error("Internal Error: asking for an altDisplay for a non-code type")
        if (altValues == null) error("Schema Error: missing alt values for $fieldMapping")
        val altValue = altValues.find { altDisplay.equals(it.display, ignoreCase = true) }
            ?: altValues.find { "*" == it.display }
        return altValue?.code
    }

    /**
     * Convert a string [code] to the code in the element's valueset.
     * @return a code of null if the code is not found
     */
    fun toCode(code: String): String? {
        if (!isCodeType) error("Internal Error: asking for codeValue for a non-code type")
        // if there are alt values, use those, otherwise, use the valueSet
        val values = valueSetRef?.values ?: error("Unable to find a value set for $fieldMapping.")
        val codeValue = values.find {
            code.equals(it.code, ignoreCase = true) || code.equals(it.replaces, ignoreCase = true)
        } ?: values.find { "*" == it.code }
        return codeValue?.code
    }

    companion object {
        const val datePattern = "yyyyMMdd"
        const val datetimePattern = "yyyyMMddHHmmZZZ"
        const val variableDateTimePattern = "[yyyyMMddHHmmssZ][yyyyMMddHHmmZ][yyyyMMddHHmmss]"
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH)
        val datetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datetimePattern, Locale.ENGLISH)
        const val displayToken = "\$display"
        const val caretToken = "\$code^\$display^\$system"
        const val codeToken = "\$code"
        const val systemToken = "\$system"
        const val altDisplayToken = "\$alt"
        const val areaCodeToken = "\$area"
        const val exchangeToken = "\$exchange"
        const val subscriberToken = "\$subscriber"
        const val countryCodeToken = "\$country"
        const val extensionToken = "\$extension"
        const val e164Token = "\$e164"
        const val defaultPhoneFormat = "\$area\$exchange\$subscriber"
        const val phoneDelimiter = ":"
        const val hdDelimiter = "^"
        const val hdNameToken = "\$name"
        const val hdUniversalIdToken = "\$universalId"
        const val hdSystemToken = "\$system"
        const val hdCompleteFormat = "\$complete"
        const val eiDelimiter = "^"
        const val eiNameToken = "\$name"
        const val eiNamespaceIdToken = "\$namespaceId"
        const val eiUniversalIdToken = "\$universalId"
        const val eiSystemToken = "\$system"
        const val eiCompleteFormat = "\$complete"
        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
        const val zipFiveToken = "\$zipFive"
        const val zipFivePlusFourToken = "\$zipFivePlusFour"
        const val usZipFormat = """^(\d{5})[- ]?(\d{4})?$"""
        const val zipDefaultFormat = zipFiveToken

        fun csvFields(name: String, format: String? = null): List<CsvField> {
            return listOf(CsvField(name, format))
        }

        fun parseHD(value: String, maximumLength: Int? = null): HDFields {
            val parts = value.split(hdDelimiter)
            val namespace = parts[0].let {
                if (maximumLength == null || maximumLength > it.length) {
                    it
                } else {
                    it.substring(0, maximumLength)
                }
            }
            return when (parts.size) {
                3 -> HDFields(namespace, parts[1], parts[2])
                1 -> HDFields(namespace, universalId = null, universalIdSystem = null)
                else -> error("Internal Error: Invalid HD value '$value'")
            }
        }

        fun parseEI(value: String): EIFields {
            val parts = value.split(eiDelimiter)
            return when (parts.size) {
                4 -> EIFields(parts[0], parts[1], parts[2], parts[3])
                1 -> EIFields(parts[0], namespace = null, universalId = null, universalIdSystem = null)
                else -> error("Internal Error: Invalid EI value '$value'")
            }
        }
    }
}