package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.AbstractMessage
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import fhirengine.utils.ReportStreamCanonicalModelClassFactory
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
    internal fun getMessage(hl7Class: String): Message = try {
            val messageClass = Class.forName(hl7Class)
            if (AbstractMessage::class.java.isAssignableFrom(messageClass)) {
                // We verify above that we have a valid subclass of Message as required for parsing
                // but the compiler does not know that, so we have to cast
                @Suppress("UNCHECKED_CAST")
                val context =
                    DefaultHapiContext(ReportStreamCanonicalModelClassFactory(messageClass as Class<out Message>))
                context.validationContext = ValidationContextFactory.noValidation()
                val message = context.newMessage(messageClass)
                message
            } else {
                throw IllegalArgumentException("$hl7Class is not a subclass of ca.uhn.hl7v2.model.Message.")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("$hl7Class is not a class to use for the conversion.")
        }

    /**
     * Checks if a specific HL7 message [hl7Class] is supported.
     * @return true if the HL7 message is supported, false otherwise
     */
    fun supports(hl7Class: String): Boolean = try {
            getMessage(hl7Class)
            true
        } catch (e: java.lang.IllegalArgumentException) {
            false
        }

    /**
     * Get an instance of a message.
     * @return an instance of a supported message
     */
    fun getMessageInstance(hl7Class: String): Message {
        val message = getMessage(hl7Class)

        // Add some default fields.  Note these could still be overridden in the schema
        try {
            val terser = Terser(message)
            terser.getSegment("MSH").let {
                val msh2Length = it.getLength(2)
                terser.set("MSH-1", defaultHl7Delimiter)
                terser.set("MSH-2", defaultHl7EncodingFourChars.take(msh2Length))
                terser.set("MSH-12", message.version)
            }
        } catch (e: HL7Exception) {
            logger.error("Could not set MSH delimiters.", e)
            throw e
        }

        // Sanity check: Check to make sure a mistake was not made when adding types.
        return message
    }

    /**
     * removes the index from an HL7 field if one is present
     *
     * ex: "ORC-12(0)-1" -> "ORC-12-1"
     */
    fun removeIndexFromHL7Field(field: String): String {
        val start = field.indexOf("(")
        val end = field.indexOf(")")

        return if (start != -1 && end != -1) {
            field.replaceRange(start, end + 1, "")
        } else {
            field
        }
    }

    /**
     * Encodes a message while avoiding an error when MSH-2 is five characters long
     *
     * @return the encoded message as a string
     */
    fun Message.encodePreserveEncodingChars(): String {
        // get encoding characters ...
        val msh = this.get("MSH") as Segment
        val encCharString = Terser.get(msh, 2, 0, 1, 1)
        val hasFiveEncodingChars = encCharString == defaultHl7EncodingFiveChars
        if (hasFiveEncodingChars) Terser.set(msh, 2, 0, 1, 1, defaultHl7EncodingFourChars)
        var encodedMsg = encode()
        if (hasFiveEncodingChars) {
            encodedMsg = encodedMsg.replace(defaultHl7EncodingFourChars, defaultHl7EncodingFiveChars)
            // Set MSH-2 back in the in-memory message to preserve original value
            Terser.set(msh, 2, 0, 1, 1, defaultHl7EncodingFiveChars)
        }
        return encodedMsg
    }
}