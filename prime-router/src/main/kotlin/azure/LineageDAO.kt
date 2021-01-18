package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables
import org.jooq.Configuration
import org.jooq.JSONB
import org.jooq.impl.DSL

/**
 * A data access object for interacting with the lineage/history tables
 */
class LineageDAO(val db: DatabaseAccess) {

    /**
     * Put an action into the database.
     */
    fun insertAction(
        currentEvent: Event,
        actionResult: String? = null,
        txn: DataAccessTransaction? = null,
    ) {
        fun insert(txn: Configuration) {
            DSL.using(txn)
                .insertInto(
                    Tables.ACTION,
                    Tables.ACTION.ACTION_NAME,
                    Tables.ACTION.ACTION_RESULT,
                ).values(
                    currentEvent.action.toTaskAction(),
                    JSONB.valueOf(actionResult),
                ).execute()
        }
        if (txn != null) {
            insert(txn)
        } else {
            db.transact { innerTxn -> insert(innerTxn) }
        }
    }
}