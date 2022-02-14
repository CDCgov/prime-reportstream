package gov.cdc.prime.router.encoding

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser
import org.apache.logging.log4j.kotlin.Logging

class HL7 {
    companion object : Logging {
        val hparser = HL7HapiParser()

        fun deserialize(message: String): Message {
            try {
                // val iterator = Hl7InputStreamMessageStringIterator(message);
                // only supports single message conversion.
                // TODO change to handle bulk?
                // if (iterator.hasNext()) {

                val parser = hparser.getParser()
                logger.info("Encoding: ${parser.getEncoding(message)}")
                logger.info("Version: ${parser.getVersion(message)}")

                return parser.parse(message)
                // }
            } catch (e: HL7Exception) {
                throw IllegalArgumentException("Cannot parse the message.", e)
            }
        }
    }
}