package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.v251.datatype.DTM
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import org.apache.logging.log4j.kotlin.Logging
import java.util.Date

object HL7MessageHelpers : Logging {

    private const val REPORT_STREAM_UNIVERSAL_ID = "2.16.840.1.114222.4.1.237821"
    private const val REPORT_STREAM_UNIVERSAL_ID_TYPE = "ISO"
    private const val REPORT_STREAM_APPLICATION_NAME = "CDC PRIME - Atlanta"

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
        val time = DTM(null)
        time.setValue(Date())
        // The extraction of these values mimics how the COVID HL7 serializer works
        var sendingApp =
            "${firstMessage?.get("MSH-3-1") ?: REPORT_STREAM_APPLICATION_NAME}^" +
                "${firstMessage?.get("MSH-3-2") ?: REPORT_STREAM_UNIVERSAL_ID}^" +
                "${firstMessage?.get("MSH-3-3") ?: REPORT_STREAM_UNIVERSAL_ID_TYPE}"
        val receivingApp = receiver.translation.receivingApplicationName ?: firstMessage?.get("MSH-5-1") ?: ""
        val receivingFacility = receiver.translation.receivingFacilityName ?: firstMessage?.get("MSH-6-1") ?: ""
        val messageCreateDate = firstMessage?.get("MSH-7") ?: time.value

        val builder = StringBuilder()
        builder.append(
            "FHS|$hl7BatchHeaderEncodingChar|" +
                "$sendingApp|" +
                "$sendingApp|" +
                "$receivingApp|" +
                "$receivingFacility|" +
                messageCreateDate
        )
        builder.append(hl7SegmentDelimiter)
        builder.append(
            "BHS|$hl7BatchHeaderEncodingChar|" +
                "$sendingApp|" +
                "$sendingApp|" +
                "$receivingApp|" +
                "$receivingFacility|" +
                messageCreateDate
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