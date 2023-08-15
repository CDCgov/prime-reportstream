package gov.cdc.prime.router.fhirengine.translation

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import io.github.linuxforhealth.hl7.message.HL7MessageModel
import io.github.linuxforhealth.hl7.resource.ResourceReader
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Patient

/**
 * Translate an HL7 message to FHIR.
 */
class HL7toFhirTranslator internal constructor(
    private val messageEngine: HL7MessageEngine = FhirTranscoder.getMessageEngine()
) : Logging {
    companion object {
        init {
            // TODO Change to use the local classpath per documentation
            val props = System.getProperties()
            props.setProperty("hl7converter.config.home", "./metadata/fhir_mapping")
        }

        /**
         * A Default set of message templates for HL7 -> FHIR translation
         */
        internal val defaultMessageTemplates: MutableMap<String, HL7MessageModel> =
            ResourceReader.getInstance().messageTemplates

        /**
         * Singleton object
         */
        private val singletonInstance: HL7toFhirTranslator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            HL7toFhirTranslator()
        }

        /**
         * Get the singleton instance.
         * @return the translator instance
         */
        fun getInstance(): HL7toFhirTranslator {
            return singletonInstance
        }
    }

    /**
     * Get the HL7 Message Model used to translate an [hl7Message] between HL7 and FHIR.
     * @return the message model
     */
    internal fun getHL7MessageModel(
        hl7Message: Message
    ): HL7MessageModel {
        val messageTemplateType = getMessageTemplateType(hl7Message)
        return defaultMessageTemplates[messageTemplateType]
            ?: throw UnsupportedOperationException("Message type not yet supported $messageTemplateType")
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
        enhanceBundleMetadata(bundle, hl7Message)
        FHIRBundleHelpers.addProvenanceReference(bundle)
        handleBirthTime(bundle, hl7Message)
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

    /**
     * Enhance the [bundle] metadata with data from an [hl7Message].  This is not part of the library configuration.
     */
    private fun enhanceBundleMetadata(bundle: Bundle, hl7Message: Message) {
        // For bundles of type MESSAGE the timestamp is the time the HL7 was generated.
        bundle.timestamp = HL7Reader.getMessageTimestamp(hl7Message)

        // The HL7 message ID
        val identifierValue = when (val mshSegment = hl7Message["MSH"]) {
            is ca.uhn.hl7v2.model.v27.segment.MSH -> mshSegment.messageControlID.value
            is ca.uhn.hl7v2.model.v251.segment.MSH -> mshSegment.messageControlID.value
            else -> ""
        }
        bundle.identifier.value = identifierValue
        bundle.identifier.system = "https://reportstream.cdc.gov/prime-router"
    }

    /**
     *  As documented in https://docs.google.com/spreadsheets/d/1_MOAJOykRWct_9cBG-EcPcWSpSObQFLboPB579DIoAI/edit#gid=0,
     *  the birthDate value needs an extension with a valueDateTime if PID.7 length is greater than 8. According to the
     *  fhir documentation https://hl7.org/fhir/json.html#primitive, if a value has an id attribute or extension,
     *  it is represented with an underscore before the name. Currently, it seems hl7v2-fhir-converter library does not
     *  support this, so this method is a workaround to add an extension to birthDate. There is also no support for
     *  getting the length of the field, for which this issue was created:
     *  https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues/499
     *  This method looks in the [hl7Message] for the birthdate and add an extension to the [bundle] if it includes
     *  the time
     */

    private fun handleBirthTime(bundle: Bundle, hl7Message: Message) {
        // If it is an ORM message, we want to check if it is a timestamp and add it as an extension if it is.

        val birthTime = HL7Reader.getBirthTime(hl7Message)
        if (birthTime.length > 8) {
            val patient = try {
                bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
            } catch (e: NoSuchElementException) {
                bundle.addEntry().resource = Patient()
                bundle.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
            }

            val extension = Extension(
                "http://hl7.org/fhir/StructureDefinition/patient-birthTime",
                DateTimeType(Hl7RelatedGeneralUtils.dateTimeWithZoneId(birthTime, ""))
            )

            patient.birthDateElement.addExtension(extension)
        }
    }
}