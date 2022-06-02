package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.ACTION_LOG
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.CommonTableExpression
import org.jooq.Condition
import org.jooq.SelectFieldOrAsterisk
import org.jooq.SortField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType.BIGINT
import org.jooq.impl.SQLDataType.UUID
import java.time.OffsetDateTime

interface SubmissionAccess {
    enum class SortOrder {
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
     * Enumeration used to filter submission.
     * ternary value: &resultfilter='all'|'successOnly'|'failedOnly'
     * CONST(string) don't match to decouple cgi params strings from code.
     *
     * @property ALL No filter, return all matches
     * @property ONLY_SUCCESSFUL Return only successfully delivered submissions
     * @property ONLY_FAILED Return only failed delivered submissions
     */
    enum class SubmissionResultFilter(val cgiparam: String) {
        ALL("all"),
        ONLY_SUCCESSFUL("successOnly"), // default
        ONLY_FAILED("failedOnly");

        companion object Parser {
            fun fromCgiParam(cgiparam: String) =
                SubmissionResultFilter.values()
                    .find { it.cgiparam.equals(cgiparam, ignoreCase = true) }
                    ?: SubmissionResultFilter.ONLY_SUCCESSFUL
        }
    }

    fun <T> fetchActions(
        sendingOrg: String,
        order: SortOrder,
        sortColumn: SortColumn,
        cursor: OffsetDateTime? = null,
        toEnd: OffsetDateTime? = null,
        limit: Int = 10,
        filterResult: SubmissionResultFilter = SubmissionResultFilter.ALL,
        klass: Class<T>
    ): List<T>

    fun <T, P, U> fetchAction(
        sendingOrg: String,
        submissionId: Long,
        klass: Class<T>,
        reportsKlass: Class<P>,
        logsKlass: Class<U>,
    ): T?

    fun <T, P, U> fetchRelatedActions(
        submissionId: Long,
        klass: Class<T>,
        reportsKlass: Class<P>,
        logsKlass: Class<U>,
    ): List<T>
}

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(private val db: DatabaseAccess = WorkflowEngine.databaseAccessSingleton) :
    SubmissionAccess {

    /**
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param order sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param toEnd is the OffsetDateTime that dictates how far back returned results date.
     * @param limit is an Integer used for setting the number of results per page.
     * @param filterResult SubmissionResultFilter ternary enum (only_success|only_fail|all)
     * @return a list of results matching the SQL Query.
     */
    override fun <T> fetchActions(
        sendingOrg: String,
        order: SubmissionAccess.SortOrder,
        sortColumn: SubmissionAccess.SortColumn,
        cursor: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        limit: Int,
        filterResult: SubmissionAccess.SubmissionResultFilter,
        klass: Class<T>
    ): List<T> {
        val sortedColumn = createColumnSort(sortColumn, order)
        val condition = createWhereCondition(sendingOrg, cursor, toEnd, filterResult)
        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                // Note the report file and action tables have columns with the same name, so we must specify what we need.
                .select(
                    ACTION.ACTION_ID,
                    ACTION.CREATED_AT,
                    ACTION.SENDING_ORG,
                    ACTION.HTTP_STATUS,
                    ACTION.EXTERNAL_NAME,
                    REPORT_FILE.REPORT_ID,
                    REPORT_FILE.SCHEMA_TOPIC,
                    REPORT_FILE.ITEM_COUNT,
                    REPORT_FILE.RECEIVING_ORG,
                    REPORT_FILE.BODY_FORMAT,
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
     * @param order sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @return a jooq Condition statement to use in where().
     */
    private fun createColumnSort(
        sortColumn: SubmissionAccess.SortColumn,
        order: SubmissionAccess.SortOrder
    ): SortField<OffsetDateTime> {
        val column = when (sortColumn) {
            /* Decides sort column by enum */
            SubmissionAccess.SortColumn.CREATED_AT -> ACTION.CREATED_AT
        }

        val sortedColumn = when (order) {
            /* Applies sort order by enum */
            SubmissionAccess.SortOrder.ASC -> column.asc()
            SubmissionAccess.SortOrder.DESC -> column.desc()
        }

        return sortedColumn
    }

    /**
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param toEnd is the OffsetDateTime that dictates how far back returned results date.
     * @param filterResult SubmissionResultFilter ternary enum (only_success|only_fail|all)
     * @return a jooq Condition statement to use in where().
     */
    private fun createWhereCondition(
        sendingOrg: String,
        cursor: OffsetDateTime? = null,
        toEnd: OffsetDateTime? = null,
        filterResult: SubmissionResultFilter = SubmissionResultFilter.ALL,
    ): Condition {
        val dateFilter: Condition = ACTION.ACTION_NAME.eq(TaskAction.receive)

        when (sendingOrg) {
            "*" -> {
                /* admin only option to look across all orgs (restriction is enforced by callers)
                   Would be nice to be able to enforce at this level. Renamed to include Admin
                   to help call attention to this */
            }
            else -> {
                dateFilter.and(ACTION.SENDING_ORG.eq(sendingOrg))
            }
        }

        if (toEnd != null && cursor != null) {
            /* Both given: all results between cursor and cutoff */
            dateFilter.and(ACTION.CREATED_AT.between(toEnd, cursor))
        }

        when (filterResult) {
            SubmissionResultFilter.ALL -> {
                // No action for ALL
            }
            SubmissionResultFilter.ONLY_FAILED -> {
                dateFilter.and(ACTION.HTTP_STATUS.between(300, 600))
            }
            SubmissionResultFilter.ONLY_SUCCESSFUL -> {
                dateFilter.and(ACTION.HTTP_STATUS.between(200, 299))
            }
        }

        return dateFilter
    }

    fun <P, U> detailedSubmissionSelect(
        reportsKlass: Class<P>,
        logsKlass: Class<U>,
    ): List<SelectFieldOrAsterisk> {
        return listOf(
            ACTION.asterisk(),
            DSL.multiset(
                DSL.select()
                    .from(ACTION_LOG)
                    .where(ACTION_LOG.ACTION_ID.eq(ACTION.ACTION_ID))
            ).`as`("logs").convertFrom { r ->
                r?.into(logsKlass)
            },
            DSL.multiset(
                DSL.select()
                    .from(REPORT_FILE)
                    .where(REPORT_FILE.ACTION_ID.eq(ACTION.ACTION_ID))
            ).`as`("reports").convertFrom { r ->
                r?.into(reportsKlass)
            },
        )
    }

    /**
     * fetch the details of a single action
     */
    override fun <T, P, U> fetchAction(
        sendingOrg: String,
        submissionId: Long,
        klass: Class<T>,
        reportsKlass: Class<P>,
        logsKlass: Class<U>,
    ): T? {
        return db.transactReturning { txn ->
            DSL.using(txn)
                .select(detailedSubmissionSelect(reportsKlass, logsKlass))
                .from(ACTION)
                .where(
                    ACTION.ACTION_NAME.eq(TaskAction.receive)
                        .and(ACTION.SENDING_ORG.eq(sendingOrg))
                        .and(ACTION.ACTION_ID.eq(submissionId))
                )
                .fetchOne()?.into(klass)
        }
    }

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

    /**
     * Fetch the details of an actions relations (descendents)
     *
     * This is done through a recursive query on the report_lineage table
     */
    override fun <T, P, U> fetchRelatedActions(
        submissionId: Long,
        klass: Class<T>,
        reportsKlass: Class<P>,
        logsKlass: Class<U>,
    ): List<T> {
        val cte = reportDescendantExpression(submissionId)
        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(cte)
                .selectDistinct(detailedSubmissionSelect(reportsKlass, logsKlass))
                .from(ACTION)
                .join(cte)
                .on(ACTION.ACTION_ID.eq(cte.field("action_id", BIGINT)))
                .where(ACTION.ACTION_ID.ne(submissionId))
                .fetchInto(klass)
        }
    }
}