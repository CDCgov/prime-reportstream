package gov.cdc.prime.router.fhirengine.utils

import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.ProcessEvent
import gov.cdc.prime.router.azure.ReportEvent
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage

object HL7MessageHelpers {
    /**
     * Takes [nextAction] and an [hl7Message], convert it into a Report for the [receiver] specified with
     * any passed in [metadata]. Uploads to blob storage. Adds lineage showing the newly generated report
     * came from [sourceReportId]. Tracks the generated report with the [actionHistory] provided.
     * @return the newly generated Report, nextAction event, and blobInfo
     */
    fun takeHL7GetReport(
        nextAction: Event.EventAction,
        hl7Message: ByteArray,
        sourceReportId: ReportId,
        receiver: Receiver,
        metadata: Metadata,
        actionHistory: ActionHistory
    ): Triple<Report, Event, BlobAccess.BlobInfo> {
        // create report object
        val sources = emptyList<Source>()
        val report = Report(
            Report.Format.HL7,
            sources,
            1,
            metadata = metadata,
            destination = receiver
        )

        // create item lineage
        report.itemLineages = listOf(
            ItemLineage(
                null,
                sourceReportId,
                1,
                report.id,
                1,
                null,
                null,
                null,
                report.getItemHashForRow(1)
            )
        )

        // create batch event
        // if timing is null, a batch event will be created but it will never be picked up
        val time = receiver.timing?.nextTime()
        // this is hacky and needs to be fixed, but that would have to happen as part of a refactor
        val event: Event =
            if (nextAction == Event.EventAction.SEND) {
                ReportEvent(
                    nextAction,
                    report.id,
                    false
                )
            } else {
                ProcessEvent(
                    nextAction,
                    report.id,
                    Options.None,
                    emptyMap(),
                    emptyList(),
                    at = time
                )
            }

        // upload the translated copy to blobstore
        val blobInfo = BlobAccess.uploadBody(
            Report.Format.HL7,
            hl7Message,
            report.name,
            receiver.fullName,
            event.eventAction
        )

        // track generated reports, one per receiver
        actionHistory.trackCreatedReport(event, report, blobInfo)

        return Triple(report, event, blobInfo)
    }
}