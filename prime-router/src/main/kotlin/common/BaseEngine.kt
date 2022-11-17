package gov.cdc.prime.router.common

import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.SettingsFacade
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.apache.logging.log4j.kotlin.Logging

/**
 * Holds functionality that is shared between WorkflowEngine and FhirEngine.
 * [queue] mockable queueAccess instance to be used by the pipeline engines
 * TODO: This class will need to be further refactored / fleshed out. Only minimal changes required for #4824 are
 *  included in this file at this time to limit scope
 */
abstract class BaseEngine(
    val queue: QueueAccess = QueueAccess
) : Logging {
    companion object {
        /**
         * These are all potentially heavyweight objects that
         * should only be created once.
         */
        val databaseAccessSingleton: DatabaseAccess by lazy {
            DatabaseAccess()
        }

        val settingsProviderSingleton: SettingsProvider by lazy {
            getSettingsProvider(Metadata.getInstance())
        }

        internal val csvSerializerSingleton: CsvSerializer by lazy {
            CsvSerializer(Metadata.getInstance())
        }

        internal val hl7SerializerSingleton: Hl7Serializer by lazy {
            Hl7Serializer(Metadata.getInstance(), settingsProviderSingleton)
        }

        /**
         * Get a settings provider for a given [metadata] instance.
         * @return a settings provider
         */
        internal fun getSettingsProvider(metadata: Metadata): SettingsProvider {
            val baseDir = System.getenv("AzureWebJobsScriptRoot") ?: "."
            val settingsEnabled: String? = System.getenv("FEATURE_FLAG_SETTINGS_ENABLED")
            return if (settingsEnabled == null || settingsEnabled.equals("true", ignoreCase = true)) {
                SettingsFacade(metadata, databaseAccessSingleton)
            } else {
                val ext = "-${Environment.get().toString().lowercase()}"
                FileSettings("$baseDir/settings", orgExt = ext)
            }
        }

        /**
         * Always find tasks at least this old.  This covers for  extended downtime due to a crash,
         * as well as for 25-hour days etc.
         */
        const val BATCH_LOOKBACK_PADDING_MINS: Long = 180 // 3 hours

        /**
         * BatchFunction uses a backstop time to prevent it from processing too-old records.
         * We also use this to prevent it from retrying unrecoverable batches over and over.
         * So the backstop time is based on the frequency of batching for that receiver,
         * as found in the receiver's [Receiver.Timing.numberPerDay].
         *
         * Note the effect of the padding is that for frequent batching, we'll actually allow
         * more than [minNumRetries] retries.
         *
         * Calculation is done in minutes.
         */
        internal fun getBatchLookbackMins(numberBatchesPerDay: Int, minNumRetries: Int): Long {
            val frequencyMins = if (numberBatchesPerDay > 0) {
                1440 / numberBatchesPerDay
            } else {
                1440
            }
            return ((minNumRetries + 1) * frequencyMins + BATCH_LOOKBACK_PADDING_MINS)
        }
    }
}