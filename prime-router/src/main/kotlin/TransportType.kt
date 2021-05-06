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
    JsonSubTypes.Type(RedoxTransportType::class, name = "REDOX"),
    JsonSubTypes.Type(BlobStoreTransportType::class, name = "BLOBSTORE"),
    JsonSubTypes.Type(NullTransportType::class, name = "NULL"),
)
abstract class TransportType(val type: String)

data class SFTPTransportType
@JsonCreator constructor(
    val host: String,
    val port: String,
    val filePath: String
) :
    TransportType("SFTP")

data class EmailTransportType
@JsonCreator constructor(
    val addresses: List<String>,
    val from: String = "qtv1@cdc.gov" // TODO: default to a better choice
) :
    TransportType("EMAIL")

data class RedoxTransportType
@JsonCreator constructor(
    val apiKey: String,
    val baseUrl: String?,
) :
    TransportType("REDOX")

data class BlobStoreTransportType
@JsonCreator constructor(
    val storageName: String,   // this looks for an env var with this name. env var value is the connection string.
    val containerName: String  // eg, hhsprotect
) :
    TransportType("BLOBSTORE")



data class NullTransportType
@JsonCreator constructor(
    val dummy: String? = null,
) : TransportType("NULL")