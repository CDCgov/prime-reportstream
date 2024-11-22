package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ACK
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import java.time.Clock
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID

/**
 * Helper class to generate HL7 ACK response
 */
class HL7ACKUtils(
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * Parses out raw HL7 message and then checks if MSH.15 == "AL".
     *
     * @return HL7 ACK message body or null
     */
    fun generateOutgoingACKMessageIfRequired(rawHL7: String): String? {
        val maybeMessage = HL7Reader(ActionLogger())
            .getMessages(rawHL7)
            .firstOrNull()

        return if (maybeMessage != null && HL7Reader.requiresAckMessageResponse(maybeMessage)) {
            generateOutgoingACKMessage(maybeMessage)
        } else {
            null
        }
    }

    private fun generateOutgoingACKMessage(incomingACKMessage: Message): String {
        val outgoingAck = ACK()

        val ackMsh = outgoingAck.msh
        ackMsh.msh1_FieldSeparator.value = "|"
        ackMsh.msh2_EncodingCharacters.value = "^~\\&"
        ackMsh.msh3_SendingApplication.parse("ReportStream")
        ackMsh.msh4_SendingFacility.parse("CDC")
        ackMsh.msh5_ReceivingApplication.parse(HL7Reader.getSendingApplication(incomingACKMessage))
        ackMsh.msh6_ReceivingFacility.parse(HL7Reader.getSendingFacility(incomingACKMessage))
        ackMsh.msh7_DateTimeOfMessage.time.setValue(getTimestamp())
        ackMsh.msh9_MessageType.parse("ACK")
        ackMsh.msh10_MessageControlID.parse(UUID.randomUUID().toString())
        ackMsh.msh11_ProcessingID.parse(if (Environment.isProd()) "P" else "T")
        ackMsh.msh12_VersionID.versionID.parse("2.5.1")
        ackMsh.msh15_AcceptAcknowledgmentType.parse("NE")
        ackMsh.msh16_ApplicationAcknowledgmentType.parse("NE")

        val ackMsa = outgoingAck.msa
        ackMsa.msa1_AcknowledgmentCode.parse("CA")
        ackMsa.msa2_MessageControlID.parse(HL7Reader.getMessageControlId(incomingACKMessage))

        return outgoingAck.toString()
    }

    /**
     * HL7 library requires old Java date libraries, so we do the conversion here.
     *
     * We must directly specify the UTC timezone or else the HL7 library will use
     * your machines local timezone.
     */
    private fun getTimestamp(): Calendar {
        val instant = clock.instant()
        val date = Date.from(instant)

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        return calendar
    }
}