package gov.cdc.prime.router.serializers.soapimpl

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import gov.cdc.prime.router.serializers.XmlObject

/**
 * This represents a file that has been sent in to AR.
 */

@JacksonXmlRootElement(localName = "stag:SubmitMessage")
data class Soap12Message(
    @field:JacksonXmlProperty(localName = "stag:payload")
    val textFileContents: String
) : XmlObject