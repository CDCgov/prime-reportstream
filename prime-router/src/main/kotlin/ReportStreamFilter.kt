package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * A ReportStreamFilter is the use (call) of one or more ReportStreamFilterDefinitions.
 */
typealias ReportStreamFilter = List<String>
typealias ReportStreamConditionFilter = List<ConditionFilter>
typealias ReportStreamReceiverRoutingFilter = List<FHIRExpressionFilter>

fun ReportStreamConditionFilter.codes(): List<String> = this.flatMap { it.codes() }

data class FHIRExpressionFilter(
    val filterName: String,
    val filterDescription: String,
    val filterExpressions: List<String>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CodeStringConditionFilter::class, name = "codeString"),
    JsonSubTypes.Type(ConditionLookupConditionFilter::class, name = "conditionLookup"),
    JsonSubTypes.Type(FHIRExpressionConditionFilter::class, name = "fhirExpression"),
)
abstract class ConditionFilter(val value: String) {
    abstract fun codes(): List<String>

    override fun toString(): String = value
}

class CodeStringConditionFilter(value: String) : ConditionFilter(value) {
    private val codeList = value.split(",").map { it.trim() }
    override fun codes() = codeList
}

class ConditionLookupConditionFilter(value: String) : ConditionFilter(value) {
    override fun codes() = emptyList<String>()
}

class FHIRExpressionConditionFilter(value: String) : ConditionFilter(value) {
    override fun codes() = emptyList<String>()
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