package gov.cdc.prime.router.encoding

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.segment.MSH
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import org.apache.logging.log4j.kotlin.Logging

/**
 * Obtain the message type from an HL7 Message
 */
fun Message.messageType(): String {
    val header = this.get("MSH")
    check(header is MSH)
    return header.getMessageType().getMsg1_MessageCode().getValue() +
        "_" +
        header.getMessageType().getMsg2_TriggerEvent().getValue()
}

/**
 * Object containing the behaviors required for decoding a HL7 message
 */
object HL7 : Logging {
    /**
     * Decode the provided *message* into a list of one or more HL7 *Message*s
     *
     * Errors on a null string, allowing things like a request body that may be nullable to be passed in.
     *
     * @param message The encoded HL7 String for decoding
     */
    fun decode(message: String?): List<Message> {
        requireNotNull(message)
        try {
            val messages: MutableList<Message> = mutableListOf()
            val iterator = Hl7InputStreamMessageIterator(message.byteInputStream())
            while (iterator.hasNext()) {
                messages.add(iterator.next())
            }
            // NOTE for batch hl7; should we be doing anything with the BHS and other headers
            return messages
        } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
            throw IllegalArgumentException("Cannot parse the message.", e)
        }
    }
}