package gov.cdc.prime.router.fhirengine.engine

import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.observability.event.AzureEventService
import gov.cdc.prime.router.azure.observability.event.AzureEventServiceImpl
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.report.ReportService
import org.jooq.Field
import java.time.OffsetDateTime

class FHIRReceiverEnrichment(
    metadata: Metadata = Metadata.getInstance(),
    settings: SettingsProvider = this.settingsProviderSingleton,
    db: DatabaseAccess = this.databaseAccessSingleton,
    blob: BlobAccess = BlobAccess(),
    azureEventService: AzureEventService = AzureEventServiceImpl(),
    reportService: ReportService = ReportService(),
    reportStreamEventService: IReportStreamEventService,
) : FHIREngine(metadata, settings, db, blob, azureEventService, reportService, reportStreamEventService) {
    /**
     * Accepts a [FhirTranslateQueueMessage] [message] and, based on its parameters, sends a report to the next pipeline
     * step containing either the first ancestor's blob or a new blob that has been translated per
     * the receiver's settings, pending the passed topic's (found in [message]) isSendOriginal property
     * [actionHistory] and [actionLogger] ensure all activities are recorded to the database and logged
     */
    override fun <T : QueueMessage> doWork(
        message: T,
        actionLogger: ActionLogger,
        actionHistory: ActionHistory,
    ): List<FHIREngineRunResult> {
        return emptyList()
    }

    // TODO Change these to correct values.
    override val finishedField: Field<OffsetDateTime> = Tables.TASK.TRANSLATED_AT
    override val engineType: String = "Translate"
    override val taskAction: TaskAction = TaskAction.receiver_enrichment
}