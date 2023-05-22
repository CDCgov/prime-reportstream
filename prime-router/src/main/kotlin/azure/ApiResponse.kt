package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped

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
)

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
data class ApiResponse<T>(val data: T, @JsonProperty("meta") val metaApiResponse: MetaApiResponse)