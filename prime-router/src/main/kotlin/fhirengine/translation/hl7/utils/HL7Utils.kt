package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ORU_R01

/**
 * Utilities to handle HL7 messages
 */
class HL7Utils {
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
            val instance = type.getDeclaredConstructor().newInstance()
            return if (instance is Message) instance
            else throw IllegalArgumentException("Type ${type.name} is not of type message.")
        }

        companion object {
            /**
             * Get an instance of a message for the given HL7 message [type] and [version].
             * @return an instance of a supported message
             */
            fun getMessageInstance(type: String, version: String): Message? {
                val list = SupportedMessages.values().filter {
                    it.getMessageInstance().version == version && it.type.simpleName == type
                }
                return if (list.isEmpty() || list.size > 1) null
                else list[0].getMessageInstance()
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