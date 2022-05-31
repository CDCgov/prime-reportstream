package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.Condition
import org.jooq.SortField
import org.jooq.impl.DSL
import java.time.OffsetDateTime

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseDeliveryAccess(private val db: DatabaseAccess = WorkflowEngine.databaseAccessSingleton) :
    ReportFileAccess {

    /**
     * Get multiple results based on a particular organization.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param order sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param toEnd is the OffsetDateTime that dictates how far back returned results date.
     * @param limit is an Integer used for setting the number of results per page.
     * @param showFailed whether or not to include actions that failed to be sent.
     * @param klass the class that the found data will be converted to.
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        organization: String,
        order: ReportFileAccess.SortOrder,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        limit: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T> {
        val sortedColumn = createColumnSort(sortColumn, order)
        val condition = createWhereCondition(organization, cursor, toEnd)
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
                        // .and(
                        //     REPORT_FILE.SENDING_ORG.eq(
                        //         ACTION.SENDING_ORG
                        //     )
                        // )
                    )
                )
                .where(condition)
                .orderBy(sortedColumn)

            /* This nullifies our selection if we're using the 'between'
            *  where() statement returned from createWhereCondition()
            *  For this, it's set to run only when a cursor is given
            *  with no endCursor. */
            if (cursor != null && toEnd == null) {
                query.seek(cursor)
            }
            query.limit(limit)
                .fetchInto(klass)
        }
    }

    /**
     * Add sorting elements to the DB query.
     *
     * @param sortColumn sort the table by specific column; default created_at.
     * @param order sort the table in ASC or DESC order.
     * @return a jooq sorting statement.
     */
    private fun createColumnSort(
        sortColumn: ReportFileAccess.SortColumn,
        order: ReportFileAccess.SortOrder
    ): SortField<OffsetDateTime> {
        val column = when (sortColumn) {
            /* Decides sort column by enum */
            ReportFileAccess.SortColumn.CREATED_AT -> ACTION.CREATED_AT
        }

        val sortedColumn = when (order) {
            /* Applies sort order by enum */
            ReportFileAccess.SortOrder.ASC -> column.asc()
            ReportFileAccess.SortOrder.DESC -> column.desc()
        }

        return sortedColumn
    }

    /**
     * Add various filters to the DB query.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param toEnd is the OffsetDateTime that dictates how far back returned results date.
     * @return a jooq Condition statement to use in where().
     */
    private fun createWhereCondition(
        organization: String,
        cursor: OffsetDateTime? = null,
        toEnd: OffsetDateTime? = null
    ): Condition {
        val filters: Condition = ACTION.ACTION_NAME.eq(TaskAction.send)
            .and(REPORT_FILE.RECEIVING_ORG.eq(organization))

        if (cursor != null && toEnd != null) {
            return filters.and(ACTION.CREATED_AT.between(toEnd, cursor))
        } else if (cursor != null) {
            return filters.and(ACTION.CREATED_AT.ge(cursor))
        } else if (toEnd != null) {
            return filters.and(ACTION.CREATED_AT.le(toEnd))
        }

        return filters
    }

    /**
     * Fetch the details of a single submission.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param actionId the action id attached to this submission.
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
     * Fetch the details of an action's relations (descendents).
     *
     * @param submissionId the action id attached to the action to find relations for.
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