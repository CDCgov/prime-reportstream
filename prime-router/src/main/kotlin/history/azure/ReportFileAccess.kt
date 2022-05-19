package gov.cdc.prime.router.history.azure

import java.time.OffsetDateTime

interface ReportFileAccess {
    enum class SortOrder {
        DESC,
        ASC,
    }

    /* As sorting Submission / Delivery results expands, we can add
    * column names to this enum. Make sure the column you
    * wish to sort by is indexed. */
    enum class SortColumn {
        CREATED_AT
    }

    fun <T> fetchActions(
        organization: String,
        order: SortOrder,
        sortColumn: SortColumn,
        cursor: OffsetDateTime? = null,
        toEnd: OffsetDateTime? = null,
        limit: Int = 10,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T>

    fun <T> fetchAction(
        organization: String,
        actionId: Long,
        klass: Class<T>
    ): T?

    fun <T> fetchRelatedActions(
        actionId: Long,
        klass: Class<T>
    ): List<T>
}