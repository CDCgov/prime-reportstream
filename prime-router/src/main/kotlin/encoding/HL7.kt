package gov.cdc.prime.router.encoding

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser
import org.apache.logging.log4j.kotlin.Logging

object HL7 : Logging {
    val hparser = HL7HapiParser()

    fun deserialize(message: String): List<Message> {
        val messages: MutableList<Message> = mutableListOf()
        try {
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