package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.SortField
import org.jooq.TableField

enum class SortDirection {
    ASC,
    DESC
}

interface SearchTermNames
interface SearchTerms<RecordType : Record, SearchTermType : SearchTerm<RecordType, *>, Names : SearchTermNames> {
    fun getTerm(termName: Names): Class<out SearchTermType>?
}

interface SearchTerm<RecordType : Record, T> {
    val value: T
    val tableField: TableField<RecordType, T>
}

abstract class ApiSearchParser<
    PojoType,
    ApiSearchType : ApiSearch<PojoType, RecordType, Names>,
    RecordType : Record,
    Names : SearchTerm<RecordType, *>
    > {
    private fun parseFromQueryString(query: String): RawApiSearch {
        throw NotImplementedError(query)
    }

    private fun parseRawFromRequestBody(body: String): RawApiSearch {
        return JacksonMapperUtilities.defaultMapper.readValue(body, RawApiSearch::class.java)
    }

    abstract fun parseRawApiSearch(rawApiSearch: RawApiSearch): ApiSearchType

    fun parse(query: String): ApiSearchType {
        val rawApiSearch = parseFromQueryString(query)
        return parseRawApiSearch(rawApiSearch)
    }

    fun parse(request: HttpRequestMessage<String?>): ApiSearchType {
        val rawApiSearch = parseRawFromRequestBody(request.body!!) // TODO throw on missing body
        return parseRawApiSearch(rawApiSearch)
    }
}

data class RawFilter(val value: String, val filterName: String)
data class RawPagination(val page: Int, val limit: Int)
data class RawApiSort(val direction: SortDirection, val property: String)
data class RawApiSearch(val sort: RawApiSort, val pagination: RawPagination, val filters: List<RawFilter>)

abstract class ApiSearch<PojoType, RecordType : Record, SearchTermType : SearchTerm<RecordType, *>>(
    private val recordClass: Class<PojoType>,
    private val page: Int,
    val limit: Int
) {
    abstract val filters: List<SearchTermType>
    abstract val sortParameter: Field<*>?
    open val sortDirection: SortDirection = SortDirection.DESC
    abstract fun getWhereClause(): Condition?
    abstract fun getSortClause(): SortField<*>

    fun fetchResults(dslContext: DSLContext, select: SelectJoinStep<Record>): List<PojoType> {
        return dslContext.fetch(
            select
                .where(getWhereClause())
                .orderBy(getSortClause())
                .limit(limit)
                .offset(getOffset())
        ).into(recordClass)
    }

    private fun getOffset(): Int {
        return limit * page
    }
}