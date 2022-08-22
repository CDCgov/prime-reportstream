package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.ElementResult
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.DateUtilities.asFormattedString
import gov.cdc.prime.router.common.DateUtilities.toOffsetDateTime
import gov.cdc.prime.router.common.DateUtilities.toYears
import gov.cdc.prime.router.common.NPIUtilities
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.serializers.Hl7Serializer
import java.security.MessageDigest
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.xml.bind.DatatypeConverter
import kotlin.reflect.full.memberProperties

/**
 * A *Mapper* is defined as a property of a schema element. It is used to create
 * a value for the element when no value is present. For example, the middle_initial element has
 * this mapper:
 *
 *  `mapper: middleInitial(patient_middle_name)`
 *
 * A mapper object is stateless. It has a name which corresponds to
 * the function name in the property. It has a set of arguments, which
 * corresponding to the arguments of the function. Before applying the
 * mapper, the elementName list is generated from the arguments. All element values
 * are then fetched and provided to the apply function.
 */
interface Mapper {
    /**
     * Name of the mapper
     */
    val name: String

    /**
     *
     * The element names of the values that should be requested. Called before apply.
     *
     * For example, if the schema had a mapper field defined for a `minus` mapper
     *
     * - name: some_element
     *   mapper: minus(x, 1)
     *
     * `valueNames` would be called with an args list of ["x", "1"].
     * The minus mapper would return ["x"]. The minus mapper is treating
     * the second argument as a parameter, not as an element name.
     *
     * @param element that contains the mapper definition
     * @param args from the schema
     */
    fun valueNames(element: Element, args: List<String>): List<String>

    /**
     * Apply the mapper using the values from the current report item. Called after valueNames().
     *
     * For example, if the schema had a mapper field defined for a `minus` mapper
     *
     * - name: some_element
     *   mapper: minus(x, 1)
     *
     * `apply` would be called with an `args` list of ["x", "1"] and a
     * `values` list of [ElementAndValue(Element(x, ...), "9")] where "9" is
     * the value of the x element for the current item. The `apply` method
     * would return "8", thus mapping the `some_element` value to the `x` value minus 1.
     *
     * @param args from the schema
     * @param values that where fetched based on valueNames
     */
    fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender? = null
    ): ElementResult
}

data class ElementAndValue(
    val element: Element,
    val value: String
)

class MiddleInitialMapper : Mapper {
    override val name = "middleInitial"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size != 1) error("Schema Error: Invalid number of arguments")
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.isEmpty()) {
                null
            } else {
                if (values.size != 1)
                    error("Found ${values.size} values.  Expecting 1 value. Args: $args, Values: $values")
                else if (values.first().value.isEmpty()) null
                else values.first().value.substring(0..0).uppercase()
            }
        )
    }
}

/**
 * Based on the comparison operator (==,!=,<=,>=,<,>) in arg[0], compare value[1] to value[2]
 * IF the comparison is true, THEN use value[3]; ELSE use value[4]
 *
 * Example call
 *   - name: source_state
 *      mapper: ifThenElse(==, otc_flag, OTC, patient_state, ordering_provider_state)
 *
 * CAUTION: Because this mapper allows every argument to either be an existing element's value OR
 * a string literal, extra care must be used in typing the args list in the schema.  A misspelled
 * or non-existent element name will be treated as a string literal, not an error.
 */
class IfThenElseMapper : Mapper {
    override val name = "ifThenElse"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size != 5) error("Schema Error: Invalid number of arguments - 5 required")
        return args
    }

    /**
     * Determines the meaning of a mapper argument
     *
     * Determines if a mapperArg exists a List of <ElementAndValue>s.
     * If it does, the found element's .value is returned; otherwise, the argument itself is
     * returned as a String literal.  This allows the mapper property of a schema to have literal
     * arguments.  The downside is that misspelled or non-existent schema elements are treated
     * as string literals.  Use with care.
     *
     * @param values the list of ElementAndValue objects passed to the apply function
     * @param mapperArg one of the arguments from a schema elements mapper property
     * @return Either a passed-in Elements .value or the mapper argument as a string literal
     */
    internal fun decodeArg(values: List<ElementAndValue>, mapperArg: String): String {
        return values.find { it.element.name.equals(mapperArg, ignoreCase = true) }?.value ?: mapperArg
    }

    /**
     * Compares two strings for equalities
     *
     * If BOTH strings are numeric ("12"), it compares them as numbers.
     * Otherwise they are compared as string literals
     * @param op a String which denotes a comparison
     * @param val1 a String literal which is either numeric or not
     * @param val2 a String literal which is either numeric or not
     * @return the Boolean result of properly applying the operator implied in op on val1 and val2
     */
    fun comp(op: String, val1: String, val2: String): Boolean {
        val dVal1 = val1.toDoubleOrNull()
        val dVal2 = val2.toDoubleOrNull()
        if ((dVal1 == null) && (dVal2 != null)) {
            error("ifThenElse Type Mismatch Error: $val1 is not numeric")
        }
        if ((dVal1 != null) && (dVal2 == null)) {
            error("ifThenElse Type Mismatch Error: $val2 is not numeric")
        }
        return if ((dVal1 != null) && (dVal2 != null)) { // only if both val1 and val2 are Double strings
            when (op) {
                "==" -> (dVal1 == dVal2) // all Double comparisons (covers Float, Int)
                "!=" -> (dVal1 != dVal2)
                ">=" -> (dVal1 >= dVal2)
                "<=" -> (dVal1 <= dVal2)
                "<" -> (dVal1 < dVal2)
                ">" -> (dVal1 > dVal2)
                else -> error("ifThenElse Mapper Argument Error: not a valid operator: $op")
            }
        } else {
            when (op) {
                "==" -> (val1 == val2) // all string comparisons
                "!=" -> (val1 != val2)
                ">=" -> (val1 >= val2)
                "<=" -> (val1 <= val2)
                "<" -> (val1 < val2)
                ">" -> (val1 > val2)
                else -> error("ifThenElse Mapper Argument Error: not a valid operator: $op")
            }
        }
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return if (
            comp(
                decodeArg(values, args[0]),
                decodeArg(values, args[1]),
                decodeArg(values, args[2])
            ) // see comp()
        )
            ElementResult(decodeArg(values, args[3])) else
            ElementResult(decodeArg(values, args[4])) // see decodeArg()
    }
}

/**
 * The args for the use mapper is a list of element names in order of priority.
 * The mapper will use the first with a value
 */
class UseMapper : Mapper {
    override val name = "use"

    override fun valueNames(element: Element, args: List<String>) = args

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.isEmpty()) {
                null
            } else {
                val (fromElement, fromValue) = values.first()
                when {
                    element.type == fromElement.type -> fromValue
                    element.type == Element.Type.DATE && fromElement.type == Element.Type.DATETIME -> {
                        DateUtilities.parseDate(fromValue).asFormattedString(DateUtilities.datePattern)
                    }
                    element.type == Element.Type.TEXT -> fromValue
                    // TODO: Unchecked conversions should probably be removed, but the PIMA schema relies on this, right now.
                    else -> fromValue
                }
            }
        )
    }
}

/**
 *
 */
class PatientAgeMapper : Mapper {
    /** the name of the mapper */
    override val name = "patientAge"

    /** what values are passed in */
    override fun valueNames(element: Element, args: List<String>) = args

    /** */
    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        // pull our values
        val patientAge = values.firstOrNull { ev -> ev.element.name == "patient_age" }?.value.trimToNull()
        val specimenCollectionDate = values.firstOrNull { ev ->
            ev.element.name == "specimen_collection_date_time"
        }?.value.trimToNull()
        val patientDob = values.firstOrNull { ev -> ev.element.name == "patient_dob" }?.value.trimToNull()
        // get our error and warning collections
        val errors: MutableList<ActionLogDetail> = mutableListOf()
        val warnings: MutableList<ActionLogDetail> = mutableListOf()
        // calculate a result
        val result = when {
            // if they give us a patient age, use that
            patientAge != null -> patientAge
            // if we have both a patient age and a DOB calculate
            specimenCollectionDate != null && patientDob != null -> {
                // parse out the DOB
                val d = try {
                    DateUtilities.parseDate(patientDob).toOffsetDateTime()
                } catch (e: DateTimeParseException) {
                    // DO NOT OUTPUT THE PATIENT DOB HERE IN THE ERROR MESSAGE. That would be PII leakage
                    errors.add(InvalidParamMessage("Unable to parse provided patient DOB to offset date time"))
                    null
                }
                // parse out the specimen collection date
                val s = try {
                    DateUtilities.parseDate(specimenCollectionDate).toOffsetDateTime()
                } catch (e: DateTimeParseException) {
                    // Specimen collection date is not PII but let's not leak that either
                    errors.add(
                        InvalidParamMessage(
                            "Unable to parse provided specimen collection date to offset date time"
                        )
                    )
                    null
                }
                // if both are not null, meaning they both parsed, we can attempt to calculate the age
                if (d != null && s != null) {
                    // check to make sure the birth date is before the specimen collection date
                    if (d.isBefore(s)) {
                        Duration.between(d, s).toYears().toString()
                    } else {
                        // add a warning about the patient DOB and specimen collection date not
                        // being able to be calculated DOB should be before the specimen collection date
                        warnings.add(
                            // don't leak the DOB here either, because it's still PII
                            InvalidParamMessage(
                                "Patient DOB is after specimen collection date, " +
                                    "so cannot correctly calculate patient age"
                            )
                        )
                        null
                    }
                } else {
                    null
                }
            }
            // we have no good datapoints to work off of, so use null
            else -> null
        }

        return ElementResult(result, errors, warnings)
    }
}

/**
 * Use a string value found in the Sender's setting object as the value for this element.
 * Example call
 *   - name: sender_id
 *      cardinality: ONE
 *      mapper: useSenderSetting(fullName)
 *      csvFields: [{ name: senderId}]
 *
 * As of this writing mappers are called in three places:
 *  - during initial read (in [Element.processValue])
 *  - during creation of internal data (in [Report.buildColumnPass2])
 *  - during creation of outgoing data (in, eg, [Hl7Serializer.setComponentForTable])
 *  ONLY the first of those has access to Sender info. However, the mapper may be called in
 *  any or all of those three places. Therefore, its incumbent upon the
 *  writer of any mapper to ensure it works without failure if the Sender obj is null.
 *
 *  Notes:
 *  1. If you use mapperOverridesValue with this, you will get unexpected results.
 *  2. [UseSenderSettingMapper] always returns the toString() value regardless of the field type.
 *  3. This does not work with commandline ./prime, because the CLI knows nothing about settings.
 */
class UseSenderSettingMapper : Mapper {
    override val name = "useSenderSetting"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size != 1) {
            error("Schema Error for ${element.name}: useSenderSetting expects a single argument")
        }
        // The arg is the name of the field in the Sender settings to extract, not a field in the data.
        return emptyList()
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val valueToUse =
            when {
                sender == null -> null // will null out existing value if you use mapperOverridesValue
                args.size != 1 -> error("Schema Error for ${element.name}: useSenderSetting expects a single argument")
                else -> {
                    try {
                        // get the member properties of the correct Sender subclass (or Sender itself if applicable)
                        val senderProperty = sender.javaClass.kotlin.memberProperties.first {
                            it.name == args[0]
                        }
                        senderProperty.get(sender).toString()
                    } catch (e: NoSuchElementException) {
                        return ElementResult(
                            null,
                            mutableListOf(
                                InvalidReportMessage(
                                    "ReportStream internal error in $name: ${args[0]} is not a sender setting field"
                                )
                            )
                        )
                    }
                }
            }
        return ElementResult(valueToUse)
    }
}

/**
 * The mapper concatenates a list of column values together.
 * Call this like this:
 * concat(organization_name, ordering_facility_name)
 * @todo add a separator arg.
 * @todo generalize this to call any kotlin string function?
 */
class ConcatenateMapper : Mapper {
    override val name = "concat"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size < 2)
            error(
                "Schema Error: concat mapper expects to concat two or more column names"
            )
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.isEmpty()) {
                null
            } else {
                // default ", " separator for now.
                values.joinToString(separator = element.delimiter ?: ", ") { it.value }
            }
        )
    }
}

/**
 * The args for the ifPresent mapper are an element name and a value.
 * If the elementName is present, the value is used
 */
class IfPresentMapper : Mapper {
    override val name = "ifPresent"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size != 2) error("Schema Error: ifPresent expects dependency and value parameters")
        return args.subList(0, 1) // The element name
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.size == 1) {
                args[1]
            } else {
                null
            }
        )
    }
}

/**
 * This mapper checks if one or more elements are blank or not present on a row,
 * and if so, will replace an element's value with either some literal string value
 * or the value from a different field on a row
 * ex. ifNotPresent($mode:literal, $string:NO ADDRESS, patient_zip_code, patient_state)
 *      - if patient_zip_code and patient_state are missing or blank, then replace element's value with "NO ADDRESS"
 *     ifNotPresent($mode:lookup, ordering_provider_city, patient_zip_code)
 *      - if patient_zip_code is missing or blank, then replace element's value with that of the ordering_provider_city
 */
class IfNotPresentMapper : Mapper {
    override val name = "ifNotPresent"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isEmpty()) error("Schema Error: ifNotPresent expects dependency and value parameters")
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val mode = args[0].split(":")[1]
        val modeOperator = if (args[1].contains(":")) args[1].split(":")[1] else args[1]
        val conditionList = args.subList(2, args.size)
        conditionList.forEach {
            val valuesElement = values.find { v -> v.element.name == it }
            if (valuesElement != null && valuesElement.value.isNotBlank()) {
                return ElementResult(null)
            }
        }
        return ElementResult(
            when (mode) {
                "literal" -> modeOperator
                "lookup" -> {
                    val lookupValue = values.find { v -> v.element.name == modeOperator }
                    lookupValue?.value.toString()
                }
                else -> null
            }
        )
    }
}

/**
 * The args for the [IfNPIMapper] mapper are an element name, true value and false value.
 *
 * Example Usage:
 * ```
 * ifNPI(ordering_provider_id, NPI, U)
 * ```
 *
 * Test if the value is a valid NPI according to CMS. Return the second parameter if test is true.
 * Return third parameter if the test is false and the parameter is present
 */
class IfNPIMapper : Mapper {
    override val name = "ifNPI"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size !in 2..3) error("Schema Error: ifPresent expects dependency and value parameters")
        return args.subList(0, 1) // The element name
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.size != 1) null
            else if (NPIUtilities.isValidNPI(values[0].value)) args[1]
            else if (args.size == 3) args[2]
            else null
        )
    }
}

/**
 * The LookupMapper is used to lookup values from a lookup table
 * The args for the lookup mapper is the name of the element with the index value
 * The table involved is the element.table field
 * The lookupColumn is the element.tableColumn field
 */
class LookupMapper : Mapper {
    override val name = "lookup"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size !in 2..4) {
            error("Schema Error: lookup mapper expected 2 or 4 args")
        }
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            // args should be twice size of values as it includes the index column names
            // ex: lookup(patient_state, $Column:State, patient_county, $Column:County)
            // there are four args two represent fields with values. Two are lookup table columns
            if (values.size.times(2) != args.size) {
                null
            } else {
                val lookupTable = element.tableRef
                    ?: error("Schema Error: could not find table ${element.table}")
                val tableFilter = lookupTable.FilterBuilder()
                values.forEachIndexed { index, elementAndValue ->
                    // retrieve column name to use for lookup
                    // Specified in sender settings lookup mapper parameters
                    // ex: lookup(patient_state, $Column:State, patient_county, $Column:County)
                    val indexColumn = args[index.times(2).plus(1)].split(":")[1]
                    if (indexColumn.isEmpty())
                        error("Schema Error: no tableColumn for element ${elementAndValue.element.name}")
                    tableFilter.equalsIgnoreCase(indexColumn, elementAndValue.value)
                }
                val lookupColumn = element.tableColumn
                    ?: error("Schema Error: no tableColumn for element ${element.name}")
                tableFilter.findSingleResult(lookupColumn)
            }
        )
    }
}

/**
 * The lookupSenderAutomationValuesetsMapper is used to lookup values from
 *      any particular valueset table/csv
 * The args for the mapper are:
 *      args[0] --> elementName = the name of the element as defined on the schema (i.e. patient_gender)
 *      args[1] --> valueSetName = the name of the value set itself (i.e. gender)
 *      args[2] --> (optional) version = the version of the valueSet value
 * The mapper uses the above arguments to retrieve a row from the table
 */
class LookupSenderAutomationValuesets : Mapper {
    override val name = "lookupSenderAutomationValuesets"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val lookupTable = element.tableRef
            ?: return ElementResult(
                null,
                mutableListOf(
                    InvalidReportMessage(
                        "Schema Error: could not find table ${element.table}"
                    )
                )
            )
        val lookupColumn = element.tableColumn
            ?: return ElementResult(
                null,
                mutableListOf(
                    InvalidReportMessage(
                        "Schema Error: no tableColumn for element ${element.name}"
                    )
                )
            )

        if (!element.tableRef.hasColumn(lookupColumn)) {
            return ElementResult(
                null,
                mutableListOf(
                    InvalidReportMessage(
                        "Schema Error: no tableColumn named $lookupColumn for element ${element.name}"
                    )
                )
            )
        }

        val tableFilter = lookupTable.FilterBuilder()
        val valueSetName = args[1] // args[0] is the incoming element name (see kdoc above)
        var version = ""
        if (args.size > 2) {
            version = args[2]
        }

        tableFilter
            .equalsIgnoreCase("name", valueSetName)
            .equalsIgnoreCase("version", version)
            .equalsIgnoreCase("display", values[0].value)

        val value = tableFilter.findSingleResult(lookupColumn)
            ?: return ElementResult(
                null,
                mutableListOf(
                    InvalidReportMessage(
                        "Schema Error: no value for element ${element.name} with " +
                            "value set name of $valueSetName " +
                            "display value ${values[0].value} " +
                            "and version ${version.ifEmpty { "[no version specified]" }}"
                    )
                )
            )

        return ElementResult(value)
    }
}

/**
 * The NpiLookupMapper is a specific implementation of the lookupMapper and
 * thus no output values are present in this function. This function requires
 * the same lookup table configuration as lookupMapper.
 *
 * In-schema usage:
 * ```
 * type: TABLE
 * table: my-table
 * tableColumn: my-column
 * mapper: npiLookup(provider_id, facility_clia, sender_id)
 * ```
 */
class NpiLookupMapper : Mapper {
    override val name = "npiLookup"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isEmpty())
            error("Schema Error: lookup mapper expected one or more args")
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        /* The table ref and column from the element that called the mapper */
        val lookupTable = element.tableRef
            ?: error("Schema Error: could not find table ${element.table}")
        val lookupColumn = element.tableColumn
            ?: error("Schema Error: no tableColumn for element ${element.name}")

        /* Column names passed in via schema */
        /* Because of the specificity here, we need args = provider_id (npi), facility_id, sender_id */
        val npiColumn = args[0]
        val facilityCliaColumn = args[1]
        val senderIdColumn = args[2]

        /* The values provided from the incoming row of data */
        val npiSent = values.find { it.element.name == npiColumn }?.value ?: ""
        val facilityCliaSent = values.find { it.element.name == facilityCliaColumn }?.value ?: ""
        val senderIdSent = values.find { it.element.name == senderIdColumn }?.value ?: ""

        /* The result stored after filtering the table */
        val filterResult: String?

        if (npiSent.isBlank()) {
            /* Returns the lookupColumn value based on Facility_CLIA and Sender_ID where Default is true */
            filterResult = lookupTable.FilterBuilder()
                .equalsIgnoreCase(facilityCliaColumn, facilityCliaSent)
                .equalsIgnoreCase(senderIdColumn, senderIdSent)
                .equalsIgnoreCase("default", "true")
                .findSingleResult(lookupColumn)
        } else {
            /* Uses NPI to lookup value */
            filterResult = lookupTable.FilterBuilder()
                .equalsIgnoreCase(npiColumn, npiSent)
                .findSingleResult(lookupColumn)
        }

        return ElementResult(filterResult)
    }
}

/**
 * The obx8 mapper fills in OBX-8. This indicates the normalcy of OBX-5
 *
 * @See <a href=https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification>HHS Submission Guidance</a>Do not use it for other fields and tables.
 */
class Obx8Mapper : Mapper {
    override val name = "obx8"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return listOf("test_result")
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(
            if (values.isEmpty() || values.size > 1) {
                null
            } else {
                when (values[0].value) {
                    "260373001" -> "A" // Detected
                    "260415000" -> "N" // Not Detected
                    "720735008" -> "A" // Presumptive positive
                    "42425007" -> "N" // Equivocal
                    "260385009" -> "N" // Negative
                    "10828004" -> "A" // Positive
                    "895231008" -> "N" // Not detected in pooled specimen
                    "462371000124108" -> "A" // Detected in pooled specimen
                    "419984006" -> "N" // Inconclusive
                    "125154007" -> "N" // Specimen unsatisfactory for evaluation
                    "455371000124106" -> "N" // Invalid result
                    "840539006" -> "A" // Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
                    "840544004" -> "A" // Disease caused by severe acute respiratory coronavirus 2 (situation)
                    "840546002" -> "A" // Exposure to severe acute respiratory syndrome coronavirus 2 (event)
                    "840533007" -> "A" // Severe acute respiratory syndrome coronavirus 2 (organism)
                    "840536004" -> "A" // Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
                    "840535000" -> "A" // Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
                    "840534001" -> "A" // Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
                    "373121007" -> "N" // Test not done
                    "82334004" -> "N" // Indeterminate
                    else -> null
                }
            }
        )
    }
}

class TimestampMapper : Mapper {
    override val name = "timestamp"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return emptyList()
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val tsFormat = if (args.isEmpty()) {
            "yyyyMMddHHmmss.SSSSZZZ"
        } else {
            args[0]
        }

        val ts = OffsetDateTime.now()
        return ElementResult(
            try {
                val formatter = DateTimeFormatter.ofPattern(tsFormat)
                formatter.format(ts)
            } catch (_: Exception) {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                formatter.format(ts)
            }
        )
    }
}

class DateTimeOffsetMapper : Mapper {
    override val name = "offsetDateTime"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return listOf(args[0])
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        fun parseDateTime(value: String): OffsetDateTime {
            return try {
                DateUtilities.parseDate(value.trim()).toOffsetDateTime()
            } catch (t: Throwable) {
                error("Invalid date: '$value' for element '${element.name}'")
            }
        }
        return ElementResult(
            if (values.isEmpty() || values.size > 1 || values[0].value.isBlank()) {
                null
            } else {
                val unit = args[1]
                val offsetValue = args[2].toLong()
                val normalDate = parseDateTime(values[0].value)
                val adjustedDateTime = when (unit.lowercase()) {
                    "second", "seconds" -> normalDate.plusSeconds(offsetValue)
                    "minute", "minutes" -> normalDate.plusMinutes(offsetValue)
                    "day", "days" -> normalDate.plusDays(offsetValue)
                    "month", "months" -> normalDate.plusMonths(offsetValue)
                    "year", "years" -> normalDate.plusYears(offsetValue)
                    else -> error("Unit passed into mapper is not valid: $unit")
                }
                DateUtilities.getDateAsFormattedString(adjustedDateTime)
            }
        )
    }
}

// todo: add the option for a default value
class CoalesceMapper : Mapper {
    override val name = "coalesce"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (values.isEmpty()) return ElementResult(null)
        val ev = values.firstOrNull {
            it.value.isNotEmpty()
        }
        return ElementResult(ev?.value ?: "")
    }
}

class TrimBlanksMapper : Mapper {
    override val name = "trimBlanks"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return listOf(args[0])
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val ev = values.firstOrNull()?.value ?: ""
        return ElementResult(ev.trim())
    }
}

class StripPhoneFormattingMapper : Mapper {
    override val name = "stripPhoneFormatting"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isEmpty()) error("StripFormatting mapper requires one or more arguments")
        return listOf(args[0])
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (values.isEmpty()) return ElementResult(null)
        val returnValue = values.firstOrNull()?.value ?: ""
        val nonDigitRegex = "\\D".toRegex()
        val cleanedNumber = nonDigitRegex.replace(returnValue, "")
        return ElementResult("$cleanedNumber:1:")
    }
}

class StripNonNumericDataMapper : Mapper {
    override val name = "stripNonNumeric"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (values.isEmpty()) return ElementResult(null)
        val returnValue = values.firstOrNull()?.value ?: ""
        val nonDigitRegex = "\\D".toRegex()
        return ElementResult(nonDigitRegex.replace(returnValue, "").trim())
    }
}

class StripNumericDataMapper : Mapper {
    override val name = "stripNumeric"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (values.isEmpty()) return ElementResult(null)
        val returnValue = values.firstOrNull()?.value ?: ""
        val nonDigitRegex = "\\d".toRegex()
        return ElementResult(nonDigitRegex.replace(returnValue, "").trim())
    }
}

class SplitMapper : Mapper {
    override val name = "split"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return listOf(args[0])
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (values.isEmpty()) return ElementResult(null)
        val value = values.firstOrNull()?.value ?: ""
        val delimiter = if (args.count() > 2) {
            args[2]
        } else {
            " "
        }
        val splitElements = value.split(delimiter)
        val index = args[1].toInt()
        return ElementResult(splitElements.getOrNull(index)?.trim())
    }
}

class SplitByCommaMapper : Mapper {
    override val name = "splitByComma"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return listOf(args[0])
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (values.isEmpty()) return ElementResult(null)
        val value = values.firstOrNull()?.value ?: ""
        val delimiter = ","
        val splitElements = value.split(delimiter)
        val index = args[1].toInt()
        return ElementResult(splitElements.getOrNull(index)?.trim())
    }
}

class ZipCodeToCountyMapper : Mapper {
    override val name = "zipCodeToCounty"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val table = element.tableRef ?: error("Cannot perform lookup on a null table")
        val zipCode = values.firstOrNull()?.value ?: return ElementResult(null)
        val cleanedZip = if (zipCode.contains("-")) {
            zipCode.split("-").first()
        } else {
            zipCode
        }
        return ElementResult(
            table.FilterBuilder().equalsIgnoreCase("zipcode", cleanedZip)
                .findSingleResult("county")
        )
    }
}
/**
 * [ZipCodeToStateMapper] runs a lookup using zip code and returns the single associated state. Null is returned if
 * no zip code is provided, the zip code is not found, or if multiple records are found in the lookup.
 */

class ZipCodeToStateMapper : Mapper {
    override val name = "zipCodeToState"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val table = element.tableRef ?: error("Cannot perform lookup on a null table")
        val zipCode = values.firstOrNull()?.value ?: return ElementResult(null)
        val cleanedZip = if (zipCode.contains("-")) {
            zipCode.split("-").first()
        } else {
            zipCode
        }
        return ElementResult(
            table.FilterBuilder().equalsIgnoreCase("zipcode", cleanedZip)
                .findSingleResult(element.tableColumn.toString())
        )
    }
}

/**
 * The CountryMapper examines both the patient_country and the patient_zip_code fields for a row
 * and makes a determination based on both values whether the patient is in the US or is in Canada.
 * It is currently restricted to those two choices because we do not have a need to extend it beyond
 * those countries.
 *
 * The functionality works as follows:
 * - If a value exists for patient_country, pass that through untouched. We always assume that whatever
 *   the sender gives us is correct and should not be enriched.
 * - If there is not a value for patient_country we then look at the patient_zip_code field. If the patient
 *   zip code matches the pattern for a Canadian postal code then we assume we are dealing with a Canadian
 *   address. Canadian postal codes follow a pattern of A9A 9A9 where 'A' represents a letter between A-Z
 *   and '9' represents a number between 0-9.
 * - If the patient_zip_code doesn't match a Canadian postal code pattern, then we default to USA.
 */
class CountryMapper : Mapper {
    override val name = "countryMapper"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return args
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        val patientCountry = values.firstOrNull { it.element.name == element.name }?.value
        val patientPostalCode = values.firstOrNull { it.element.name == "patient_zip_code" }?.value
        return ElementResult(
            when (patientCountry.isNullOrEmpty()) {
                true -> {
                    if (patientPostalCode != null && canadianPostalCodeRegex.matches(patientPostalCode)) {
                        CAN
                    } else {
                        USA
                    }
                }
                else -> patientCountry
            }
        )
    }

    companion object {
        /**
         * This regex matches Canadian postal codes, which follow a pattern of A9A 9A9, where 'A' means any
         * alpha character from [A-Z] and 9 represents any number between [0-9]. The space between the two
         * groupings is technically part of their postal code standard, but in the case of our processing,
         * we typically have stripped it out. So a Canadian postal code could look like J0A 0A0, or J0A0A0.
         *
         * A Canadian postal code is broken down by the first character being the postal district, for example,
         * Ontario, and the next two characters representing the forward sortation area.The second set of three
         * digits is the local delivery unit. Small fun fact, Canada Post has a special postal code for Santa
         * so children can address their letters to him at H0H 0H0.
         */
        private val canadianPostalCodeRegex = "[A-Z][0-9][A-Z]\\s?[0-9][A-Z][0-9]".toRegex(RegexOption.IGNORE_CASE)
        /** No magic strings. */
        /** No magic strings. */
        private const val USA = "USA"
        /** No magic strings. */
        private const val CAN = "CAN"
    }
}

/**
 * Create a SHA-256 digest hash of the concatenation of values
 * Example:   hash(patient_last_name,patient_first_name)
 */
class HashMapper : Mapper {
    override val name = "hash"

    override fun valueNames(element: Element, args: List<String>) = args

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        if (args.isEmpty()) error("Must pass at least one element name to $name")
        if (values.isEmpty()) return ElementResult(null)
        val concatenation = values.joinToString("") { it.value }
        if (concatenation.isEmpty()) return ElementResult(null)
        return ElementResult(digest(concatenation.toByteArray()).lowercase())
    }

    companion object {
        fun digest(input: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input)
            return DatatypeConverter.printHexBinary(digest)
        }
    }
}

/**
 * This mapper performs no operation and is meant to override mappers set on parent schemas, so no mapper runs.
 * It does not change any values.
 * If you want to 'blank out' a value, use 'defaultOverridesValue: true', along with an empty 'default:'
 * Arguments: None
 * Returns: null
 */
class NullMapper : Mapper {
    override val name = "none"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isNotEmpty())
            error("Schema Error: none mapper does not expect args")
        return emptyList()
    }

    override fun apply(
        element: Element,
        args: List<String>,
        values: List<ElementAndValue>,
        sender: Sender?
    ): ElementResult {
        return ElementResult(null)
    }
}

object Mappers {
    fun parseMapperField(field: String): Pair<String, List<String>> {
        val match = Regex("([a-zA-Z0-9]+)\\x28([a-z, \\x2E_\\x2DA-Z0-9?&$*:^><=!]*)\\x29").find(field)
            ?: error("Mapper field $field does not parse")
        val args = if (match.groupValues[2].isEmpty())
            emptyList()
        else
            match.groupValues[2].split(',').map { it.trim() }
        val mapperName = match.groupValues[1]
        return Pair(mapperName, args)
    }
}