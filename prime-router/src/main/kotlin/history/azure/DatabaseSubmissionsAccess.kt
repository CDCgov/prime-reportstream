package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.ACTION_LOG
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedReport
import org.jooq.CommonTableExpression
import org.jooq.Condition
import org.jooq.SelectFieldOrAsterisk
import org.jooq.SortField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType.BIGINT
import org.jooq.impl.SQLDataType.UUID
import java.time.OffsetDateTime

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(private val db: DatabaseAccess = BaseEngine.databaseAccessSingleton) :
    ReportFileAccess {

    /**
     * Get multiple submissions based on a particular organization.
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
        val whereClause = createWhereCondition(organization, orgService, since, until, showFailed)

        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                // Note the report file and action tables have columns with the same name, so we must specify what we need.
                .select(
                    ACTION.ACTION_ID, ACTION.CREATED_AT, ACTION.SENDING_ORG, ACTION.HTTP_STATUS,
                    ACTION.EXTERNAL_NAME, REPORT_FILE.REPORT_ID, REPORT_FILE.SCHEMA_TOPIC, REPORT_FILE.ITEM_COUNT
                )
                .from(
                    ACTION.join(REPORT_FILE).on(
                        REPORT_FILE.ACTION_ID.eq(ACTION.ACTION_ID)
                            .and(
                                REPORT_FILE.SENDING_ORG.eq(
                                    ACTION.SENDING_ORG
                                )
                            )
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

        val sortDirection = when (sortDir) {
            /* Applies sort order by enum */
            ReportFileAccess.SortDir.ASC -> column.asc()
            ReportFileAccess.SortDir.DESC -> column.desc()
        }

        return sortDirection
    }

    /**
     * Add various filters to the DB query.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @param since is the OffsetDateTime that dictates how far back returned results date.
     * @param until is the OffsetDateTime that dictates how recently returned results date.
     * @param showFailed filter out submissions that failed to send.
     * @return a jooq Condition statement to use in where().
     */
    private fun createWhereCondition(
        organization: String,
        orgService: String?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        showFailed: Boolean
    ): Condition {
        var senderFilter = ACTION.ACTION_NAME.eq(TaskAction.receive)
            .and(ACTION.SENDING_ORG.eq(organization))

        if (orgService != null) {
            senderFilter = senderFilter.and(ACTION.SENDING_ORG_CLIENT.eq(orgService))
        }

        if (since != null) {
            senderFilter = senderFilter.and(ACTION.CREATED_AT.ge(since))
        }

        if (until != null) {
            senderFilter = senderFilter.and(ACTION.CREATED_AT.lt(until))
        }

        val failedFilter: Condition = when (showFailed) {
            true -> {
                ACTION.HTTP_STATUS.between(200, 600)
            }
            false -> {
                ACTION.HTTP_STATUS.between(200, 299)
            }
        }

        return senderFilter.and(failedFilter)
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
        return db.transactReturning { txn ->
            DSL.using(txn)
                .select(detailedSubmissionSelect())
                .from(ACTION)
                .where(
                    ACTION.ACTION_NAME.eq(TaskAction.receive)
                        .and(ACTION.SENDING_ORG.eq(organization))
                        .and(ACTION.ACTION_ID.eq(actionId))
                )
                .fetchOne()?.into(klass)
        }
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
        val cte = reportDescendantExpression(actionId)
        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(cte)
                .selectDistinct(detailedSubmissionSelect())
                .from(ACTION)
                .join(cte)
                .on(ACTION.ACTION_ID.eq(cte.field("action_id", BIGINT)))
                .where(ACTION.ACTION_ID.ne(actionId))
                .fetchInto(klass)
        }
    }

    /**
     * Add logs and reports related to the submission being fetched.
     *
     * @return a jooq select statement adding additional DB columns.
     */
    fun detailedSubmissionSelect(): List<SelectFieldOrAsterisk> {
        return listOf(
            ACTION.asterisk(),
            DSL.multiset(
                DSL.select()
                    .from(ACTION_LOG)
                    .where(ACTION_LOG.ACTION_ID.eq(ACTION.ACTION_ID))
            ).`as`("logs").convertFrom { r ->
                r?.into(DetailedActionLog::class.java)
            },
            DSL.multiset(
                DSL.select()
                    .from(REPORT_FILE)
                    .where(REPORT_FILE.ACTION_ID.eq(ACTION.ACTION_ID))
            ).`as`("reports").convertFrom { r ->
                r?.into(DetailedReport::class.java)
            },
        )
    }

    /**
     * Helper query used recursively to get the descendants of a submission.
     *
     * @param actionId the action id attached to the child submission to get details for.
     * @return a jooq sub-query finding the submission's descendants.
     */
    private fun reportDescendantExpression(actionId: Long): CommonTableExpression<*> {
        return DSL.name("t").fields(
            "action_id",
            "child_report_id",
            "parent_report_id"
            // Backticks escape the kotlin reserved word, so JOOQ can use it's "as"
        ).`as`(
            DSL.select(
                REPORT_LINEAGE.ACTION_ID,
                REPORT_LINEAGE.CHILD_REPORT_ID,
                REPORT_LINEAGE.PARENT_REPORT_ID,
            )
                .from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.ACTION_ID.eq(actionId))
                .unionAll(
                    DSL.select(
                        REPORT_LINEAGE.ACTION_ID,
                        REPORT_LINEAGE.CHILD_REPORT_ID,
                        REPORT_LINEAGE.PARENT_REPORT_ID,
                    )
                        .from(DSL.table(DSL.name("t")))
                        .join(REPORT_LINEAGE)
                        .on(
                            DSL.field(DSL.name("t", "child_report_id"), UUID)
                                .eq(REPORT_LINEAGE.PARENT_REPORT_ID)
                        )
                )
        )
    }
}