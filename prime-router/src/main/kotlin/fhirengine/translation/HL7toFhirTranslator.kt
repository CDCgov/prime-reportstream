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

        // don't load the templtaes every time
        // get individual isntances of the transaltor
        // translro has its own templates

        private val hl7ToFhirTranslatorInstances =
            Collections.synchronizedMap(mutableMapOf<String, HL7toFhirTranslator>())

        fun getHL7ToFhirTranslatorInstance(configFolderPath: String = "./metadata/HL7/catchall"): HL7toFhirTranslator {
            return hl7ToFhirTranslatorInstances.getOrPut(configFolderPath) {
                HL7toFhirTranslator(configFolderPath)
            }
        }

        /**
         * TODO tech debt ticket to clean everything below
         */

        /**
         * List of all available mapping directory locations
         */
        internal val configPaths: List<String> = listOf(
            "./metadata/HL7/catchall",
            "./metadata/HL7/v251-elr"
        )

        /**
         * Stored message templates for HL7 -> FHIR translation
         */
        private var messageTemplates: Map<String, Map<String, HL7MessageModel>>

        /**
         * Function to retrieve stored message templates
         */
        internal fun getMessageTemplates(): Map<String, Map<String, HL7MessageModel>> {
            return messageTemplates
        }

        /**
         * Calls the HL7 to FHIR Translator resource reader to deserialize all message templates for all available
         * configuration paths and stores the templates in the companion object
         */
        init {
            messageTemplates = loadTemplates(configPaths)
        }

        /**
         * Load templates from the list of [configPaths] sequentially and return a map of messageTemplates
         */
        internal fun loadTemplates(configPaths: List<String>): MutableMap<String, MutableMap<String, HL7MessageModel>> {
            val loadedTemplates: MutableMap<String, MutableMap<String, HL7MessageModel>> = mutableMapOf()

            // templates must be loaded sequentially due to ResourceReader being a singleton instance
            for (configPath in configPaths) {
                if (loadedTemplates[configPath] == null) {
                    loadedTemplates[configPath] = ResourceReader(ConverterConfiguration(configPath)).messageTemplates
                }
            }

            return loadedTemplates
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
        val messageModel = getHL7MessageModel(hl7Message)
        val bundle = messageModel.convert(hl7Message, messageEngine)
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