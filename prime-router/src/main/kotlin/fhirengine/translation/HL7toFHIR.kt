package gov.cdc.prime.router.translation

import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.encoding.messageType
import io.github.linuxforhealth.fhir.FHIRContext
import io.github.linuxforhealth.hl7.ConverterOptions
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import io.github.linuxforhealth.hl7.message.HL7MessageModel
import io.github.linuxforhealth.hl7.resource.ResourceReader
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

/**
 * Object containing the behaviors required for translating a HL7 message
 */
object HL7toFHIR : Logging {
    init {
        val props = System.getProperties()
        props.setProperty("hl7converter.config.home", "./metadata/fhir_mapping")
    }

    /**
     * A Default set of message templates for HL7 -> FHIR translation
     */
    val defaultMessageTemplates = ResourceReader.getInstance().getMessageTemplates()

    /**
     * A default engine for translating HL7 -> FHIR
     */
    val defaultEngine = getMessageEngine()

    /**
     * Build a HL7MessageEngine for converting HL7 -> FHIR
     */
    fun getMessageEngine(options: ConverterOptions = ConverterOptions.SIMPLE_OPTIONS): HL7MessageEngine {
        val context = FHIRContext(options.isPrettyPrint(), options.isValidateResource(), options.getProperties())

        return HL7MessageEngine(context, options.getBundleType())
    }

    /**
     * Get a HL7MessageModel used to translate between HL7 and FHIR
     */
    fun getHL7MessageModel(
        messageType: String,
        messageTemplates: Map<String, HL7MessageModel>,
    ): HL7MessageModel {
        return messageTemplates.get(messageType)?.let {
            it
        } ?: throw UnsupportedOperationException("Message type not yet supported $messageType")
    }

    /**
     * Translate a *Message* into a FHIR Bundle
     *
     * Custom translations can be handled by adding a customized message type
     * such as test_ORU_R01 to the list in config.properties. Then creating the accompanying files, such as
     * hl7/message/test_ORU_R01. Any files in fhir_mapping/hl7/resource will override the defaults allowing customization
     * of resources like Address or Organization.
     */
    fun translate(
        hl7Message: Message,
    ): Bundle {
        // NOTE:
        // extracted from
        // https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/d5e43fffa96654e7c5bc896e020ff2fa8aac4ff2/src/main/java/io/github/linuxforhealth/hl7/HL7ToFHIRConverter.java#L135-L159
        // If timezone specification is needed it can be provided via a custom HL7MessageEngine with a custom FHIRContext that has the time zone ID set

        val messageModel = getHL7MessageModel(hl7Message.messageType(), defaultMessageTemplates)
        return translate(hl7Message, messageModel, defaultEngine)
    }

    /**
     * Translate a *Message* into a FHIR Bundle
     *
     * Specify the HL7MessageModel and engine to use for the translation
     */
    fun translate(
        hl7Message: Message,
        messageModel: HL7MessageModel,
        engine: HL7MessageEngine,
    ): Bundle {
        return messageModel.convert(hl7Message, engine)
    }
}