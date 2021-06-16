package gov.cdc.prime.router

import org.apache.logging.log4j.kotlin.Logging
import tech.tablesaw.api.Table
import tech.tablesaw.selection.Selection

/**
 * A *JurisdictionalFilter* can be used in the jurisdictionalFilter property in an OrganizationService.
 * It allowed you to create arbitrarily complex filters on data.
 * Each filter in the list does a "and" boolean operation with the other filters in the list.
 * Here is an example use:
 *  `jurisdictionalFilter: { FilterByPatientOrFacilityLoc(AZ, Pima) }`
 *
 * The name `filterByPatientOrFacility` then maps via pseudo-reflection to an implementation of JurisdictionalFilter
 * here.
 *
 * If you add a implementation here, you have to add it to the list of jurisdictionalFilters in Metadata.kt.
 *
 * A JurisdictionFilter is stateless.   It has a name property, which should be used in the filter definition -
 * basically a simple way of implementing Reflection.
 *
 * Currently JurisdictionFilter implements its filtering by just re-using the very rich `Selection` functionality already in tablesaw.
 *
 * Hoping we implement some geospatial searches someday.
 *
 */
interface JurisdictionalFilter : Logging {
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
}

/**
 * Implements a regex match.  If any of the regexes matches, the row is selected.
 * If the column name does not exist, nothing passes thru the filter.
 * matches(columnName, regex, regex, regex)
 */
class Matches : JurisdictionalFilter {
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
class DoesNotMatch : JurisdictionalFilter {
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
        if (selection.size() < table.rowCount()) {
            JurisdictionalFilters.logFiltering(
                Selection.withRange(0, table.rowCount()), selection,
                "$name(${args.joinToString(",")})",
                receiver,
                doAuditing
            )
        }
        return selection
    }
}

/**
 * This may or may not be a unicorn.
 */
class FilterByCounty : JurisdictionalFilter {
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
class OrEquals : JurisdictionalFilter {
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
 * A filter that filter nothing -- allows all data through
 */
class AllowAll : JurisdictionalFilter {
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
 * Implements a quality check match.  If a row has valid data for all the columns, the row is selected.
 * If any column name does not exist, nothing passes thru the filter.
 * hasValidDataFor(columnName1, columnName2, columnName3, ...)
 * If no columns are passed, all rows are selected.  So, any number of args is acceptable.
 */
class HasValidDataFor : JurisdictionalFilter {
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
                val before = Selection.with(*selection.toArray()) // hack way to copy to a new Selection obj
                selection = selection.andNot(table.stringColumn(colName).isEmptyString)

                JurisdictionalFilters.logFiltering(before, selection, "$name($colName)", receiver, doAuditing)
            } else {
                JurisdictionalFilters.logAllEliminated(
                    table.rowCount(),
                    "$name($colName): column not found",
                    receiver,
                    doAuditing
                )
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
class IsValidCLIA : JurisdictionalFilter {
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
        var atLeastOneColumnFound = false
        args.forEach { colName ->
            if (columnNames.contains(colName)) {
                selection = selection.or(
                    table.stringColumn(colName).lengthEquals(10).and(table.stringColumn(colName).isAlphaNumeric)
                )
                atLeastOneColumnFound = true
            }
        }
        if (!atLeastOneColumnFound) {
            JurisdictionalFilters.logAllEliminated(
                table.rowCount(),
                "$name(${args.joinToString(",")}): none of these columns found.",
                receiver,
                doAuditing
            )
        } else {
            if (selection.size() < table.rowCount()) {
                JurisdictionalFilters.logFiltering(
                    Selection.withRange(0, table.rowCount()), selection,
                    "$name(${args.joinToString(",")})", receiver, doAuditing
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
class HasAtLeastOneOf : JurisdictionalFilter {
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
        var atLeastOneColumnFound = false
        args.forEach { colName ->
            if (columnNames.contains(colName)) {
                selection = selection.or(table.stringColumn(colName).isNotMissing)
                atLeastOneColumnFound = true
            }
        }
        if (!atLeastOneColumnFound) {
            JurisdictionalFilters.logAllEliminated(
                table.rowCount(),
                "$name(${args.joinToString(",")}): none of these columns found.", receiver, doAuditing
            )
        } else {
            if (selection.size() < table.rowCount()) {
                JurisdictionalFilters.logFiltering(
                    Selection.withRange(0, table.rowCount()), selection,
                    "$name(${args.joinToString(",")})", receiver, doAuditing
                )
            }
        }
        return selection
    }
}

object JurisdictionalFilters : Logging {
    // covid-19 default quality check consists of these filters
    // todo move this to a GLOBAL Setting in the settings table
    val defaultCovid19QualityCheck = listOf(
        // valid human and valid test
        "hasValidDataFor(" +
            "message_id," +
            "equipment_model_name," +
            "specimen_type," +
            "test_result," +
            "patient_last_name," +
            "patient_first_name," +
            "patient_dob" +
            ")",
        // has minimal valid location or other contact info (for contact tracing)
        "hasAtLeastOneOf(patient_street,patient_zip_code,patient_phone_number,patient_email)",
        // has valid date (for relevance/urgency)
        "hasAtLeastOneOf(order_test_date,specimen_collection_date_time,test_result_date)",
        // has at least one valid CLIA
        "isValidCLIA(testing_lab_clia,reporting_facility_clia)",
        // never send T (Training/Test) or D (Debug) data to the states.
        "doesNotMatch(processing_mode_code,T,D)",
    )

    /**
     * Map from topic-name to a list of filter-function-strings
     */
    val defaultQualityFilters: Map<String, List<String>> = mapOf(
        "covid-19" to defaultCovid19QualityCheck,
        "CsvFileTests-topic" to listOf("hasValidDataFor(lab,state,test_time,specimen_id,observation)"),
    )

    /**
     * filterFunction must be of form "funName(arg1, arg2, etc)"
     */
    fun parseJurisdictionalFilter(filterFunction: String): Pair<String, List<String>> {
// REMOVE        val match = Regex("([a-zA-Z0-9]+)\\x28([a-z, \\x2E_\\x2DA-Z0-9]*)\\x29").find(filterFunction)
        // Using a permissive match in the (arg1, arg2) section, to allow most regexs to be passed as args.
        // Somehow this works with "(?i).*Pima.*", I guess because the \\x29 matches rightmost ')' char
        val match = Regex("([a-zA-Z0-9]+)\\x28(.*)\\x29").find(filterFunction)
            ?: error("JurisdictionalFilter field $filterFunction does not parse")
        return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
    }

    fun logAllEliminated(beforeSize: Int, filterDescription: String, receiver: Receiver, doAuditing: Boolean) {
        if (!doAuditing) return
        logger.warn(
            "For ${receiver.fullName}, qualityFilter $filterDescription" +
                " reduced the Items from $beforeSize to 0.  All rows eliminated"
        )
    }

    fun logFiltering(
        before: Selection,
        after: Selection,
        filterDescription: String,
        receiver: Receiver,
        doAuditing: Boolean
    ) {
        if (!doAuditing) return

        if (after.size() < before.size()) {
            if (after.size() == 0) {
                logAllEliminated(before.size(), filterDescription, receiver, true)
            } else {
                // Note:  the expression 'before.andNot(after)' actually changes the 'before' obj!
                val beforeSize = before.size()
                val eliminatedRows = before.andNot(after)
                logger.warn(
                    "For ${receiver.fullName}, qualityFilter $filterDescription" +
                        " reduced the Item count from $beforeSize to ${after.size()}.  " +
                        "Row numbers eliminated: ${eliminatedRows.joinToString(",")}"
                )
            }
        }
    }
}