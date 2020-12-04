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
 * @todo Clean up this comment:
 * A mapper object is stateless. It has a name which corresponds to
 * the function name in the property. It has a set of arguments, which
 * corresponding to the arguments of the function. Before applying the
 * mapper, the elementName list is generated from the arguments. All element values
 * are then fetched and provided to the apply function.
 */
interface JurisdictionalFilter {
    /**
     * Name of the filter function
     */
    val name: String

    /**
     * @args values eg, "AZ", "Pima"
     */
    fun getSelection(args: List<String>, table: Table): Selection
}

class FilterByCounty : JurisdictionalFilter {
    override val name = "filterByCounty"

    override fun getSelection(args: List<String>, table: Table): Selection {
        if (args.size != 2) error("Expecting two args to filter $name:  (TwoLetterState, County)")
        val patientState = table.stringColumn("standard.patient_state")
        val patientCounty = table.stringColumn("standard.patient_county")
        val facilityState = table.stringColumn("standard.ordering_facility_state")
        val facilityCounty = table.stringColumn("standard.ordering_facility_county")

        // Try to be very loose on county matching.   Anything with the county name embedded is ok.
        val countyRegex =  "(?i).*${args[2]}.*"
        // Overall, this is "true" if either the patient is in the county/state
        //   OR, if the facility is in the county/state.
        val patientSelection = patientState.isEqualTo(args[1]).and(patientCounty.matchesRegex(args[2]))
        val facilitySelection = facilityState.isEqualTo(args[1]).and(facilityCounty.matchesRegex(args[2]))
        return patientSelection.or(facilitySelection)
    }
}


class FilterByZip : JurisdictionalFilter {
    override val name = "filterByZip"


    override fun getSelection(args: List<String>, table: Table): Selection {
        return table.stringColumn("foo").matchesRegex("foobar")
    }

}




object JurisdictionalFilters {
    /**
     * filterFunction must be of form "funName(arg1, arg2, etc)"
     */
    fun parseJurisdictionalFilter(filterFunction: String): Pair<String, List<String>> {
        val match = Regex("([a-zA-Z0-9]+)\\x28([a-z, \\x2E_\\x2DA-Z0-9]*)\\x29").find(filterFunction)
            ?: error("JurisdictionalFilter field $filterFunction does not parse")
        return match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }
    }
}

