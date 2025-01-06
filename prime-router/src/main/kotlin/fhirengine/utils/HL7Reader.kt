package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.AbstractHL7Exception
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.ErrorCode
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.ParserConfiguration
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.validation.ValidationException
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import fhirengine.translation.hl7.structures.fhirinventory.message.OML_O21
import fhirengine.translation.hl7.structures.fhirinventory.message.ORM_O01
import fhirengine.translation.hl7.structures.fhirinventory.message.ORU_R01
import fhirengine.utils.ReportStreamCanonicalModelClassFactory
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.logger
import java.util.Date
import ca.uhn.hl7v2.model.v251.segment.MSH as v251_MSH
import ca.uhn.hl7v2.model.v27.segment.MSH as v27_MSH
import fhirengine.translation.hl7.structures.nistelr251.segment.MSH as NIST_MSH

private const val MSH_SEGMENT_NAME = "MSH"

/**
 * Converts raw HL7 data (message or batch) to HL7 message objects.
 */
class HL7Reader {
    companion object {

        // This regex is used to replace \n with \r while not replacing \r\n
        val newLineRegex = Regex("(?<!\r)\n")
        private val logger = logger()

        /**
         * Class captures the details from the MSH segment and can be used to map
         * to which instance of a Message and which HL7 -> FHIR mappings should be used
         *
         * @param msh93 the message structure, i.e. ORU_R01
         * @param msh12 the message version id, i.e. 2.5.1
         * @param msh213 message profile universal id, i.e. 2.16.840.1.113883.9.10
         */
        data class HL7MessageType(val msh93: String, val msh12: String, val msh213: String)

        /**
         * Accepts a raw HL7 string and uses the MSH segment to detect the [HL7MessageType] which is then used
         * to parse the string into an instance of [Message]. If the type is not one that is supported in
         * [getHL7ParsingContext] the default HAPI parsing logic is used
         *
         * @param rawHL7 the HL7 string to convert into a [Message]
         *
         * @return a [Message] with parsed message and optional type
         */
        fun parseHL7Message(
            rawHL7: String,
        ): Message {
            // A carriage return is the official segment delimiter; a newline is not recognized so we replace
            // them

            val carriageReturnFixedHL7 = rawHL7.replace(newLineRegex, "\r")
            val hl7MessageType = getMessageType(carriageReturnFixedHL7)
            return getHL7ParsingContext(hl7MessageType).pipeParser.parse(carriageReturnFixedHL7)
        }

        /**
         * Creates a HAPI context that can be used to parse an HL7 string.  If no configuration is passed, the function
         * will return a context with the HAPI defaults which will defer to that library to determine the kind of message
         *
         */
        private fun getHL7ParsingContext(
            hl7MessageType: HL7MessageType?,
        ): HapiContext {
            return when (hl7MessageType?.msh93) {
                "ORU_R01" -> {
                    DefaultHapiContext(
                        ParserConfiguration(),
                        ValidationContextFactory.noValidation(),
                        ReportStreamCanonicalModelClassFactory(ORU_R01::class.java),
                    )
                }
                "OML_O21" -> {
                    DefaultHapiContext(
                        ParserConfiguration(),
                        ValidationContextFactory.noValidation(),
                        ReportStreamCanonicalModelClassFactory(OML_O21::class.java),
                    )
                }
                "ORM_O01" -> {
                    DefaultHapiContext(
                        ParserConfiguration(),
                        ValidationContextFactory.noValidation(),
                        ReportStreamCanonicalModelClassFactory(ORM_O01::class.java),
                    )
                }
                else -> {
                    DefaultHapiContext(ValidationContextFactory.noValidation())
                }
            }
        }

        /**
         * Parses just the first line of an HL7 string to determine
         * - the event trigger
         * - the HL7 version
         * - the conformance profile
         *
         * The returned type can be used to see if there is custom configuration set in [messageToConfigMap]
         *
         * @param rawHL7 the raw HL7 to determine the type for
         *
         * @return the details on the HL7 message type
         */
        @Throws(HL7Exception::class)
        internal fun getMessageType(rawHL7: String): HL7MessageType {
            val message = getHL7ParsingContext(null)
                .pipeParser
                // In order to determine the message configuration, only parse the MSH segment since the type of message
                // is required in order to accurately parse the message in its entirety
                // HL7 messages can use \n or \r for new lines, so split on either
                .parse(rawHL7.lines()[0])
            val terser = Terser(message)
            return HL7MessageType(
                terser.get("MSH-9-3") ?: "",
                terser.get("MSH-12") ?: "",
                terser.get("MSH-21-3") ?: ""
            )
        }

        // map of HL7 message profiles: maps profile to configuration directory path
        @Deprecated("This field is only in use for the CLI", level = DeprecationLevel.WARNING)
        val profileDirectoryMap: Map<MessageProfile, String> = mapOf(
            // TODO: https://github.com/CDCgov/prime-reportstream/issues/14124
            // Pair(MessageProfile("ORU", "NIST_ELR"), "./metadata/HL7/v251-elr"),
        )

        // map of HL7 OIDs to supported conformance profiles
        // list of OIDs for NIST ELR retrieved from https://oidref.com/2.16.840.1.113883.9
        private val oidProfileMap: Map<String, String> = mapOf(
            Pair("2.16.840.1.113883.9.10", "NIST_ELR"),
            Pair("2.16.840.1.113883.9.11", "NIST_ELR")
        )

        // data class to uniquely identify a message profile
        data class MessageProfile(val typeID: String, val profileID: String)

        /**
         * Get the [message] timestamp from MSH-7.
         * @return the timestamp or null if not specified
         */
        fun getMessageTimestamp(message: Message): Date? {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
                is NIST_MSH -> structure.msh7_DateTimeOfMessage.ts1_Time.valueAsDate
                is v27_MSH -> structure.msh7_DateTimeOfMessage.valueAsDate
                is v251_MSH -> structure.msh7_DateTimeOfMessage.ts1_Time.valueAsDate
                else -> null
            }
        }

        /**
         * Get the type of the [message]
         * @return the type of message ex. ORU
         */
        fun getMessageType(message: Message): String {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
                is NIST_MSH -> structure.msh9_MessageType.msg1_MessageCode.toString()
                is v27_MSH -> structure.msh9_MessageType.msg1_MessageCode.toString()
                is v251_MSH -> structure.msh9_MessageType.msg1_MessageCode.toString()
                else -> ""
            }
        }

        /**
         * Get the profile of the [rawmessage]
         * If there are multiple HL7 messages the first message's data will be returned
         * @param rawmessage string representative of hl7 messages
         * @return the message profile, or null if there is no message
         */
        @Deprecated("This function is only in use for the CLI", level = DeprecationLevel.WARNING)
        fun getMessageProfile(rawmessage: String): MessageProfile? {
            val iterator = Hl7InputStreamMessageIterator(rawmessage.byteInputStream())
            if (!iterator.hasNext()) return null
            val hl7message = iterator.next()
            val msh9 = Terser(hl7message).get("MSH-9")
            val profileID = oidProfileMap[Terser(hl7message).get("MSH-21-3")] ?: ""
            return MessageProfile(msh9 ?: "", profileID)
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
            } catch (e: NullPointerException) {
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
                "OML" -> "PATIENT"
                "ORU" -> "PATIENT_RESULT/PATIENT"
                else -> null
            }
        }

        /**
         * Reads MSH.3 which is the Sending Application field
         */
        fun getSendingApplication(message: Message): String? {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
                is NIST_MSH -> structure.msh3_SendingApplication.encode()
                is v27_MSH -> structure.msh3_SendingApplication.encode()
                is v251_MSH -> structure.msh3_SendingApplication.encode()
                else -> null
            }
        }

        /**
         * Reads MSH.4 which is the Sending Facility field
         */
        fun getSendingFacility(message: Message): String? {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
                is NIST_MSH -> structure.msh4_SendingFacility.encode()
                is v27_MSH -> structure.msh4_SendingFacility.encode()
                is v251_MSH -> structure.msh4_SendingFacility.encode()
                else -> null
            }
        }

        /**
         * Reads MSH.10 which is the Message Control ID field
         */
        fun getMessageControlId(message: Message): String? {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
                is NIST_MSH -> structure.msh10_MessageControlID.encode()
                is v27_MSH -> structure.msh10_MessageControlID.encode()
                is v251_MSH -> structure.msh10_MessageControlID.encode()
                else -> null
            }
        }

        /**
         * Reads MSH.15 which is the Accept Acknowledgment Type field
         */
        fun getAcceptAcknowledgmentType(message: Message): String? {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
                is NIST_MSH -> structure.msh15_AcceptAcknowledgmentType.encode()
                is v27_MSH -> structure.msh15_AcceptAcknowledgmentType.encode()
                is v251_MSH -> structure.msh15_AcceptAcknowledgmentType.encode()
                else -> null
            }
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
        fun logHL7ParseFailure(
            exception: Hl7InputStreamMessageStringIterator.ParseFailureError,
            actionLogger: ActionLogger,
            logLevel: Level = Level.ERROR,
        ) {
            logger.log(logLevel, "Failed to parse message: ${exception.message}")

            // Get the exception root cause and log it accordingly
            when (val rootCause = ExceptionUtils.getRootCause(exception)) {
                is AbstractHL7Exception -> recordError(rootCause, actionLogger)
                else -> throw rootCause
            }
        }

        fun recordError(exception: AbstractHL7Exception, actionLogger: ActionLogger) {
            val errorMessage: String = when (exception) {
                is ValidationException -> "Validation Failed: ${exception.message}"

                is HL7Exception -> {
                    when (exception.errorCode) {
                        ErrorCode.REQUIRED_FIELD_MISSING.code -> "Required field missing: ${exception.message}"
                        ErrorCode.DATA_TYPE_ERROR.code -> "Data type error: ${exception.message}"
                        else -> "Failed to parse message"
                    }
                }

                else -> "Failed to parse message"
            }
            actionLogger.error(InvalidReportMessage(errorMessage))
        }
    }
}