package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.records.ActionRecord
import org.jooq.impl.DSL
/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess {
    /**
     * Object to access the database.
     */
    private val db = DatabaseAccess()

    fun fetchSubmissions(sendingOrg: String): List<Action> {
        var submissions = emptyList<Action>()
        db.transact { txn ->
            submissions = DSL.using(txn)
                .selectFrom(ACTION)
                .where(ACTION.ACTION_NAME.eq(TaskAction.receive).and(ACTION.SENDING_ORG.eq(sendingOrg)))
                // TODO: this is just for testing
                .limit(10)
                .fetchInto(Action::class.java)
        }
        return submissions
    }
}