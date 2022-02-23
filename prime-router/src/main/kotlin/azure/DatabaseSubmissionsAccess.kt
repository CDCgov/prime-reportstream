package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.ACTION_LOG
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.CommonTableExpression
import org.jooq.SelectFieldOrAsterisk
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType.BIGINT
import org.jooq.impl.SQLDataType.UUID
import java.time.OffsetDateTime

interface SubmissionAccess {
    enum class SortOrder {
        DESC,
        ASC,
    }

    /* TODO: Needs to take sortColumn */
    fun <T> fetchActions(
        sendingOrg: String,
        order: SortOrder,
        resultsAfterDate: OffsetDateTime? = null,
        limit: Int = 10,
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
class DatabaseSubmissionsAccess(private val db: DatabaseAccess = DatabaseAccess()) : SubmissionAccess {

    /**
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param sortOrder sort the table by date in ASC or DESC order.
     * @param resultsAfterDate is the Action Id of the last result in the previous list.
     * @param limit is an Integer used for setting the number of results per page.
     * @return a list of results matching the SQL Query.
     */
    /* TODO: Needs to take sortColumn */
    override fun <T> fetchActions(
        sendingOrg: String,
        order: SubmissionAccess.SortOrder,
        resultsAfterDate: OffsetDateTime?,
        limit: Int,
        klass: Class<T>
    ): List<T> {

        /* TODO: FINALLY, this is where we can apply sortColumn instead of
         *  defaulting to ACTION.CREATED_AT
         */
        val sorted = if (order == SubmissionAccess.SortOrder.ASC) {
            ACTION.CREATED_AT.asc()
        } else ACTION.CREATED_AT.desc()

        return db.transactReturning { txn ->
            val query = DSL.using(txn)
                .selectFrom(ACTION)
                .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                .orderBy(sorted)
            if (resultsAfterDate != null) {
                query.seek(resultsAfterDate)
            }
            query.limit(limit)
                .fetchInto(klass)
        }
    }

    private fun <P, U> detailedSubmissionSelect(
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

    private fun reportDecendentExpression(submissionId: Long): CommonTableExpression<*> {
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
        val cte = reportDecendentExpression(submissionId)
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