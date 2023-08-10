package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.ErrorCode
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.segment.MSH
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.validation.ValidationException
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.Logging
import java.util.Date

/**
 * Converts raw HL7 data (message or batch) to HL7 message objects.
 */
class HL7Reader(private val actionLogger: ActionLogger) : Logging {
    /**
     * Returns one or more messages read from the raw HL7 data.
     * @return one or more HL7 messages
     * @throws IllegalArgumentException if the raw data cannot be parsed or no messages were read
     */
    fun getMessages(rawMessage: String): List<Message> {
        val messages: MutableList<Message> = mutableListOf()
        if (rawMessage.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
        } else {
            try {
                val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream())
                while (iterator.hasNext()) {
                    messages.add(iterator.next())
                }
                // NOTE for batch hl7; should we be doing anything with the BHS and other headers
            } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
                logHL7ParseFailure(e)
            }

            if (messages.isEmpty() && !actionLogger.hasErrors()) {
                actionLogger.error(InvalidReportMessage("Unable to find HL7 messages in provided data."))
            }
        }
        return messages
    }

    /**
     * Takes a [rawMessage] and the number of messages [numMessages] in the rawMessage and determines if it is a batch
     * or singular HL7 message. It will qualify as a batch message if it follows the HL7 standards and have the Hl7
     * batch headers which start with "FHS" or if they left off the batch headers and just sent multiple messages
     */
    fun isBatch(rawMessage: String, numMessages: Int): Boolean {
        return rawMessage.startsWith("FHS") || numMessages > 1
    }

    /**
     * Takes an [exception] thrown by the HL7 HAPI library, gets the root cause and logs the error into [actionLogger].
     * Sample error messages returned by the HAPI library are:
     *  Error Code = DATA_TYPE_ERROR-102: 'test' in record 3 is invalid for version 2.5.1
     *  Error Code = REQUIRED_FIELD_MISSING-101: Can't find version ID - MSH.12 is null
     * This functions only logs messages that contain meaningful data.
     *
     */
    private fun logHL7ParseFailure(exception: Hl7InputStreamMessageStringIterator.ParseFailureError) {
        logger.error("Failed to parse message", exception)
        // Get the exception root cause and log it accordingly
        val errorMessage: String = when (val rootCause = ExceptionUtils.getRootCause(exception)) {
            is ValidationException -> "Validation Failed: ${rootCause.message}"

            is HL7Exception -> {
                when (rootCause.errorCode) {
                    ErrorCode.REQUIRED_FIELD_MISSING.code -> "Required field missing: ${rootCause.message}"
                    ErrorCode.DATA_TYPE_ERROR.code -> "Data type error: ${rootCause.message}"
                    else -> "Failed to parse message"
                }
            }

            else -> "Failed to parse message"
        }
        actionLogger.error(InvalidReportMessage(errorMessage))
    }

    companion object {
        /**
         * Get the [message] timestamp from MSH-7.
         * @return the timestamp or null if not specified
         */
        fun getMessageTimestamp(message: Message): Date? {
            val timestamp = (message["MSH"] as MSH).msh7_DateTimeOfMessage
            return if (!timestamp.isEmpty && !timestamp.ts1_Time.isEmpty) {
                timestamp.ts1_Time.valueAsDate
            } else null
        }

        /**
         * Get the type of the [message]
         * @return the type of message ex. ORU
         */
        fun getMessageType(message: Message): String {
            return (message["MSH"] as MSH).msh9_MessageType.msg1_MessageCode.toString()
        }

        /**
         * Get the birthTime from the [message]
         * @return the birthTime, if available or blank if not
         */
        fun getBirthTime(message: Message): String {
            return try {
                Terser(message).get("${getPatientPath(message)}/PID-7")
            } catch (e: HL7Exception) {
                ""
            }
        }

        /**
         * Get the path that is needed to retrieve the patient info, based on the type of the [hl7Message]
         * @return the path for retrieving patient info
         */
        fun getPatientPath(hl7Message: Message): String? {
            return when (getMessageType(hl7Message)) {
                "ORM" -> "PATIENT"
                "ORU" -> "PATIENT_RESULT/PATIENT"
                else -> null
            }
        }
    }
}