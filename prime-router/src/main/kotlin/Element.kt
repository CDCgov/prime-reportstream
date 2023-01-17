package gov.cdc.prime.router

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import gov.cdc.prime.router.Element.Cardinality.ONE
import gov.cdc.prime.router.Element.Cardinality.ZERO_OR_ONE
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.DateUtilities.asFormattedString
import gov.cdc.prime.router.common.DateUtilities.toOffsetDateTime
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.metadata.ElementAndValue
import gov.cdc.prime.router.metadata.LIVDLookupMapper
import gov.cdc.prime.router.metadata.LookupMapper
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.metadata.Mapper
import org.apache.commons.lang3.StringUtils
import java.text.DecimalFormat
import java.time.LocalDate

class AltValueNotDefinedException(message: String) : IllegalStateException(message)

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
 * To describe the intent of an element there are references to the national standards.
 */
data class Element(
    // An element can either be a new element or one based on previously defined element
    // - A name of form [A-Za-z0-9_]+ is a new element
    // - A name of form [A-Za-z0-9_]+.[A-Za-z0-9_]+ is an element based on a previously defined element
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
    val defaultOverridesValue: Boolean? = null,
    val mapper: String? = null,
    val mapperOverridesValue: Boolean? = null,
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

    // used to be able to send blank values for fields that get validated/normalized
    // in serializers.
    // for instance, a badly formatted yet optional date field.
    val nullifyValue: Boolean = false
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
        BLANK
    }

    data class CsvField(
        val name: String,
        val format: String?
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

    /**
     * An element can have subfields, for example when more than CSV field makes up a single element.
     * See ElementTests for an example.
     **/
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

    val isOptional
        get() = this.cardinality == null ||
            this.cardinality == Cardinality.ZERO_OR_ONE || canBeBlank

    val canBeBlank
        get() = type == Type.TEXT_OR_BLANK ||
            type == Type.STREET_OR_BLANK ||
            type == Type.TABLE_OR_BLANK ||
            type == Type.BLANK

    /**
     * True if this element has a table lookup.
     */
    val isTableLookup get() = mapperRef != null && type == Type.TABLE

    /**
     * String showing the external field name(s) if any and the element name.
     */
    val fieldMapping: String get() {
        return when {
            !csvFields.isNullOrEmpty() -> "${csvFields.map { it.name }.joinToString(",")} ($name)"
            !hl7Field.isNullOrBlank() -> "$hl7Field ($name)"
            !hl7OutputFields.isNullOrEmpty() -> "${hl7OutputFields.joinToString(",")}} ($name)"
            else -> "($name)"
        }
    }

    fun inheritFrom(baseElement: Element): Element {
        return Element(
            name = this.name,
            type = this.type ?: baseElement.type,
            valueSet = this.valueSet ?: baseElement.valueSet,
            valueSetRef = this.valueSetRef ?: baseElement.valueSetRef,
            altValues = this.altValues ?: baseElement.altValues,
            table = this.table ?: baseElement.table,
            tableColumn = this.tableColumn ?: baseElement.tableColumn,
            cardinality = this.cardinality ?: baseElement.cardinality,
            pii = this.pii ?: baseElement.pii,
            phi = this.phi ?: baseElement.phi,
            maxLength = this.maxLength ?: baseElement.maxLength,
            mapper = this.mapper ?: baseElement.mapper,
            mapperOverridesValue = this.mapperOverridesValue ?: baseElement.mapperOverridesValue,
            default = this.default ?: baseElement.default,
            defaultOverridesValue = this.defaultOverridesValue ?: baseElement.defaultOverridesValue,
            reference = this.reference ?: baseElement.reference,
            referenceUrl = this.referenceUrl ?: baseElement.referenceUrl,
            hhsGuidanceField = this.hhsGuidanceField ?: baseElement.hhsGuidanceField,
            natFlatFileField = this.natFlatFileField ?: baseElement.natFlatFileField,
            hl7Field = this.hl7Field ?: baseElement.hl7Field,
            hl7OutputFields = this.hl7OutputFields ?: baseElement.hl7OutputFields,
            hl7AOEQuestion = this.hl7AOEQuestion ?: baseElement.hl7AOEQuestion,
            documentation = this.documentation ?: baseElement.documentation,
            csvFields = this.csvFields ?: baseElement.csvFields,
            delimiter = this.delimiter ?: baseElement.delimiter
        )
    }

    /**
     * Generate validation error messages if this element is not valid.
     * @return a list of error messages, or an empty list if no errors
     */
    fun validate(): List<String> {
        val errorList = mutableListOf<String>()

        /**
         * Add an error [message].
         */
        fun addError(message: String) {
            errorList.add("Element $name - $message.")
        }

        // All elements require a type
        if (type == null) addError("requires an element type.")

        // Table lookups require a table
        if ((mapperRef?.name == LookupMapper().name || mapperRef?.name == LIVDLookupMapper().name) &&
            (tableRef == null || tableColumn.isNullOrBlank())
        ) {
            addError("requires a table and table column.")
        }

        // Elements of type table need a table ref
        if ((type == Type.TABLE || type == Type.TABLE_OR_BLANK || !tableColumn.isNullOrBlank()) && tableRef == null) {
            addError("requires a table.")
        }

        // Elements with mapper parameters require a mapper
        if ((mapperOverridesValue == true || !mapperArgs.isNullOrEmpty()) && mapperRef == null) {
            addError("has mapper related parameters, but no mapper.")
        }

        // Elements that can be blank should not have a default
        if (canBeBlank && default != null) {
            addError("has a default specified, but can be blank")
        }

        return errorList
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
        // trim the normalized value down to null and if it is null return empty string
        val cleanedNormalizedValue = normalizedValue.trimToNull() ?: return ""
        val formattedValue = when (type) {
            // sometimes you just need to send through an empty column
            Type.BLANK -> ""
            Type.DATETIME -> {
                try {
                    // parse the date back out from the normalized value
                    val ta = DateUtilities.parseDate(cleanedNormalizedValue)
                    // output the date time value as a formatted string, either using the format
                    // provided or defaulting to the date time pattern
                    DateUtilities.getDateAsFormattedString(ta, format ?: DateUtilities.datetimePattern)
                } catch (_: Throwable) {
                    cleanedNormalizedValue
                }
            }
            Type.DATE -> {
                try {
                    // parse the date back out from the normalized value
                    val ta = DateUtilities.parseDate(cleanedNormalizedValue)
                    // output the date value as a formatted string, either using the format
                    // provided or defaulting to the date time pattern
                    DateUtilities.getDateAsFormattedString(ta, format ?: DateUtilities.datePattern)
                } catch (_: Throwable) {
                    cleanedNormalizedValue
                }
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                when (format) {
                    // TODO Revisit: there may be times that normalizedValue is not an altValue
                    altDisplayToken ->
                        toAltDisplay(cleanedNormalizedValue)
                            ?: throw AltValueNotDefinedException(
                                "Outgoing receiver schema problem:" +
                                    " '$cleanedNormalizedValue' is not in altValues set for $fieldMapping."
                            )
                    codeToken ->
                        toCode(cleanedNormalizedValue)
                            ?: error(
                                "Schema Error: " +
                                    "'$cleanedNormalizedValue' is not in valueSet " +
                                    "'$valueSet' for $fieldMapping/'$format'. " +
                                    "\nAvailable values are " +
                                    "${valueSetRef?.values?.joinToString { "${it.code} -> ${it.display}" }}" +
                                    "\nAlt values (${altValues?.count()}) are " +
                                    "${altValues?.joinToString { "${it.code} -> ${it.display}" }}"
                            )
                    caretToken -> {
                        val display = valueSetRef?.toDisplayFromCode(cleanedNormalizedValue)
                            ?: error("Internal Error: '$cleanedNormalizedValue' cannot be formatted for $fieldMapping")
                        "$cleanedNormalizedValue^$display^${valueSetRef.systemCode}"
                    }
                    displayToken -> {
                        valueSetRef?.toDisplayFromCode(cleanedNormalizedValue)
                            ?: error("Internal Error: '$cleanedNormalizedValue' cannot be formatted for $fieldMapping")
                    }
                    systemToken -> {
                        // Very confusing, but this special case is in the HHS Guidance Confluence page
                        if (valueSetRef?.name == "hl70136" && cleanedNormalizedValue == "UNK") {
                            "NULLFL"
                        } else {
                            valueSetRef?.systemCode ?: error("valueSetRef for $valueSet is null!")
                        }
                    }
                    else -> cleanedNormalizedValue
                }
            }
            Type.TELEPHONE -> {
                // normalized telephone always has 3 values national:country:extension
                val parts = if (cleanedNormalizedValue.contains(phoneDelimiter)) {
                    cleanedNormalizedValue.split(phoneDelimiter)
                } else {
                    // remove parens from HL7 formatting
                    listOf(
                        cleanedNormalizedValue
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
                        val matchResult = Regex(usZipFormat).matchEntire(cleanedNormalizedValue)
                        matchResult?.groupValues?.get(1)
                            ?: cleanedNormalizedValue.padStart(5, '0')
                    }
                    zipFivePlusFourToken -> {
                        // If this a US zip, either 5 or 9 digits depending on the value
                        val matchResult = Regex(usZipFormat).matchEntire(cleanedNormalizedValue)
                        if (matchResult != null && matchResult.groups[2] == null) {
                            matchResult.groups[1]?.value ?: ""
                        } else if (matchResult != null && matchResult.groups[2] != null) {
                            "${matchResult.groups[1]?.value}-${matchResult.groups[2]?.value}"
                        } else {
                            cleanedNormalizedValue.padStart(5, '0')
                        }
                    }
                    else -> cleanedNormalizedValue.padStart(5, '0')
                }
            }
            Type.HD -> {
                val hdFields = parseHD(cleanedNormalizedValue)
                when (format) {
                    null,
                    hdNameToken -> hdFields.name
                    hdUniversalIdToken -> hdFields.universalId ?: ""
                    hdSystemToken -> hdFields.universalIdSystem ?: ""
                    else -> error("Schema Error: unsupported HD format for output: '$format' in $fieldMapping")
                }
            }
            Type.EI -> {
                val eiFields = parseEI(cleanedNormalizedValue)
                when (format) {
                    null,
                    eiNameToken -> eiFields.name
                    eiNamespaceIdToken -> eiFields.namespace ?: ""
                    eiUniversalIdToken -> eiFields.universalId ?: ""
                    eiSystemToken -> eiFields.universalIdSystem ?: ""
                    else -> error("Schema Error: unsupported EI format for output: '$format' in $fieldMapping")
                }
            }
            else -> cleanedNormalizedValue
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
                if (str.length <= maxLength) {
                    str
                } else {
                    str.substring(0, maxLength)
                }
            }
            else -> str
        }
    }

    /**
     * Take a formatted value and check to see if it can be stored in a report.
     */
    fun checkForError(formattedValue: String, format: String? = null): ActionLogDetail? {
        // remove trailing spaces
        val cleanedValue = StringUtils.trimToNull(formattedValue)
        if (cleanedValue == null && !isOptional && !canBeBlank) return MissingFieldMessage(fieldMapping)

        return when (type) {
            // in this case, we can try to parse both a date and a date time
            // the date utilities method will handle either and if it succeeds, it's valid
            Type.DATE, Type.DATETIME -> {
                try {
                    DateUtilities.parseDate(cleanedValue)
                    null
                } catch (_: Throwable) {
                    if (nullifyValue) {
                        null
                    } else {
                        InvalidDateMessage(cleanedValue, fieldMapping, format)
                    }
                }
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                return if (format == altDisplayToken) {
                    if (toAltCode(cleanedValue) != null) null else {
                        InvalidCodeMessage(cleanedValue, fieldMapping, format)
                    }
                } else {
                    if (valueSetRef == null) error("Schema Error: missing value set for $fieldMapping")
                    when (format) {
                        displayToken ->
                            if (valueSetRef.toCodeFromDisplay(cleanedValue) != null) null else {
                                InvalidCodeMessage(cleanedValue, fieldMapping, format)
                            }
                        codeToken -> {
                            val values = altValues ?: valueSetRef.values
                            if (values.find { it.code == cleanedValue } != null) null else {
                                InvalidCodeMessage(cleanedValue, fieldMapping, format)
                            }
                        }
                        else ->
                            if (valueSetRef.toNormalizedCode(cleanedValue) != null) null else {
                                InvalidCodeMessage(cleanedValue, fieldMapping, format)
                            }
                    }
                }
            }
            Type.TELEPHONE -> {
                return checkPhoneNumber(cleanedValue, fieldMapping)
            }
            Type.POSTAL_CODE -> {
                // Let in all formats defined by http://www.dhl.com.tw/content/dam/downloads/tw/express/forms/postcode_formats.pdf
                return if (!Regex("^[A-Za-z\\d\\- ]{3,12}\$").matches(cleanedValue)) {
                    InvalidPostalMessage(cleanedValue, fieldMapping, format)
                } else {
                    null
                }
            }
            Type.HD -> {
                when (format) {
                    null,
                    hdNameToken -> null
                    hdUniversalIdToken -> null
                    hdSystemToken -> null
                    hdCompleteFormat -> {
                        val parts = cleanedValue.split(hdDelimiter)
                        if (parts.size == 1 || parts.size == 3) null else UnsupportedHDMessage(format, fieldMapping)
                    }
                    else -> UnsupportedHDMessage(format, fieldMapping)
                }
            }
            Type.EI -> {
                when (format) {
                    null,
                    eiNameToken -> null
                    eiNamespaceIdToken -> null
                    eiSystemToken -> null
                    eiCompleteFormat -> {
                        val parts = cleanedValue.split(eiDelimiter)
                        if (parts.size == 1 || parts.size == 4) null else UnsupportedEIMessage(format, fieldMapping)
                    }
                    else -> UnsupportedEIMessage(format, fieldMapping)
                }
            }

            else -> null
        }
    }

    /**
     * Take a formatted value and turn into a normalized value stored in a report
     * @param formattedValue the value to normalize
     * @param format optional format value specific to some Element types
     * @throws ElementNormalizeException
     */
    fun toNormalized(formattedValue: String, format: String? = null): String {
        val cleanedFormattedValue = formattedValue.trim()
        if (cleanedFormattedValue.isEmpty()) return ""
        return when (type) {
            Type.BLANK -> ""
            Type.DATE -> {
                try {
                    DateUtilities
                        .parseDate(cleanedFormattedValue)
                        .asFormattedString(DateUtilities.datePattern)
                } catch (_: Throwable) {
                    if (nullifyValue) {
                        ""
                    } else {
                        throw ElementNormalizeException(
                            "Invalid date: '$cleanedFormattedValue' for format '$format' for element $fieldMapping",
                            fieldMapping,
                            cleanedFormattedValue,
                            ErrorCode.INVALID_MSG_PARSE_DATE
                        )
                    }
                }
            }
            Type.DATETIME -> {
                try {
                    DateUtilities
                        .parseDate(cleanedFormattedValue)
                        .toOffsetDateTime()
                        .asFormattedString(DateUtilities.datetimePattern)
                } catch (_: Throwable) {
                    if (nullifyValue) {
                        ""
                    } else {
                        throw ElementNormalizeException(
                            "Invalid date time: '$cleanedFormattedValue' " +
                                "for format '$format' for element $fieldMapping",
                            fieldMapping,
                            cleanedFormattedValue,
                            ErrorCode.INVALID_MSG_PARSE_DATETIME
                        )
                    }
                }
            }
            Type.CODE -> {
                // First, prioritize use of a local $alt format, even if no value set exists.
                when (format) {
                    altDisplayToken ->
                        toAltCode(cleanedFormattedValue)
                            ?: throw ElementNormalizeException(
                                "Invalid code: '$cleanedFormattedValue' is not a display value in altValues set ",
                                fieldMapping,
                                cleanedFormattedValue,
                                ErrorCode.INVALID_MSG_PARSE_CODE_ALT_VALUES
                            )
                    codeToken ->
                        toCode(cleanedFormattedValue)
                            ?: throw ElementNormalizeException(
                                "Invalid code '$cleanedFormattedValue' is not a display value in valueSet " +
                                    "for $fieldMapping",
                                fieldMapping,
                                cleanedFormattedValue,
                                ErrorCode.INVALID_MSG_PARSE_CODE_VALUES
                            )
                    displayToken ->
                        valueSetRef?.toCodeFromDisplay(cleanedFormattedValue)
                            ?: throw ElementNormalizeException(
                                "Invalid code: '$cleanedFormattedValue' is not a display value " +
                                    "for element $fieldMapping",
                                fieldMapping,
                                cleanedFormattedValue,
                                ErrorCode.INVALID_MSG_PARSE_ELEMENT_CODE
                            )
                    else ->
                        valueSetRef?.toNormalizedCode(cleanedFormattedValue)
                            ?: throw ElementNormalizeException(
                                "Invalid code: '$cleanedFormattedValue' does not match any codes " +
                                    "for $fieldMapping",
                                fieldMapping,
                                cleanedFormattedValue,
                                ErrorCode.INVALID_MSG_PARSE_CODE
                            )
                }
            }
            Type.TELEPHONE -> {
                try {
                    val number = phoneNumberUtil.parse(cleanedFormattedValue, "US")
                    if (!number.hasNationalNumber() || number.nationalNumber > 999999999999L) {
                        throw ElementNormalizeException(
                            "Invalid phone number '$cleanedFormattedValue' for $fieldMapping",
                            fieldMapping,
                            cleanedFormattedValue,
                            ErrorCode.INVALID_MSG_PARSE_TELEPHONE
                        )
                    }
                    val nationalNumber = DecimalFormat("0000000000").format(number.nationalNumber)
                    "${nationalNumber}$phoneDelimiter${number.countryCode}$phoneDelimiter${number.extension}"
                } catch (e: NumberParseException) {
                    throw ElementNormalizeException(
                        e.localizedMessage,
                        fieldMapping,
                        cleanedFormattedValue,
                        ErrorCode.INVALID_MSG_PARSE_TELEPHONE
                    )
                }
            }
            Type.POSTAL_CODE -> {
                // Let in all formats defined by http://www.dhl.com.tw/content/dam/downloads/tw/express/forms/postcode_formats.pdf
                if (!Regex("^[A-Za-z\\d\\- ]{3,12}\$").matches(cleanedFormattedValue)) {
                    throw ElementNormalizeException(
                        "Input Error: invalid postal code '$cleanedFormattedValue' for $fieldMapping",
                        fieldMapping,
                        cleanedFormattedValue,
                        ErrorCode.INVALID_MSG_PARSE_POSTAL_CODE
                    )
                }
                cleanedFormattedValue.replace(" ", "")
            }
            Type.HD -> {
                try {
                    when (format) {
                        null,
                        hdCompleteFormat -> {
                            parseHD(cleanedFormattedValue) // to check
                            cleanedFormattedValue
                        }
                        hdNameToken -> {
                            val hd = parseHD(cleanedFormattedValue)
                            hd.name
                        }
                        else -> throw ElementNormalizeException(
                            "Schema Error: invalid format value",
                            fieldMapping,
                            cleanedFormattedValue,
                            ErrorCode.INVALID_MSG_PARSE_HD
                        )
                    }
                } catch (e: IllegalStateException) {
                    throw ElementNormalizeException(
                        e.localizedMessage,
                        fieldMapping,
                        cleanedFormattedValue,
                        ErrorCode.INVALID_MSG_PARSE_HD
                    )
                }
            }
            Type.EI -> {
                try {
                    when (format) {
                        null,
                        eiCompleteFormat -> {
                            parseEI(cleanedFormattedValue) // to check
                            cleanedFormattedValue
                        }
                        eiNameToken -> {
                            val ei = parseEI(cleanedFormattedValue)
                            ei.name
                        }
                        else -> throw ElementNormalizeException(
                            "Schema Error: invalid format value",
                            fieldMapping,
                            cleanedFormattedValue,
                            ErrorCode.INVALID_MSG_PARSE_EI
                        )
                    }
                } catch (e: IllegalStateException) {
                    throw ElementNormalizeException(
                        e.localizedMessage,
                        fieldMapping,
                        cleanedFormattedValue,
                        ErrorCode.INVALID_MSG_PARSE_EI
                    )
                }
            }
            else -> cleanedFormattedValue
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
        if (!isCodeType) throw ElementNormalizeException(
            "Internal Error: asking for an altDisplay for a non-code type",
            fieldMapping,
            code,
            ErrorCode.INVALID_MSG_PARSE_CODE_ALT_VALUES
        )
        if (altValues == null) throw ElementNormalizeException(
            "Schema Error: missing alt values for $fieldMapping",
            fieldMapping,
            code,
            ErrorCode.INVALID_MSG_PARSE_CODE_ALT_VALUES
        )
        val altValue = altValues.find { code.equals(it.code, ignoreCase = true) }
            ?: altValues.find { "*" == it.code }
        return altValue?.display
    }

    fun toAltCode(altDisplay: String): String? {
        if (!isCodeType) throw ElementNormalizeException(
            "Internal Error: asking for an altDisplay for a non-code type",
            fieldMapping,
            altDisplay,
            ErrorCode.INVALID_MSG_PARSE_CODE_ALT_VALUES
        )
        if (altValues == null) throw ElementNormalizeException(
            "Schema Error: missing alt values for $fieldMapping",
            fieldMapping,
            altDisplay,
            ErrorCode.INVALID_MSG_PARSE_CODE_ALT_VALUES
        )
        val altValue = altValues.find { altDisplay.equals(it.display, ignoreCase = true) }
            ?: altValues.find { "*" == it.display }
        return altValue?.code
    }

    /**
     * Convert a string [code] to the code in the element's valueset.
     * @return a code of null if the code is not found
     */
    fun toCode(code: String): String? {
        if (!isCodeType) throw ElementNormalizeException(
            "Internal Error: asking for codeValue for a non-code type",
            fieldMapping,
            code,
            ErrorCode.INVALID_MSG_PARSE_CODE_VALUES
        )
        // if there are alt values, use those, otherwise, use the valueSet
        val values = valueSetRef?.values ?: throw ElementNormalizeException(
            "Unable to find a value set for $fieldMapping.",
            fieldMapping,
            code,
            ErrorCode.INVALID_MSG_PARSE_CODE_VALUES
        )
        val codeValue = values.find {
            code.equals(it.code, ignoreCase = true) || code.equals(it.replaces, ignoreCase = true)
        } ?: values.find { "*" == it.code }
        return codeValue?.code
    }

    /**
     * Determines if an element needs to use a mapper given the [elementValue].
     * @return true if a mapper needs to be run
     */
    fun useMapper(elementValue: String?): Boolean {
        val overrideValue = mapperOverridesValue != null && mapperOverridesValue
        return mapperRef != null && (overrideValue || elementValue.isNullOrBlank())
    }

    /**
     * Determines if an element needs to use a default given the [elementValue].
     * @return true if a default needs to be used
     */
    fun useDefault(elementValue: String?): Boolean {
        val overrideValue = defaultOverridesValue != null && defaultOverridesValue
        return overrideValue || elementValue.isNullOrBlank()
    }

    /**
     * Determine the value for this element based on the schema configuration.  This function checks if a
     * mapper needs to be run or if a default needs to be applied.
     * @param allElementValues the values for all other elements which are updated as needed
     * @param schema the schema
     * @param defaultOverrides element name and value pairs of defaults that override schema defaults
     * @param itemIndex the index of the item from a report being processed
     * @param sender Sender who submitted the data.  Can be null if called at a point in code where its not known
     * @return a mutable set with the processed value or empty string
     */
    fun processValue(
        allElementValues: Map<String, String>,
        schema: Schema,
        defaultOverrides: Map<String, String> = emptyMap(),
        itemIndex: Int,
        sender: Sender? = null
    ): ElementResult {
        check(itemIndex > 0) { "Item index was $itemIndex, but must be larger than 0" }
        val retVal = ElementResult(if (allElementValues[name].isNullOrEmpty()) "" else allElementValues[name]!!)
        if (useMapper(retVal.value) && mapperRef != null) {
            // This gets the required value names, then gets the value from mappedRows that has the data
            val args = mapperArgs ?: emptyList()
            val valueNames = mapperRef.valueNames(this, args)
            val valuesForMapper = valueNames.mapNotNull { elementName ->
                if (elementName.contains("$")) {
                    tokenizeMapperValue(elementName, itemIndex)
                } else {
                    val valueElement = schema.findElement(elementName)
                    if (valueElement != null && allElementValues.containsKey(elementName) &&
                        !allElementValues[elementName].isNullOrEmpty()
                    ) {
                        ElementAndValue(valueElement, allElementValues[elementName]!!)
                    } else {
                        null
                    }
                }
            }
            // Only overwrite an existing value if the mapper returns a string
            val mapperResult = mapperRef.apply(this, args, valuesForMapper, sender)
            val value = mapperResult.value
            if (!value.isNullOrBlank() && value != "null") {
                retVal.value = value
            }

            // Add any errors or warnings.  Use warnings as errors for required fields.
            if (this.isOptional) {
                retVal.warnings.addAll(mapperResult.errors)
                retVal.warnings.addAll(mapperResult.warnings)
            } else if (mapperResult.errors.isNotEmpty()) {
                retVal.errors.addAll(mapperResult.errors)
                retVal.warnings.addAll(mapperResult.warnings)
            } else {
                retVal.errors.addAll(mapperResult.warnings)
            }
        }

        // Finally, add a default value or empty string to elements that still have a null value.
        // Confusing: default values can be provided in the URL ("defaultOverrides"), or in the schema, or both.
        // Normally, default values are only apply if the value is blank at this point in the code.
        // However, if the Element has defaultOverridesValue=true set, that forces this code to run.
        // todo get rid of defaultOverrides in the URL.  I think its always an empty map!
        if (useDefault(retVal.value)) {
            retVal.value = if (defaultOverrides.containsKey(name)) { // First the URL default is used if it exists.
                defaultOverrides[name] ?: ""
            } else if (!default.isNullOrBlank()) { // otherwise, use the default in the schema
                default
            } else {
                // Check for cardinality and force the value to be empty/blank.
                if (retVal.value.isNullOrBlank() && !isOptional) {
                    retVal.errors += MissingFieldMessage(fieldMapping)
                }
                ""
            }
        }

        return retVal
    }

    /**
     * Populates the value of a specialized mapper token, indicated by a $ prefix
     * @param elementName the token name
     * @param index optional int value used with the $index token
     */
    fun tokenizeMapperValue(elementName: String, index: Int = 0): ElementAndValue {
        val tokenElement = Element(elementName)
        var retVal = ElementAndValue(tokenElement, "")
        when {
            elementName == "\$index" -> {
                retVal = ElementAndValue(tokenElement, index.toString())
            }
            elementName == "\$currentDate" -> {
                val currentDate = LocalDate.now().format(DateUtilities.dateFormatter)
                retVal = ElementAndValue(tokenElement, currentDate)
            }
            elementName.contains("\$mode:") -> {
                retVal = ElementAndValue(tokenElement, elementName.split(":")[1])
            }
            elementName.contains("\$string:") -> {
                retVal = ElementAndValue(tokenElement, elementName.split(":")[1])
            }
        }

        return retVal
    }

    companion object {
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
        // A regex to check for the presence of only valid phone number characters. This will fail if
        // someone passes through character values, like a name, or some other text info. This checks for
        // proper format which is crucial when parsing data.
        /**
         * Breaking down the regex
         * The following regex is broken down in three main parts
         * Regex("""(country code) phone number (extension number)""")
         * ----------------------------------------------------------------------------------------------------
         * Country Code: (\+\d{1,4}(\s|-)?)?
         * Allows for 1 to 4 digits.
         * Example: +1 or +91
         * It takes up to 4 digits, this prepares our codebase for future addition of country codes that have 4 digits.
         * The country region needs to be in the phoneRegions list, if not, it will error out.
         * ----------------------------------------------------------------------------------------------------
         * Phone Number: \(?(\d{1,3})\)?(\s|-)?(\d{3,4})(\s|-)?(\d{3,4})(\s|-)?
         * This takes into account international numbers and US numbers.
         * Examples:
         * 2 9667 9111 and 491 578 888 for AU. (complete: +61 2 9667 9111 and +61 491 578 888)
         * (213) 353-4836 for US. (complete: +1 (213) 353-4836)
         * ----------------------------------------------------------------------------------------------------
         * Extension Number: ((x|ext\.|ext|#)(\s|-)?\d+)?
         * Allows for extension number for any international or domestic phone number with the above regex format
         * (NOTE: no length restriction). To add an extension, the phone number needs to use the
         * following annotations: (#, x, ext, or ext.).
         * Examples:
         * (213) 353-4836 ext. 1234 for US (complete: +1 (213) 353-4836 ext. 1234)
         * 2-9667-9111 x 1234 for AU (complete: +61 2-9667-9111 x 1234)
         * One thing to consider is that THERE IS NO RESTRICTION ON THE LENGTH OF THE EXTENSION, this is something
         * that we might have to change in the future.
         * ----------------------------------------------------------------------------------------------------
         * NOTE: (\s|-)?
         * Gives the option of using a space or a dash, but it is not mandatory.
         * This allows for many permutations of phone numbers.
         * Examples:
         * 213-353 4836
         * 213-3534836x1234
         * 213-353-4836 x 1234
         * The idea here is to narrow down on only phone numbers but to expand on the permutations.
         * ----------------------------------------------------------------------------------------------------
         * Formats that will not match the regex
         * Examples:
         * Any international number that their country code is not on the phoneregions list
         * +503 (555) 123-0987 for ES
         * +55 11-2541-3652 for BR
         * Any non-numberic entry
         * adfadljl;lj
         * j;ljoudfg
         * Any alpha-numberic that doesn't follow the regex
         * adged456ljlij - ere45
         * (ab)987-ab45
         * (1234) 555-5555
         * 123-555-123456
         * Phone numbers that have partial extension format
         * (213) 353-4836 ext.
         * (213) 353-4836x
         * (213) 353-4836 4521
         * (213) 353-48364521
         */
        private val maybeAPhoneNumber = Regex(
            """(\+\d{1,4}(\s|-)?)?\(?(\d{1,3})\)?(\s|-)?(\d{3,4})(\s|-)?(\d{3,4})(\s|-)?((x|ext\.|ext|#)(\s|-)?\d+)?"""
        )

        // possible regions a phone number could be from. This is a wider list than we will we probably
        // pull from, at least initially, because there are many more places in North America that use
        // the +1 country code for international phone numbers, and therefore, our system could break if
        // someone presents a number from the Caribbean and we're set up to allow it
        val phoneRegions = listOf(
            "US",
            "MX",
            "CA",
            "AU", // Australia
            "AG", // Antigua
            "AI", // Anguilla
            "AS", // American Samoa
            "BB", // Barbados
            "BM", // Bermuda
            "BS", // Bahamas
            "DM", // Dominica
            "DO", // Dominican Republic
            "GD", // Grenada
            "GU", // Guam
            "JM", // Jamaica
            "KN", // St. Kitts and Nevis
            "KY", // Cayman Islands
            "LC", // St. Lucia
            "MP", // Northern Mariana Islands
            "MS", // Montserrat
            "PR", // Puerto Rico
            "SX", // Sint Maarten
            "TC", // Turks and Caicos
            "TT", // Trinidad and Tobago
            "VC", // Saint Vincent and the Grenadines
            "VG", // British Virgin Islands
            "VI", // Virgin Islands
            "UM" // US Minor Outlying Islands
        )

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

        // TODO: Look into using PhoneUtilities.kt instead of having this phone checking here. Mo wanted to check
        //  into if the 'maybeAPhoneNumber' regex is even still needed. It appears the 'default region' list is
        //  not needed, as in a real use case it never gets past 'US' since it is just assigning default region
        //  if there is not a country code. Leaving this here for now (9/22/2022) for future tech debt evaluation
        /**
         * Given [cleanedValue] this method checks to see if it is possibly a phone number in a safe
         * way without blowing up all the processing of rows in a report. If the phone number is probably
         * valid it returns null, as the `checkForErrors` method would expect, otherwise it returns an
         * InvalidPhoneMessage
         */
        fun checkPhoneNumber(cleanedValue: String, fieldMapping: String): InvalidPhoneMessage? {
            // use a quick regex to see if the normalized value contains something other than our
            // expected values, and also use the `isPossibleNumber` method our phone number util gives us
            if (
                maybeAPhoneNumber.matchEntire(cleanedValue) != null &&
                phoneNumberUtil.isPossibleNumber(cleanedValue, "US")
            ) {
                // attempt to parse the number. if it is parseable then we can return null and move
                // on with our work, otherwise, return an InvalidPhoneMessage
                val phoneNumber = tryParsePhoneNumber(cleanedValue)
                if (phoneNumber != null) {
                    return null
                }
            }
            // all attempts have failed. bad number
            return InvalidPhoneMessage(cleanedValue, fieldMapping)
        }

        /**
         * Given a nullable [cleanedValue] this method tries to parse a phone number in
         * a safe way according to our four most common phone regions as following:
         *      "US",  // United states
         *      "MX",  // Mexico
         *      "CA",  // Canada
         *      "AU", // Australia
         *      "AG", // Antigua
         *      "AI", // Anguilla
         *      "AS", // American Samoa
         *      "BB", // Barbados
         *      "BM", // Bermuda
         *      "BS", // Bahamas
         *      "DM", // Dominica
         *      "DO", // Dominican Republic
         *      "GD", // Grenada
         *      "GU", // Guam
         *      "JM", // Jamaica
         *      "KN", // St. Kitts and Nevis
         *      "KY", // Cayman Islands
         *      "LC", // St. Lucia
         *      "MP", // Northern Mariana Islands
         *      "MS", // Montserrat
         *      "PR", // Puerto Rico
         *      "SX", // Sint Maarten
         *      "TC", // Turks and Caicos
         *      "TT", // Trinidad and Tobago
         *      "VC", // Saint Vincent and the Grenadines
         *      "VG", // British Virgin Islands
         *      "VI", // Virgin Islands
         *      "UM", // US Minor Outlying Islands
         * If the number really can't be parsed at all (and the phoneNumberUtil is very lenient) then
         * it is almost certainly not a phone number, so return null
         */
        fun tryParsePhoneNumber(cleanedValue: String?): Phonenumber.PhoneNumber? {
            for (region in phoneRegions) {
                try {
                    return phoneNumberUtil.parse(cleanedValue, region)
                } catch (_: NumberParseException) {
                    continue
                }
            }

            return null
        }
    }
}

/**
 * A result for a given element with a [value] that may include [errors] or [warnings].
 */
data class ElementResult(
    var value: String?,
    val errors: MutableList<ActionLogDetail> = mutableListOf(),
    val warnings: MutableList<ActionLogDetail> = mutableListOf()
) {
    /**
     * Add an error [message] to the result.
     * @return the same instance of the result
     */
    fun error(message: ActionLogDetail) = apply {
        errors.add(message)
    }

    /**
     * Add a warning [message] to the result.
     * @return the same instance of the result
     */
    fun warning(message: ActionLogDetail) = apply {
        warnings.add(message)
    }
}

/**
 * Exception to be used when there is an issue normalizing an element
 *
 * @param message the error message
 * @param field the Hl7 field, csv column, et al.
 * @param value the value associated with the field
 * @param errorCode the ErrorCode associated with the normalization error
 */
data class ElementNormalizeException(
    override val message: String,
    val field: String,
    val value: String,
    val errorCode: ErrorCode
) : Exception(message)