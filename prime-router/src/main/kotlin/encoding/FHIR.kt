package gov.cdc.prime.router.encoding

import ca.uhn.hl7v2.model.Message
import io.github.linuxforhealth.fhir.FHIRContext
import io.github.linuxforhealth.hl7.ConverterOptions
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor
import io.github.linuxforhealth.hl7.resource.ResourceReader
import org.hl7.fhir.r4.model.Bundle

class FHIR {
    companion object {

        val messagetemplates = ResourceReader.getInstance().getMessageTemplates()

        fun getMessageEngine(options: ConverterOptions = ConverterOptions.SIMPLE_OPTIONS): HL7MessageEngine {
            val context = FHIRContext(options.isPrettyPrint(), options.isValidateResource(), options.getProperties())

            return HL7MessageEngine(context, options.getBundleType())
        }

        fun translate(hl7message: Message, engine: HL7MessageEngine = getMessageEngine()): Bundle {
            // extracted from
            // https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/d5e43fffa96654e7c5bc896e020ff2fa8aac4ff2/src/main/java/io/github/linuxforhealth/hl7/HL7ToFHIRConverter.java#L135-L159

            // If zoneIdText has been provide via run properties, it overrides the default and any value from the config file.
            /*
            if (options.getZoneIdText()!=null) {
                ConverterConfiguration.getInstance().setZoneId(options.getZoneIdText());
            }
            */
            val messageType = HL7DataExtractor.getMessageType(hl7message)
            val hl7MessageTemplateModel = messagetemplates.get(messageType)
            if (hl7MessageTemplateModel != null) {
                return hl7MessageTemplateModel.convert(hl7message, engine)
            } else {
                throw UnsupportedOperationException("Message type not yet supported " + messageType)
            }
        }

        fun encode(bundle: Bundle): String {
            return getMessageEngine().getFHIRContext().encodeResourceToString(bundle)
        }
    }
}