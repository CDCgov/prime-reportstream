package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

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
 * The GAEN UUID Format instructs how the UUID field of the GAEN payload is built
 */
enum class GAENUUIDFormat {
    PHONE_DATE, // Default if IV specified hmac_md5(phone+date, iv).toHex()
    REPORT_ID, // Default if IV is not specified, use report stream's report id
    WA_NOTIFY, // Use the format that WA_NOTIFY uses, must specify IV
}

/**
 * The Google/Apple Exposure Notification transport sends a report to the Exposure Notification service
 */
data class GAENTransportType
@JsonCreator constructor(
    /**
     * [apiUrl] is API URL to post to. Typically, something like https://adminapi.encv.org/api/issue.
     * [uuidFormat] is format that is used for generating the UUID of the message.
     * The UUID enables the GAEN system deduplicate notifications.
     * [uuidIV] is the HMAC initialization vector (aka key) in hex
     */
    val apiUrl: String,
    val uuidFormat: GAENUUIDFormat? = null,
    val uuidIV: String? = null,
) : TransportType("GAEN") {
    override fun toString(): String = "url=$apiUrl, uuidFormat=$uuidFormat, uuidIV=$uuidIV"
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