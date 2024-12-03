package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.AbstractHL7Exception
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.ErrorCode
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.AbstractMessage
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
import org.apache.logging.log4j.kotlin.Logging
import java.util.Date
import ca.uhn.hl7v2.model.v251.message.ORU_R01 as v251_ORU_R01
import ca.uhn.hl7v2.model.v251.segment.MSH as v251_MSH
import ca.uhn.hl7v2.model.v27.message.ORU_R01 as v27_ORU_R01
import ca.uhn.hl7v2.model.v27.segment.MSH as v27_MSH
import fhirengine.translation.hl7.structures.nistelr251.message.ORU_R01 as NIST_ELR_ORU_R01
import fhirengine.translation.hl7.structures.nistelr251.segment.MSH as NIST_MSH

private const val MSH_SEGMENT_NAME = "MSH"

/**
 * Converts raw HL7 data (message or batch) to HL7 message objects.
 */
class HL7Reader(private val actionLogger: ActionLogger) : Logging {

    /**
     * Returns one or more messages read from the raw HL7 data.
     *
     * This function takes a couple of different approaches to transforming the raw string into messages.
     *
     * First, it will read the message type from MSH.9 and attempt to find the list of mapped MessageModels.
     * See [getMessageModelClasses].  These mappings will typically consist of the v27 structure and the v25 structure
     * for that message type.  If models are found, the code will iterate over the models and attempt to parse the
     * message.  If messages are parsed, loop short circuits.
     *
     * The reason we need to use multiple message models is due to inconsistencies of the specs across different
     * organizations.  For example, the NIST profile for v251 includes fields that are only available in the v27
     * standard spec.  To get around this fact, we take advantage that the specs are mostly backwards compatible;
     * a NIST v251 can be parsed using the v271 structure successfully and will now also include the data from the
     * fields only available in the standard v27.  The only caveat to this approach is that the HAPI library itself
     * is not 100% backwards compatible.  A common error is that a v251 message will specify a component is a CE, but
     * the v27 spec says it must be a CWE; though these two data types are compatible from a field standpoint, the HAPI
     * library will throw a type error along the lines of "a CWE field cannot be set to a CE type".  To get around this
     * issue, if the message cannot be parsed to v27 we fall back to parsing it as a v251 message.
     *
     *
     * If no message models are returned by [getMessageModelClasses], the string is parsed using the default behavior
     * of [Hl7InputStreamMessageIterator].
     *
     *
     * @return one or more HL7 messages
     * @throws IllegalArgumentException if the raw data cannot be parsed or no messages were read
     */
    fun getMessages(rawMessage: String): List<Message> {
        val messageModelsToTry = getMessageModelClasses(rawMessage)
        val messages: MutableList<Message> = mutableListOf()
        if (rawMessage.isBlank()) {
            actionLogger.error(InvalidReportMessage("Provided raw data is empty."))
        } else if (messageModelsToTry.isEmpty()) {
            try {
                val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream())
                while (iterator.hasNext()) {
                    messages.add(iterator.next())
                }
            } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
                logHL7ParseFailure(e)
            }
        } else {
            val validationContext = ValidationContextFactory.noValidation()
            val parseError = mutableListOf<Hl7InputStreamMessageStringIterator.ParseFailureError>()
            run modelLoop@{
                messageModelsToTry.forEach { model ->
                    val context = DefaultHapiContext(ReportStreamCanonicalModelClassFactory(model))
                    context.validationContext = validationContext
                    try {
                        val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream(), context)
                        while (iterator.hasNext()) {
                            messages.add(iterator.next())
                        }
                    } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
                        messages.clear()
                        parseError.add(e)
                    }

                    if (messages.isNotEmpty()) {
                        // Don't try other message models if we were able to parse
                        return@modelLoop
                    }
                }
            }

            // if it was able to parse the message through one of the models, then we do not want to log it as an error
            val parseLogLevel = if (parseError.size == messageModelsToTry.size) Level.ERROR else Level.WARN
            parseError.forEach { currentError ->
                logHL7ParseFailure(currentError, messages.isEmpty(), parseLogLevel)
            }
        }

        if (messages.isEmpty() && !actionLogger.hasErrors()) {
            actionLogger.error(InvalidReportMessage("Unable to find HL7 messages in provided data."))
        }

        return messages
    }

    /**
     * Extracts the message type from the MSH segment and returns the list of message models to use to
     * try to parse the messages.
     *
     * This function assumes all the message types will be the same if this is a HL7 batch.
     */
    private fun getMessageModelClasses(rawMessage: String): List<Class<out AbstractMessage>> {
        try {
            val messageProfile = getMessageProfile(rawMessage)
            if (messageProfile != null) {
                when (messageProfile.typeID) {
                    "ORU" -> {
                        return when (messageProfile.profileID) {
                            // TODO: NIST ELR conformance profile to be enabled in a future PR (rename to "NIST_ELR")
                            "NIST_ELR_TEST" -> listOf(
                                NIST_ELR_ORU_R01::class.java
                            )
                            else -> listOf(
                                v27_ORU_R01::class.java,
                                v251_ORU_R01::class.java
                            )
                        }
                    }
                    else -> {
                        logger.warn(
                            "${messageProfile.typeID} did not have any mapped message model classes, " +
                                "using default behavior"
                        )
                        return emptyList()
                    }
                }
            }
        } catch (ex: Hl7InputStreamMessageStringIterator.ParseFailureError) {
            logHL7ParseFailure(ex)
            return emptyList()
        }
        actionLogger.error(InvalidReportMessage("String did not contain any HL7 messages"))
        return emptyList()
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
    private fun logHL7ParseFailure(
        exception: Hl7InputStreamMessageStringIterator.ParseFailureError,
        isError: Boolean = true,
        logLevel: Level = Level.ERROR,
    ) {
        logger.log(logLevel, "Failed to parse message: ${exception.message}")

        // Get the exception root cause and log it accordingly
        when (val rootCause = ExceptionUtils.getRootCause(exception)) {
            is AbstractHL7Exception -> recordError(rootCause, isError)
            else -> throw rootCause
        }
    }

    private fun recordError(exception: AbstractHL7Exception, isError: Boolean) {
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
        if (isError) {
            actionLogger.error(InvalidReportMessage(errorMessage))
        } else {
            actionLogger.warn(InvalidReportMessage(errorMessage))
        }
    }

    companion object {

        // This regex is used to replace \n with \r while not replacing \r\n
        val newLineRegex = Regex("(?<!\r)\n")

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
         * Configuration class that contains details on how to parse an HL7 message and then how
         * to convert it to FHIR
         *
         * @param messageModelClass a class that inherits from [Message]
         * @param hl7toFHIRMappingLocation the location of the mappings files to convert the message to FHIR
         */
        data class HL7MessageParseAndConvertConfiguration(
            val messageModelClass: Class<out Message>,
            val hl7toFHIRMappingLocation: String,
        )

        /**
         * Map of configured message types to their configuration
         */
        val messageToConfigMap = mapOf(
            HL7MessageType(
                "ORU_R01",
                "2.5.1",
                "2.16.840.1.113883.9.10"
            ) to HL7MessageParseAndConvertConfiguration(
                ORU_R01::class.java,
                "./metadata/HL7/catchall"
            ),
            HL7MessageType(
                "ORU_R01",
                "2.5.1",
                "2.16.840.1.113883.9.11"
            ) to HL7MessageParseAndConvertConfiguration(
                ORU_R01::class.java,
                "./metadata/HL7/catchall"
            )
        )

        // TODO: https://github.com/CDCgov/prime-reportstream/issues/14116
        /**
         * Accepts a raw HL7 string and uses the MSH segment to detect the [HL7MessageType] which is then used
         * to parse the string into an instance of [Message]. If the type is not one that is configured in
         * [messageToConfigMap] the default HAPI parsing logic is used
         *
         * @param rawHL7 the HL7 string to convert into a [Message]
         *
         * @return a [Pair<Message, HL7MessageParseAndConvertConfiguration?>] with parsed message and optional type
         */
        fun parseHL7Message(
            rawHL7: String,
            parseConfiguration: HL7MessageParseAndConvertConfiguration?,
        ): Message {
            // A carriage return is the official segment delimiter; a newline is not recognized so we replace
            // them

            val carriageReturnFixedHL7 = rawHL7.replace(newLineRegex, "\r")
            val hl7MessageType = getMessageType(carriageReturnFixedHL7)
            return getHL7ParsingContext(hl7MessageType, parseConfiguration).pipeParser.parse(carriageReturnFixedHL7)
        }

        /**
         * Creates a HAPI context that can be used to parse an HL7 string.  If no configuration is passed, the function
         * will return a context with the HAPI defaults which will defer to that library to determine the kind of message
         *
         * @param hl7MessageParseAndConvertConfiguration optional configuration to use when creating a context
         */
        private fun getHL7ParsingContext(
            hl7MessageType: HL7MessageType?,
            hl7MessageParseAndConvertConfiguration: HL7MessageParseAndConvertConfiguration?,
        ): HapiContext {
            return if (hl7MessageParseAndConvertConfiguration == null) {
                if (hl7MessageType?.msh93 == "ORU_R01") {
                    DefaultHapiContext(
                        ParserConfiguration(),
                        ValidationContextFactory.noValidation(),
                        ReportStreamCanonicalModelClassFactory(ORU_R01::class.java),
                    )
                } else if (hl7MessageType?.msh93 == "OML_O21") {
                    DefaultHapiContext(
                        ParserConfiguration(),
                        ValidationContextFactory.noValidation(),
                        ReportStreamCanonicalModelClassFactory(OML_O21::class.java),
                    )
                } else if (hl7MessageType?.msh93 == "ORM_O01") {
                    DefaultHapiContext(
                        ParserConfiguration(),
                        ValidationContextFactory.noValidation(),
                        ReportStreamCanonicalModelClassFactory(ORM_O01::class.java),
                    )
                } else {
                    DefaultHapiContext(ValidationContextFactory.noValidation())
                }
            } else {
                DefaultHapiContext(
                    ParserConfiguration(),
                    ValidationContextFactory.noValidation(),
                    ReportStreamCanonicalModelClassFactory(hl7MessageParseAndConvertConfiguration.messageModelClass),
                )
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
            val message = getHL7ParsingContext(null, null)
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
    }
}