package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import org.jooq.CommonTableExpression
import org.jooq.Condition
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(
    db: DatabaseAccess = BaseEngine.databaseAccessSingleton,
) : HistoryDatabaseAccess(db) {

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
        var senderFilter = ACTION.ACTION_NAME.eq(TaskAction.receive)
            .and(ACTION.SENDING_ORG.eq(organization))

        if (orgService != null) {
            senderFilter = senderFilter.and(ACTION.SENDING_ORG_CLIENT.eq(orgService))
        }

        return senderFilter
    }

    override fun <T> fetchRelatedActions(actionId: Long, klass: Class<T>): List<T> {
        val cte = reportDescendantExpression(actionId)
        return db.transactReturning { txn ->
            DSL.using(txn)
                .withRecursive(cte)
                .selectDistinct(detailedSelect())
                .from(ACTION)
                .join(cte)
                .on(ACTION.ACTION_ID.eq(cte.field("action_id", SQLDataType.BIGINT)))
                .where(ACTION.ACTION_ID.ne(actionId))
                .fetchInto(klass)
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
                Tables.REPORT_LINEAGE.PARENT_REPORT_ID,
            )
                .from(Tables.REPORT_LINEAGE)
                .where(Tables.REPORT_LINEAGE.ACTION_ID.eq(actionId))
                .unionAll(
                    DSL.select(
                        Tables.REPORT_LINEAGE.ACTION_ID,
                        Tables.REPORT_LINEAGE.CHILD_REPORT_ID,
                        Tables.REPORT_LINEAGE.PARENT_REPORT_ID,
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