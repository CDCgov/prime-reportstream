package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.NamedType

/**
 * A Source can either be a client, a test, or a local file or another report.
 * It is useful for debugging and auditing
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = [
    // also setup
    JsonSubTypes.Type(value = FileSource::class, name = "FileSource"),
    JsonSubTypes.Type(value = ReportSource::class, name = "ReportSource"),
    JsonSubTypes.Type(value = ClientSource::class, name = "ClientSource"),
    JsonSubTypes.Type(value = TestSource::class, name = "TestSource")
])
sealed class Source {
    companion object {
        fun registerSubTypes(mapper: ObjectMapper) {
            mapper.registerSubtypes(NamedType(FileSource::class.java, "FileSource"))
            mapper.registerSubtypes(NamedType(ReportSource::class.java, "ReportSource"))
            mapper.registerSubtypes(NamedType(ClientSource::class.java, "ClientSource"))
            mapper.registerSubtypes(NamedType(TestSource::class.java, "TestSource"))
        }
    }
}

data class FileSource(val fileName: String) : Source()

data class ReportSource(val id: ReportId, val action: String) : Source()

data class ClientSource(val organization: String, val client: String) : Source()

object TestSource : Source()

