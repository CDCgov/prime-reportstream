package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import gov.cdc.prime.router.db.ReportFileApiSearch
import org.jooq.Condition
import org.jooq.Field
import org.jooq.Name
import org.jooq.Record2
import org.jooq.Row2
import org.jooq.SortOrder
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.impl.DSL
import org.jooq.impl.DSL.name
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl
import org.jooq.impl.UpdatableRecordImpl
import java.time.OffsetDateTime
import kotlin.test.Test

@Suppress("UNCHECKED_CAST")
class ApiSearchTest {

    class TestRecord : UpdatableRecordImpl<TestRecord>(TestPojo.TEST_POJO), Record2<String, OffsetDateTime> {
        override fun fieldsRow(): Row2<String, OffsetDateTime> {
            return super.fieldsRow() as Row2<String, OffsetDateTime>
        }

        override fun valuesRow(): Row2<String, OffsetDateTime> {
            return super.valuesRow() as Row2<String, OffsetDateTime>
        }

        override fun field1(): Field<String> {
            return TestPojo.TEST_POJO.FOO
        }

        override fun field2(): Field<OffsetDateTime> {
            return TestPojo.TEST_POJO.CREATED_AT
        }

        override fun value1(): String {
            return get(0) as String
        }

        override fun value1(value: String?): Record2<String, OffsetDateTime> {
            set(0, value)
            return this
        }

        override fun value2(): OffsetDateTime {
            return get(1) as OffsetDateTime
        }

        override fun value2(value: OffsetDateTime?): Record2<String, OffsetDateTime> {
            set(1, value)
            return this
        }

        override fun values(t1: String?, t2: OffsetDateTime?): Record2<String, OffsetDateTime> {
            value1(t1)
            value2(t2)
            return this
        }

        override fun component1(): String {
            return get(0) as String
        }

        override fun component2(): OffsetDateTime {
            return get(1) as OffsetDateTime
        }
    }

    class TestPojo(alias: Name, aliased: Table<TestRecord>?, parameters: List<Field<*>>?) : TableImpl<TestRecord>(
        alias, null, aliased, parameters?.toTypedArray(),
        DSL.comment(""),
        TableOptions.table()
    ) {

        public val FOO = createField(name("foo"), SQLDataType.VARCHAR)
        public val CREATED_AT = createField(name("created_at"), SQLDataType.OFFSETDATETIME)
        companion object {
            public val TEST_POJO = TestPojo(name("test"), null, null)
        }
    }

    enum class TestApiFilterNames : ApiFilterNames {
        FOO
    }

    sealed class TestApiFilter<T> : ApiFilter<TestRecord, T> {
        class FooFilter(override val value: String) : TestApiFilter<String>() {
            override val tableField: TableField<TestRecord, String> = TestPojo.TEST_POJO.FOO
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
            return sortParameter ?: TestPojo.TEST_POJO.CREATED_AT
        }

        companion object : ApiSearchParser<TestPojo, TestApiSearch, TestRecord, TestApiFilter<*>>() {
            override fun parseRawApiSearch(rawApiSearch: RawApiSearch): TestApiSearch {
                val sort = TestPojo.TEST_POJO.field(rawApiSearch.sort.property)
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
        assertThat(search.page).isEqualTo(1)
        assertThat(search.sortDirection).isEqualTo(SortDirection.DESC)
        assertThat(search.sortParameter).isEqualTo(TestPojo.TEST_POJO.CREATED_AT)
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