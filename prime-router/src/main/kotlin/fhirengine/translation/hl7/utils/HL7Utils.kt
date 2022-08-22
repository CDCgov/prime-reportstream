package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ORU_R01
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
    private const val defaultHl7EncodingChars = "^~\\&#"

    /**
     * The supported HL7 output messages.
     */
    enum class SupportedMessages(val type: Class<*>) {
        ORU_R01_2_5_1(ORU_R01::class.java);

        /**
         * Get an instance of a message.
         * @return an instance of a supported message
         */
        fun getMessageInstance(): Message {
            val message = type.getDeclaredConstructor().newInstance()
            if (message !is Message)
                throw IllegalArgumentException("Type ${type.name} is not of type ca.uhn.hl7v2.model.Message.")

            // Add some default fields.  Note these could still be overridden in the schema
            try {
                val typeParts = type.simpleName.split("_")
                check(typeParts.size == 2)
                val terser = Terser(message)
                val msh2Length = terser.getSegment("MSH").getLength(2)
                terser.set("MSH-1", defaultHl7Delimiter)
                terser.set("MSH-2", defaultHl7EncodingChars.take(msh2Length))
                terser.set("MSH-9-1", typeParts[0])
                terser.set("MSH-9-2", typeParts[1])
                terser.set("MSH-9-3", type.simpleName)
                terser.set("MSH-12", message.version)
            } catch (e: HL7Exception) {
                logger.error("Could not set MSH delimiters.", e)
                throw e
            }

            // Sanity check: Check to make sure a mistake was not made when adding types.
            return message
        }

        companion object {
            /**
             * Get an instance of a message for the given HL7 message [type] and [version].
             * @return an instance of a supported message
             */
            fun getMessageInstance(type: String, version: String): Message? {
                val messageType = SupportedMessages.values().firstOrNull {
                    it.getMessageInstance().version == version && it.type.simpleName == type
                }
                return messageType?.getMessageInstance()
            }

            /**
             * Checks is a specific HL7 message [type] and [version] is supported.
             * @return true if the HL7 message is supported, false otherwise
             */
            fun supports(type: String, version: String): Boolean {
                return SupportedMessages.values()
                    .any { it.getMessageInstance().version == version && it.type.simpleName == type }
            }

            /**
             * Gets a list of comma separated HL7 message types and versions.  Useful for log messages.
             * @return a comma separated list of supported HL7 types and versions (e.g. ORU_R01(2.5.1))
             */
            fun getSupportedListAsString(): String {
                return SupportedMessages.values().joinToString(", ") {
                    "${it.type.simpleName}(${it.getMessageInstance().version})"
                }
            }
        }
    }
}