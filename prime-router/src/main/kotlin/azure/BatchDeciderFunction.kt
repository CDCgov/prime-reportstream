package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.StorageAccount
import com.microsoft.azure.functions.annotation.TimerTrigger
import org.apache.logging.log4j.kotlin.Logging
import kotlin.math.ceil
import kotlin.math.roundToInt

const val batchDecider = "batchDecider"

/**
 * This runs as a cron job every minute to determine which receivers, if any, should have batch queue message(s)
 * added to the stack.
 */
class BatchDeciderFunction : Logging {
    @FunctionName(batchDecider)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        // run every minute (NCRONTAB expression {second} {minute} {hour} {day} {month} {day-of-week})
        @TimerTrigger(name = "batchDecider", schedule = "0 * * * * *")
        @Suppress("UNUSED_PARAMETER")
        timerInfo: String,
        @Suppress("UNUSED_PARAMETER")
        context: ExecutionContext,
    ) {
        logger.info("$batchDecider: Starting")
        try {
            val workflowEngine = WorkflowEngine()
            workflowEngine.db.transact { txn ->
                // TODO: if for some reason the batch decider misses proper calculation of a receiver's batch run
                //  we would want to pull all receivers with BATCH records that have next_action_at in the past and
                //  merge those with the batchInPrevious60Seconds. Testing shows this to be an unlikely scenario, but
                //  there is always the chance that something odd happens with the timing of Azure timed functions.

                // find all receivers that should have batched within the last 60 seconds
                workflowEngine.settings.receivers.filter { it.timing != null && it.timing.batchInPrevious60Seconds() }
                    // any that should have batched in the last 60 seconds, get count of outstanding BATCH records
                    //  (how many actions with BATCH for receiver
                    .forEach { rec ->
                        // TODO: to support sending empty batches, we will need to check if the receiver wants that
                        //  and add the queue message regardless. Currently functionality only adds a message if
                        //  there is at least one batch to run
                        // get the number of messages outstanding for this receiver
                        val recordsToBatch = workflowEngine.db.fetchNumberOutstandingBatchRecords(rec.fullName, txn)
                        val queueMessages = ceil((recordsToBatch.toDouble() / rec.timing!!.maxReportCount.toDouble()))
                            .roundToInt()
                        logger.info(
                            "$batchDecider found $recordsToBatch for ${rec.fullName}," +
                                "max size ${rec.timing.maxReportCount}. Queueing $queueMessages messages to BATCH"
                        )

                        repeat(queueMessages) {
                            // build 'batch' event
                            val event = BatchEvent(Event.EventAction.BATCH, rec.fullName)
                            QueueAccess.sendMessage(event)
                        }
                    }
            }

            logger.info("$batchDecider: Ending")
        } catch (e: Exception) {
            logger.error("$batchDecider function exception", e)
        }
    }
}