package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.ACTION_LOG
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
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
 * Interface for classes that deal with submission data
 */
interface SubmissionAccess {
    /**
     * Values that results can be sorted by.
     */
    enum class SortDir {
        DESC,
        ASC,
    }

    /* As sorting Submission results expands, we can add
    * column names to this enum. Make sure the column you
    * wish to sort by is indexed. */
    enum class SortColumn {
        CREATED_AT
    }

    /**
     * Get multiple results based on a particular organization.
     *
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param pageSize is an Integer used for setting the number of results per page.
     * @param klass the class that the found data will be converted to.
     * @return a list of results matching the SQL Query.
     */
    fun <T> fetchActions(
        sendingOrg: String,
        sortDir: SortDir,
        sortColumn: SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T>

    /**
     * Fetch a single (usually detailed) action of a specific type.
     *
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param submissionId the action id attached to this submission.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    fun <T> fetchAction(
        sendingOrg: String,
        submissionId: Long,
        klass: Class<T>
    ): T?

    /**
     * Fetch the details of an action's relations (descendents).
     *
     * @param submissionId the action id attached to the action to find relations for.
     * @param klass the class that the found data will be converted to.
     * @return a list of descendants for the given action id.
     */
    fun <T> fetchRelatedActions(
        submissionId: Long,
        klass: Class<T>
    ): List<T>
}
/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(private val db: DatabaseAccess = WorkflowEngine.databaseAccessSingleton) :
    SubmissionAccess {

    /**
     * Get multiple submissions based on a particular organization.
     *
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime minimum date to get results for.
     * @param until is the OffsetDateTime maximum date to get results for.
     * @param pageSize is an Integer used for setting the number of results per page.
     * @param klass the class that the found data will be converted to.
     * @return a list of submissions matching the SQL Query.
     */
    override fun <T> fetchActions(
        sendingOrg: String,
        sortDir: SubmissionAccess.SortDir,
        sortColumn: SubmissionAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T> {
        val sortedColumn = createColumnSort(sortColumn, sortDir)
        val whereClause = createWhereCondition(sendingOrg, since, until, showFailed)
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
     * @param order sort the table in ASC or DESC order.
     * @return a jooq sorting statement.
     */
    private fun createColumnSort(
        sortColumn: SubmissionAccess.SortColumn,
        order: SubmissionAccess.SortDir
    ): SortField<OffsetDateTime> {
        val column = when (sortColumn) {
            /* Decides sort column by enum */
            SubmissionAccess.SortColumn.CREATED_AT -> ACTION.CREATED_AT
        }

        val sortDirection = when (order) {
            /* Applies sort order by enum */
            SubmissionAccess.SortDir.ASC -> column.asc()
            SubmissionAccess.SortDir.DESC -> column.desc()
        }

        return sortDirection
    }

    /**
     * Add various filters to the DB query.
     *
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param since is the OffsetDateTime minimum date to get results for.
     * @param until is the OffsetDateTime maximum date to get results for.
     * @param showFailed filter out submissions that failed to send.
     * @return a jooq Condition statement to use in where().
     */
    private fun createWhereCondition(
        sendingOrg: String,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        showFailed: Boolean
    ): Condition {
        var senderFilter = ACTION.ACTION_NAME.eq(TaskAction.receive)
            .and(ACTION.SENDING_ORG.eq(sendingOrg))

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
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param submissionId the action id attached to this submission.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    override fun <T> fetchAction(
        sendingOrg: String,
        submissionId: Long,
        klass: Class<T>
    ): T? {
        return db.transactReturning { txn ->
            DSL.using(txn)
                .select(detailedSubmissionSelect())
                .from(ACTION)
                .where(
                    ACTION.ACTION_NAME.eq(TaskAction.receive)
                        .and(ACTION.SENDING_ORG.eq(sendingOrg))
                        .and(ACTION.ACTION_ID.eq(submissionId))
                )
                .fetchOne()?.into(klass)
        }
    }

    /**
     * Fetch the details of an action's relations (descendents).
     * This is done through a recursive query on the report_lineage table.
     *
     * @param submissionId the action id attached to the action to find relations for.
     * @param klass the class that the found data will be converted to.
     * @return a list of descendants for the given action id.
     */
    override fun <T> fetchRelatedActions(
        submissionId: Long,
        klass: Class<T>
    ): List<T> {
        val cte = reportDescendantExpression(submissionId)
        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(cte)
                .selectDistinct(detailedSubmissionSelect())
                .from(ACTION)
                .join(cte)
                .on(ACTION.ACTION_ID.eq(cte.field("action_id", BIGINT)))
                .where(ACTION.ACTION_ID.ne(submissionId))
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
     * @param submissionId the action id attached to the child submission to get details for.
     * @return a jooq subquery finding the submission's descendants.
     */
    private fun reportDescendantExpression(submissionId: Long): CommonTableExpression<*> {
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
                .where(REPORT_LINEAGE.ACTION_ID.eq(submissionId))
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