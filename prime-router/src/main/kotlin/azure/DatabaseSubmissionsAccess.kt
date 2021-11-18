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
     * @param order sorts the results by ASC or DESC order.
     * @param cursor is the Action Id of the last result in the previous list.
     * @param limit is an Integer used for setting the number of results per page.
     * @return a list of results matching the SQL Query.
     */
    fun fetchActions(
        sendingOrg: String,
        order: String,
        cursor: OffsetDateTime,
        limit: Int,
    ): List<Action> {
        var actions = emptyList<Action>()

        if (order == "DESC") {
            db.transact { txn ->
                actions = DSL.using(txn)
                    .selectFrom(ACTION)
                    .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                    .orderBy(ACTION.CREATED_AT.desc())
                    .seek(cursor)
                    .limit(limit)
                    .fetchInto(Action::class.java)
            }
        }

        if (order == "ASC") {
            db.transact { txn ->
                actions = DSL.using(txn)
                    .selectFrom(ACTION)
                    .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                    .orderBy(ACTION.CREATED_AT.asc())
                    .seek(cursor)
                    .limit(limit)
                    .fetchInto(Action::class.java)
            }
        }

        return actions
    }
}