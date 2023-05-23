package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.tables.records.ReportFileRecord
import org.apache.poi.ss.formula.functions.T
import org.jooq.Condition
import org.jooq.Record
import org.jooq.SelectForUpdateStep
import org.jooq.SelectSelectStep
import org.jooq.SortField
import org.jooq.TableField
import java.time.OffsetDateTime

enum class SortDirection {
    ASC,
    DESC
}

interface SearchTerms<R : Record>
interface SearchTerm<R : Record, T> : SearchTerms<R> {
    val value: T
    val property: TableField<R, T>
}
interface ApiSearchParser<F : Record, S : TableField<F, Any>> {
    fun parseFromQuery(query: String): ApiSearch<F, S>
    fun parseFromRequest(body: String): ApiSearch<F, S>
}

abstract class ApiSearch<F : Record, S : TableField<F, Any>> {
    abstract val filters: List<SearchTerms<F>>
    abstract val sortParameter: S
    val sortDirection: SortDirection = SortDirection.DESC
    val page: Int = 1
    val limit: Int = 25

    abstract fun getWhereClause(): Condition
    abstract fun getSortClause(): SortField<F>

    fun setPage(select: SelectSelectStep<F>): SelectForUpdateStep<F> {
        return select.limit(25).offset(limit * page)
    }
}

sealed class SubmitterSearchTerms() : SearchTerms<ReportFileRecord> {
    class StartDate(override val value: OffsetDateTime) :
        SearchTerm<ReportFileRecord, OffsetDateTime>, SubmitterSearchTerms() {
        override val property: TableField<ReportFileRecord, OffsetDateTime> =
            gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.CREATED_AT
    }

    class EndDate(override val value: OffsetDateTime) :
        SearchTerm<ReportFileRecord, OffsetDateTime>, SubmitterSearchTerms() {
        override val property: TableField<ReportFileRecord, OffsetDateTime> =
            gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE.CREATED_AT
    }
}