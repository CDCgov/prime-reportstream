package gov.cdc.prime.router

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
interface JurisdictionalFilter {
    /**
     * Name of the filter function
     */
    val name: String

    /**
     * @args values passed to the filter
     *
     * @return the Selection object to be used to filter the
     * tablesaw table.
     */
    fun getSelection(args: List<String>, table: Table): Selection
}

/**
 * Implements the most basic filter.
 * matches(columnName, regex)
 */
class Matches : JurisdictionalFilter {
    override val name = "matches"

    override fun getSelection(args: List<String>, table: Table): Selection {
        if (args.size != 2) error("Expecting two args to filter $name:  (columnName, regex)")
        val columnName = args[0]
        val pattern = args[1]
        return table.stringColumn(columnName).matchesRegex(pattern)
    }
}

/**
 * Implements the opposite of the matches filter.
 * Regexes have a hard time with "not", and this just seemed clearner
 * and more obvious to the user what's going on.
 * does_not_match(columnName, val, val, ...)
 *
 * A row of data is "allowed" if it does not match any of the values.
 */
class DoesNotMatch : JurisdictionalFilter {
    override val name = "doesNotMatch"

    override fun getSelection(args: List<String>, table: Table): Selection {
        if (args.size < 2) error("Expecting two or more args to filter $name:  (columnName, value, value, ...)")
        val columnName = args[0]
        val pattern = args[1]
        val values = args.subList(1, args.size)
        return table.stringColumn(columnName).isNotIn(values)
    }
}

/**
 * This may or may not be a unicorn.
 */
class FilterByCounty : JurisdictionalFilter {
    override val name = "filterByCounty"

    // @todo need tons of error checking.
    override fun getSelection(args: List<String>, table: Table): Selection {
        if (args.size != 2) error("Expecting two args to filter $name:  (TwoLetterState, County)")
        val patientState = table.stringColumn("patient_state")
            ?: error("Unable to filterByCounty:  column patient_state not found.")
        val patientCounty = table.stringColumn("patient_county")
            ?: error("Unable to filterByCounty:  column patient_county not found.")
        val facilityState = table.stringColumn("ordering_facility_state")
            ?: error("Unable to filterByCounty:  column ordering_facility_state not found.")
        val facilityCounty = table.stringColumn("ordering_facility_county")
            ?: error("Unable to filterByCounty:  column ordering_facility_county not found.")

        // Try to be very loose on county matching.   Anything with the county name embedded is ok.
        val countyRegex = "(?i).*${args[1]}.*"
        // Overall, this is "true" if either the patient is in the county/state
        //   OR, if the facility is in the county/state.
        val patientSelection = patientState.isEqualTo(args[0]).and(patientCounty.matchesRegex(countyRegex))
        val facilitySelection = facilityState.isEqualTo(args[0]).and(facilityCounty.matchesRegex(countyRegex))
        return patientSelection.or(facilitySelection)
    }
}

object JurisdictionalFilters {
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
}