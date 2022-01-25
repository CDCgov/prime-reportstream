package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.CommonTableExpression
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType.BIGINT
import org.jooq.impl.SQLDataType.UUID
import java.time.OffsetDateTime

interface SubmissionAccess {
    enum class SortOrder {
        DESC,
        ASC,
    }

    fun <T> fetchActions(
        sendingOrg: String,
        order: SortOrder,
        resultsAfterDate: OffsetDateTime? = null,
        limit: Int = 10,
        klass: Class<T>
    ): List<T>

    fun <T> fetchAction(
        sendingOrg: String,
        submissionId: Long,
        klass: Class<T>
    ): T?

    fun <T> fetchRelatedActions(
        submissionId: Long,
        klass: Class<T>
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
    override fun <T> fetchActions(
        sendingOrg: String,
        order: SubmissionAccess.SortOrder,
        resultsAfterDate: OffsetDateTime?,
        limit: Int,
        klass: Class<T>
    ): List<T> {

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

    override fun <T> fetchAction(
        sendingOrg: String,
        submissionId: Long,
        klass: Class<T>,
    ): T? {
        return db.transactReturning { txn ->
            DSL.using(txn)
                .select()
                .from(ACTION)
                .where(
                    ACTION.ACTION_NAME.eq(TaskAction.receive)
                        .and(ACTION.SENDING_ORG.eq(sendingOrg))
                        .and(ACTION.ACTION_ID.eq(submissionId))
                )
                .fetchOne()?.into(klass)
        }
    }

    fun reportDecendentExpression(submissionId: Long): CommonTableExpression<*> {
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

    override fun <T> fetchRelatedActions(
        submissionId: Long,
        klass: Class<T>,
    ): List<T> {
        val cte = reportDecendentExpression(submissionId)
        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(cte)
                .select()
                .from(ACTION)
                .join(cte)
                .on(ACTION.ACTION_ID.eq(cte.field("action_id", BIGINT)))
                .join(REPORT_FILE)
                .on(REPORT_FILE.REPORT_ID.eq(cte.field("child_report_id", UUID)))
                .where(ACTION.ACTION_ID.ne(submissionId))
                .fetchInto(klass)
        }
    }
}