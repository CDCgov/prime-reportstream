package gov.cdc.prime.router

import org.apache.logging.log4j.kotlin.Logging

/**
 * Currently there are three FilterType*:
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
class FilterType(
    val topic: String, // eg, 'covid-19'
    val defaultFilter: List<String>,
) : Logging {

    companion object : Logging {
        // covid-19 default quality check consists of these filters
        // todo move this to a GLOBAL Setting in the settings table
        val defaultCovid19QualityFilter = FilterType(
            "covid-19",
            listOf(
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
        )
        val testDefaultFilter = FilterType(
            "CsvFileTests-topic",
            listOf("hasValidDataFor(lab,state,test_time,specimen_id,observation)")
        )

        /**
         * Map from topic-name to a list of filter-function-strings
         */
        val defaultQualityFilters: Map<String, FilterType> = mapOf(
            defaultCovid19QualityFilter.topic to defaultCovid19QualityFilter,
            testDefaultFilter.topic to testDefaultFilter,
        )
    }
}