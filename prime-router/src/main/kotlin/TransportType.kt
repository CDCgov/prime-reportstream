package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class FtpsProtocol {
    SSL,
    TLS
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(SFTPTransportType::class, name = "SFTP"),
    JsonSubTypes.Type(EmailTransportType::class, name = "EMAIL"),
    JsonSubTypes.Type(BlobStoreTransportType::class, name = "BLOBSTORE"),
    JsonSubTypes.Type(NullTransportType::class, name = "NULL"),
    JsonSubTypes.Type(AS2TransportType::class, name = "AS2"),
    JsonSubTypes.Type(FTPSTransportType::class, name = "FTPS"),
    JsonSubTypes.Type(SoapTransportType::class, name = "SOAP"),
    JsonSubTypes.Type(GAENTransportType::class, name = "GAEN")
)
abstract class TransportType(val type: String)

data class SFTPTransportType
@JsonCreator constructor(
    val host: String,
    val port: String,
    val filePath: String,
    val credentialName: String? = null
) :
    TransportType("SFTP")

data class EmailTransportType
@JsonCreator constructor(
    val addresses: List<String>,
    val from: String = "qtv1@cdc.gov" // TODO: default to a better choice
) :
    TransportType("EMAIL")

data class BlobStoreTransportType
@JsonCreator constructor(
    val storageName: String, // this looks for an env var with this name. env var value is the connection string.
    val containerName: String // eg, hhsprotect
) :
    TransportType("BLOBSTORE")

data class AS2TransportType
@JsonCreator constructor(
    val receiverUrl: String,
    val receiverId: String,
    val senderId: String,
    val senderEmail: String = "reportstream@cdc.gov", // Default,
    val mimeType: String = "application/hl7-v2",
    val contentDescription: String = "SARS-CoV-2 Electronic Lab Results"
) :
    TransportType("AS2")

/**
 * FTPSTransportType
 */
data class FTPSTransportType
@JsonCreator constructor(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val protocol: FtpsProtocol = FtpsProtocol.SSL,
    val binaryTransfer: Boolean = true,
    /**
     * @param acceptAllCerts  pass true to ignore all cert checks, helpful for testing
     */
    val acceptAllCerts: Boolean = false
) : TransportType("FTPS") {
    /**
     * toString()
     *
     * Print out the parameters of the FTPSTransportType but obfuscate the password
     *
     * @return String
     */
    override fun toString(): String =
        "host=$host, port=$port, username=$username, protocol=$protocol, binaryTransfer=$binaryTransfer"
}

data class GAENTransportType
@JsonCreator constructor(
    /**
     * [apiUrl] is API URL to post to. Typically, something like https://adminapi.encv.org/api/issue.
     */
    val apiUrl: String,
) : TransportType("GAEN") {
    override fun toString(): String = "url=$apiUrl"
}

data class NullTransportType
@JsonCreator constructor(
    val dummy: String? = null,
) : TransportType("NULL")

/**
 * Holds the [gov.cdc.prime.router.transport.SoapTransport] parameters
 */
data class SoapTransportType
@JsonCreator constructor(
    /** The URL endpoint to connect to */
    val endpoint: String,
    /** The SOAP action to invoke */
    val soapAction: String,
    /** The credential name */
    val credentialName: String? = null,
    /** The namespaces used in the creation of the object */
    val namespaces: Map<String, String>? = null
) : TransportType("SOAP") {
    override fun toString(): String = "endpoint=$endpoint, soapAction=$soapAction"
}