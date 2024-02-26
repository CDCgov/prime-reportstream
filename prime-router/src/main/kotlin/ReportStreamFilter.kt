package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.prime.router.cli.ObservationMappingConstants
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.deleteResource
import gov.cdc.prime.router.fhirengine.utils.getMappedConditions
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Resource
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * A ReportStreamFilter is the use (call) of one or more ReportStreamFilterDefinitions.
 */
typealias ReportStreamFilter = List<String>

/**
 * Interface for determining if a bundle passes a filter
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(FHIRExpressionFilter::class, name = "fhirExpression"),
    JsonSubTypes.Type(BundleResourceFilter::class, name = "bundleResource"),
    JsonSubTypes.Type(BundleObservationFilter::class, name = "bundleObservation"),
    JsonSubTypes.Type(BundleConditionFilter::class, name = "bundleCondition"),
)
interface BundleFilterable {
    /**
     * Check if a [bundle] passes this filter
     * @return whether the bundle passed
     */
    fun pass(bundle: Bundle): Boolean
}

/**
 * Interface for pruning T resources from a bundle
 */
/**
 * A bundle filter that uses resources as the basis for filtering
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ConditionCodePruner::class, name = "conditionCode"),
    JsonSubTypes.Type(ConditionKeywordPruner::class, name = "conditionKeyword"),
    JsonSubTypes.Type(FHIRExpressionPruner::class, name = "fhirExpression"),
)
interface BundlePrunable<T> {
    /**
     * Check if a [resource] in a [bundle] passes this filter
     * @return whether the observation passed
     */
    fun evaluateResource(bundle: Bundle, resource: T): Boolean

    fun fetchResources(bundle: Bundle): List<T>

    /**
     * Check if a [bundle] passes this filter
     * @return whether the bundle passed
     */
    fun prune(bundle: Bundle): List<T>
}

open class BundleResourceFilter<T : Resource>(val resourceFilter: BundlePrunable<T>) : BundleFilterable {
    override fun pass(bundle: Bundle): Boolean {
        return resourceFilter.fetchResources(bundle).any {
            resourceFilter.evaluateResource(bundle, it)
        }
    }
}

class BundleObservationFilter(
    val observationFilter: BundlePrunable<Observation>,
) : BundleResourceFilter<Observation>(observationFilter)

class BundleConditionFilter(
    val conditionFilter: BundlePrunable<Observation>,
) : BundleResourceFilter<Observation>(conditionFilter) {
    override fun pass(bundle: Bundle): Boolean =
        conditionFilter.fetchResources(bundle).filter { observation ->
            conditionFilter.evaluateResource(bundle, observation)
        }.let {
            // never pass a bundle with only AOE conditions
            val conditions = it.getMappedConditions()
            it.isNotEmpty() && (
                conditions.isEmpty() ||
                    !conditions.all { it.equals("AOE", true) }
                )
        }
}

/**
 * Interface for pruning observations from a bundle
 */
interface ObservationPrunable : BundlePrunable<Observation> {
    /**
     * Check if an observation [resource] in a [bundle] passes this filter
     * @return whether the observation passed
     */
    override fun evaluateResource(bundle: Bundle, resource: Observation): Boolean

    override fun fetchResources(bundle: Bundle): List<Observation> = bundle.getObservations()

    /**
     * Evaluate this filter on a [bundle]'s observations and optionally [filter] them from the bundle
     * @return the result of running the observation filter
     */
    override fun prune(bundle: Bundle): List<Observation> =
        bundle.getObservations().filterNot { observation ->
            evaluateResource(bundle, observation).also { pass ->
                if (!pass) bundle.deleteResource(observation)
            }
        }
}

/**
 * A filter that checks if any of a bundle's observations is stamped with a condition code
 * @param value A comma-delimited list of condition codes
 * @property codeList A list of condition code strings
 */
open class ConditionCodePruner(val codes: String) : ObservationPrunable {
    open val codeList = codes.split(",").map { it.trim() }

    override fun evaluateResource(bundle: Bundle, resource: Observation): Boolean =
        resource.getMappedConditions().any(codeList::contains)
}

/**
 * A filter that checks if any of a bundle's observations is stamped with a condition keyword
 * @param value A comma-delimited list of condition keywords
 * @property codeList A list of condition code strings looked up using condition keywords
 */
class ConditionKeywordPruner(val keywords: String) : ConditionCodePruner(keywords) {
    override val codeList = getConditionCodes(keywords)

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
        }.distinct().toList()
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
    val fhirExpression: String,
    val defaultResponse: Boolean = true,
    val reverseFilter: Boolean = false,
) : BundleFilterable {
    override fun pass(bundle: Bundle) = evaluateFhirExpression(
        fhirExpression,
        bundle,
        bundle,
        defaultResponse,
        reverseFilter
    )
}

/**
 * A filter that evaluates a FHIR expression on a bundle's observations
 *
 * @param filters A list of FHIR expressions to run
 * @param useOr What operand to use to combine filter results (&& or ||)
 * @param defaultResponse What the default response should be for empty filters (deprecated?)
 * @param reverseFilter Whether the filter result should be reversed
 */
class FHIRExpressionPruner(
    val fhirExpression: String,
    val defaultResponse: Boolean = true,
    val reverseFilter: Boolean = false,
) : ObservationPrunable {
    override fun evaluateResource(bundle: Bundle, resource: Observation) = evaluateFhirExpression(
        fhirExpression,
        bundle,
        resource,
        defaultResponse,
        reverseFilter
    )
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
    OBSERVATION_FILTER("observationFilter"),
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
    val observationFilter: List<BundleResourceFilter<Observation>>? = null,
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

/**
 * Check if a [bundle] passes this filter with an optional [focusResource]
 * @return the result of running the filter
 */
fun evaluateFhirExpression(
    fhirExpression: String,
    bundle: Bundle,
    focusResource: Base,
    defaultResponse: Boolean,
    reverseFilter: Boolean,
): Boolean {
    if (fhirExpression.isEmpty()) return defaultResponse
    val log = mutableListOf<ActionLogDetail>()
    try {
        return FhirPathUtils.evaluateCondition(bundle, focusResource, fhirExpression) == !reverseFilter
    } catch (e: SchemaException) {
        log.add(EvaluateFilterConditionErrorMessage(e.message)) // TODO: do something with log
        return false
    }
}