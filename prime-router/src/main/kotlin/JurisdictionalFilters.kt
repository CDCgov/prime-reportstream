package gov.cdc.prime.router

import tech.tablesaw.api.Table
import tech.tablesaw.api.Row
import tech.tablesaw.selection.Selection


/**
 * A *JurisdictionalFilter* can be used in the jurisdictionalFilter property in an OrganizationService.
 * It allowed you to create arbitrarily complex filters on data.
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
        System.out.println("Searching column $columnName for $pattern")
        return table.stringColumn(columnName).matchesRegex(pattern)
    }
}


class FilterByCounty : JurisdictionalFilter {
    override val name = "filterByCounty"

    // @todo need tons of error checking.
    override fun getSelection(args: List<String>, table: Table): Selection {
        if (args.size != 2) error("Expecting two args to filter $name:  (TwoLetterState, County)")
        val patientState = table.stringColumn("standard.patient_state")
        val patientCounty = table.stringColumn("standard.patient_county")
        val facilityState = table.stringColumn("standard.ordering_facility_state")
        val facilityCounty = table.stringColumn("standard.ordering_facility_county")

        // Try to be very loose on county matching.   Anything with the county name embedded is ok.
        val countyRegex =  "(?i).*${args[1]}.*"
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
//REMOVE        val match = Regex("([a-zA-Z0-9]+)\\x28([a-z, \\x2E_\\x2DA-Z0-9]*)\\x29").find(filterFunction)
        // Using a permissive match in the (arg1, arg2) section, to allow most regexs to be passed as args.
        // Somehow this works with "(?i).*Pima.*", I guess because the \\x29 matches rightmost ')' char
        val match = Regex("([a-zA-Z0-9]+)\\x28(.*)\\x29").find(filterFunction)
            ?: error("JurisdictionalFilter field $filterFunction does not parse")
        return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
    }
}

