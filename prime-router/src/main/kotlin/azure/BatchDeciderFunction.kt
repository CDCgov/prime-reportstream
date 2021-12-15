package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.StorageAccount
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.prime.router.cli.tests.CoolTest.Companion.settings
import java.util.logging.Level
import kotlin.math.ceil
import kotlin.math.roundToInt

const val batchDecider = "batchDecider"

/**
 * This runs as a cron job every minute to determine which receivers, if any, should have batch queue message(s)
 * added to the stack.
 */
class BatchDeciderFunction {
    @FunctionName(batchDecider)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        // run every minute (NCRONTAB expression {second} {minute} {hour} {day} {month} {day-of-week})
        @TimerTrigger(name = "batchDecider", schedule = "0 * * * * *")
        timerInfo: String,
        context: ExecutionContext,
    ) {
        context.logger.info("$batchDecider: Starting")
        try {
            val db = WorkflowEngine().db
            db.transact { txn ->
                // find all receivers that should have batched within the last 60 seconds
                settings.receivers.filter { it.timing != null && it.timing.batchInPrevious60Seconds() }
                    // any that should have, get count of outstanding BATCH records (how many actions with BATCH for
                    //  receiver
                    .forEach { rec ->
                        // todo: to support sending empty batches, we will need to check if the receiver wants that
                        //  and add the queue message regardless. Currently functionality only adds a message if
                        //  there is at least one batch to run
                        // get the number of messages outstanding for this receiver
                        val recordsToBatch = db.fetchNumberOutstandingBatchRecords(rec.organizationName, rec.name, txn)
                        val queueWorkers = ceil((recordsToBatch.toDouble() / rec.timing!!.maxReportCount.toDouble()))
                            .roundToInt()
                        if (queueWorkers > 0)
                            context.logger.info("Found $recordsToBatch for ${rec.fullName}," +
                                "max size ${rec.timing.maxReportCount}. Starting $queueWorkers queue workers")
                        repeat (queueWorkers) {
                            // build 'batch' event
                            val event = BatchEvent(Event.EventAction.BATCH, rec.fullName)
                            QueueAccess.sendMessage(event)
                        }
                    }
            }

            context.logger.info("$batchDecider: Ending")

        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "Batch decider function exception", e)
        }
    }
}