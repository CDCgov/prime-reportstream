package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import org.jooq.impl.DSL
import java.util.logging.Level

const val batchDecider = "batchDecider"

/**
 * This runs as a cron job every minute to determine which receivers, if any, should have a batch queue message
 * added to the stack
 */
class BatchDeciderFunction {
    @FunctionName(batchDecider)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        // run every minute (NCRONTAB expression {second} {minute} {hour} {day} {month} {day-of-week})
        @TimerTrigger(name = "batchNotifier", schedule = "0 * * * * *")
        timerInfo: String,
        context: ExecutionContext,
    ) {
        try {
            val workflowEngine = WorkflowEngine()
            val db = WorkflowEngine().db
            db.transact { txn ->
                // get maximum actionId - any task associated with an action <= this is ready to be batched.
                // This gives a hard 'stop point' to the batching window to ensure that a batch that is expected at :05
                // (for example) cannot have records that came in at :06 if it takes a while to process, and ensures
                // we can definitively determine when we are done with all records of a batch
                val maxActionId = workflowEngine.db.fetchHighestActionId(txn)

                // TODO determine which receivers should be getting a batch now
//                val receivers = workflowEngine.db.determineCurrentReceivers(txn)

                // TODO add message to batch queue for each receiver, with max actionId

                // TODO need to add an action or some record that a 'batch' queue message was added?
            }

            context.logger.info("$batchDecider: Ending")

        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "Batch decider function exception", e)
        }
    }
}