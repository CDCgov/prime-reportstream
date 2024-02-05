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
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * A ReportStreamFilter is the use (call) of one or more ReportStreamFilterDefinitions.
 */
typealias ReportStreamFilter = List<String>
typealias ReportStreamConditionFilter = List<ConditionFilter>

data class ConditionFilterResult(
    val pass: Boolean = false,
    val log: List<ActionLogDetail> = emptyList(),
    val fails: Map<String, List<Observation>> = emptyMap(),
)

fun ConditionFilterResult.and(other: ConditionFilterResult) = this.andOr(other, true)
fun ConditionFilterResult.or(other: ConditionFilterResult) = this.andOr(other)
fun ConditionFilterResult.andOr(other: ConditionFilterResult, useOr: Boolean = true) = ConditionFilterResult(
    if (useOr) other.pass || this.pass else other.pass && this.pass,
    other.log + this.log,
    this.fails.toMutableMap().also { resultMap ->
        other.fails.forEach {
            resultMap[it.key] = resultMap.getOrDefault(it.key, emptyList()) + it.value
        }
    }
)

fun List<ConditionFilter>.evaluate(
    bundle: Bundle,
    filter: Boolean = true,
    useOr: Boolean = true,
    perResult: (
    (result: ConditionFilterResult) -> Unit
)? = null,
): ConditionFilterResult {
    return this.fold(ConditionFilterResult(!useOr)) { result, conditionFilter ->
        perResult?.invoke(result)
        result.andOr(conditionFilter.evaluate(bundle, filter), useOr)
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CodeStringConditionFilter::class, name = "codeString"),
    JsonSubTypes.Type(ConditionLookupConditionFilter::class, name = "conditionLookup"),
    JsonSubTypes.Type(FHIRExpressionConditionFilter::class, name = "fhirExpression"),
)
abstract class ConditionFilter(val value: String) {
    abstract fun evaluate(
        bundle: Bundle,
        filter: Boolean = true,
    ): ConditionFilterResult
}

abstract class ObservationConditionFilter(value: String) : ConditionFilter(value) {
    abstract fun evaluateObservation(bundle: Bundle, observation: Observation): ConditionFilterResult

    override fun evaluate(bundle: Bundle, filter: Boolean): ConditionFilterResult {
        val passedObservations = mutableListOf<Observation>()
        var result = bundle.getObservations().fold(ConditionFilterResult()) { result, observation ->
            val obsResult = evaluateObservation(bundle, observation)
            if (obsResult.pass) {
                passedObservations.add(observation)
            } else if (filter) {
                bundle.deleteResource(observation)
            }
            result.or(obsResult)
        }
        if (result.pass && passedObservations.all { it.getMappedConditions().all { code -> code == "AOE" } }) {
            // TODO: all AOE add appropriate message
            result = ConditionFilterResult(false, result.log, result.fails)
        }
        return result
    }
}

open class CodeStringConditionFilter(value: String) : ObservationConditionFilter(value) {
    open val codeList = value.split(",").map { it.trim() }

    override fun evaluateObservation(bundle: Bundle, observation: Observation): ConditionFilterResult {
        val passes = observation.getMappedConditions().any(codeList::contains)
        return if (passes) {
            ConditionFilterResult(passes)
        } else {
            ConditionFilterResult(passes, fails = mapOf(codeList.joinToString(",") to listOf(observation)))
        }
    }
}

class ConditionLookupConditionFilter(value: String) : CodeStringConditionFilter(value) {
    override val codeList = getConditionCodes(value)

    private fun getConditionCodes(value: String): List<String> {
        val conditionStrings = value.split(",").map { it.trim() }
        val metadata = Metadata.getInstance()
        val table = metadata.findLookupTable("observation-mapping")!!

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

class FHIRExpressionConditionFilter(
    value: String,
    val filters: List<String>,
    val useOr: Boolean = true,
    private val defaultResponse: Boolean = true,
    private val reverseFilter: Boolean = false,
) : ObservationConditionFilter(value) {
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
    override fun evaluateObservation(bundle: Bundle, observation: Observation): ConditionFilterResult {
        if (filters.isEmpty()) {
            return ConditionFilterResult(defaultResponse)
        }
        val failingFilters = mutableListOf<String>()
        val exceptionFilters = mutableListOf<String>()
        val successfulFilters = mutableListOf<String>()
        val log = mutableListOf<ActionLogDetail>()
        filters.forEach { filterElement ->
            try {
                val filterElementResult = FhirPathUtils.evaluateCondition(
                    CustomContext(bundle, observation, shorthandLookupTable, CustomFhirPathFunctions()),
                    observation,
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
                log.add(EvaluateFilterConditionErrorMessage(e.message))
                exceptionFilters += filterElement
            }
        }

        val filterResult = if (useOr) successfulFilters.isNotEmpty() else failingFilters.isEmpty()
        val interestFilters = if (useOr) successfulFilters else failingFilters

        return if (exceptionFilters.isNotEmpty()) {
            ConditionFilterResult(
                false,
                log,
                exceptionFilters.associate { Pair("(exception found) $it", listOf(observation)) }
            )
        } else if (filterResult) {
            if (reverseFilter) {
                ConditionFilterResult(
                    false,
                    log,
                    interestFilters.associate { Pair("(exception found) $it", listOf(observation)) }
                )
            } else {
                ConditionFilterResult(true, log)
            }
        } else {
            if (reverseFilter) {
                ConditionFilterResult(true, log)
            } else {
                ConditionFilterResult(false, log)
            }
        }
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
    val mappedConditionFilter: ReportStreamConditionFilter? = null,
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