package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.StorageAccount
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.common.BaseEngine
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import kotlin.math.ceil
import kotlin.math.roundToInt

const val batchDecider = "batchDecider"

/**
 * This runs as a cron job every minute to determine which receivers, if any, should have batch queue message(s)
 * added to the stack. A [workflowEngine] can be passed in for mocking/testing purposes.
 */
class BatchDeciderFunction(private val workflowEngine: WorkflowEngine = WorkflowEngine()) : Logging {
    @FunctionName(batchDecider)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        // run every minute (NCRONTAB expression {second} {minute} {hour} {day} {month} {day-of-week})
        @TimerTrigger(name = "batchDecider", schedule = "0 * * * * *")
        @Suppress("UNUSED_PARAMETER")
        timerInfo: String,
        @Suppress("UNUSED_PARAMETER")
        context: ExecutionContext?,
    ) {
        logger.trace("$batchDecider: Starting")
        try {
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
                        val (queueMessages, isEmpty) = determineQueueMessageCount(rec, txn)

                        repeat(queueMessages) {
                            // build 'batch' event
                            val event = BatchEvent(Event.EventAction.BATCH, rec.fullName, isEmpty)
                            workflowEngine.queue.sendMessage(event)
                        }
                    }
            }

            logger.trace("$batchDecider: Ending")
        } catch (e: Exception) {
            // Catching all exceptions, so Azure does not auto-implement the queue retry
            logger.error("$batchDecider function exception", e)
        }
    }

    /**
     * Determines how many, if any, batch queue messages should be added to the batch queue for [receiver].
     * Handles checking the receiver's configured whenEmpty element of timing, and adding a batch message
     * if needed to send empty batch file.
     *
     * @param txn DataAccessTransaction to use. If not present, underlying data queries will create their own
     */
    internal fun determineQueueMessageCount(receiver: Receiver, txn: DataAccessTransaction?): Pair<Int, Boolean> {
        // Calculate how far to look back based on how often this receiver batches.
        val backstopTime = OffsetDateTime.now().minusMinutes(
            BaseEngine.getBatchLookbackMins(
                receiver.timing?.numberPerDay ?: 1, NUM_BATCH_RETRIES
            )
        )
        // get the number of messages outstanding for this receiver
        val recordsToBatch = workflowEngine.db.fetchNumReportsNeedingBatch(
            receiver.fullName, backstopTime, txn
        )
        var queueMessages = ceil((recordsToBatch.toDouble() / receiver.timing!!.maxReportCount.toDouble()))
            .roundToInt()
        val logMessage = "$batchDecider found $recordsToBatch for ${receiver.fullName}," +
            "max size ${receiver.timing.maxReportCount}. Queueing $queueMessages messages to BATCH"
        if (recordsToBatch > 0) logger.info(logMessage)
        else logger.debug(logMessage)

        var isEmpty = false
        // if there are no records to send but the receiver is set on 'send when empty' check if
        //  a message should be added anyway
        if (receiver.timing.whenEmpty.action == Receiver.EmptyOperation.SEND && queueMessages == 0) {

            val oneDayAgo = OffsetDateTime.now().minusDays(1)
            // lazy load this, may not need it if empty file is set to go every batch run
            val sentInLastDay: Boolean by lazy {
                workflowEngine.db.checkRecentlySent(
                    receiver.organizationName,
                    receiver.name,
                    oneDayAgo,
                    txn
                )
            }
            // determine if we need to send an 'empty' file. This is true if either we send an empty
            //  file every time the batch runs for this receiver of if we have not had a SEND
            //  action within the last 24 hours
            if (!receiver.timing.whenEmpty.onlyOncePerDay || !sentInLastDay) {
                queueMessages = 1
                isEmpty = true
            }
        }

        return Pair(queueMessages, isEmpty)
    }
}