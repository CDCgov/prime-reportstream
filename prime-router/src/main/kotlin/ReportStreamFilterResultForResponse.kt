package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Response use for the API for the filtered report items. This removes unneeded properties that exist in
 * ReportStreamFilterResult. ReportStreamFilterResult is used to serialize and deserialize to/from the database.
 * @param filterResult the filter result to use
 */
data class ReportStreamFilterResultForResponse(@JsonIgnore private val filterResult: ReportStreamFilterResult) {
    val filterType = filterResult.filterType
    val filterName = filterResult.filterName
    val filteredTrackingElement = filterResult.filteredTrackingElement
    val filterArgs = filterResult.filterArgs
    val message = filterResult.message
}