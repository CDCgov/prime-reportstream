package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.segment.MSH
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import org.apache.logging.log4j.kotlin.Logging
import java.util.Date

/**
 * Converts raw HL7 data (message or batch) to HL7 message objects.
 */
class HL7Reader(private val actionLogger: ActionLogger) : Logging {
    /**
     * Returns one or more messages read from the raw HL7 data.
     * @return one or more HL7 messages
     * @throws IllegalArgumentException if the raw data cannot be parsed or no messages were read
     */
    fun getMessages(rawMessage: String): List<Message> {
        if (rawMessage.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
            throw IllegalArgumentException("Empty Hl7 data")
        }

        val messages: MutableList<Message> = mutableListOf()
        try {
            val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream())
            while (iterator.hasNext()) {
                messages.add(iterator.next())
            }
            // NOTE for batch hl7; should we be doing anything with the BHS and other headers
        } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
            actionLogger.error(InvalidReportMessage("Unable to parse HL7 data."))
            throw IllegalArgumentException("Cannot parse the message.", e)
        }

        if (messages.isEmpty()) {
            actionLogger.error(InvalidReportMessage("Unable to find HL7 messages in provided data."))
            throw IllegalArgumentException("Empty Hl7 data")
        } else return messages
    }

    companion object {
        /**
         * Get the [message] timestamp from MSH-7.
         * @return the timestamp or null if not specified
         */
        fun getMessageTimestamp(message: Message): Date? {
            val timestamp = (message["MSH"] as MSH).msh7_DateTimeOfMessage
            return if (!timestamp.isEmpty && !timestamp.ts1_Time.isEmpty) {
                timestamp.ts1_Time.valueAsDate
            } else null
        }
    }
}