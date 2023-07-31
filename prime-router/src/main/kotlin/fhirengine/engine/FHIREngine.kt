package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import java.time.Duration

const val elrConvertQueueName = "elr-fhir-convert"
const val elrRoutingQueueName = "elr-fhir-route"
const val elrTranslationQueueName = "elr-fhir-translate"

/**
 * All logical processing for full ELR / FHIR processing should be within this class.
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
abstract class FHIREngine(
    val metadata: Metadata = Metadata.getInstance(),
    val settings: SettingsProvider = this.settingsProviderSingleton,
    val db: DatabaseAccess = this.databaseAccessSingleton,
    val blob: BlobAccess = BlobAccess(),
    queue: QueueAccess = QueueAccess,
) : BaseEngine(queue) {

    /**
     * Custom builder for Workflow engine
     * [metadata] mockable metadata
     * [settingsProvider] mockable settingsProvider
     * [databaseAccess] mockable data access class
     * [blobAccess] mockable blob storage access class
     * [queueAccess] mockable azure queue access class
     * [hl7Serializer] legacy pipeline hl7 serializer
     * [csvSerializer] legacy pipeline csv serializer
     */
    data class Builder(
        var metadata: Metadata? = null,
        var settingsProvider: SettingsProvider? = null,
        var databaseAccess: DatabaseAccess? = null,
        var blobAccess: BlobAccess? = null,
        var queueAccess: QueueAccess? = null,
        var hl7Serializer: Hl7Serializer? = null,
        var csvSerializer: CsvSerializer? = null
    ) {
        /**
         * Set the metadata instance.
         * @return the modified workflow engine
         */
        fun metadata(metadata: Metadata) = apply { this.metadata = metadata }

        /**
         * Set the settings provider instance.
         * @return the modified workflow engine
         */
        fun settingsProvider(settingsProvider: SettingsProvider) = apply { this.settingsProvider = settingsProvider }

        /**
         * Set the database access instance.
         * @return the modified workflow engine
         */
        fun databaseAccess(databaseAccess: DatabaseAccess) = apply { this.databaseAccess = databaseAccess }

        /**
         * Set the blob access instance.
         * @return the modified workflow engine
         */
        fun blobAccess(blobAccess: BlobAccess) = apply { this.blobAccess = blobAccess }

        /**
         * Set the queue access instance.
         * @return the modified workflow engine
         */
        fun queueAccess(queueAccess: QueueAccess) = apply { this.queueAccess = queueAccess }

        /**
         * Build the fhir engine instance.
         * @return the fhir engine instance
         */
        fun build(taskAction: TaskAction): FHIREngine {
            settingsProvider = if (metadata != null) {
                settingsProvider ?: getSettingsProvider(metadata!!)
            } else {
                settingsProvider ?: settingsProviderSingleton
            }

            // create the correct FHIREngine type for the action being taken
            return when (taskAction) {
                TaskAction.process -> FHIRConverter(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    queueAccess ?: QueueAccess
                )
                TaskAction.route -> FHIRRouter(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    queueAccess ?: QueueAccess
                )
                TaskAction.translate -> FHIRTranslator(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    queueAccess ?: QueueAccess
                )
                else -> throw NotImplementedError("Invalid action type for FHIR engine")
            }
        }
    }

    /**
     * The functional part of any given type of FHIR engine, taking in the [message] to do whatever work needs to
     * be done, tracking with the [actionLogger] and [actionHistory], and making use of [metadata] if present
     */
    abstract fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory
    )

    /**
     * This value is used to delay the next step in the pipeline from processing the event in order to make sure
     * that the requisite DB transaction has been committed.  This is only configured for production so as not to
     * arbitrarily slow down staging or local development
     *
     * It should be removed as part of implementing https://github.com/CDCgov/prime-reportstream/issues/10638
     */
    val queueVisibilityTimeout: Duration =
        if (Environment.get() == Environment.PROD) Duration.ofMinutes(5) else Duration.ZERO
}