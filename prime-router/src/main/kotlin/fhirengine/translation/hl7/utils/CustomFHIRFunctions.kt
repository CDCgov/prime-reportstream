package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import fhirengine.translation.hl7.utils.FhirPathFunctions
import fhirengine.translation.hl7.utils.helpers.convertDateToAge
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.hl7.fhir.r4.fhirpath.FHIRPathUtilityClasses.FunctionDetails
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.TimeZone

/**
 * Custom FHIR functions created by report stream to help map from FHIR -> HL7
 * only used in cases when the same logic couldn't be accomplished using the FHIRPath
 */
object CustomFHIRFunctions : FhirPathFunctions {

    /**
     * Custom FHIR Function names used to map from the string used in the FHIR path
     * to the function name in the CustomFHIRFunctions class
     */
    enum class CustomFHIRFunctionNames {
        GetPhoneNumberCountryCode,
        GetPhoneNumberAreaCode,
        GetPhoneNumberLocalNumber,
        GetPhoneNumberExtension,
        GetCodingSystemMapping,
        Split,
        GetId,
        GetIdType,
        HasPhoneNumberExtension,
        ChangeTimezone,
        ConvertDateToAge,
        DeidentifyHumanName,
        ;

        companion object {
            /**
             * Get from a [functionName].
             * @return the function name enum or null if not found
             */
            fun get(functionName: String?): CustomFHIRFunctionNames? {
                return try {
                    functionName?.let { CustomFHIRFunctionNames.valueOf(it.replaceFirstChar(Char::titlecase)) }
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    /**
     * Get the function details for a given [functionName] and adds the ability to provide [additionalFunctions] in case
     * additional custom FHIR functions are needed.
     * @return the function details
     */
    override fun resolveFunction(
        functionName: String?,
        additionalFunctions: FhirPathFunctions?,
    ): FunctionDetails? {
        return when (CustomFHIRFunctionNames.get(functionName)) {
            CustomFHIRFunctionNames.GetPhoneNumberCountryCode -> {
                FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetPhoneNumberAreaCode -> {
                FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetPhoneNumberLocalNumber -> {
                FunctionDetails("extract country code from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetPhoneNumberExtension -> {
                FunctionDetails("extract extension from FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.HasPhoneNumberExtension -> {
                FunctionDetails("see if extension exists in FHIR phone number", 0, 0)
            }

            CustomFHIRFunctionNames.GetCodingSystemMapping -> {
                FunctionDetails("convert FHIR coding system url to HL7 ID", 0, 0)
            }

            CustomFHIRFunctionNames.Split -> {
                FunctionDetails("splits a string by provided delimiter", 1, 1)
            }

            CustomFHIRFunctionNames.GetId -> {
                FunctionDetails("extracts an ID from a resource property", 0, 0)
            }

            CustomFHIRFunctionNames.GetIdType -> {
                FunctionDetails(
                    "determines the ID type from a resource property",
                    0,
                    0
                )
            }

            CustomFHIRFunctionNames.ChangeTimezone -> {
                FunctionDetails(
                    "changes the timezone of a dateTime, instant, or date resource to the timezone passed in. " +
                        "optional params: " +
                        "dateTimeFormat ('OFFSET', 'LOCAL', 'HIGH_PRECISION_OFFSET', 'DATE_ONLY')(default: 'OFFSET')," +
                        " convertPositiveDateTimeOffsetToNegative (boolean)(default: false)," +
                        " useHighPrecisionHeaderDateTimeFormat (boolean)(default: false)",
                    1,
                    4
                )
            }

            CustomFHIRFunctionNames.ConvertDateToAge -> {
                FunctionDetails(
                    "returns the age of a person from the comparison date (defaulted to current time) in " +
                        "the unit specified or in a default unit. Params can be as follows:" +
                        "(time unit), (comparison date), (timeUnit, comparisonDate), or (comparisonDate, timeUnit)",
                    0,
                    2
                )
            }

            CustomFHIRFunctionNames.DeidentifyHumanName -> {
                FunctionDetails(
                    "removes PII from a name",
                    0,
                    1
                )
            }

            else -> additionalFunctions?.resolveFunction(functionName)
        }
    }

    /**
     * Execute the function on a [focus] resource for a given [functionName] and [parameters]. [additionalFunctions] can
     * be executed if present
     *
     * @return the function result
     */
    override fun executeFunction(
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?,
        additionalFunctions: FhirPathFunctions?,
    ): MutableList<Base> {
        check(focus != null)
        return (
            when (CustomFHIRFunctionNames.get(functionName)) {
                CustomFHIRFunctionNames.GetPhoneNumberCountryCode -> {
                    getPhoneNumberCountryCode(focus)
                }

                CustomFHIRFunctionNames.GetPhoneNumberAreaCode -> {
                    getPhoneNumberAreaCode(focus)
                }

                CustomFHIRFunctionNames.GetPhoneNumberLocalNumber -> {
                    getPhoneNumberLocalNumber(focus)
                }

                CustomFHIRFunctionNames.GetPhoneNumberExtension -> {
                    getPhoneNumberExtension(focus)
                }

                CustomFHIRFunctionNames.HasPhoneNumberExtension -> {
                    hasPhoneNumberExtension(focus)
                }

                CustomFHIRFunctionNames.GetCodingSystemMapping -> {
                    getCodingSystemMapping(focus)
                }

                CustomFHIRFunctionNames.Split -> {
                    split(focus, parameters)
                }

                CustomFHIRFunctionNames.GetId -> {
                    getId(focus)
                }

                CustomFHIRFunctionNames.GetIdType -> {
                    getIdType(focus)
                }

                CustomFHIRFunctionNames.ChangeTimezone -> {
                    changeTimezone(focus, parameters)
                }

                CustomFHIRFunctionNames.ConvertDateToAge -> {
                    convertDateToAge(focus, parameters)
                }

                CustomFHIRFunctionNames.DeidentifyHumanName -> {
                    deidentifyHumanName(focus, parameters)
                }

                else -> additionalFunctions?.executeFunction(focus, functionName, parameters)
                    ?: throw IllegalStateException("Tried to execute invalid FHIR Path function $functionName")
            }
            )
    }

    /**
     * Gets the phone number country code from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the country code
     */
    private fun getPhoneNumberCountryCode(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.Country)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Gets the phone number area code from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the phone number area code
     */
    private fun getPhoneNumberAreaCode(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.AreaCode)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Gets the local phone number from the full FHIR phone number stored in
     * the [focus] element.
     * @return a mutable list containing the local number
     */
    private fun getPhoneNumberLocalNumber(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.Local)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Gets the extension from the full FHIR phone number stored in the [focus] element.
     * @return a mutable list containing the extension if present
     */
    private fun getPhoneNumberExtension(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        val part = PhoneUtilities.getPhoneNumberPart(primVal, PhonePart.Extension)
        return if (part != null) mutableListOf(IntegerType(part)) else mutableListOf()
    }

    /**
     * Determines if the [focus] passed in is a phone number with an extension.
     * @return a boolean indicating if the focus has an extension or not
     */
    private fun hasPhoneNumberExtension(focus: MutableList<Base>): MutableList<Base> {
        val primVal = focus[0].primitiveValue()
        return mutableListOf(BooleanType(PhoneUtilities.hasPhoneNumberExtension(primVal)))
    }

    /**
     * Splits the [focus] into multiple strings using the delimiter provided in [parameters]
     * @returns list of strings
     */
    fun split(focus: MutableList<Base>, parameters: MutableList<MutableList<Base>>?): MutableList<Base> {
        return if (!parameters.isNullOrEmpty() &&
            parameters.size == 1 &&
            parameters.first().size == 1 &&
            focus.size == 1 &&
            focus.first() is StringType
        ) {
            val delimiter = (parameters.first().first()).primitiveValue()
            val stringToSplit = focus.first().primitiveValue()

            stringToSplit.split(delimiter).map { StringType(it) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    /**
     * Enum representing the CodingSystemMapping.
     * FHIR, urls [fhirURL] and their corresponding HL7 Ids [hl7ID]
     */
    enum class CodingSystemMapper(val fhirURL: String, val hl7ID: String) {
        ICD10("http://hl7.org/fhir/sid/icd-10-cm", "I10"),
        LOINC("http://loinc.org", "LN"),
        LOCAL("https://terminology.hl7.org/CodeSystem-v2-0396.html#v2-0396-99zzzorL", "L"),
        SNOMED_CLINICAL("http://snomed.info/sct", "SCT"),
        HL70189("http://terminology.hl7.org/CodeSystem/v2-0189", "HL70189"),
        HL70005("http://terminology.hl7.org/CodeSystem/v3-Race", "HL70005"),
        HL70006("http://terminology.hl7.org/CodeSystem/v2-0006", "HL70006"),
        HL70136("http://terminology.hl7.org/ValueSet/v2-0136", "HL70136"),
        HL70078("http://terminology.hl7.org/CodeSystem/v2-0078", "HL70078"),
        HL70131("http://terminology.hl7.org/CodeSystem/v2-0131", "HL70131"),
        UCUM("http://unitsofmeasure.org", "UCUM"),
        NONE("", ""),
        NULLFL("http://terminology.hl7.org/CodeSystem/v3-NullFlavor", "NULLFL"),
        ;

        companion object {
            /**
             * Get a coding system mapper by its [fhirURL]
             * @return an enum instance representing the appropriate mapping
             */
            fun getByFhirUrl(fhirURL: String): CodingSystemMapper {
                return CodingSystemMapper.values().find {
                    it.fhirURL == fhirURL
                } ?: NONE
            }
        }
    }

    /**
     * Gets the FHIR System URL stored in the [focus] element and maps it to the appropriate
     * HL7 v2.5.1 - 0396 - Coding system.
     * @return a mutable list containing the single character HL7 result status
     */
    private fun getCodingSystemMapping(focus: MutableList<Base>): MutableList<Base> {
        return mutableListOf(StringType(CodingSystemMapper.getByFhirUrl(focus[0].primitiveValue()).hl7ID))
    }

    /**
     * Regex to identify OIDs.  Source: https://www.hl7.org/fhir/datatypes.html#oid
     */
    private val oidRegex = """^[0-2](.(0|[1-9]\d*))+$""".toRegex()

    /**
     * Regex to identify CLIAs.
     * CLIAs are 10-character strings
     * The first char is alphanumeric, the 3rd is always a letter (usually "D").
     * Every other char is numeric.
     */
    private val cliaRegex = """^[a-zA-Z\d]\d[a-zA-Z]\d{7}$""".toRegex()

    /**
     * Regex to extract type and ID from a URN. Note the Clia NID is not part of the mapping docs.
     */
    private val urnRegex = """^urn:(oid|uuid|dns|uri|clia|id):(.*)$""".toRegex()

    /**
     * Regex to extract type and ID from an unknown ID type
     */
    private val unknownId = """^(.*)-(.*):(.*)$""".toRegex()

    /**
     * Get the ID for the value in [focus].
     * @return a list with one ID value or an empty list
     */
    fun getId(focus: MutableList<Base>): MutableList<Base> {
        val type = when {
            // We can only process one value and primitives
            focus.isEmpty() || focus.size > 1 || !focus[0].isPrimitive || focus[0].primitiveValue().isNullOrBlank() ->
                null

            urnRegex.matches(focus[0].primitiveValue()) -> {
                val matches = urnRegex.matchEntire(focus[0].primitiveValue())
                matches?.groupValues?.get(2)
            }

            unknownId.matches(focus[0].primitiveValue()) -> {
                val matches = unknownId.matchEntire(focus[0].primitiveValue())
                matches?.groupValues?.get(3)
            }

            else -> focus[0].primitiveValue()
        }
        return if (type != null) mutableListOf(StringType(type)) else mutableListOf()
    }

    fun deidentifyHumanName(focus: MutableList<Base>, parameters: MutableList<MutableList<Base>>?): MutableList<Base> {
        val deidentifiedValue = parameters?.firstOrNull()?.filterIsInstance<StringType>()?.firstOrNull()?.value ?: ""
        focus.filterIsInstance<HumanName>().forEach { name ->
            if (deidentifiedValue.isNotEmpty()) {
                val updatedGiven = name.given.map { StringType(deidentifiedValue) }
                name.setGiven(updatedGiven.toMutableList())
            } else {
                name.setGiven(emptyList())
            }

            name.setFamily(deidentifiedValue)
        }

        return focus
    }

    /**
     * Get the ID type for the value in [focus].
     * @return a list with one value denoting the ID type, or an empty list
     */
    fun getIdType(focus: MutableList<Base>): MutableList<Base> {
        val oidType = "ISO"
        val cliaType = "CLIA"
        val type = when {
            // We can only process one value and primitives
            focus.isEmpty() || focus.size > 1 || !focus[0].isPrimitive || focus[0].primitiveValue().isNullOrBlank() ->
                null

            urnRegex.matches(focus[0].primitiveValue()) -> {
                val matches = urnRegex.matchEntire(focus[0].primitiveValue())
                when (val type = matches?.groupValues?.get(1)?.uppercase()) {
                    "OID" -> oidType
                    "ID" -> null
                    else -> type
                }
            }

            unknownId.matches(focus[0].primitiveValue()) -> {
                val matches = unknownId.matchEntire(focus[0].primitiveValue())
                val universalId = matches?.groupValues?.get(3)
                when {
                    universalId == null -> null
                    universalId.matches(oidRegex) -> oidType
                    universalId.matches(cliaRegex) -> cliaType
                    // Return the stated type if any
                    else -> matches.groupValues[2]
                }
            }

            focus[0].primitiveValue().matches(cliaRegex) -> cliaType

            else -> null
        }
        return if (type != null) mutableListOf(StringType(type)) else mutableListOf()
    }

    fun getPrimitiveValue(focus: MutableList<Base>): MutableList<Base> {
        return focus.map {
            if (it.isPrimitive) {
                it.copy()
            } else {
                it
            }
        }.toMutableList()
    }

    /**
     * Applies a timezone given by [parameters] to a dateTime in [focus] and returns the result.
     * @return a date in the new timezone
     */
    fun changeTimezone(
        focus: MutableList<Base>,
        parameters: MutableList<MutableList<Base>>?,
    ): MutableList<Base> {
        if (focus.size != 1) {
            throw SchemaException("Must call changeTimezone on a single element")
        }

        if (parameters == null || parameters.first().isEmpty()) {
            throw SchemaException("Must pass a timezone as the parameter")
        }

        var dateTimeFormat = DateUtilities.DateTimeFormat.OFFSET
        if (parameters.size > 1) {
            try {
                dateTimeFormat = DateUtilities.DateTimeFormat.valueOf(parameters.get(1).first().primitiveValue())
            } catch (e: IllegalArgumentException) {
                throw SchemaException("Date time format not found.")
            }
        }

        val inputTimeZone = parameters.first().first().primitiveValue()
        val timezonePassed = try {
            TimeZone.getTimeZone(ZoneId.of(inputTimeZone))
        } catch (e: DateTimeException) {
            throw SchemaException(
                "Invalid timezone $inputTimeZone passed. See FHIR timezone valueSet " +
                    "(https://hl7.org/fhir/valueset-timezones.html) for available timezone values.",
                        e
            )
        }

        return if (focus[0] is StringType) {
            val inputDate = try {
                DateUtilities.parseDate((focus[0].toString()))
            } catch (e: DateTimeParseException) {
                throw SchemaException("Error trying to change time zone: " + e.message)
            }

            if (inputDate is LocalDate) {
                return mutableListOf(StringType(focus[0].toString()))
            }

            val formattedDate = DateUtilities.formatDateForReceiver(
                inputDate,
                ZoneId.of(inputTimeZone),
                dateTimeFormat,
                parameters.getOrNull(2)?.first()?.primitiveValue()?.toBoolean() ?: false,
                parameters.getOrNull(3)?.first()?.primitiveValue()?.toBoolean() ?: false
            )
            mutableListOf(StringType(formattedDate))
        } else {
            val inputDate = focus[0] as? BaseDateTimeType ?: throw SchemaException(
                "Must call changeTimezone on a dateTime, instant, or date; " +
                    "was attempted on a ${focus[0].fhirType()}"
            )

            when (inputDate.precision) {
                TemporalPrecisionEnum.YEAR, TemporalPrecisionEnum.MONTH, TemporalPrecisionEnum.DAY, null ->
                    mutableListOf(inputDate)

                TemporalPrecisionEnum.MINUTE, TemporalPrecisionEnum.SECOND, TemporalPrecisionEnum.MILLI ->
                    mutableListOf(DateTimeType(inputDate.value, inputDate.precision, timezonePassed))
            }
        }
    }
}