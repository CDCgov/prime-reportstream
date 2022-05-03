package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidHL7Message
import org.apache.logging.log4j.kotlin.Logging

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
        require(rawMessage.isNotBlank())
        val messages: MutableList<Message> = mutableListOf()
        try {
            val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream())
            while (iterator.hasNext()) {
                messages.add(iterator.next())
            }
            // NOTE for batch hl7; should we be doing anything with the BHS and other headers
        } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
            actionLogger.error(InvalidHL7Message("Unable to parse HL7 data."))
            throw IllegalArgumentException("Cannot parse the message.", e)
        }

        if (messages.isEmpty()) {
            actionLogger.error(InvalidHL7Message("Unable to find HL7 messages in provided data."))
            throw IllegalArgumentException("Empty Hl7 data")
        } else return messages
    }
}