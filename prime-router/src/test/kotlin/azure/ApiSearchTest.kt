package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.apache.poi.ss.formula.functions.T
import org.jooq.Condition
import org.jooq.Field
import org.jooq.Record
import org.jooq.SortField
import kotlin.test.Test

class ApiSearchTest {
    data class TestPojo(val foo: String)

    enum class TestApiFilterNames : ApiFilterNames {
        TEST_A,
        TEST_B
    }

    sealed class TestApiFilter<T> : ApiFilter<Record, T>
    class TestApiSearch(
        override val filters: List<TestApiFilter<*>>,
        override val sortParameter: Field<*>?,
        page: Int = 1,
        limit: Int = 25
    ) :
        ApiSearch<TestPojo, Record, TestApiFilter<*>>(TestPojo::class.java, page, limit) {
        override fun getWhereClause(): Condition? {
            TODO("Not yet implemented")
        }

        override fun getSortClause(): SortField<*> {
            TODO("Not yet implemented")
        }

        companion object : ApiSearchParser<TestPojo, TestApiSearch, Record, TestApiFilter<*>>() {
            override fun parseRawApiSearch(rawApiSearch: RawApiSearch): TestApiSearch {
                return TestApiSearch(
                    emptyList(),
                    null,
                    limit = rawApiSearch.pagination.limit,
                    page = rawApiSearch.pagination.page
                )
            }
        }
    }

    @Test
    fun `Test parses a request body`() {
        val rawSearchString = """
            {
                "sort": {
                    "direction": "DESC",
                    "property": "created_at"
                },
                "pagination": {
                    "page": 1,
                    "limit": 25
                },
                "filters": [
                ]
            }
        """.trimIndent()
        val request = MockHttpRequestMessage(rawSearchString)
        val search = TestApiSearch.parse(request)
        assertThat(search.limit).isEqualTo(25)
        assertThat(search.limit).isEqualTo(1)
    }
}