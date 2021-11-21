package gov.cdc.prime.router.serializers.soapimpl

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import gov.cdc.prime.router.serializers.XmlObject

/**
 * Our credentials object
 */
@JacksonXmlRootElement(localName = "elr:cred")
data class Credentials(
    @field:JacksonXmlProperty(localName = "elr:Password")
    val password: String,
    @field:JacksonXmlProperty(localName = "elr:TimeStamp")
    val timestamp: String,
    @field:JacksonXmlProperty(localName = "elr:UserName")
    val userName: String
) : XmlObject

/**
 * Our lab file object. Each of these gets put into a list
 */
@JacksonXmlRootElement(localName = "elr:LabFile")
data class LabFile(
    @field:JacksonXmlProperty(localName = "elr:FileName")
    val fileName: String,
    @field:JacksonXmlProperty(localName = "elr:Index")
    val index: Int,
    @field:JacksonXmlProperty(localName = "elr:bytLabFile")
    val fileContents: String,
    @field:JacksonXmlProperty(localName = "elr:bytSignatureToStore")
    val signature: String = "W1Bd",
    @field:JacksonXmlProperty(localName = "elr:purpose")
    val purpose: String = "HL7251",
    @field:JacksonXmlProperty(localName = "elr:strFileExtension")
    val fileExtension: String = ".HL7"
) : XmlObject

/**
 * Our PA payload, called UploadFiles in their WSDL. This just wraps around
 * two other classes that contain the actual information
 */
@JacksonXmlRootElement(localName = "elr:UploadFiles")
data class UploadFiles(
    /** The credentials used to log in and post the data in the array of lab files */
    @field:JacksonXmlProperty(localName = "elr:cred")
    val credentials: Credentials,
    /** An array of lab files to send to the web service */
    // these feel reversed in my head. I feel like
    // xml property should be the property of the
    // labFiles collection and the wrapper should be
    // the value we use for each element in the list
    @field:JacksonXmlProperty(localName = "elr:LabFile")
    @field:JacksonXmlElementWrapper(localName = "elr:arrLabFile")
    val labFiles: Array<LabFile>
) : XmlObject {
    /**
     * checks for equality with other data classes
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadFiles

        if (credentials != other.credentials) return false
        if (!labFiles.contentEquals(other.labFiles)) return false

        return true
    }

    /**
     * creates a unique hash
     */
    override fun hashCode(): Int {
        var result = credentials.hashCode()
        result = 31 * result + labFiles.contentHashCode()
        return result
    }
}