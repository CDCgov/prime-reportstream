package gov.cdc.prime.router.fhirengine.translation

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.addProvenanceReference
import gov.cdc.prime.router.fhirengine.utils.enhanceBundleMetadata
import gov.cdc.prime.router.fhirengine.utils.handleBirthTime
import io.github.linuxforhealth.core.config.ConverterConfiguration
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import io.github.linuxforhealth.hl7.message.HL7MessageModel
import io.github.linuxforhealth.hl7.resource.ResourceReader
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
import java.util.Collections

/**
 * Creates a HL7toFhirTranslator object to perform HL7v2 to FHIR translations.
 * @param configFolderPath path to a config.properties file. A default is used if none is provided.
 * @param messageEngine HL7MessageEngine to be used. A default is used if none is provided.
 */
class HL7toFhirTranslator(
    private val configFolderPath: String = "./metadata/HL7/catchall",
    private val messageEngine: HL7MessageEngine = FhirTranscoder.getMessageEngine(),
) : Logging {

    private val templates: Map<String, HL7MessageModel> =
        ResourceReader(ConverterConfiguration(configFolderPath)).messageTemplates

    companion object {

        private val hl7ToFhirTranslatorInstances =
            Collections.synchronizedMap(mutableMapOf<String, HL7toFhirTranslator>())

        fun getHL7ToFhirTranslatorInstance(configFolderPath: String = "./metadata/HL7/catchall"): HL7toFhirTranslator =
            hl7ToFhirTranslatorInstances.getOrPut(configFolderPath) {
                HL7toFhirTranslator(configFolderPath)
            }
    }

    /**
     * Get the HL7 Message Model used to translate an [hl7Message] between HL7 and FHIR.
     * @return the message model
     */
    internal fun getHL7MessageModel(
        hl7Message: Message,
    ): HL7MessageModel {
        val messageTemplateType = getMessageTemplateType(hl7Message)

        return templates[messageTemplateType]
            ?: throw UnsupportedOperationException("Message type not yet supported: $messageTemplateType")
    }

    /**
     * Translate an [hl7Message] into a FHIR Bundle.
     * @return a FHIR bundle
     *
     * Custom translations can be handled by adding a customized message type
     * such as test_ORU_R01 to the list in config.properties. Then creating the accompanying files, such as
     * hl7/message/test_ORU_R01. Any files in metadata/HL7/catchall/hl7/ will override the defaults allowing customization
     * of resources like Address or Organization.
     */
    fun translate(
        hl7Message: Message,
    ): Bundle {
        // NOTE:
        // extracted from
        // https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/d5e43fffa96654e7c5bc896e020ff2fa8aac4ff2/src/main/java/io/github/linuxforhealth/hl7/HL7ToFHIRConverter.java#L135-L159
        // If timezone specification is needed it can be provided via a custom HL7MessageEngine with a custom FHIRContext that has the time zone ID set
        val messageModel = getHL7MessageModel(hl7Message)
        val bundle = messageModel.convert(hl7Message, messageEngine)
            ?: throw RuntimeException("Exception occurred while converting HL7 to FHIR ")
        // TODO https://github.com/CDCgov/prime-reportstream/issues/14117
        bundle.enhanceBundleMetadata(hl7Message)
        bundle.addProvenanceReference()
        bundle.handleBirthTime(hl7Message)
        return bundle
    }

    /**
     * Obtain the message type for a given HL7 [message].
     * @return the message type
     */
    internal fun getMessageTemplateType(message: Message): String {
        val header = message.get("MSH") as Segment
        return Terser.get(header, 9, 0, 3, 1)
    }
}