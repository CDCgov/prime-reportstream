package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.SortField
import org.jooq.TableField

data class ApiSearchResult<T>(val totalCount: Int, val filteredCount: Int, val results: List<T>)
enum class SortDirection {
    ASC,
    DESC
}

/**
 * Interface that can be extended to represent a set of search term names that can be applied
 * to a specific search type
 */
interface ApiFilterNames

/**
 * Interface that represents an interface for retrieving a specific term
 * @see ApiFilter
 * @see gov.cdc.prime.router.db.ReportFileApiSearch.Companion
 */
interface ApiFilters<RecordType : Record, ApiFilterType : ApiFilter<RecordType, *>, Names : ApiFilterNames> {

    val terms: Map<Names, Class<out ApiFilterType>>
    fun getTerm(termName: Names): Class<out ApiFilterType>? {
        return terms[termName]
    }
}

interface ApiFilter<RecordType : Record, T> {

    /** The value that will be applied to the operator */
    val value: T

    /** The field the value will be applied against */
    val tableField: TableField<RecordType, T>
}

/**
 * Abstract class that provides helpers for parsing either a query string or request body into a specific
 * kind of ApiSearch
 *
 * @see gov.cdc.prime.router.db.ReportFileApiSearch.Companion
 */
abstract class ApiSearchParser<
    PojoType,
    ApiSearchType : ApiSearch<PojoType, RecordType, Names>,
    RecordType : Record,
    Names : ApiFilter<RecordType, *>
    > {
    /**
     * Converts a query string into a RawApiSearch. Currently, not implemented
     * TODO https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/9664
     *
     * @param query the query string
     */
    private fun parseFromQueryString(query: String): RawApiSearch {
        throw NotImplementedError(query)
    }

    /**
     * Converts a request body into a RawApiSearch
     *
     * @param body the request body
     */
    private fun parseRawFromRequestBody(body: String): RawApiSearch {
        return JacksonMapperUtilities.defaultMapper.readValue(body, RawApiSearch::class.java)
    }

    /**
     * Function that the subclass must implement to convert a RawApiSearch into the specific
     * type
     */
    abstract fun parseRawApiSearch(rawApiSearch: RawApiSearch): ApiSearchType

    /**
     * Converts a query string into the parameterized ApiSearchType
     */
    fun parse(query: String): ApiSearchType {
        val rawApiSearch = parseFromQueryString(query)
        return parseRawApiSearch(rawApiSearch)
    }

    /**
     * Converts a request body into the parameterized ApiSearchType
     */
    fun parse(request: HttpRequestMessage<String?>): ApiSearchType {
        val body = request.body ?: throw IllegalArgumentException("Request body must not be null to be parsed")
        val rawApiSearch = parseRawFromRequestBody(body)
        return parseRawApiSearch(rawApiSearch)
    }
}

/**
 * A raw filter that a parser will convert
 */
data class RawFilter(val value: String, val filterName: String)
/** Pagination data */
data class RawPagination(val page: Int, val limit: Int)
/** A raw sort that a parser will convert */
data class RawApiSort(val direction: SortDirection, val property: String)
/** A raw API search that is parsed via Jackson and then parsed by a specific API Search type */
data class RawApiSearch(val sort: RawApiSort?, val pagination: RawPagination, val filters: List<RawFilter>)

/**
 * Abstract class that can be subclassed to create a specific kind of API search.  It handles combining the
 * where and sort clauses and then fetching the data into the specified class.  Implementors are expected to provide
 * implementations for getWhereClause and getSortClause
 *
 * @see gov.cdc.prime.router.db.ReportFileApiSearch
 *
 * @param PojoType - the class that the search should be fetched into
 * @param RecordType - the record class that the search will be run against
 * @param ApiFilterType - the type for the API filters that will be used
 * @param recordClass -  the class object that the results will be fetched into
 * @param page - which page to read out of the search results
 * @param limit - the number of results to to return
 */
abstract class ApiSearch<PojoType, RecordType : Record, ApiFilterType : ApiFilter<RecordType, *>>(
    private val recordClass: Class<PojoType>,
    val page: Int,
    val limit: Int
) {
    /** The list of filters that should be applied */
    abstract val filters: List<ApiFilterType>
    /** The sort parameter that should be applied */
    abstract val sortParameter: Field<*>?
    /** The sort direction that should be applied */
    open val sortDirection: SortDirection = SortDirection.DESC

    abstract fun getCondition(filter: ApiFilterType): Condition

    /**
     * Converts the [filters] into a JOOQ [Condition]
     */
    fun getWhereClause(): Condition? {
        return filters.fold(null) { condition: Condition?, filter ->
            if (condition == null) {
                return@fold getCondition(filter)
            }
            return@fold condition.and(getCondition(filter))
        }
    }

    abstract fun getSortColumn(): Field<*>

    abstract fun getPrimarySortColumn(): Field<*>

    /**
     * Converts the [sortParameter] and [sortDirection] into a JOOQ [SortField]
     */
    fun getSortClause(): SortField<*> {
        val sortColumn = getSortColumn()
        return when (sortDirection) {
            SortDirection.ASC -> sortColumn.asc()
            SortDirection.DESC -> sortColumn.desc()
        }
    }

    fun getPrimarySortClause(): SortField<*> {
        val sortColumn = getPrimarySortColumn()
        return when (sortDirection) {
            SortDirection.ASC -> sortColumn.asc()
            SortDirection.DESC -> sortColumn.desc()
        }
    }

    /**
     * Runs the specified select using the passed context applying the where, sort, pagination
     * clauses and finally reading the results into the specified [PojoType].
     *
     *
     * @param dslContext  the JOOQ context that will be used to execute the results
     * @param select the JOOQ select that will be used
     * @return the list of the records parsed into the [PojoType]
     *
     */
    open fun <T : Record> fetchResults(dslContext: DSLContext, select: SelectJoinStep<T>): ApiSearchResult<PojoType> =
        runBlocking {
            flow<ApiSearchResult<PojoType>> {
                emit(
                    coroutineScope {
                        val totalCount = async(Dispatchers.IO, CoroutineStart.UNDISPATCHED) {
                            dslContext.fetchCount(select)
                        }
                        val filteredCount = async(Dispatchers.IO, CoroutineStart.UNDISPATCHED) {
                            dslContext.fetchCount(
                                select
                                    .where(getWhereClause())
                            )
                        }
                        val results = async(Dispatchers.IO, CoroutineStart.UNDISPATCHED) {
                            dslContext.fetch(
                                select
                                    .where(getWhereClause())
                                    .orderBy(getSortClause(), getPrimarySortClause())
                                    .limit(limit)
                                    .offset(getOffset())
                            ).into(recordClass)
                        }
                        ApiSearchResult(totalCount.await(), filteredCount.await(), results.await())
                    }
                )
            }.first()
        }

    /** Converts the limit and page value into an offset */
    private fun getOffset(): Int {
        return limit * (page - 1)
    }
}