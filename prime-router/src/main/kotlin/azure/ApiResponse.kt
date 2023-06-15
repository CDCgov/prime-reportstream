package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import kotlin.math.ceil

/**
 * Contains the metadata on the pagination for an API response
 *
 * @param totalPages the total number of pages available
 * @param previousPage the index (1-based) of the previous page
 * @param nextPage the index (1-based) of the nexdt page
 */
data class PaginationApiResponse(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val totalPages: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val previousPage: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val nextPage: Int? = null
) {
    /**
     * Static functions for generating a [PaginationApiResponse]
     */
    companion object {

        /**
         * Conversion function going from an [ApiSearch] and [ApiSearchResult] into an [PaginationApiResponse]
         * It's responsible for calculating the total pages, next and previous page
         *
         * @param search the [ApiSearchResult] used to retrieve the results
         * @param results the [ApiSearchResult] resulting running the search
         */
        fun buildPaginationFromApiSearch(
            search: ApiSearch<*, *, *>,
            results: ApiSearchResult<*>
        ): PaginationApiResponse {
            val totalPages = ceil(results.filteredCount.toDouble() / search.limit).toInt()
            return PaginationApiResponse(
                totalPages = totalPages,
                previousPage = if (search.page == 1) null else search.page - 1,
                nextPage = if (totalPages < search.page + 1) null else search.page + 1
            )
        }
    }
}

/**
 * Metadata on a return API response
 *
 * @param type the kind of return data for the response i.e. "PublicKey"
 * @param totalCount the number of items in the returned data
 * @param totalFilteredCount the number of items included after the filter was applied
 * @param paginationApiResponse the pagination data for the response
 */
data class MetaApiResponse(
    val type: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val totalCount: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val totalFilteredCount: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonUnwrapped
    val paginationApiResponse: PaginationApiResponse? = null
)

/**
 * A generic wrapper for an API response
 *
 * @param data a JSON serializable object that contains the data in the response
 * @param metaApiResponse the metadata about the response
 */
data class ApiResponse<T>(val data: T, @JsonProperty("meta") val metaApiResponse: MetaApiResponse) {
    /**
     * Static functions to help parse [ApiResponse]
     */
    companion object {
        /**
         * Conversion function going from an [ApiSearch] and [ApiSearchResult] into an [ApiResponse]
         * It's responsible for calculating the pagination response and settings the remaining fields for the response
         *
         * @param type the type of the results
         * @param search the [ApiSearchResult] used to retrieve the results
         * @param results the [ApiSearchResult] resulting running the search
         */
        fun <T> buildFromApiSearch(
            type: String,
            search: ApiSearch<T, *, *>,
            results: ApiSearchResult<T>
        ): ApiResponse<List<T>> {
            return ApiResponse(
                results.results,
                MetaApiResponse(
                    totalCount = results.totalCount,
                    totalFilteredCount = results.filteredCount,
                    paginationApiResponse = PaginationApiResponse.buildPaginationFromApiSearch(search, results),
                    type = type
                )
            )
        }
    }
}