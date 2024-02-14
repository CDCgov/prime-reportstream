package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ReportStreamFilterDefinition.Companion.logger
import gov.cdc.prime.router.cli.ObservationMappingConstants
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.deleteResource
import gov.cdc.prime.router.fhirengine.utils.getMappedConditions
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * A ReportStreamFilter is the use (call) of one or more ReportStreamFilterDefinitions.
 */
typealias ReportStreamFilter = List<String>

/**
 * The result of running a filter on a bundle
 *
 * @property pass whether the bundle passed the filter
 * @property failingFilter which filter the bundle failed
 */
open class FilterResult(
    val pass: Boolean = false,
    val failingFilter: String? = null,
) {
    /**
     * Convenience function to AND two FilterResults together
     * @return the result of the AND operation
     */
    fun and(other: FilterResult) = this.andOr(other, false)

    /**
     * Convenience function to OR two FilterResults together
     * @return the result of the OR operation
     */
    fun or(other: FilterResult) = this.andOr(other)

    /**
     * Combines two FilterResults using the operand (&& or ||) specified by [useOr] on the pass member
     * @return a combined FilterResult using the specified operand and concatenated failingFilters
     */
    fun andOr(other: FilterResult, useOr: Boolean = true) = FilterResult(
        if (useOr) other.pass || this.pass else other.pass && this.pass, // use specified operand
        listOf(this.failingFilter, other.failingFilter).joinToString(",") // combine strings as array tokens
    )
}

/**
 *  The result of running an observation filter on a bundle
 *
 *  @property pass whether the bundle passed the filter
 *  @property failingFilter which filter the bundle failed; equivalent to failingObservations.keys
 *  @property failingObservations a map of failed observations grouped by the filter they failed
 */
class ObservationFilterResult(
    pass: Boolean = false,
    failingFilter: String? = null,
    val failingObservations: Map<String, List<Observation>> = emptyMap(),
) : FilterResult(pass, failingFilter) {
    /**
     * Convenience function to AND two ObservationFilterResults together
     * @return the result of the AND operation
     */
    fun and(other: ObservationFilterResult) = this.andOr(other, false)

    /**
     * Convenience function to OR two ObservationFilterResults together
     * @return the result of the OR operation
     */
    fun or(other: ObservationFilterResult) = this.andOr(other)

    /**
     * Combines two ObservationFilterResults using the operand (&& or ||) specified by [useOr] on the pass member
     * @return a combined ObservationFilterResult using the specified operand, concatenated failingFilters, and merged
     *         failingObservations
     */
    fun andOr(other: ObservationFilterResult, useOr: Boolean = true) =
        super.andOr(other, useOr).let { superResult ->
            ObservationFilterResult(
                superResult.pass,
                superResult.failingFilter,
                this.failingObservations.toMutableMap().also { allFiltered ->
                    other.failingObservations.forEach {
                        allFiltered[it.key] = allFiltered.getOrDefault(it.key, emptyList()) + it.value
                    }
                }
            )
        }
}

/**
 * Interface for determining if a bundle passes a filter
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CodeStringConditionFilter::class, name = "codeString"),
    JsonSubTypes.Type(ConditionLookupConditionFilter::class, name = "conditionLookup"),
    JsonSubTypes.Type(FHIRExpressionFilter::class, name = "fhirExpression"),
    JsonSubTypes.Type(FHIRExpressionConditionFilter::class, name = "fhirExpressionCondition"),
)
interface BundleFilterable {
    /**
     * Check if a [bundle] passes this filter
     * @return the result of running the filter
     */
    fun pass(bundle: Bundle): FilterResult

    companion object {
        /**
         * Check if a bundle passes a list of filters
         * @return the combined result of running all the filters (with [useOr] logic)
         */
        fun List<BundleFilterable>.pass(bundle: Bundle, useOr: Boolean = true): FilterResult {
            return this.fold(FilterResult(!useOr)) { result, filter ->
                result.andOr(filter.pass(bundle), useOr)
            }
        }
    }
}

/**
 * Interface for pruning observations from a bundle and determining if its observations pass a filter
 */
interface ObservationFilterable : BundleFilterable {
    /**
     * Check if an [observation] in a [bundle] passes this filter
     * @return the result of running the filter
     */
    fun evaluateObservation(bundle: Bundle, observation: Observation): FilterResult

    /**
     * Prune the observations in a [bundle]
     * @return the pruned observations grouped by the filter that pruned them
     */
    fun prune(bundle: Bundle): Map<String, List<Observation>> {
        val result = evaluate(bundle, true)
        return result.failingObservations
    }

    /**
     * Check if any of this [bundle]'s observations pass this filter
     * @return the result of running the filter on the bundle's observations
     */
    override fun pass(bundle: Bundle): FilterResult {
        val result = evaluate(bundle, false)
        return FilterResult(result.pass, result.failingFilter)
    }

    /**
     * Evaluate this filter on a [bundle]'s observations and optionally [filter] them from the bundle
     * @return the result of running the observation filter
     */
    fun evaluate(bundle: Bundle, filter: Boolean): ObservationFilterResult {
        val passingObservations = mutableListOf<Observation>()
        val failingObservations = mutableMapOf<String, MutableList<Observation>>()
        var result = bundle.getObservations().fold(ObservationFilterResult(true)) { result, observation ->
            // evaluate each observation and sort into appropriate data structure
            val obsResult = evaluateObservation(bundle, observation)
            if (obsResult.pass) {
                passingObservations.add(observation)
            } else {
                failingObservations.getOrPut(obsResult.failingFilter!!) { mutableListOf() }.add(observation)
                if (filter) bundle.deleteResource(observation) // optionally filter this observation from the bundle
            }
            result.andOr(ObservationFilterResult(obsResult.pass, obsResult.failingFilter)) // track filter result
        }
        //  never pass a bundle with only AOE conditions
        if (result.pass && passingObservations.all { it.getMappedConditions().all { code -> code == "AOE" } }) {
            return ObservationFilterResult(false, "All AOE Conditions", failingObservations)
        }
        return ObservationFilterResult(result.pass, result.failingFilter, failingObservations)
    }
}

/**
 * A filter that checks if any of a bundle's observations is stamped with a condition code
 * @param value A comma-delimited list of condition codes
 * @property codeList A list of condition code strings
 */
open class CodeStringConditionFilter(val value: String) : ObservationFilterable {
    open val codeList = value.split(",").map { it.trim() }

    override fun evaluateObservation(bundle: Bundle, observation: Observation): FilterResult {
        val passes = observation.getMappedConditions().any(codeList::contains)
        return FilterResult(passes, value)
    }
}

/**
 * A filter that checks if any of a bundle's observations is stamped with a condition keyword
 * @param value A comma-delimited list of condition keywords
 * @property codeList A list of condition code strings looked up using condition keywords
 */
class ConditionLookupConditionFilter(value: String) : CodeStringConditionFilter(value) {
    override val codeList = getConditionCodes(value)

    /**
     * Parse a string [value] comma-delimited list of condition keywords and lookup their condition codes
     * @return a list of condition codes as strings
     */
    private fun getConditionCodes(value: String): List<String> {
        // get observation mapping lookup table
        val conditionStrings = value.split(",").map { it.trim() }
        val metadata = Metadata.getInstance()
        val table = metadata.findLookupTable("observation-mapping")!!

        // check the condition name column of every row for the keyword
        return table.caseSensitiveDataRowsMap.mapNotNull { row ->
            val conditionName = row.getValue(ObservationMappingConstants.CONDITION_NAME_KEY)
            if (conditionStrings.any { conditionName.contains(it, true) }) {
                row.getValue(ObservationMappingConstants.CONDITION_CODE_KEY)
            } else {
                null
            }
        }.toList()
    }
}

/**
 * A filter that evaluates a FHIR expression on a bundle
 *
 * @param filters A list of FHIR expressions to run
 * @param useOr What operand to use to combine filter results (&& or ||)
 * @param defaultResponse What the default response should be for empty filters (deprecated?)
 * @param reverseFilter Whether the filter result should be reversed
 */
open class FHIRExpressionFilter(
    val filters: List<String>,
    val useOr: Boolean = true,
    private val defaultResponse: Boolean = true,
    private val reverseFilter: Boolean = false,
) : BundleFilterable {

    // TODO: remove with fhirpath filter shorhand filter (see https://github.com/CDCgov/prime-reportstream/issues/13252)
    companion object {
        /**
         * The name of the lookup table to load the shorthand replacement key/value pairs from
         */
        const val fhirPathFilterShorthandTableName = "fhirpath_filter_shorthand"

        /**
         * The name of the column in the shorthand replacement lookup table that will be used as the key.
         */
        const val fhirPathFilterShorthandTableKeyColumnName = "variable"

        /**
         * The name of the column in the shorthand replacement lookup table that will be used as the value.
         */
        const val fhirPathFilterShorthandTableValueColumnName = "fhirPath"

        /**
         * Lookup table `fhirpath_filter_shorthand` containing all the shorthand fhirpath replacements for filtering.
         */
        val shorthandLookupTable by lazy { loadFhirPathShorthandLookupTable() }

        /**
         * Load the fhirpath_filter_shorthand lookup table into a map if it can be found and has the expected columns,
         * otherwise log warnings and return an empty lookup table with the correct columns. This is valid since having
         * a populated lookup table is not required to run the universal pipeline routing
         *
         * @returns Map containing all the values in the fhirpath_filter_shorthand lookup table. Empty map if the
         * lookup table was not found, or it does not contain the expected columns. If an empty map is returned, a
         * warning indicating why will be logged.
         */
        fun loadFhirPathShorthandLookupTable(): MutableMap<String, String> {
            val metadata = Metadata.getInstance()
            val lookup = metadata.findLookupTable(fhirPathFilterShorthandTableName)
            // log a warning and return an empty table if either lookup table is missing or has incorrect columns
            return if (lookup != null &&
                lookup.hasColumn(fhirPathFilterShorthandTableKeyColumnName) &&
                lookup.hasColumn(fhirPathFilterShorthandTableValueColumnName)
            ) {
                lookup.table.associate {
                    it.getString(fhirPathFilterShorthandTableKeyColumnName) to
                        it.getString(fhirPathFilterShorthandTableValueColumnName)
                }.toMutableMap()
            } else {
                if (lookup == null) {
                    logger.warn("Unable to find $fhirPathFilterShorthandTableName lookup table")
                } else {
                    logger.warn(
                        "$fhirPathFilterShorthandTableName does not contain " +
                            "expected columns 'variable' and 'fhirPath'"
                    )
                }
                emptyMap<String, String>().toMutableMap()
            }
        }
    }

    /**
     * Check if a [bundle] passes this filter with an optional [focusResource]
     * @return the result of running the filter
     */
    fun evaluate(bundle: Bundle, focusResource: Base = bundle): FilterResult {
        if (filters.isEmpty()) {
            return FilterResult(defaultResponse)
        }
        val failingFilters = mutableListOf<String>()
        val exceptionFilters = mutableListOf<String>()
        val successfulFilters = mutableListOf<String>()
        val log = mutableListOf<ActionLogDetail>()
        filters.forEach { filterElement ->
            try {
                val filterElementResult = FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, focusResource, shorthandLookupTable, CustomFhirPathFunctions()),
                    focusResource,
                    bundle,
                    bundle,
                    filterElement
                )
                if (!filterElementResult) {
                    failingFilters += filterElement
                } else {
                    successfulFilters += filterElement
                }
            } catch (e: SchemaException) {
                log.add(EvaluateFilterConditionErrorMessage(e.message)) // TODO: do something with log
                exceptionFilters += filterElement
            }
        }

        // the filter result and filters of interest depend on our boolean logic; mind the dragons (to include return if)
        val filterResult = if (useOr) successfulFilters.isNotEmpty() else failingFilters.isEmpty()
        val interestFilters = if (useOr) successfulFilters else failingFilters

        return if (exceptionFilters.isNotEmpty()) {
            FilterResult(
                false,
                "(exception found) $exceptionFilters"
            )
        } else if (filterResult) {
            if (reverseFilter) {
                FilterResult(false, "(reversed) $interestFilters")
            } else {
                FilterResult(true)
            }
        } else {
            if (reverseFilter) {
                FilterResult(true)
            } else {
                FilterResult(false, failingFilters.toString())
            }
        }
    }

    override fun pass(bundle: Bundle): FilterResult {
        return evaluate(bundle)
    }
}

/**
 * A filter that evaluates a FHIR expression on a bundle's observations
 *
 * @param filters A list of FHIR expressions to run
 * @param useOr What operand to use to combine filter results (&& or ||)
 * @param defaultResponse What the default response should be for empty filters (deprecated?)
 * @param reverseFilter Whether the filter result should be reversed
 */
class FHIRExpressionConditionFilter(
    filters: List<String>,
    useOr: Boolean = true,
    defaultResponse: Boolean = true,
    reverseFilter: Boolean = false,
) : FHIRExpressionFilter(filters, useOr, defaultResponse, reverseFilter), ObservationFilterable {
    override fun evaluateObservation(bundle: Bundle, observation: Observation): FilterResult =
        this.evaluate(bundle, observation)

    override fun pass(bundle: Bundle): FilterResult {
        val result = this.evaluate(bundle, false)
        return FilterResult(result.pass, result.failingFilter)
    }
}

/**
 * Enum of Fields in the ReportStreamFilters, below.  Used to iterate thru all the filters in ReportStreamFilters
 */
enum class ReportStreamFilterType(val field: String) {
    JURISDICTIONAL_FILTER("jurisdictionalFilter"),
    QUALITY_FILTER("qualityFilter"),
    ROUTING_FILTER("routingFilter"),
    PROCESSING_MODE_FILTER("processingModeFilter"),
    CONDITION_FILTER("conditionFilter"),
    MAPPED_CONDITION_FILTER("mappedConditionFilter"),
    ;

    // Reflection, so that we can write a single routine to handle all types of filters.
    @Suppress("UNCHECKED_CAST")
    val filterProperty = ReportStreamFilters::class.memberProperties.first { it.name == this.field }
        as KProperty1<ReportStreamFilters, ReportStreamFilter?>

    @Suppress("UNCHECKED_CAST")
    val receiverFilterProperty = Receiver::class.memberProperties.first { it.name == this.field }
        as KProperty1<Receiver, ReportStreamFilter>
}

/**
 * The set of filtering objects available per Organization or per Receiver
 * (and someday, per Sender too?)
 *
 * Examples of FilterTypes that can be applied:
 *  @param jurisdictionalFilter - used to limit the data received or sent by geographical region
 *  @param qualityFilter - used to limit the data received or sent, by quality
 *  @param routingFilter - used to limit the data received or sent, by who sent it.
 *  @param processingModeFilter - used to limit the data received to be either "Training", "Debug", or "Production"
 *  @param conditionFilter - used to limit the data sent to the receiver by the conditions they want to receive.
 * We allow a different set of filters per [topic]
 */
data class ReportStreamFilters(
    val topic: Topic,
    val jurisdictionalFilter: ReportStreamFilter?,
    val qualityFilter: ReportStreamFilter?,
    val routingFilter: ReportStreamFilter?,
    val processingModeFilter: ReportStreamFilter?,
    val conditionFilter: ReportStreamFilter? = null,
    val mappedConditionFilter: List<ObservationFilterable>? = null,
) {

    companion object {

        // For each 'topic' that ReportStream handles, there is a set of default filters.
        // Currently these are defined here in code.
        // Each Organization and Receiver can override the defaults.
        // todo move all of these to a GLOBAL Setting in the settings table
        val defaultCovid19QualityFilter: ReportStreamFilter = listOf(
            // valid basic test info
            "hasValidDataFor(message_id)",
            "hasValidDataFor(equipment_model_name)",
            "hasValidDataFor(specimen_type)",
            "hasValidDataFor(test_result)",
            // valid basic human info
            "hasValidDataFor(patient_last_name, patient_first_name)",
            "hasValidDataFor(patient_dob)",
            // has minimal valid location or other contact info (for contact tracing)
            "hasAtLeastOneOf(patient_street,patient_zip_code,patient_phone_number,patient_email)",
            // has valid date (for relevance/urgency)
            "hasAtLeastOneOf(order_test_date,specimen_collection_date_time,test_result_date)",
            // has at least one valid CLIA
            "isValidCLIA(testing_lab_clia,reporting_facility_clia)",
        )
        private val defaultCovid19Filters = ReportStreamFilters(
            topic = Topic.COVID_19,
            jurisdictionalFilter = listOf("allowNone()"), // Receiver *must* override this to get data!
            qualityFilter = defaultCovid19QualityFilter,
            routingFilter = listOf("allowAll()"),
            processingModeFilter = listOf("doesNotMatch(processing_mode_code, T, D)") // No Training/Debug data
        )

        private val defaultMonkeypoxFilters = ReportStreamFilters(
            topic = Topic.MONKEYPOX,
            jurisdictionalFilter = listOf("allowNone()"), // Receiver *must* override this to get data!
            qualityFilter = listOf("allowAll()"),
            routingFilter = listOf("allowAll()"),
            processingModeFilter = listOf("doesNotMatch(processing_mode_code, T, D)") // No Training/Debug data
        )

        private val defaultTestFilters = ReportStreamFilters(
            topic = Topic.TEST,
            jurisdictionalFilter = null,
            qualityFilter = listOf("matches(a, no)"),
            routingFilter = listOf("matches(b, false)"),
            processingModeFilter = listOf("allowAll()")
        )

        /**
         * Map from topic-name to a list of filter-function-strings
         */
        val defaultFiltersByTopic: Map<Topic, ReportStreamFilters> = mapOf(
            defaultCovid19Filters.topic to defaultCovid19Filters,
            defaultMonkeypoxFilters.topic to defaultMonkeypoxFilters,
            defaultTestFilters.topic to defaultTestFilters
        )
    }
}