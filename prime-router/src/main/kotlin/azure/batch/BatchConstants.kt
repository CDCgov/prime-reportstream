package gov.cdc.prime.router.azure.batch

/**
 * Collection of constants for Batch functions and queues
 */
object BatchConstants {

    // batch functions
    object Function {
        const val COVID_BATCH_FUNCTION = "covid-batch-fn"
        const val UNIVERSAL_BATCH_FUNCTION = "universal-batch-fn"
    }

    // batch queues
    object Queue {
        const val COVID_BATCH_QUEUE = "covid-batch-queue"
        const val UNIVERSAL_BATCH_QUEUE = "universal-batch-queue"
    }

    // batch size
    const val DEFAULT_BATCH_SIZE = 100

    // Min number of times to retry a failed batching operation.
    const val NUM_BATCH_RETRIES = 2
}