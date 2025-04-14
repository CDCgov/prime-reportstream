package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidHL7Message
import fhirengine.translation.hl7.structures.nistelr251.segment.MSH as NIST_MSH

/**
 * Class representing a specific HL7 message type.
 *
 * This class is used to validate that a given HL7 message matches the expected message type.
 *
 * @property expectedType The HL7 message type this instance represents.
 */
enum class MessageType {

    ORU_R01,
    ORM_O01,
    OML_O21,
    ;

    companion object {

        /**
         * Creates a MessageType instance from a string representation.
         *
         * @param type The string representation of the HL7 message type.
         * @return The corresponding MessageType instance, or null if the type is unsupported.
         */
        private fun fromString(type: String): MessageType? = when (type) {
                "ORU_R01" -> ORU_R01
                "ORM_O01" -> ORM_O01
                "OML_O21" -> OML_O21
                else -> null
            }

        /**
         * Validates the type of the given message.
         *
         * This method retrieves the message type from the message and checks if it matches any of the supported types.
         * If the type is unsupported, an error is logged.
         *
         * @param message The HL7 message to be validated.
         * @param actionLogs Logger to record the result of the validation.
         * @param itemIndex The index of the item being processed, used for logging.
         */
        fun validateMessageType(message: Message, actionLogs: ActionLogger, itemIndex: Int) {
            // Determine the message type string from the MSH segment
            val messageTypeStr = when (val msh = message.get("MSH")) {
                is NIST_MSH -> msh.messageType.messageStructure.toString()
                is ca.uhn.hl7v2.model.v251.segment.MSH -> msh.messageType.messageStructure.toString()
                is ca.uhn.hl7v2.model.v27.segment.MSH -> msh.messageType.messageStructure.toString()
                else -> ""
            }

            // Convert the string representation to a MessageType instance and validate it
            val messageType = fromString(messageTypeStr)
            messageType ?: actionLogs.getItemLogger(itemIndex)
            .error(InvalidHL7Message("Ignoring unsupported HL7 message type $messageType"))
        }
    }
}