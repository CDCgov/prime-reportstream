package gov.cdc.prime.router

import gov.cdc.prime.router.common.DateUtilities
import org.apache.logging.log4j.kotlin.Logging
import tech.tablesaw.api.Table
import tech.tablesaw.selection.Selection
import java.time.DateTimeException
import java.time.OffsetDateTime
import java.time.Period
import java.time.format.DateTimeParseException

/**
 * This is a library or toolkit of useful filter definitions.  Filters remove "rows" of data.
 * (as opposed to Mappers, which manipulate columns of data)
 *
 * A call to a [ReportStreamFilterDefinition] can be used in the filters property in an Organization
 * It allowed you to create arbitrarily complex filters on data.
 * Each filter in the list does an "and" boolean operation with the other filters in the list.
 *
 * Here is an example use:
 * ```
 *  jurisdictionalFilter: { filterByPatientOrFacilityLoc(AZ, Pima) }
 * ```
 *
 * The name `filterByPatientOrFacility` then maps via pseudo-reflection to an implementation of a
 * ReportStreamFilterDef here.
 *
 * If you add an implementation here, you have to add it to the list of reportStreamFilters in Metadata.kt.
 *
 * A ReportStreamFilterDef is stateless.   It has a name property, which should be used in the filter definition -
 * basically a simple way of implementing Reflection. Currently ReportStreamFilterDef implements its filtering
 * by just re-using the very rich `Selection` functionality already in tablesaw.
 *
 * Hoping we implement some geospatial searches someday.
 *
 */
interface ReportStreamFilterDefinition : Logging {
    /**
     * Name of the filter function
     */
    val name: String

    /**
     * @args values passed to the filter
     * receiver is for logging purposes
     * doAuditing - if true, keep track of details of what was filtered.  If false, do not track.
     *
     *
     * @return the Selection object to be used to filter the
     * tablesaw table.
     */
    fun getSelection(args: List<String>, table: Table, receiver: Receiver, doAuditing: Boolean = true): Selection

    companion object : Logging {
        /**
         * filterFunction must be of form "funName(arg1, arg2, etc)"
         */
        fun parseReportStreamFilter(filterFunction: String): Pair<String, List<String>> {
            // Using a permissive match in the (arg1, arg2) section, to allow most regexs to be passed as args.
            // Somehow this works with "(?i).*Pima.*", I guess because the \\x29 matches rightmost ')' char
            val match = Regex("([a-zA-Z0-9]+)\\x28(.*)\\x29").find(filterFunction)
                ?: error("ReportStreamFilter field $filterFunction does not parse")
            return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
        }
    }
}

/**
 * Implements a regex match.  If any of the regexes matches, the row is selected.
 * If the column name does not exist, nothing passes thru the filter.
 * matches(columnName, regex, regex, regex)
 */
class Matches : ReportStreamFilterDefinition {
    override val name = "matches"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.size < 2) error(
            "For ${receiver.fullName}: Expecting two or more args to filter $name:" +
                " (columnName, regex [, regex, regex])"
        )
        val columnName = args[0]
        val values = args.subList(1, args.size)
        val columnNames = table.columnNames()
        return if (columnNames.contains(columnName)) {
            val selection = Selection.withRange(0, 0)
            values.forEach { regex ->
                selection.or(table.stringColumn(columnName).matchesRegex(regex))
            }
            selection
        } else
            Selection.withRange(0, 0)
    }
}

/**
 * Implements the opposite of the matches filter.
 * Regexes have a hard time with "not", and this just seemed clearer
 * and more obvious to the user what's going on.
 * does_not_match(columnName, val, val, ...)
 *
 * A row of data is "allowed" if it does not match any of the values, or if the column does not exist
 */
class DoesNotMatch : ReportStreamFilterDefinition {
    override val name = "doesNotMatch"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.size < 2) error(
            "For ${receiver.fullName}: Expecting two or more args to filter $name:" +
                " (columnName, value, value, ...)"
        )
        val columnName = args[0]
        val values = args.subList(1, args.size)
        val columnNames = table.columnNames()
        val selection = if (columnNames.contains(columnName)) {
            var colSelection = Selection.withRange(0, table.rowCount())
            values.forEach { regex ->
                colSelection = colSelection.andNot(table.stringColumn(columnName).matchesRegex(regex))
            }
            colSelection
        } else {
            Selection.withRange(0, table.rowCount())
        }
        return selection
    }
}

/**
 * Implements the special filter to filter out the negative test result for "Antigen" Test type.
 * It obtains negative test result from args[1], args[2], ... Which obtained from the covid-19.valuesets
 * covid-19/test_result (SNOMED_CT).  Then, it goes through the report table to find the negative value in the
 * test_result colunm and check the test_type column to see if it is "Antigen".  If it is, then it will remove
 * the row from the table. If it is not, then, it leaves it alone.
 *
 * filterOutNegativeAntigenTestType(test_result, 260385009, 260415000, 895231008)
 *
 * Return: the selection to the modified table.
 */
class FilterOutNegativeAntigenTestType : ReportStreamFilterDefinition {
    override val name = "filterOutNegativeAntigenTestType"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.size < 2) error(
            "For ${receiver.fullName}: Expecting two or more args to filter $name:" +
                " (columnName, value, value, ...)"
        )
        val columnName = args[0]
        val values = args.subList(1, args.size)
        val columnNames = table.columnNames()
        val selection = if (columnNames.contains(columnName)) {
            var colSelection = Selection.withRange(0, table.rowCount())
            values.forEach { value ->
                val testType = table.stringColumn("test_type")
                val selection = table.stringColumn(columnName).matchesRegex(value)
                val rowIndex = selection.toArray()
                for (i in 0..rowIndex.size - 1) {
                    if (testType.getString(rowIndex[i]).equals("Antigen", ignoreCase = true))
                        colSelection = colSelection.andNot(Selection.withRange(rowIndex[i], rowIndex[i] + 1))
                }
            }
            colSelection
        } else {
            Selection.withRange(0, table.rowCount())
        }
        return selection
    }
}
/**
 * This may or may not be a unicorn.
 */
class FilterByCounty : ReportStreamFilterDefinition {
    override val name = "filterByCounty"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.size != 2) error(
            "For ${receiver.fullName}: Expecting two args to filter $name:" +
                "  (TwoLetterState, County)"
        )
        // Try to be very loose on county matching.   Anything with the county name embedded is ok.
        val countyRegex = "(?i).*${args[1]}.*"

        val columnNames = table.columnNames()
        val patientSelection = if (columnNames.contains("patient_state") && columnNames.contains("patient_county")) {
            val patientState = table.stringColumn("patient_state")
            val patientCounty = table.stringColumn("patient_county")
            patientState.isEqualTo(args[0]).and(patientCounty.matchesRegex(countyRegex))
        } else null

        val facilitySelection = if (columnNames.contains("ordering_facility_state") &&
            columnNames.contains("ordering_facility_county")
        ) {
            val facilityState = table.stringColumn("ordering_facility_state")
            val facilityCounty = table.stringColumn("ordering_facility_county")
            facilityState.isEqualTo(args[0]).and(facilityCounty.matchesRegex(countyRegex))
        } else null

        // Overall, this is "true" if either the patient is in the county/state
        //   OR, if the facility is in the county/state.
        // If unable to find the right patient_* nor ordering_facility_* columns, filter is always false.
        return when {
            (facilitySelection != null && patientSelection != null) -> patientSelection.or(facilitySelection)
            (facilitySelection != null) -> facilitySelection
            (patientSelection != null) -> patientSelection
            else -> Selection.withRange(0, 0)
        }
    }
}

/**
 * Do an "or" of any number of regex matching expressions.
 * Pass args like this  or(elem1_name, regex1, elem2_name, regex2, ...)
 * This filter is true if
 *      (elem1.value matches regex1) || (elem2.value matches regex2) || ...
 * If the elem name is missing, the filter is false; this is not an error.
 * Example:
 * jurisdictionalFilter:  orEquals(ordering_facility_state, PA, patient_state, PA)
 */
class OrEquals : ReportStreamFilterDefinition {
    override val name = "orEquals"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.isEmpty()) error("Expecting at least two args for filter $name.  Got none.")
        if (args.size % 2 != 0)
            error(
                "For ${receiver.fullName}: Expecting a positive even number " +
                    "of args to filter $name: (col,val, col,val,...)." +
                    " Instead got ${args.size} args"
            )
        val selection = Selection.withRange(0, 0)
        for (i in args.indices step 2) {
            val elemName = args[i]
            val regexStr = args[i + 1]
            val colSelection = if (table.columnNames().contains(elemName)) {
                val elemColumn = table.stringColumn(elemName)
                elemColumn.matchesRegex(regexStr)
            } else null
            colSelection?.let { selection.or(colSelection) }
        }
        return selection
    }
}

/**
 * A filter that filter nothing -- allows all data through.  Useful for overriding more strict defaults.
 */
class AllowAll : ReportStreamFilterDefinition {
    override val name = "allowAll"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        // On empty args (eg, "allowAll()"), our regex returns args of size 1, with a single empty string.
        // Didn't bother trying to fix the regex.
        if (args.size > 1) error(
            "For rcvr ${receiver.fullName} Expecting no args for filter $name." +
                " Got ${args.joinToString(",")}"
        )
        return Selection.withRange(0, table.rowCount())
    }
}

/**
 * A filter that filter everything -- allows no data through.  Useful as a default to be overridden.
 */
class AllowNone : ReportStreamFilterDefinition {
    override val name = "allowNone"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        // See note on allowAll above on regex weirdness.
        if (args.size > 1) error(
            "For rcvr ${receiver.fullName} Expecting no args for filter $name." +
                " Got ${args.joinToString(",")}"
        )
        return Selection.withRange(0, 0)
    }
}

/**
 * Implements a quality check match.  If a row has valid data for all the columns, the row is selected.
 * If any column name does not exist, nothing passes thru the filter.
 * hasValidDataFor(columnName1, columnName2, columnName3, ...)
 * If no columns are passed, all rows are selected.  So, any number of args is acceptable.
 */
class HasValidDataFor : ReportStreamFilterDefinition {
    override val name = "hasValidDataFor"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        var selection = Selection.withRange(0, table.rowCount())

        val columnNames = table.columnNames()
        args.forEach { colName ->
            if (columnNames.contains(colName)) {
                Selection.with(*selection.toArray()) // hack way to copy to a new Selection obj
                selection = selection.andNot(table.stringColumn(colName).isEmptyString)
            } else {
                return Selection.withRange(0, 0)
            }
        }
        return selection
    }
}

/**
 * Implements a specific check for CLIA number format.
 * Pass in any number of columns you expect to be valid clia numbers.
 * Example:  isValidCLIA(testing_lab_clia,reporting_facility_clia)
 * This test passes if at least one of the columns exists, and its value has exactly 10 alphanumeric chars.
 * Otherwise, this test fails.
 *
 * (It appears that the 3rd position in all CLIAs is the letter "D", but I could
 * find no official documentation confirming that, so that is not enforced)
 *
 */
class IsValidCLIA : ReportStreamFilterDefinition {
    override val name = "isValidCLIA"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.isEmpty()) error("Expecting at least one arg for filter $name.  Got none.")
        var selection = Selection.withRange(0, 0)
        val columnNames = table.columnNames()
        args.forEach { colName ->
            if (columnNames.contains(colName)) {
                selection = selection.or(
                    table.stringColumn(colName).lengthEquals(10).and(table.stringColumn(colName).isAlphaNumeric)
                )
            }
        }
        return selection
    }
}

/**
 * hasAtLeastOneOf(columnName1, columnName2, columnName3, ...)
 * Implements a quality check match.  If a row has valid data for any of the columns, the row is selected.
 */
class HasAtLeastOneOf : ReportStreamFilterDefinition {
    override val name = "hasAtLeastOneOf"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.isEmpty()) error("Expecting at least one arg for filter $name.  Got none.")
        var selection = Selection.withRange(0, 0)
        val columnNames = table.columnNames()
        args.forEach { colName ->
            if (columnNames.contains(colName)) {
                selection = selection.or(table.stringColumn(colName).isNotMissing)
            }
        }
        return selection
    }
}

/**
 * AtLeastOneHasValue(columnName1, columnName2, columnName3, ...)
 * Implements a quality check match.  If a row has true value for any of the columns, the row is selected.
 */
class AtLeastOneHasValue : ReportStreamFilterDefinition {
    override val name = "atLeastOneHasValue"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        if (args.isEmpty()) error("Expecting at least one arg for filter $name.  Got none.")
        val searchValue = args[0]
        var selection = Selection.withRange(0, 0)
        val columnNames = table.columnNames()
        args.drop(1).forEach { colName ->
            if (columnNames.contains(colName)) {
                selection = selection.or(table.stringColumn(colName).equalsIgnoreCase(searchValue))
            }
        }
        return selection
    }
}

/**
 * InDateInterval(elementName, date, period)
 *
 * Implements a test that the specified element (e.g. table column) has a date value between or equal to the
 * `date` and less than the `date + period`. In other words,
 * ```
 * // if period has a positive value
 * date <= value < date + period
 * // or if period has a negative value
 * date + period <= value < date
 * ```
 *
 * ## Examples
 * ```
 *   // Specimens collected in 2020
 *   inDateInterval(specimen_collection_date_time, 202001010000-0000, P1Y)
 *
 *   // Specimens collected after 2022-01-01
 *   inDateInterval(specimen_collection_date_time, 202001010000-0000, P1000Y)
 *
 *   // Specimens collected in the past 19 days
 *   inDateInterval(specimen_collection_date_time, now, -P19D)
 * ```
 *
 * ## Notes
 *
 * * There always must be 3 arguments.
 *
 * * Empty values in the table never match the interval
 *
 * * A `date` argument can be `now` to indicate the current time. Otherwise, it must follow
 * one of the formats that ReportStream understands. The HL7 date-time format is recommended.
 *
 * * A `duration` argument must follow the ISO 8061 standard format as implemented by Java.
 * This format is `[-]P[n]Y[n]M[n]D` or `P[n]W`.
 * The period can be negative to indicate an interval that starts before the `date` argument.
 * The period can be zero but that will never match any value.
 */
class InDateInterval : ReportStreamFilterDefinition {
    override val name = "inDateInterval"

    override fun getSelection(
        args: List<String>,
        table: Table,
        receiver: Receiver,
        doAuditing: Boolean
    ): Selection {
        // Check args
        if (args.size != 3) error("Expecting 3 arguments: Got ${args.size}")
        if (args[0].isBlank()) error("Expecting first argument is not blank")
        val intervalDate = if (args[1].contentEquals("now", ignoreCase = true)) {
            OffsetDateTime.now()!!
        } else {
            try {
                DateUtilities.getDateTime(args[1], format = null)
            } catch (ex: DateTimeParseException) {
                error("Invalid date value in date arg: ${ex.message}")
            }
        }
        val intervalPeriod = Period.parse(args[2])
        val intervalStart = if (intervalPeriod.isNegative) intervalDate.plus(intervalPeriod) else intervalDate
        val intervalEnd = if (intervalPeriod.isNegative) intervalDate else intervalDate.plus(intervalPeriod)

        // Add indexes to form a selection
        val selection = Selection.withRange(0, 0)
        val column = table.stringColumn(args[0])
        column.forEachIndexed { index, value ->
            try {
                val dateTime = DateUtilities.getDateTime(value, format = null)
                if (!dateTime.isBefore(intervalStart) && dateTime.isBefore(intervalEnd)) {
                    selection.add(index)
                }
            } catch (_: DateTimeException) {
            } catch (_: DateTimeParseException) {
            }
        }
        return selection
    }
}