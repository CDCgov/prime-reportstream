package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class ApiResponseTest {

    private fun createResults(size: Int): List<ApiSearchTest.TestPojo> {
        val list = mutableListOf<ApiSearchTest.TestPojo>()
        for (i in 1..size) {
            list.add(ApiSearchTest.TestPojo("foo", OffsetDateTime.now()))
        }
        return list
    }

    @Test
    fun `Test create buildFromApiSearch from api search`() {
        val search = ApiSearchTest.TestApiSearch(emptyList(), null, page = 1, limit = 2)
        val results = ApiSearchResult(10, 6, createResults(2))
        val response = ApiResponse.buildFromApiSearch("Test", search, results)
        assertThat(response).isEqualTo(
            ApiResponse(
                data = results.results,
                MetaApiResponse(
                    totalCount = results.totalCount,
                    totalFilteredCount = results.filteredCount,
                    type = "Test",
                    paginationApiResponse = PaginationApiResponse.buildPaginationFromApiSearch(search, results)
                )
            )
        )
    }

    @Test
    fun `test buildPaginationFromApiSearch`() {
        // Verifies that previous page is null when the current page is 1
        var search = ApiSearchTest.TestApiSearch(emptyList(), null, page = 1, limit = 2)
        var results = ApiSearchResult<ApiSearchTest.TestPojo>(10, 6, createResults(2))
        var pagination = PaginationApiResponse.buildPaginationFromApiSearch(search, results)
        assertThat(pagination).isEqualTo(PaginationApiResponse(3, previousPage = null, nextPage = 2))

        // Verifies the next and previous page are correct
        search = ApiSearchTest.TestApiSearch(emptyList(), null, page = 2, limit = 2)
        results = ApiSearchResult<ApiSearchTest.TestPojo>(10, 6, createResults(2))
        pagination = PaginationApiResponse.buildPaginationFromApiSearch(search, results)
        assertThat(pagination).isEqualTo(PaginationApiResponse(3, previousPage = 1, nextPage = 3))

        // Verifies the next page correctly is set to null when there are no more results
        search = ApiSearchTest.TestApiSearch(emptyList(), null, page = 3, limit = 2)
        results = ApiSearchResult<ApiSearchTest.TestPojo>(10, 6, createResults(2))
        pagination = PaginationApiResponse.buildPaginationFromApiSearch(search, results)
        assertThat(pagination).isEqualTo(PaginationApiResponse(3, previousPage = 2, nextPage = null))

        // Verifies the correct number of total pages
        search = ApiSearchTest.TestApiSearch(emptyList(), null, page = 3, limit = 1)
        results = ApiSearchResult<ApiSearchTest.TestPojo>(10, 6, createResults(2))
        pagination = PaginationApiResponse.buildPaginationFromApiSearch(search, results)
        assertThat(pagination).isEqualTo(PaginationApiResponse(6, previousPage = 2, nextPage = 4))
    }
}