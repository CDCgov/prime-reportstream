package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.AbstractHL7Exception
import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.ErrorCode
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.AbstractMessage
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import ca.uhn.hl7v2.util.Terser
import ca.uhn.hl7v2.validation.ValidationException
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.kotlin.Logging
import java.util.Date
import ca.uhn.hl7v2.model.v251.message.ORU_R01 as v251_ORU_R01
import ca.uhn.hl7v2.model.v251.segment.MSH as v251_MSH
import ca.uhn.hl7v2.model.v27.message.ORU_R01 as v27_ORU_R01
import ca.uhn.hl7v2.model.v27.segment.MSH as v27_MSH

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
            var parseError: Hl7InputStreamMessageStringIterator.ParseFailureError? = null
            run modelLoop@{
                messageModelsToTry.forEach { model ->
                    val context = DefaultHapiContext(CanonicalModelClassFactory(model))
                    context.validationContext = validationContext
                    try {
                        val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream(), context)
                        while (iterator.hasNext()) {
                            messages.add(iterator.next())
                        }
                    } catch (e: Hl7InputStreamMessageStringIterator.ParseFailureError) {
                        messages.clear()
                        parseError = e
                    }

                    if (messages.isNotEmpty()) {
                        // Don't try other message models if we were able to parse
                        return@modelLoop
                    }
                }
            }

            // This is a known kotlin bug where the compiler does not think parseError can be smart-casted because
            // it is operated on in multiple forEach closures, the solution is to just reassign
            // https://youtrack.jetbrains.com/issue/KT-19446/False-positive-Smart-cast-to-Foo-is-impossible-due-to-same-variable-names-in-different-closures
            val parseErrorToLog = parseError
            if (parseErrorToLog != null) {
                // Only log a parse failure if all the model classes have been tried and no messages have been parsed
                logHL7ParseFailure(parseErrorToLog, messages.isEmpty())
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
        val iterator = Hl7InputStreamMessageIterator(rawMessage.byteInputStream())
        if (iterator.hasNext()) {
            try {
                val firstMessage = iterator.next()
                return when (val messageType = getMessageType(firstMessage)) {
                    "ORU" -> listOf(
                        v27_ORU_R01::class.java,
                        v251_ORU_R01::class.java
                    )

                    else -> {
                        logger.warn(
                            "$messageType did not have any mapped message model classes, using default behavior"
                        )
                        emptyList()
                    }
                }
            } catch (ex: Hl7InputStreamMessageStringIterator.ParseFailureError) {
                logHL7ParseFailure(ex)
                return emptyList()
            }
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
        isError: Boolean = true
    ) {
        logger.error("Failed to parse message", exception)
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
        /**
         * Get the [message] timestamp from MSH-7.
         * @return the timestamp or null if not specified
         */
        fun getMessageTimestamp(message: Message): Date? {
            return when (val structure = message[MSH_SEGMENT_NAME]) {
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
                is v27_MSH -> structure.msh9_MessageType.msg1_MessageCode.toString()
                is v251_MSH -> structure.msh9_MessageType.msg1_MessageCode.toString()
                else -> ""
            }
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