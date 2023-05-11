package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonInclude

data class PaginationApiResponse(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val totalPages: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val previousPage: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val nextPage: Int? = null
)

data class MetaApiResponse(
    val type: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val totalCount: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val totalFilteredCount: Int? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val paginationApiResponse: PaginationApiResponse? = null
)

data class ApiResponse<T>(val data: T, val metaApiResponse: MetaApiResponse)