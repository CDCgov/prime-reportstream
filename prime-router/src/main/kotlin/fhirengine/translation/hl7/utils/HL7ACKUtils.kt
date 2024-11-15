package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ACK
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import java.time.Clock
import java.util.Date
import java.util.UUID

/**
 * Helper class to generate HL7 ACK response
 */
class HL7ACKUtils(
    private val clock: Clock = Clock.systemUTC(),
) {

    fun generateOutgoingACKMessage(incomingACKMessage: Message): String {
        val outgoingAck = ACK()

        val ackMsh = outgoingAck.msh
        ackMsh.msh1_FieldSeparator.value = "|"
        ackMsh.msh2_EncodingCharacters.value = "^~\\&"
        ackMsh.msh3_SendingApplication.hd1_NamespaceID.value = "ReportStream"
        ackMsh.msh4_SendingFacility.hd1_NamespaceID.value = "CDC"
        ackMsh.msh5_ReceivingApplication.hd1_NamespaceID.value = HL7Reader.getSendingApplication(incomingACKMessage)
        ackMsh.msh6_ReceivingFacility.hd1_NamespaceID.value = HL7Reader.getSendingFacility(incomingACKMessage)
        ackMsh.msh7_DateTimeOfMessage.time.setValueToSecond(Date.from(clock.instant()))
        ackMsh.msh9_MessageType.msg1_MessageCode.value = "ACK"
        ackMsh.msh10_MessageControlID.value = UUID.randomUUID().toString()
        ackMsh.msh11_ProcessingID.pt1_ProcessingID.value = if (Environment.isProd()) "P" else "T"
        ackMsh.msh12_VersionID.versionID.value = "2.5.1"
        ackMsh.msh15_AcceptAcknowledgmentType.value = "NE"
        ackMsh.msh16_ApplicationAcknowledgmentType.value = "NE"

        val ackMsa = outgoingAck.msa
        ackMsa.msa1_AcknowledgmentCode.value = "CA"
        ackMsa.msa2_MessageControlID.value = HL7Reader.getMessageControlId(incomingACKMessage)

        return outgoingAck.toString()
    }
}