package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import org.jooq.Condition
import org.jooq.SortField
import org.jooq.impl.DSL
import java.time.OffsetDateTime

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess(private val db: DatabaseAccess = BaseEngine.databaseAccessSingleton) :
    ReportFileAccess {

    /**
     * Get multiple results based on a particular organization.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime that dictates how far back returned results date.
     * @param until is the OffsetDateTime that dictates how recently returned results date.
     * @param pageSize is an Integer used for setting the number of results per page.
     * @param showFailed whether to include actions that failed to be sent.
     * @param klass the class that the found data will be converted to.
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        organization: String,
        orgService: String?,
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T> {
        val sortedColumn = createColumnSort(sortColumn, sortDir)
        val whereClause = createWhereCondition(organization, orgService, since, until)

        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                // Note the report file and action tables have columns with the same name, so we must specify what we need.
                .select(
                    ACTION.ACTION_ID, ACTION.CREATED_AT, REPORT_FILE.RECEIVING_ORG, REPORT_FILE.RECEIVING_ORG_SVC,
                    ACTION.HTTP_STATUS, ACTION.EXTERNAL_NAME, REPORT_FILE.REPORT_ID, REPORT_FILE.SCHEMA_TOPIC,
                    REPORT_FILE.ITEM_COUNT, REPORT_FILE.BODY_URL, REPORT_FILE.SCHEMA_NAME, REPORT_FILE.BODY_FORMAT
                )
                .from(
                    ACTION.join(REPORT_FILE).on(
                        REPORT_FILE.ACTION_ID.eq(ACTION.ACTION_ID)
                    )
                )
                .where(whereClause)
                .orderBy(sortedColumn)

            if (cursor != null) {
                query.seek(cursor)
            }

            query.limit(pageSize)
                .fetchInto(klass)
        }
    }

    /**
     * Add sorting elements to the DB query.
     *
     * @param sortColumn sort the table by specific column; default created_at.
     * @param sortDir sort the table in ASC or DESC order.
     * @return a jooq sorting statement.
     */
    private fun createColumnSort(
        sortColumn: ReportFileAccess.SortColumn,
        sortDir: ReportFileAccess.SortDir
    ): SortField<OffsetDateTime> {
        val column = when (sortColumn) {
            /* Decides sort column by enum */
            ReportFileAccess.SortColumn.CREATED_AT -> ACTION.CREATED_AT
        }

        val sortedColumn = when (sortDir) {
            /* Applies sort order by enum */
            ReportFileAccess.SortDir.ASC -> column.asc()
            ReportFileAccess.SortDir.DESC -> column.desc()
        }

        return sortedColumn
    }

    /**
     * Add various filters to the DB query.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @param since is the OffsetDateTime that dictates how far back returned results date.
     * @param until is the OffsetDateTime that dictates how recently returned results date.
     * @return a jooq Condition statement to use in where().
     */
    private fun createWhereCondition(
        organization: String,
        orgService: String?,
        since: OffsetDateTime?,
        until: OffsetDateTime?
    ): Condition {
        var senderFilter = ACTION.ACTION_NAME.eq(TaskAction.send)
            .and(REPORT_FILE.RECEIVING_ORG.eq(organization))

        if (orgService != null) {
            senderFilter = senderFilter.and(REPORT_FILE.RECEIVING_ORG_SVC.eq(orgService))
        }

        if (since != null) {
            senderFilter = senderFilter.and(ACTION.CREATED_AT.ge(since))
        }

        if (until != null) {
            senderFilter = senderFilter.and(ACTION.CREATED_AT.lt(until))
        }

        return senderFilter
    }

    /**
     * Fetch the details of a single delivery.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param actionId the action id attached to this delivery.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    override fun <T> fetchAction(
        organization: String,
        actionId: Long,
        klass: Class<T>
    ): T? {
        println("$organization $actionId $klass")
        return null
    }

    /**
     * Fetch the details of an action's relations (descendants).
     * This is done through a recursive query on the report_lineage table.
     *
     * @param actionId the action id attached to the action to find relations for.
     * @param klass the class that the found data will be converted to.
     * @return a list of descendants for the given action id.
     */
    override fun <T> fetchRelatedActions(
        actionId: Long,
        klass: Class<T>
    ): List<T> {
        println("$actionId $klass")
        return emptyList()
    }
}