package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import org.jooq.impl.DSL
/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess {
    /**
     * Object to access the database.
     */
    private val db = DatabaseAccess()

    /**
     * @param sendingOrg is the Organization Name returned from the Okta JWT Claim.
     * @param limit is an Integer used for setting the number of results per page.
     * @return a list of results matching the SQL Query.
     */
    fun fetchActions(sendingOrg: String, limit: Int): List<Action> {
        var actions = emptyList<Action>()
        db.transact { txn ->
            actions = DSL.using(txn)
                .selectFrom(ACTION)
                .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                .limit(limit)
                .fetchInto(Action::class.java)
        }
        return actions
    }
}