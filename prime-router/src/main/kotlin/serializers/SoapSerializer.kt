package gov.cdc.prime.router.serializers

import com.ctc.wstx.stax.WstxOutputFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.SoapTransportType
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.credentials.SoapCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.serializers.soapimpl.Credentials
import gov.cdc.prime.router.serializers.soapimpl.LabFile
import gov.cdc.prime.router.serializers.soapimpl.Soap12Message
import gov.cdc.prime.router.serializers.soapimpl.UploadFiles
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.xml.stream.XMLOutputFactory
import kotlin.reflect.full.findAnnotation

/**
 * our base XML interface
 */
interface XmlObject {
    /**
     * Takes the XML object and writes it out as a string using the Jackson Mapper annotations
     * that are on the class. This is not necessary for serialization to XML. This is used primarily
     * for logging, but also by the [SoapEnvelope] class before it is sent to an endpoint
     */
    fun toXml(): String = mapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this)

    companion object {
        private val oFactory: XMLOutputFactory = WstxOutputFactory()
        private val xmlFactory: XmlFactory = XmlFactory.builder()
            .xmlOutputFactory(oFactory)
            .build()
        private val mapper: XmlMapper = XmlMapper(xmlFactory).also {
            it.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        }
    }
}

/**
 * A generic SOAP envelope to wrap our objects in
 */
@JsonSerialize(using = SoapSerializer::class)
@JacksonXmlRootElement(localName = "soapenv:Envelope")
class SoapEnvelope(
    /** The payload gets wrapped in the SOAP envelope */
    val payload: Any,
    /** A map of namespaces that get injected into the XML header */
    val namespaces: Map<String, String>,
    /** The Soap version being implemented*/
    val soapVersion: String?,
) : XmlObject

/**
 * The SOAP serializer that takes our [SoapEnvelope] and turns it into a proper SOAP object that can be sent
 * to our endpoint
 */
class SoapSerializer(private val envelope: Class<SoapEnvelope>?) : StdSerializer<SoapEnvelope>(envelope) {
    // Jackson Mapper requires this even if we don't use it in our code
    constructor() : this(null)

    /**
     * takes the SoapEnvelope, and its payload, and writes the data out as an XML object in two different ways based on whether the receiver is using SOAP ver 1.1 or SOAP ver 1.2
     */
    override fun serialize(value: SoapEnvelope?, gen: JsonGenerator?, provider: SerializerProvider?) {
        if (value?.payload == null) {
            error("Cannot deserialize a null packet")
        }
        val xmlGen = gen as? ToXmlGenerator
        // unfortunately it's very hard to get the payload's root element automatically, given
        // the way that Jackson Mapper works, so I have just cheated here and pulled the
        // root element by force, so I can use it down below
        val payloadName = value.payload::class.findAnnotation() as? JacksonXmlRootElement
        if (xmlGen != null) {
            writeEnvelopeNamespaces(xmlGen, value)
            when (value.soapVersion) {
                // SOAP version 1.2 has a very different structure than SOAP 1.1 and uses WS security elements in the header.
                "SOAP12" -> {
                    writeSoap12Header(xmlGen)
                }

                else -> {
                    xmlGen.writeNullField("$soapNamespaceAlias:Header")
                }
            }
            // write out the body of the envelope
            xmlGen.writeObjectFieldStart("$soapNamespaceAlias:Body")
            // write out the payload itself
            xmlGen.writePOJOField(payloadName?.localName ?: defaultPayloadName, value.payload)
            xmlGen.writeEndObject()
        }
    }

    /**
     * Writes out the SOAP Envelope namespaces in XML as defined in receiver settings
     */
    private fun writeEnvelopeNamespaces(xmlGen: ToXmlGenerator, value: SoapEnvelope) {
        // start SOAP Envelope xml
        xmlGen.writeStartObject()
        xmlGen.setNextIsAttribute(true)
        // write out the SOAP namespace into the header
        xmlGen.writeFieldName("xmlns:$soapNamespaceAlias")
        // and the namespace as well
        if (value.soapVersion == "SOAP12") {
            xmlGen.writeString(soap12Namespace)
        } else {
            xmlGen.writeString(soapNamespace)
        }
        value.namespaces.forEach {
            xmlGen.setNextIsAttribute(true)
            xmlGen.writeFieldName(it.key)
            xmlGen.writeString(it.value)
        }
        xmlGen.setNextIsAttribute(false)
    }

    /**
     * If SOAP Version 1.2 is used by receiver this creates the needed security headers
     */
    private fun writeSoap12Header(xmlGen: ToXmlGenerator) {
        // this timestamp id just needs to be a random unique identifier
        val timestampId = "TS-" + (java.util.UUID.randomUUID())
        val timeCreated = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        val timeExpires = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now().plusSeconds(180))

        xmlGen.writeObjectFieldStart("$soapNamespaceAlias:Header")
        // write out XML element for wsse Security element
        xmlGen.writeObjectFieldStart("wsse:Security")
        // include required attribute in wsse:Security element
        xmlGen.setNextIsAttribute(true)
        xmlGen.writeStringField("soapenv:mustUnderstand", "1")
        xmlGen.setNextIsAttribute(true)
        xmlGen.writeStringField(
            "xmlns:wsse",
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
        )
        // write out timestamp elements and attributes for wsse security header
        xmlGen.writeObjectFieldStart("wsu:Timestamp")
        xmlGen.setNextIsAttribute(true)
        xmlGen.writeFieldName("wsu:Id")
        xmlGen.writeString(timestampId)
        xmlGen.setNextIsAttribute(true)
        xmlGen.writeStringField(
            "xmlns:wsu",
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
        )
        xmlGen.setNextIsAttribute(false)
        xmlGen.writeStringField("wsu:Created", timeCreated)
        xmlGen.setNextIsAttribute(false)
        xmlGen.writeStringField("wsu:Expires", timeExpires)
        // close the objects for the wsu:timestamp, wsse:Security and soapenv:Header elements
        repeat(3) { xmlGen.writeEndObject() }
    }

    companion object {
        /** our default SOAP namespace */
        private const val soapNamespace = "http://schemas.xmlsoap.org/soap/envelope/"

        /** our default alias */
        private const val soapNamespaceAlias = "soapenv"

        /** if we don't get a value for the payload via an annotation we use this */
        private const val defaultPayloadName = "payload"
        private const val soap12Namespace = "http://www.w3.org/2003/05/soap-envelope"
    }
}

/** Based on what the SOAP action is, we create the payload and put it into the SOAP envelope */
object SoapObjectService {
    /**
     * Given a [SoapTransportType], a [WorkflowEngine.Header], [ExecutionContext], and [SoapCredential]
     * we create the requisite object based on the [SoapTransportType.soapAction] we will submit to.
     * This is where we tightly couple a SOAP action to a type that gets returned. This is the most
     * common sense way to map things as an action requires a specific parameter
     */
    fun getXmlObjectForAction(
        soapTransportType: SoapTransportType,
        header: WorkflowEngine.Header,
        context: ExecutionContext,
        credential: SoapCredential,
        externalFileName: String,
    ): XmlObject? {
        context.logger.info("Creating object for ${soapTransportType.soapAction}")
        return when (soapTransportType.soapAction) {
            // I detest magic strings, I need to think on this more
            "http://nedss.state.pa.us/2012/B01/elrwcf/IUploadFile/UploadFiles" -> {
                val userPassCredential = credential as? UserPassCredential
                    ?: error("Unable to cast credential for ${header.receiver?.name} to UserPassCredential")
                // PA object - this is very specific to PA
                // get the timestamp for the credential object
                val timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
                // create the credential object
                val credentials = Credentials(
                    password = userPassCredential.pass,
                    timestamp = timestamp,
                    userName = userPassCredential.user
                )
                // create the lab file object
                val labFile = LabFile(
                    fileName = externalFileName,
                    index = 1,
                    fileContents = Base64.getEncoder().encodeToString(header.content!!)
                )
                // return the composite object for PA
                UploadFiles(credentials, arrayOf(labFile))
            }
            // Else serialize objects for AR, our only other SOAP client
            else -> {
                val encodedfileContents = Base64.getEncoder().encodeToString(header.content!!)
                val textFileContents = String(Base64.getDecoder().decode(encodedfileContents))
                Soap12Message(textFileContents)
            }
        }
    }
}