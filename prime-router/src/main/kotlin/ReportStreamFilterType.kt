package gov.cdc.prime.router

/**
 * Currently there are three ReportStreamFilterType*:
 * jurisdictionFilter - used to limit the data received or sent by geographical region
 * qualityFilter - used to limit the data received or sent, by quality
 * routingFilter - used to limit the data received or sent, by who sent it.
 * This allowed you to create arbitrarily complex filters on data.
 *
 * Each filter type has a defaultFilter
 * Then each Organization and Receiver can override that default.
 * Overrides can be set at both the organization and receiver levels.
 * The overrides are combined by "and"ing them together.
 *
 */
typealias ReportStreamFilter = List<String>

data class Filters(
    val jurisdictionalFilter: ReportStreamFilter,
    val qualityFilter: ReportStreamFilter,
    val routingFilter: ReportStreamFilter,
) {

    companion object {
        // covid-19 default quality check consists of these filters
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
            // never send T (Training/Test) or D (Debug) data to the states.
            "doesNotMatch(processing_mode_code,T,D)",
        )
        val defaultCovid19Filters = Filters(
            jurisdictionalFilter = listOf("allowNone()"), // Receiver *must* override this to get data!
            qualityFilter = defaultCovid19QualityFilter,
            routingFilter = listOf("allowAll()"), // Default is no routing restrictions.
        )

        val defaultTestFilters = Filters(
            jurisdictionalFilter = listOf("allowAll()"),
            qualityFilter = listOf("hasValidDataFor(lab,state,test_time,specimen_id,observation)"),
            routingFilter = listOf("allowAll()")
        )
        /**
         * Map from topic-name to a list of filter-function-strings
         */
        val defaultFiltersByTopic: Map<String, Filters> = mapOf(
            "covid-19" to defaultCovid19Filters,
            "CsvFileTests-topic" to defaultTestFilters,
        )
    }
}