package gov.cdc.prime.router.db

import gov.cdc.prime.router.azure.ApiSearch
import gov.cdc.prime.router.azure.ApiSearchParser
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.RawApiSearch
import gov.cdc.prime.router.azure.SearchTerm
import gov.cdc.prime.router.azure.SearchTermNames
import gov.cdc.prime.router.azure.SearchTerms
import gov.cdc.prime.router.azure.SortDirection
import gov.cdc.prime.router.azure.db.tables.ReportFile
import gov.cdc.prime.router.azure.db.tables.records.ReportFileRecord
import gov.cdc.prime.router.common.BaseEngine
import org.jooq.Condition
import org.jooq.Field
import org.jooq.SortField
import org.jooq.TableField
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile as ReportFilePojo

sealed class ReportSearchTerm<T> : SearchTerm<ReportFileRecord, T> {
    class StartDate(override val value: OffsetDateTime) :
        ReportSearchTerm<OffsetDateTime>() {
        override val tableField: TableField<ReportFileRecord, OffsetDateTime> =
            gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.CREATED_AT
    }

    class EndDate(override val value: OffsetDateTime) : ReportSearchTerm<OffsetDateTime>() {
        override val tableField: TableField<ReportFileRecord, OffsetDateTime> =
            gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.CREATED_AT
    }
}

enum class ReportFileSearchTermNames : SearchTermNames {
    SINCE,
    UNTIL
}

object ReportFileSearchTerms : SearchTerms<ReportFileRecord, ReportSearchTerm<*>, ReportFileSearchTermNames> {
    private val terms = mapOf(
        Pair(ReportFileSearchTermNames.SINCE, ReportSearchTerm.StartDate::class.java),
        Pair(ReportFileSearchTermNames.UNTIL, ReportSearchTerm.EndDate::class.java)
    )

    override fun getTerm(termName: ReportFileSearchTermNames): Class<out ReportSearchTerm<*>>? {
        return terms[termName]
    }
}

class ReportApiSearch private constructor(
    override val filters: List<ReportSearchTerm<*>>,
    override val sortParameter: Field<*>?,
    override val sortDirection: SortDirection = SortDirection.DESC,
    page: Int = 1,
    limit: Int = 25
) : ApiSearch<gov.cdc.prime.router.azure.db.tables.pojos.ReportFile, ReportFileRecord, ReportSearchTerm<*>>(
    gov.cdc.prime.router.azure.db.tables.pojos.ReportFile::class.java,
    page,
    limit
) {

    private fun getCondition(filter: ReportSearchTerm<*>): Condition? {
        // This compiles, but raises a problem for intellij
        // https://youtrack.jetbrains.com/issue/KTIJ-24876/IDE-Maven-False-positive-NOELSEINWHEN-with-sealed-class-when-subject-in-test-source
        return when (filter) {
            is ReportSearchTerm.StartDate -> filter.tableField.ge(filter.value)
            is ReportSearchTerm.EndDate -> filter.tableField.le(filter.value)
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

    override fun getSortClause(): SortField<*> {
        val column = sortParameter ?: ReportFile.REPORT_FILE.CREATED_AT
        return when (sortDirection) {
            SortDirection.ASC -> column.asc()
            SortDirection.DESC -> column.desc()
        }
    }

    companion object :
        ApiSearchParser<ReportFilePojo, ReportApiSearch, ReportFileRecord, ReportSearchTerm<*>>() {

        override fun parseRawApiSearch(rawApiSearch: RawApiSearch): ReportApiSearch {
            val sortProperty =
                gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.field(rawApiSearch.sort.property)
            val filters = rawApiSearch.filters.mapNotNull { filter ->
                when (ReportFileSearchTerms.getTerm(ReportFileSearchTermNames.valueOf(filter.filterName))) {
                    ReportSearchTerm.StartDate::class.java
                    -> ReportSearchTerm.StartDate(OffsetDateTime.parse(filter.value))

                    ReportSearchTerm.EndDate::class.java
                    -> ReportSearchTerm.StartDate(OffsetDateTime.parse(filter.value))

                    else -> null // TODO log a warning
                }
            }
            return ReportApiSearch(
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

    fun getReports(search: ReportApiSearch): List<gov.cdc.prime.router.azure.db.tables.pojos.ReportFile> {
        return db.transactReturning { txn ->
            search.fetchResults(
                DSL.using(txn),
                DSL.select(ReportFile.REPORT_FILE.asterisk()).from(ReportFile.REPORT_FILE)
            )
        }
    }
}