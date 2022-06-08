package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.QueueAccess
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer

const val elrTranslateQueueName = "elr-fhir-translate"

/**
 * All logical processing for full ELR / FHIR processing should be within this class.
 * [metadata] mockable metadata
 * [settings] mockable settings
 * [db] mockable database access
 * [blob] mockable blob storage
 * [queue] mockable azure queue
 */
class FHIREngine(
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
        fun build(): FHIREngine {
            settingsProvider = if (metadata != null) {
                settingsProvider ?: getSettingsProvider(metadata!!)
            } else {
                settingsProvider ?: settingsProviderSingleton
            }

            return FHIREngine(
                metadata ?: Metadata.getInstance(),
                settingsProvider!!,
                databaseAccess ?: databaseAccessSingleton,
                blobAccess ?: BlobAccess(),
                queueAccess ?: QueueAccess
            )
        }
    }

    /**
     * Process a [message] off of the raw-elr azure queue, convert it into FHIR, and store for next step.
     * [actionHistory] and [actionLogger] ensure all activities are logged.
     * [hl7Reader] converts a string message into HL7 and ensures it is valid
     * [metadata] will usually be null; mocked metadata can be passed in for unit tests
     */
    fun processHL7(
        message: RawSubmission,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
        hl7Reader: HL7Reader = HL7Reader(actionLogger),
        metadata: Metadata? = null
    ) {
        logger.trace("Processing HL7 data for FHIR conversion.")
        try {
            // get the hl7 from the blob store
            val hl7messages = hl7Reader.getMessages(message.downloadContent())

            if (actionLogger.hasErrors()) {
                throw java.lang.IllegalArgumentException(actionLogger.errors.joinToString("\n") { it.detail.message })
            }

            // use fhir transcoder to turn hl7 into FHIR
            val fhirBundles = hl7messages.map {
                HL7toFhirTranslator.getInstance().translate(it)
            }

            logger.debug("Generated ${fhirBundles.size} FHIR bundles.")

            actionHistory.trackExistingInputReport(message.reportId)

            // operate on each fhir bundle
            for (bundle in fhirBundles) {
                // make a 'report'
                val report = Report(
                    Report.Format.FHIR,
                    emptyList(),
                    fhirBundles.size,
                    itemLineage = listOf(
                        ItemLineage()
                    ),
                    metadata = metadata
                )

                // create item lineage
                report.itemLineages = listOf(
                    ItemLineage(
                        null,
                        message.reportId,
                        1,
                        report.id,
                        1,
                        null,
                        null,
                        null,
                        report.getItemHashForRow(1)
                    )
                )

                // create translate event
                val translateEvent = ProcessEvent(
                    Event.EventAction.TRANSLATE,
                    report.id,
                    Options.None,
                    emptyMap<String, String>(),
                    emptyList<String>()
                )

                // upload to blobstore
                var bodyBytes = FhirTranscoder.encode(bundle).toByteArray()
                var blobInfo = BlobAccess.uploadBody(
                    Report.Format.FHIR,
                    bodyBytes,
                    report.name,
                    message.sender,
                    translateEvent.eventAction
                )

                // track created report
                actionHistory.trackCreatedReport(translateEvent, report, blobInfo)

                // insert task
                this.insertTranslateTask(
                    report,
                    report.bodyFormat.toString(),
                    blobInfo.blobUrl,
                    translateEvent
                )

                Options.None

                // move to translation (send to <elrTranslateQueueName> queue)
                this.queue.sendMessage(
                    elrTranslateQueueName,
                    RawSubmission(
                        report.id,
                        blobInfo.blobUrl,
                        BlobAccess.digestToString(blobInfo.digest),
                        message.sender
                    ).serialize()
                )
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e)
            actionLogger.error(InvalidReportMessage(e.message ?: ""))
        }
    }

    /**
     * Inserts a 'translate' task into the task table for the [report] in question. This is just a pass-through function
     * but is present here for proper separation of layers and testing. This may need to be modified in the future.
     * The task will track the [report] in the [format] specified and knows it is located at [reportUrl].
     * [nextAction] specifies what is going to happen next for this report
     *
     */
    private fun insertTranslateTask(
        report: Report,
        reportFormat: String,
        reportUrl: String,
        nextAction: Event
    ) {
        db.insertTask(report, reportFormat, reportUrl, nextAction, null)
    }
}