package gov.cdc.prime.router.db

import gov.cdc.prime.router.azure.ApiFilter
import gov.cdc.prime.router.azure.ApiFilterNames
import gov.cdc.prime.router.azure.ApiFilters
import gov.cdc.prime.router.azure.ApiSearch
import gov.cdc.prime.router.azure.ApiSearchParser
import gov.cdc.prime.router.azure.ApiSearchResult
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.RawApiSearch
import gov.cdc.prime.router.azure.SortDirection
import gov.cdc.prime.router.azure.db.tables.ReportFile
import gov.cdc.prime.router.azure.db.tables.records.ReportFileRecord
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.Condition
import org.jooq.Field
import org.jooq.TableField
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile as ReportFilePojo

/**
 * Sealed class containing all the filters that can be applied
 */
sealed class ReportFileApiFilter<T> : ApiFilter<ReportFileRecord, T> {

    /**
     * Filters results to those where the created_at is greater than or equal to the passed in date
     * @param value the date that results will be greater than or equal to
     */
    class Since(override val value: OffsetDateTime) :
        ReportFileApiFilter<OffsetDateTime>() {
        override val tableField: TableField<ReportFileRecord, OffsetDateTime> = ReportFile.REPORT_FILE.CREATED_AT
    }

    /**
     * Filters results to those where the created_at is less than or equal to the passed in date
     * @param value the date that results will be less than or equal to
     */
    class Until(override val value: OffsetDateTime) : ReportFileApiFilter<OffsetDateTime>() {
        override val tableField: TableField<ReportFileRecord, OffsetDateTime> = ReportFile.REPORT_FILE.CREATED_AT
    }
}

enum class ReportFileApiFilterNames : ApiFilterNames {
    SINCE,
    UNTIL
}

/**
 * Object that contains the map of filter names to the implementing filter class
 */
object ReportFileApiFilters : ApiFilters<ReportFileRecord, ReportFileApiFilter<*>, ReportFileApiFilterNames> {
    override val terms = mapOf(
        Pair(ReportFileApiFilterNames.SINCE, ReportFileApiFilter.Since::class.java),
        Pair(ReportFileApiFilterNames.UNTIL, ReportFileApiFilter.Until::class.java)
    )
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
class ReportFileApiSearch internal constructor(
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
    override fun getCondition(filter: ReportFileApiFilter<*>): Condition {
        return when (filter) {
            is ReportFileApiFilter.Since -> filter.tableField.ge(filter.value)
            is ReportFileApiFilter.Until -> filter.tableField.le(filter.value)
        }
    }

    /** Defaults to [ReportFile.CREATED_AT] if no sort is set */
    override fun getSortColumn(): Field<*> {
        return sortParameter ?: ReportFile.REPORT_FILE.CREATED_AT
    }

    override fun getPrimarySortColumn(): Field<*> {
        return ReportFile.REPORT_FILE.REPORT_ID
    }

    /**
     * Companion object that implements [ApiSearchResult] and parses a value into [ReportFileApiSearch]
     */
    companion object :
        ApiSearchParser<ReportFilePojo, ReportFileApiSearch, ReportFileRecord, ReportFileApiFilter<*>>(), Logging {

        override fun parseRawApiSearch(rawApiSearch: RawApiSearch): ReportFileApiSearch {
            val sortProperty =
                if (rawApiSearch.sort != null)
                    ReportFile.REPORT_FILE.field(rawApiSearch.sort.property)
                else
                    ReportFile.REPORT_FILE.CREATED_AT
            val filters = rawApiSearch.filters.mapNotNull { filter ->
                when (ReportFileApiFilters.getTerm(ReportFileApiFilterNames.valueOf(filter.filterName))) {
                    ReportFileApiFilter.Since::class.java
                    -> ReportFileApiFilter.Since(OffsetDateTime.parse(filter.value))

                    ReportFileApiFilter.Until::class.java
                    -> ReportFileApiFilter.Until(OffsetDateTime.parse(filter.value))

                    else -> {
                        logger.warn("${filter.filterName} did not map to a valid filter for ReportFileApiSearch")
                        null
                    }
                }
            }
            return ReportFileApiSearch(
                filters = filters,
                sortParameter = sortProperty,
                sortDirection = rawApiSearch.sort?.direction ?: SortDirection.DESC,
                page = rawApiSearch.pagination.page,
                limit = rawApiSearch.pagination.limit
            )
        }
    }
}

/**
 * Class for fetching ReportFile rows
 */
class ReportFileDatabaseAccess(val db: DatabaseAccess = BaseEngine.databaseAccessSingleton) {

    /**
     * Applies a search to a select * from report_file and returns the generated [ApiSearchResult]
     *
     * @param search the search configuration to apply to the query
     */
    fun getReports(search: ReportFileApiSearch): ApiSearchResult<ReportFilePojo> {
        return db.transactReturning { txn ->
            search.fetchResults(
                DSL.using(txn),
                DSL.select(ReportFile.REPORT_FILE.asterisk()).from(ReportFile.REPORT_FILE)
            )
        }
    }
}