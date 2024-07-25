package gov.cdc.prime.router.fhirengine.engine

import ca.uhn.hl7v2.model.Message
import fhirengine.translation.hl7.structures.nistelr251.segment.MSH
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidHL7Message

class MessageType private constructor(private val expectedType: String) {

    fun checkValidMessageType(message: Message, actionLogs: ActionLogger, itemIndex: Int) {
        val messageType = when (val msh = message.get("MSH")) {
            is MSH -> msh.messageType.messageStructure.toString()
            is ca.uhn.hl7v2.model.v251.segment.MSH -> msh.messageType.messageStructure.toString()
            is ca.uhn.hl7v2.model.v27.segment.MSH -> msh.messageType.messageStructure.toString()
            else -> ""
        }

        if (messageType != expectedType) {
            actionLogs.getItemLogger(itemIndex)
                .error(InvalidHL7Message("Ignoring unsupported HL7 message type $messageType"))
        }
    }

    companion object {
        val ORU_R01 = MessageType("ORU_R01")
        val ORM_O01 = MessageType("ORM_O01")
        val OML_O21 = MessageType("OML_O21")

        private fun fromString(type: String): MessageType? {
            return when (type) {
                "ORU_R01" -> ORU_R01
                "ORM_O01" -> ORM_O01
                "OML_O21" -> OML_O21
                else -> null
            }
        }

        fun validateMessageType(message: Message, actionLogs: ActionLogger, itemIndex: Int) {
            val messageTypeStr = when (val msh = message.get("MSH")) {
                is MSH -> msh.messageType.messageStructure.toString()
                is ca.uhn.hl7v2.model.v251.segment.MSH -> msh.messageType.messageStructure.toString()
                is ca.uhn.hl7v2.model.v27.segment.MSH -> msh.messageType.messageStructure.toString()
                else -> ""
            }

            val messageType = fromString(messageTypeStr)
            messageType?.checkValidMessageType(message, actionLogs, itemIndex)
                ?: actionLogs
                    .getItemLogger(itemIndex)
                    .error(InvalidHL7Message("Ignoring unsupported HL7 message type $messageTypeStr"))
        }
    }
}