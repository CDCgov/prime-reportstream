package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
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
    const val defaultHl7EncodingFourChars = "^~\\&"
    const val defaultHl7EncodingFiveChars = "^~\\&#"

    /**
     * Gets a new object for the given [hl7Class].
     * @return a message object
     */
    internal fun getMessage(hl7Class: String): Message {
        return try {
            val message = Class.forName(hl7Class).getDeclaredConstructor().newInstance()
            if (message is Message) {
                getMessageTypeString(message) // Just check we can get the type string
                message
            } else throw IllegalArgumentException("$hl7Class is not a subclass of ca.uhn.hl7v2.model.Message.")
        } catch (e: Exception) {
            throw IllegalArgumentException("$hl7Class is not a class to use for the conversion.")
        }
    }

    /**
     * Gets the type string for the given [message].
     * @return a list with the message code and trigger event, or an empty list if the type could not be determined
     */
    internal fun getMessageTypeString(message: Message): List<String> {
        val typeParts = message.javaClass.simpleName.split("_")
        return if (typeParts.size != 2)
            throw IllegalArgumentException("${message.javaClass.simpleName} is not a class to use for the conversion.")
        else typeParts
    }

    /**
     * Checks if a specific HL7 message [hl7Class] is supported.
     * @return true if the HL7 message is supported, false otherwise
     */
    fun supports(hl7Class: String): Boolean {
        return try {
            getMessage(hl7Class)
            true
        } catch (e: java.lang.IllegalArgumentException) {
            false
        }
    }

    /**
     * Get an instance of a message.
     * @return an instance of a supported message
     */
    fun getMessageInstance(hl7Class: String): Message {
        val message = getMessage(hl7Class)

        // Add some default fields.  Note these could still be overridden in the schema
        try {
            val typeParts = getMessageTypeString(message)
            val terser = Terser(message)
            terser.getSegment("MSH").let {
                val msh2Length = it.getLength(2)
                terser.set("MSH-1", defaultHl7Delimiter)
                terser.set("MSH-2", defaultHl7EncodingFourChars.take(msh2Length))
                terser.set("MSH-9-1", typeParts[0])
                terser.set("MSH-9-2", typeParts[1])
                terser.set("MSH-9-3", "${typeParts[0]}_${typeParts[1]}")
                terser.set("MSH-12", message.version)
            }
        } catch (e: HL7Exception) {
            logger.error("Could not set MSH delimiters.", e)
            throw e
        }

        // Sanity check: Check to make sure a mistake was not made when adding types.
        return message
    }

    /**
     * Only call from COVID pipeline!
     *
     * This is not generic enough for the UP
     */
    fun formPathSpec(spec: String, rep: Int? = null): String {
        val segment = spec.substring(0, 3)
        val components = spec.substring(3)
        val segmentSpec = formSegSpec(segment, rep)
        return "$segmentSpec$components"
    }

    /**
     * Only call from COVID pipeline!
     *
     * This is not generic enough for the UP
     */
    fun formSegSpec(segment: String, rep: Int? = null): String {
        val repSpec = rep?.let { "($rep)" } ?: ""
        return when (segment) {
            "OBR" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBR"
            "ORC" -> "/PATIENT_RESULT/ORDER_OBSERVATION/ORC"
            "SPM" -> "/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM"
            "PID" -> "/PATIENT_RESULT/PATIENT/PID"
            "OBX" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION$repSpec/OBX"
            "NTE" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/NTE$repSpec"
            else -> segment
        }
    }

    /**
     * removes the index from an HL7 field if one is present
     *
     * ex: "ORC-12(0)-1" -> "ORC-12-1"
     */
    fun removeIndexFromHL7Field(field: String): String {
        val start = field.indexOf("(")
        val end = field.indexOf(")")

        return if (start != -1 && end != -1) {
            field.replaceRange(start, end + 1, "")
        } else field
    }
}