package gov.cdc.prime.router.history.db

import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.db.DatabaseAccess
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedReport
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.CommonTableExpression
import org.jooq.Condition
import org.jooq.SelectFieldOrAsterisk
import org.jooq.exception.TooManyRowsException
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.util.*

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(
    db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
) : HistoryDatabaseAccess(db), Logging {

    /**
     * Creates a condition filter based on the given organization parameters.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @return Condition used to filter the organization involved in the requested history
     */
    override fun organizationFilter(
        organization: String,
        orgService: String?,
    ): Condition {
        var senderFilter = Tables.ACTION.ACTION_NAME.eq(TaskAction.receive)
            .and(Tables.REPORT_FILE.SENDING_ORG.eq(organization))

        if (orgService != null) {
            senderFilter = senderFilter.and(Tables.REPORT_FILE.SENDING_ORG_CLIENT.eq(orgService))
        }

        return senderFilter
    }

    /**
     * Fetch a single (usually detailed) action of a specific type.
     *
     * @param actionId the action id attached to this submission.
     * @param orgName Optional name of the organization.   Needed on submission queries.  The 'retrieve'
     * action has multiple reports associated with it.   We need to make sure we get originally submitted report.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    override fun <T> fetchAction(
        actionId: Long,
        orgName: String?,
        klass: Class<T>,
    ): T? {
        if (orgName == null) error("Submission query must be constrained by a sender's organization name")
        return db.transactReturning { txn ->
            DSL.using(txn)
                .select(detailedSelect())
                .from(Tables.ACTION)
                .where(
                    Tables.ACTION.ACTION_ID.eq(actionId)
                        .and(Tables.ACTION.SENDING_ORG.eq(orgName))
                )
                .fetchOne()?.into(klass)
        }
    }

    /**
     * Add logs and reports related to the history being fetched.
     *
     * @return a jooq select statement adding additional DB columns.
     */
    fun detailedSelect(): List<SelectFieldOrAsterisk> {
        val org = Tables.SETTING.`as`("org")
        val receiver = Tables.SETTING.`as`("receiver")

        return listOf(
            Tables.ACTION.asterisk(),
            DSL.multiset(
                DSL.select()
                    .from(Tables.ACTION_LOG)
                    .where(Tables.ACTION_LOG.ACTION_ID.eq(Tables.ACTION.ACTION_ID))
            ).`as`("logs").convertFrom { r ->
                r?.into(DetailedActionLog::class.java)
            },
            DSL.multiset(
                DSL.select(
                    Tables.REPORT_FILE.REPORT_ID,
                    Tables.REPORT_FILE.RECEIVING_ORG,
                    Tables.REPORT_FILE.RECEIVING_ORG_SVC,
                    Tables.REPORT_FILE.SENDING_ORG,
                    Tables.REPORT_FILE.SENDING_ORG_CLIENT,
                    Tables.REPORT_FILE.SCHEMA_TOPIC,
                    Tables.REPORT_FILE.EXTERNAL_NAME,
                    Tables.REPORT_FILE.CREATED_AT,
                    Tables.REPORT_FILE.NEXT_ACTION_AT,
                    Tables.REPORT_FILE.ITEM_COUNT,
                    Tables.REPORT_FILE.ITEM_COUNT_BEFORE_QUAL_FILTER,
                    DSL.field("\"receiver\".\"values\"->>'transport' IS NOT NULL")
                        .`as`("receiverHasTransport")
                )
                    .from(Tables.REPORT_FILE)
                    .leftJoin(org)
                    .on(
                        Tables.REPORT_FILE.RECEIVING_ORG.eq(org.NAME)
                            .and(org.IS_ACTIVE.eq(true))
                            .and(org.IS_DELETED.eq(false))
                    )
                    .leftJoin(receiver)
                    .on(
                        org.SETTING_ID.eq(receiver.ORGANIZATION_ID)
                            .and(Tables.REPORT_FILE.RECEIVING_ORG_SVC.eq(receiver.NAME))
                            .and(receiver.IS_ACTIVE.eq(true))
                            .and(receiver.IS_DELETED.eq(false))
                    )
                    .where(Tables.REPORT_FILE.ACTION_ID.eq(Tables.ACTION.ACTION_ID))
            ).`as`("reports").convertFrom { r ->
                r?.into(DetailedReport::class.java)
            }
        )
    }

    override fun <T> fetchRelatedActions(reportId: UUID, klass: Class<T>): List<T> {
        return db.transactReturning { txn ->
            // We need to use the report ID to find the correct start of the report lineage. This allows for
            // flexibility in the pipelines to not have a child report on the very first action on a submitted report.
            // Report lineages for a parent report that is the submitted report (what was received) always have the same
            // action ID.
            val actionId = try {
                DSL.using(txn)
                    .selectDistinct(Tables.REPORT_LINEAGE.ACTION_ID)
                    .from(Tables.REPORT_LINEAGE)
                    .where(Tables.REPORT_LINEAGE.PARENT_REPORT_ID.eq(reportId))
                    .fetchOneInto(Long::class.java)
            } catch (e: TooManyRowsException) {
                logger.warn("Invalid report file lineage with multiple action IDs for parent report $reportId")
                null
            }
            if (actionId != null) {
                val cte = reportDescendantExpression(actionId)
                DSL.using(txn)
                    .withRecursive(cte)
                    .selectDistinct(detailedSelect())
                    .from(Tables.ACTION)
                    .join(cte)
                    .on(Tables.ACTION.ACTION_ID.eq(cte.field("action_id", SQLDataType.BIGINT)))
                    .where(Tables.ACTION.ACTION_ID.ne(actionId))
                    .fetchInto(klass)
            } else {
                emptyList()
            }
        }
    }

    /**
     * Fetch the details of an action's relations (descendants).
     * This is done through a recursive query on the report_lineage table.
     *
     * @param actionId the action id attached to the action to find relations for.
     * @return a list of descendants for the given action id.
     */
    private fun reportDescendantExpression(actionId: Long): CommonTableExpression<*> {
        return DSL.name("t").fields(
            "action_id",
            "child_report_id",
            "parent_report_id"
            // Backticks escape the kotlin reserved word, so JOOQ can use it's "as"
        ).`as`(
            DSL.select(
                Tables.REPORT_LINEAGE.ACTION_ID,
                Tables.REPORT_LINEAGE.CHILD_REPORT_ID,
                Tables.REPORT_LINEAGE.PARENT_REPORT_ID
            )
                .from(Tables.REPORT_LINEAGE)
                .where(Tables.REPORT_LINEAGE.ACTION_ID.eq(actionId))
                .unionAll(
                    DSL.select(
                        Tables.REPORT_LINEAGE.ACTION_ID,
                        Tables.REPORT_LINEAGE.CHILD_REPORT_ID,
                        Tables.REPORT_LINEAGE.PARENT_REPORT_ID
                    )
                        .from(DSL.table(DSL.name("t")))
                        .join(Tables.REPORT_LINEAGE)
                        .on(
                            DSL.field(DSL.name("t", "child_report_id"), SQLDataType.UUID)
                                .eq(Tables.REPORT_LINEAGE.PARENT_REPORT_ID)
                        )
                )
        )
    }
}