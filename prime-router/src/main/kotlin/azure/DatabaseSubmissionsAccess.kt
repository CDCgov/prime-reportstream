package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables.ACTION
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

    // TODO: do we actually want to return 'Action' here?
    // TODO: needs mapping work - Jooq is not working how we need it to here
    fun fetchSubmissions(): List<Action> {
        var submissions = emptyList<Action>()
        db.transact { txn ->
            submissions = DSL.using(txn)
                .selectFrom(ACTION)
                // TODO: this is just for testing, we probably don't want to only get 10 records
                .limit(10)
                .fetchInto(Action::class.java)
        }
        return submissions
    }
}