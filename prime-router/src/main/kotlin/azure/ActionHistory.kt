package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import org.jooq.Configuration
import org.jooq.impl.DSL
import java.lang.IllegalArgumentException

/**
 * This is a container class that holds information to be stored, about a single action,
 * as well as the reports that went into that Action, and were created by that Action.
 */
class ActionHistory {

    // Use mutable jooq POJOs as convenient storage places
    var action = Action()
//    var reportsIn = listOf<ReportFile>()
//    var reportsOut = listOf<ReportFile>()

    constructor(actionStr: String, actionResult: String? = null) {
        try {
            action.actionName = TaskAction.valueOf(actionStr)
        } catch (e: IllegalArgumentException) {
            error("Unknown action $actionStr")
        }
        action.actionResult = actionResult
    }

    /*
     * Kotlin does not allow overloading the assignment operator.  Hence the setter.
     */
    fun setActionResult(actionResult: String) {
        // TODO should be able to get this max size from the jooq-generated code.
        action.actionResult = actionResult.chunked(2048)[0]
    }

    /**
     * Put the history about this action into the database.
     */
    fun saveToDb(db: DatabaseAccess, txn: Configuration? = null) {
        fun insertAction(txn: Configuration) {
            DSL.using(txn)
                .insertInto(
                    Tables.ACTION,
                    Tables.ACTION.ACTION_NAME,
                    Tables.ACTION.ACTION_RESULT,
                ).values(
                    action.actionName,
                    action.actionResult
                ).execute()
        }
        if (txn != null) {
            insertAction(txn)
        } else {
            db.transact { innerTxn -> insertAction(innerTxn) }
        }
    }
}