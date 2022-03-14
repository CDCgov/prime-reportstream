package gov.cdc.prime.router.encoding

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import org.apache.logging.log4j.kotlin.Logging

/**
 * Object containing the behaviors required for decoding a HL7 message
 */
object HL7 : Logging {
    val defaultContext = DefaultHapiContext()

    init {
        // This matches the required version in the linux4health library
        val mcf = CanonicalModelClassFactory("2.6")
        defaultContext.setModelClassFactory(mcf)
        defaultContext.getParserConfiguration().setValidating(false)
    }

    /**
     * Decode the provided *message* into a list of one or more HL7 *Message*s
     *
     * Errors on a null string, allowing things like a request body that may be nullable to be passed in.
     *
     * @param message The encoded HL7 String for decoding
     */
    fun decode(message: String?, context: HapiContext = defaultContext): List<Message> {
        requireNotNull(message)
        try {
            val messages: MutableList<Message> = mutableListOf()
            val iterator = Hl7InputStreamMessageIterator(message.byteInputStream(), context)
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