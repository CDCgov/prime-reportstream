package gov.cdc.prime.router.db

import gov.cdc.prime.router.azure.ApiFilter
import gov.cdc.prime.router.azure.ApiFilterNames
import gov.cdc.prime.router.azure.ApiFilters
import gov.cdc.prime.router.azure.ApiSearch
import gov.cdc.prime.router.azure.ApiSearchParser
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.RawApiSearch
import gov.cdc.prime.router.azure.SortDirection
import gov.cdc.prime.router.azure.db.tables.ReportFile
import gov.cdc.prime.router.azure.db.tables.records.ReportFileRecord
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.Condition
import org.jooq.Field
import org.jooq.SortField
import org.jooq.TableField
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile as ReportFilePojo

sealed class ReportFileApiFilter<T> : ApiFilter<ReportFileRecord, T> {
    class StartDate(override val value: OffsetDateTime) :
        ReportFileApiFilter<OffsetDateTime>() {
        override val tableField: TableField<ReportFileRecord, OffsetDateTime> = ReportFile.REPORT_FILE.CREATED_AT
    }

    class EndDate(override val value: OffsetDateTime) : ReportFileApiFilter<OffsetDateTime>() {
        override val tableField: TableField<ReportFileRecord, OffsetDateTime> = ReportFile.REPORT_FILE.CREATED_AT
    }
}

enum class ReportFileApiFilterNames : ApiFilterNames {
    SINCE,
    UNTIL
}

/**
 *
 */
object ReportFileApiFilters : ApiFilters<ReportFileRecord, ReportFileApiFilter<*>, ReportFileApiFilterNames> {
    private val terms = mapOf(
        Pair(ReportFileApiFilterNames.SINCE, ReportFileApiFilter.StartDate::class.java),
        Pair(ReportFileApiFilterNames.UNTIL, ReportFileApiFilter.EndDate::class.java)
    )

    override fun getTerm(termName: ReportFileApiFilterNames): Class<out ReportFileApiFilter<*>>? {
        return terms[termName]
    }
}

/**
 * Encapsulates a search against the [ReportFile] table including filters to be applied, the sorting and pagination
 *
 * @param filters the list of [ReportFileApiFilter] that should be applied
 * @param sortParameter the column to sort against
 * @param sortDirection the direction of the ordering clause
 * @param page the page of results to fetch
 * @param limit the number of results to fetch
 */
class ReportFileApiSearch private constructor(
    override val filters: List<ReportFileApiFilter<*>>,
    override val sortParameter: Field<*>?,
    override val sortDirection: SortDirection = SortDirection.DESC,
    page: Int = 1,
    limit: Int = 25
) : ApiSearch<ReportFilePojo, ReportFileRecord, ReportFileApiFilter<*>>(
    ReportFilePojo::class.java,
    page,
    limit
) {

    /** Converts a [ReportFileApiFilter] into a JOOQ condition */
    private fun getCondition(filter: ReportFileApiFilter<*>): Condition? {
        return when (filter) {
            is ReportFileApiFilter.StartDate -> filter.tableField.ge(filter.value)
            is ReportFileApiFilter.EndDate -> filter.tableField.le(filter.value)
        }
    }

    override fun getWhereClause(): Condition? {
        return filters.fold(null) { condition: Condition?, filter ->
            if (condition == null) {
                return getCondition(filter)
            }
            return condition.and(getCondition(filter))
        }
    }

    /** Defaults to [ReportFile.CREATED_AT] if no sort is set */
    override fun getSortClause(): SortField<*> {
        val column = sortParameter ?: ReportFile.REPORT_FILE.CREATED_AT
        return when (sortDirection) {
            SortDirection.ASC -> column.asc()
            SortDirection.DESC -> column.desc()
        }
    }

    companion object :
        ApiSearchParser<ReportFilePojo, ReportFileApiSearch, ReportFileRecord, ReportFileApiFilter<*>>(), Logging {

        override fun parseRawApiSearch(rawApiSearch: RawApiSearch): ReportFileApiSearch {
            val sortProperty =
                gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.field(rawApiSearch.sort.property)
            val filters = rawApiSearch.filters.mapNotNull { filter ->
                when (ReportFileApiFilters.getTerm(ReportFileApiFilterNames.valueOf(filter.filterName))) {
                    ReportFileApiFilter.StartDate::class.java
                    -> ReportFileApiFilter.StartDate(OffsetDateTime.parse(filter.value))

                    ReportFileApiFilter.EndDate::class.java
                    -> ReportFileApiFilter.StartDate(OffsetDateTime.parse(filter.value))

                    else -> {
                        logger.warn("${filter.filterName} did not map to a valid filter for ReportFileApiSearch")
                        null
                    }
                }
            }
            return ReportFileApiSearch(
                filters = filters,
                sortParameter = sortProperty,
                sortDirection = rawApiSearch.sort.direction,
                page = rawApiSearch.pagination.page,
                limit = rawApiSearch.pagination.limit
            )
        }
    }
}

class ReportFileDatabaseAccess(val db: DatabaseAccess = BaseEngine.databaseAccessSingleton) {

    fun getReports(search: ReportFileApiSearch): List<ReportFilePojo> {
        return db.transactReturning { txn ->
            search.fetchResults(
                DSL.using(txn),
                DSL.select(ReportFile.REPORT_FILE.asterisk()).from(ReportFile.REPORT_FILE)
            )
        }
    }
}