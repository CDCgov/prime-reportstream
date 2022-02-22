package gov.cdc.prime.router.encoding

import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.segment.MSH
import io.github.linuxforhealth.fhir.FHIRContext
import io.github.linuxforhealth.hl7.ConverterOptions
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import io.github.linuxforhealth.hl7.resource.ResourceReader
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

fun Bundle.encode(): String {
    return FHIR.encode(this)
}

object FHIR : Logging {
    val resourceReader = ResourceReader.getInstance()

    val messagetemplates = resourceReader.getMessageTemplates()

    val engine = getMessageEngine()

    fun getMessageEngine(options: ConverterOptions = ConverterOptions.SIMPLE_OPTIONS): HL7MessageEngine {
        val context = FHIRContext(options.isPrettyPrint(), options.isValidateResource(), options.getProperties())

        return HL7MessageEngine(context, options.getBundleType())
    }

    fun getMessageType(hl7message: Message): String {
        val header = hl7message.get("MSH")
        check(header is MSH)
        return header.getMessageType().getMsg1_MessageCode().getValue() +
            "_" +
            header.getMessageType().getMsg2_TriggerEvent().getValue()
    }

    // Custom translations can be handled by adding a customized message type
    // such as test_ORU_R01 to the list in config.properties. Then creating the accompanying files, such as
    // hl7/message/test_ORU_R01. Any files in fhir_mapping/hl7/resource will override the defaults allowing customization
    // of resources like Address or Organization.
    fun translate(
        hl7message: Message,
        messageType: String = getMessageType(hl7message),
        engine: HL7MessageEngine = FHIR.engine
    ): Bundle {
        // NOTE:
        // extracted from
        // https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/d5e43fffa96654e7c5bc896e020ff2fa8aac4ff2/src/main/java/io/github/linuxforhealth/hl7/HL7ToFHIRConverter.java#L135-L159
        // If timezone specification is needed it can be provided via a custom HL7MessageEngine with a custom FHIRContext that has the time zone ID set

        val hl7MessageTemplateModel = messagetemplates.get(messageType)
        if (hl7MessageTemplateModel != null) {
            return hl7MessageTemplateModel.convert(hl7message, engine)
        } else {
            throw UnsupportedOperationException("Message type not yet supported $messageType")
        }
    }

    fun encode(bundle: Bundle): String {
        return getMessageEngine().getFHIRContext().encodeResourceToString(bundle)
    }

    fun decode(json: String): Bundle {
        val parser = engine.getFHIRContext().parser
        return parser.parseResource(Bundle::class.java, json)
    }
}