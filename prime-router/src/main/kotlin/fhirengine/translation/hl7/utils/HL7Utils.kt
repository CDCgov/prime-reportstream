package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import org.apache.logging.log4j.kotlin.Logging

/**
 * Utilities to handle HL7 messages
 */
object HL7Utils : Logging {
    /**
     * The default HL7 field delimiter.
     */
    private const val defaultHl7Delimiter = "|"

    /**
     * The default HL7 encoding characters.  Note that depending on the message version this will be 4 or 5 characters.
     */
    const val defaultHl7EncodingFourChars = "^~\\&"
    const val defaultHl7EncodingFiveChars = "^~\\&#"

    /**
     * Gets a new object for the given [hl7Class].
     * @return a message object
     */
    internal fun getMessage(hl7Class: String): Message {
        return try {
            val message = Class.forName(hl7Class).getDeclaredConstructor().newInstance()
            if (message is Message) {
                getMessageTypeString(message) // Just check we can get the type string
                message
            } else throw IllegalArgumentException("$hl7Class is not a subclass of ca.uhn.hl7v2.model.Message.")
        } catch (e: Exception) {
            throw IllegalArgumentException("$hl7Class is not a class to use for the conversion.")
        }
    }

    /**
     * Gets the type string for the given [message].
     * @return a list with the message code and trigger event, or an empty list if the type could not be determined
     */
    internal fun getMessageTypeString(message: Message): List<String> {
        val typeParts = message.javaClass.simpleName.split("_")
        return if (typeParts.size != 2)
            throw IllegalArgumentException("${message.javaClass.simpleName} is not a class to use for the conversion.")
        else typeParts
    }

    /**
     * Checks if a specific HL7 message [hl7Class] is supported.
     * @return true if the HL7 message is supported, false otherwise
     */
    fun supports(hl7Class: String): Boolean {
        return try {
            getMessage(hl7Class)
            true
        } catch (e: java.lang.IllegalArgumentException) {
            false
        }
    }

    /**
     * Get an instance of a message.
     * @return an instance of a supported message
     */
    fun getMessageInstance(hl7Class: String): Message {
        val message = getMessage(hl7Class)

        // Add some default fields.  Note these could still be overridden in the schema
        try {
            val typeParts = getMessageTypeString(message)
            val terser = Terser(message)
            terser.getSegment("MSH").let {
                val msh2Length = it.getLength(2)
                terser.set("MSH-1", defaultHl7Delimiter)
                terser.set("MSH-2", defaultHl7EncodingFourChars.take(msh2Length))
                terser.set("MSH-9-1", typeParts[0])
                terser.set("MSH-9-2", typeParts[1])
                terser.set("MSH-9-3", "${typeParts[0]}_${typeParts[1]}")
                terser.set("MSH-12", message.version)
            }
        } catch (e: HL7Exception) {
            logger.error("Could not set MSH delimiters.", e)
            throw e
        }

        // Sanity check: Check to make sure a mistake was not made when adding types.
        return message
    }
}