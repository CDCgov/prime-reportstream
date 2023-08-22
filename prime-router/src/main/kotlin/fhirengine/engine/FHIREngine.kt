package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.jooq.Field
import java.time.OffsetDateTime

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
    val blob: BlobAccess = BlobAccess()
) : BaseEngine() {

    /**
     * Custom builder for Workflow engine
     * [metadata] mockable metadata
     * [settingsProvider] mockable settingsProvider
     * [databaseAccess] mockable data access class
     * [blobAccess] mockable blob storage access class
     * [hl7Serializer] legacy pipeline hl7 serializer
     * [csvSerializer] legacy pipeline csv serializer
     */
    data class Builder(
        var metadata: Metadata? = null,
        var settingsProvider: SettingsProvider? = null,
        var databaseAccess: DatabaseAccess? = null,
        var blobAccess: BlobAccess? = null,
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
                    blobAccess ?: BlobAccess()
                )
                TaskAction.route -> FHIRRouter(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess()
                )
                TaskAction.translate -> FHIRTranslator(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                )
                else -> throw NotImplementedError("Invalid action type for FHIR engine")
            }
        }
    }

    /**
     * The functional part of any given type of FHIR engine, taking in the [message] to do whatever work needs to
     * be done, tracking with the [actionLogger] and [actionHistory], and making use of [metadata] if present.  It
     * returns the result of the work so that messages can be passed along.
     */
    abstract fun doWork(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory
    ): List<FHIREngineRunResult>

    /**
     * Field that is updated as part of completing the work
     */
    abstract val finishedField: Field<OffsetDateTime>

    /**
     * The type of work the engine is performing
     */
    abstract val engineType: String

    /**
     * Result class that is returned as part of completing the work on a message
     *
     * @param nextEvent the next event that should be propagated
     * @param report the report generated
     * @param reportUrl the URL for the generated report
     * @param message optionally a message that should be dispatched to a queue
     *
     */
    data class FHIREngineRunResult(
        val nextEvent: Event,
        val report: Report,
        val reportUrl: String,
        val message: RawSubmission?
    )

    /**
     *
     * Responsible for invoking the [doWork] function, inserting any new tasks and updating the previous task
     * If an exception is encountered it is logged and then rethrown in order to rollback the transaction
     *
     * @param message the message to process
     * @param actionLogger  the action logger to use
     * @param actionHistory the action history to use
     * @param txn the database transaction
     *
     */
    fun run(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction
    ): List<RawSubmission> {
        try {
            // Do the FHIR work (convert, route, translate)
            val results = doWork(message, actionLogger, actionHistory)

            // Add the next task
            results.forEach {
                db.insertTask(it.report, it.report.bodyFormat.toString(), it.reportUrl, it.nextEvent, txn)
            }

            // Nullify the previous task
            db.updateTask(
                message.reportId,
                TaskAction.none,
                null,
                null,
                finishedField = this.finishedField,
                txn
            )

            // Return the result to commit the transaction and add to the queue
            return results.mapNotNull { it.message }
        } catch (ex: Exception) {
            logger.error(ex)
            actionLogger.error(InvalidReportMessage(ex.message ?: ""))
            // The error gets logged but rethrown so that the passed in transaction can get rolled back
            throw ex
        }
    }
}