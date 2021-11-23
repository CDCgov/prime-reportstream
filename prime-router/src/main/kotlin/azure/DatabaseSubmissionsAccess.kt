package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import org.jooq.impl.DSL
import java.time.OffsetDateTime

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess(private val db: DatabaseAccess = DatabaseAccess()) {

    /**
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param sortOrder sort the table by date in ASC or DESC order.
     * @param resultsAfterDate is the Action Id of the last result in the previous list.
     * @param limit is an Integer used for setting the number of results per page.
     * @return a list of results matching the SQL Query.
     */
    fun fetchActions(
        sendingOrg: String,
        sortOrder: String,
        resultsAfterDate: String,
        limit: Int,
    ): List<Action> {
        var actions = emptyList<Action>()

        val sorted = if (sortOrder == "ASC") ACTION.CREATED_AT.asc() else ACTION.CREATED_AT.desc()

        if (!resultsAfterDate.isNullOrEmpty()) {
            db.transact { txn ->
                actions = DSL.using(txn)
                    .selectFrom(ACTION)
                    .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                    .orderBy(sorted)
                    .seek(OffsetDateTime.parse(resultsAfterDate))
                    .limit(limit)
                    .fetchInto(Action::class.java)
            }
        } else {
            db.transact { txn ->
                actions = DSL.using(txn)
                    .selectFrom(ACTION)
                    .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                    .orderBy(sorted)
                    .limit(limit)
                    .fetchInto(Action::class.java)
            }
        }

        return actions
    }
}