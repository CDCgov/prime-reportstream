package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.db.ReportFileApiSearch
import org.jooq.Condition
import org.jooq.Field
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.CustomRecord
import org.jooq.impl.CustomTable
import org.jooq.impl.DSL.name
import org.jooq.impl.SQLDataType
import java.time.OffsetDateTime
import kotlin.test.Test

@Suppress("UNCHECKED_CAST")
class ApiSearchTest {

    class TestTable : CustomTable<TestRecord>(name("test")) {

        val FOO = createField(name("foo"), SQLDataType.VARCHAR)
        val CREATED_AT = createField(name("created_at"), SQLDataType.OFFSETDATETIME)

        companion object {
            val TEST = TestTable()
        }

        override fun getRecordType(): Class<out TestRecord> {
            return TestRecord::class.java
        }
    }

    class TestRecord : CustomRecord<TestRecord>(TestTable.TEST)
    data class TestPojo(val foo: String, val createdAt: OffsetDateTime)

    enum class TestApiFilterNames : ApiFilterNames {
        FOO
    }

    sealed class TestApiFilter<T> : ApiFilter<TestRecord, T> {
        class FooFilter(override val value: String) : TestApiFilter<String>() {
            override val tableField: TableField<TestRecord, String> = TestTable.TEST.FOO
        }
    }

    object TestApiFilters : ApiFilters<TestRecord, TestApiFilter<*>, TestApiFilterNames> {

        override val terms = mapOf(
            Pair(TestApiFilterNames.FOO, TestApiFilter.FooFilter::class.java)
        )
    }

    class TestApiSearch(
        override val filters: List<TestApiFilter<*>>,
        override val sortParameter: Field<*>?,
        override val sortDirection: SortDirection = SortDirection.DESC,
        page: Int = 1,
        limit: Int = 25
    ) :
        ApiSearch<TestPojo, TestRecord, TestApiFilter<*>>(TestPojo::class.java, page, limit) {
        override fun getCondition(filter: TestApiFilter<*>): Condition {
            return when (filter) {
                is TestApiFilter.FooFilter -> filter.tableField.eq(filter.value)
            }
        }

        override fun getSortColumn(): Field<*> {
            return sortParameter ?: TestTable.TEST.CREATED_AT
        }

        override fun getPrimarySortColumn(): Field<*> {
            return TestTable.TEST.FOO
        }

        companion object : ApiSearchParser<TestPojo, TestApiSearch, TestRecord, TestApiFilter<*>>() {
            override fun parseRawApiSearch(rawApiSearch: RawApiSearch): TestApiSearch {
                val sort = if (rawApiSearch.sort != null)
                    TestTable.TEST.field(rawApiSearch.sort!!.property)
                else
                    TestTable.TEST.CREATED_AT
                val filters = rawApiSearch.filters.mapNotNull { filter ->
                    when (TestApiFilters.getTerm(TestApiFilterNames.valueOf(filter.filterName))) {
                        TestApiFilter.FooFilter::class.java -> TestApiFilter.FooFilter(filter.value)
                        else -> {
                            ReportFileApiSearch.logger.warn(
                                "${filter.filterName} did not map to a valid filter for ReportFileApiSearch"
                            )
                            null
                        }
                    }
                }
                return TestApiSearch(
                    filters,
                    sort,
                    sortDirection = rawApiSearch.sort?.direction ?: SortDirection.DESC,
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
                    "direction": "ASC",
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
        assertThat(search.page).isEqualTo(1)
        assertThat(search.sortDirection).isEqualTo(SortDirection.ASC)
        assertThat(search.sortParameter).isEqualTo(TestTable.TEST.CREATED_AT)
        assertThat(search.getWhereClause()).isNull()
        assertThat(search.getSortClause()).isNotNull()
    }

    @Test
    fun `Test generates the correct sort clause`() {
        val rawSearchString = """
            {
                "sort": {
                    "direction": "DESC",
                    "property": "foo"
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
        val sortClause = search.getSortClause()
        assertThat(sortClause.order).isEqualTo(SortOrder.DESC)
        assertThat(sortClause.name).isEqualTo("foo")
    }

    @Test
    fun `Test getPrimarySortClause`() {
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
        val sortClause = search.getPrimarySortClause()
        assertThat(sortClause.order).isEqualTo(SortOrder.DESC)
        assertThat(sortClause.name).isEqualTo("foo")
    }

    @Test
    fun `Test generates the default sort clause`() {
        val rawSearchString = """
            {
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
        val sortClause = search.getSortClause()
        assertThat(sortClause.order).isEqualTo(SortOrder.DESC)
        assertThat(sortClause.name).isEqualTo("created_at")
    }

    @Test
    fun `Test generates filters correctly`() {
        val rawSearchString = """
            {
                "sort": {
                    "direction": "DESC",
                    "property": "foo"
                },
                "pagination": {
                    "page": 1,
                    "limit": 25
                },
                "filters": [
                    {
                        "value": "A",
                        "filterName": "FOO"
                    }
                ]
            }
        """.trimIndent()
        val request = MockHttpRequestMessage(rawSearchString)
        val search = TestApiSearch.parse(request)
        val condition = search.getWhereClause()
        assertThat(condition).isNotNull()
        assertThat(condition!!.toString()).contains("\"foo\" = 'A'")
    }
}