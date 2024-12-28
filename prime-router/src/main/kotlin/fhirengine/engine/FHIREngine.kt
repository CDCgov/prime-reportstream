package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.reportstream.shared.QueueMessage
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
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.azure.observability.event.ReportStreamEventService
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.jooq.Field
import java.time.OffsetDateTime

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
    val azureEventService: AzureEventService = AzureEventServiceImpl(),
    val reportService: ReportService = ReportService(ReportGraph(db), db),
    val reportEventService: IReportStreamEventService,
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
        var csvSerializer: CsvSerializer? = null,
        var azureEventService: AzureEventService? = null,
        var reportService: ReportService? = null,
        var reportEventService: IReportStreamEventService? = null,
        var submissionTableService: SubmissionTableService? = null,
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
         * Set the azure event service instance.
         * @return the modified workflow engine
         */
        fun azureEventService(azureEventService: AzureEventService) = apply {
            this.azureEventService = azureEventService
        }

        /**
         * Set the report service instance.
         * @return the modified workflow engine
         */
        fun reportService(reportService: ReportService) = apply {
            this.reportService = reportService
        }

        fun reportEventService(reportEventService: ReportStreamEventService) = apply {
            this.reportEventService = reportEventService
        }

        fun submissionTableService(submissionTableService: SubmissionTableService) = apply {
            this.submissionTableService = submissionTableService
        }

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
                    azureEventService ?: AzureEventServiceImpl(),
                    reportService ?: ReportService(),
                    ReportStreamEventService(
                        databaseAccess ?: databaseAccessSingleton,
                        azureEventService ?: AzureEventServiceImpl(),
                        reportService ?: ReportService()
                    )
                )
                TaskAction.destination_filter -> FHIRDestinationFilter(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    azureEventService ?: AzureEventServiceImpl(),
                    reportService ?: ReportService(),
                    ReportStreamEventService(
                        databaseAccess ?: databaseAccessSingleton,
                        azureEventService ?: AzureEventServiceImpl(),
                        reportService ?: ReportService()
                    )
                )
                TaskAction.receiver_enrichment -> FHIRReceiverEnrichment(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    azureEventService ?: AzureEventServiceImpl(),
                    reportService ?: ReportService(),
                    ReportStreamEventService(
                        databaseAccess ?: databaseAccessSingleton,
                        azureEventService ?: AzureEventServiceImpl(),
                        reportService ?: ReportService()
                    )
                )
                TaskAction.receiver_filter -> FHIRReceiverFilter(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    azureEventService ?: AzureEventServiceImpl(),
                    reportService ?: ReportService(),
                    ReportStreamEventService(
                        databaseAccess ?: databaseAccessSingleton,
                        azureEventService ?: AzureEventServiceImpl(),
                        reportService ?: ReportService()
                    )
                )
                TaskAction.translate -> FHIRTranslator(
                    metadata ?: Metadata.getInstance(),
                    settingsProvider!!,
                    databaseAccess ?: databaseAccessSingleton,
                    blobAccess ?: BlobAccess(),
                    azureEventService ?: AzureEventServiceImpl(),
                    reportService ?: ReportService(),
                    reportEventService ?: ReportStreamEventService(
                        databaseAccess ?: databaseAccessSingleton,
                        azureEventService ?: AzureEventServiceImpl(),
                        reportService ?: ReportService()
                    )
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
    abstract fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
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
     * The task action associated with the engine
     */
    abstract val taskAction: TaskAction

    /**
     * Result class that is returned as part of completing the work on a message
     *
     * @param nextEvent the next event that should be propagated
     * @param report the report generated
     * @param reportUrl the URL for the generated report
     * @param queueMessage optionally a message that should be dispatched to a queue
     *
     */
    data class FHIREngineRunResult(
        val nextEvent: Event,
        val report: Report,
        val reportUrl: String,
        val queueMessage: QueueMessage?,
    )

    /**
     *
     * Responsible for invoking the [doWork] function, inserting any new tasks and updating the previous task and
     * returning any messages that need to be added to the queue
     * If an exception is encountered it is logged and then rethrown in order to rollback the transaction
     *
     * @param queueMessage the message to process
     * @param actionLogger  the action logger to use
     * @param actionHistory the action history to use
     * @param txn the database transaction
     *
     */
    fun run(
        queueMessage: ReportPipelineMessage,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction,
    ): List<QueueMessage> {
        try {
            // Do the FHIR work (convert, route, translate)
            val results = doWork(queueMessage, actionLogger, actionHistory)

            // Add the next task
            results.forEach {
                db.insertTask(it.report, it.report.bodyFormat.toString(), it.reportUrl, it.nextEvent, txn)
            }

            // Nullify the previous task
            db.updateTask(
                queueMessage.reportId,
                TaskAction.none,
                null,
                null,
                finishedField = this.finishedField,
                txn
            )

            // Return the result to commit the transaction and add to the queue
            return results.mapNotNull { it.queueMessage }
        } catch (ex: Exception) {
            logger.error(ex)
            actionLogger.error(InvalidReportMessage(ex.message ?: ""))
            // The error gets logged but rethrown so that the passed in transaction can get rolled back
            throw ex
        }
    }

    /**
     * The name of the lookup table to load the shorthand replacement key/value pairs from
     */
    private val fhirPathFilterShorthandTableName = "fhirpath_filter_shorthand"

    /**
     * The name of the column in the shorthand replacement lookup table that will be used as the key.
     */
    private val fhirPathFilterShorthandTableKeyColumnName = "variable"

    /**
     * The name of the column in the shorthand replacement lookup table that will be used as the value.
     */
    private val fhirPathFilterShorthandTableValueColumnName = "fhirPath"

    /**
     * Lookup table `fhirpath_filter_shorthand` containing all the shorthand fhirpath replacements for filtering.
     */
    protected val shorthandLookupTable by lazy { loadFhirPathShorthandLookupTable() }

    /**
     * Load the fhirpath_filter_shorthand lookup table into a map if it can be found and has the expected columns,
     * otherwise log warnings and return an empty lookup table with the correct columns. This is valid since having
     * a populated lookup table is not required to run the universal pipeline routing
     *
     * @returns Map containing all the values in the fhirpath_filter_shorthand lookup table. Empty map if the
     * lookup table was not found, or it does not contain the expected columns. If an empty map is returned, a
     * warning indicating why will be logged.
     */
    internal fun loadFhirPathShorthandLookupTable(): MutableMap<String, String> {
        val lookup = metadata.findLookupTable(fhirPathFilterShorthandTableName)
        // log a warning and return an empty table if either lookup table is missing or has incorrect columns
        return if (lookup != null &&
            lookup.hasColumn(fhirPathFilterShorthandTableKeyColumnName) &&
            lookup.hasColumn(fhirPathFilterShorthandTableValueColumnName)
        ) {
            lookup.table.associate {
                it.getString(fhirPathFilterShorthandTableKeyColumnName) to
                    it.getString(fhirPathFilterShorthandTableValueColumnName)
            }.toMutableMap()
        } else {
            if (lookup == null) {
                logger.warn("Unable to find $fhirPathFilterShorthandTableName lookup table")
            } else {
                logger.warn(
                    "$fhirPathFilterShorthandTableName does not contain " +
                        "expected columns 'variable' and 'fhirPath'"
                )
            }
            emptyMap<String, String>().toMutableMap()
        }
    }
}