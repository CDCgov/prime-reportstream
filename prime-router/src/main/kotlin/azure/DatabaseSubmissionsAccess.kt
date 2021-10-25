package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.Tables.pojos.Action
import org.jooq.impl.DSL

/**
 * Class to access lookup tables stored in the database.
 */
class DatabaseSubmissionsAccess {
    /**
     * Object to access the database.
     */
    private val db = DatabaseAccess()

    fun fetchSubmission(
        txn: DataAccessTransaction
    ): Action? {
        return DSL.using(txn)
            .selectFrom(ACTION)
            .fetchOne()
            ?.into(Action::class.java)
    }
}