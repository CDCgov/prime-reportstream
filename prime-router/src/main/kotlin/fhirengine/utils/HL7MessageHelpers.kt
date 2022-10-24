package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.v251.datatype.DTM
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Hl7Configuration
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
import org.apache.logging.log4j.kotlin.Logging
import java.util.Date

object HL7MessageHelpers : Logging {
    /**
     * Takes [nextAction] and an [hl7Message], convert it into a Report for the [receiver] specified with
     * any passed in [metadata]. Uploads to blob storage. Adds lineage showing the newly generated report
     * came from [sourceReportIds]. Tracks the generated report with the [actionHistory] provided.
     * @return the newly generated Report, nextAction event, and blobInfo
     */
    fun takeHL7GetReport(
        nextAction: Event.EventAction,
        hl7Message: ByteArray,
        sourceReportIds: List<ReportId>,
        receiver: Receiver,
        metadata: Metadata,
        actionHistory: ActionHistory
    ): Triple<Report, Event, BlobAccess.BlobInfo> {
        check(hl7Message.isNotEmpty())
        check(sourceReportIds.isNotEmpty())

        // create report object
        val sources = emptyList<Source>()
        val reportFormat = if (sourceReportIds.size > 1) Report.Format.HL7_BATCH else Report.Format.HL7
        val report = Report(
            reportFormat,
            sources,
            sourceReportIds.size,
            metadata = metadata,
            destination = receiver
        )

        // create item lineage
        report.itemLineages = sourceReportIds.mapIndexed { sourceIndex, sourceReportId ->
            ItemLineage(
                null,
                sourceReportId,
                1,
                report.id,
                sourceIndex + 1, // item indexes starts at 1
                null,
                null,
                null,
                "0" // Hash is only used for deduplication when receiving
            )
        }

        // create batch event
        // if timing is null, a batch event will be created, but it will never be picked up
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
            reportFormat,
            hl7Message,
            report.name,
            receiver.fullName,
            event.eventAction
        )
        report.bodyURL = blobInfo.blobUrl
        report.nextAction = event.eventAction.toTaskAction()

        // track generated reports, one per receiver
        actionHistory.trackCreatedReport(event, report, blobInfo)

        return Triple(report, event, blobInfo)
    }

    /**
     * Encoding characters for the HL7 batch headers.
     */
    const val hl7BatchHeaderEncodingChar = "^~\\&"

    /**
     * HL7 segment delimiter.  This is the line break between segments.
     */
    const val hl7SegmentDelimiter = "\r"

    /**
     * Generate a HL7 Batch file from the list of [hl7RawMsgs] for the given [receiver].  The [hl7RawMsgs] are expected
     * to be real HL7 messages at this point, so we will not validate their contents here for performance reasons.
     * @return a string with the HL7 batch file
     */
    fun batchMessages(hl7RawMsgs: List<String>, receiver: Receiver): String {
        check(receiver.translation is Hl7Configuration)
        // Grab the first message to extract some data if not set in the settings
        val firstMessage = if (hl7RawMsgs.isNotEmpty()) {
            val messages = HL7Reader(ActionLogger()).getMessages(hl7RawMsgs[0])
            if (messages.isEmpty()) {
                logger.warn("Unable to extract batch header values from HL7: ${hl7RawMsgs[0].take(80)} ...")
                null
            } else Terser(messages[0])
        } else null

        // The extraction of these values mimics how the COVID HL7 serializer works
        var sendingApp = firstMessage?.get("MSH-3-1")
        if (sendingApp.isNullOrBlank()) sendingApp = "CDC PRIME - Atlanta"
        val receivingApp = receiver.translation.receivingApplicationName ?: firstMessage?.get("MSH-5-1") ?: ""
        val receivingFacility = receiver.translation.receivingFacilityName ?: firstMessage?.get("MSH-6-1") ?: ""
        val time = DTM(null)
        time.setValue(Date())

        val builder = StringBuilder()
        builder.append(
            "FHS|$hl7BatchHeaderEncodingChar|" +
                "$sendingApp|" +
                "|" +
                "$receivingApp|" +
                "$receivingFacility|" +
                time.value
        )
        builder.append(hl7SegmentDelimiter)
        builder.append(
            "BHS|$hl7BatchHeaderEncodingChar|" +
                "$sendingApp|" +
                "|" +
                "$receivingApp|" +
                "$receivingFacility|" +
                time.value
        )
        builder.append(hl7SegmentDelimiter)

        hl7RawMsgs.forEach {
            builder.append(it)
            if (!it.endsWith(hl7SegmentDelimiter)) builder.append(hl7SegmentDelimiter)
        }

        builder.append("BTS|${hl7RawMsgs.size}")
        builder.append(hl7SegmentDelimiter)
        builder.append("FTS|1")
        builder.append(hl7SegmentDelimiter)

        return builder.toString()
    }
}