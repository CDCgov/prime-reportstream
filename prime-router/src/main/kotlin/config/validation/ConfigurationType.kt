package gov.cdc.prime.router.config.validation

import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.io.File

enum class ConfigurationType {
    ORGANIZATIONS {
        override val jsonSchema: JsonSchema by lazy {
            getSchema("./metadata/json_schema/organizations/organizations.json")
        }
    },
    FHIR_TRANSFORMS {
        override val jsonSchema: JsonSchema by lazy<JsonSchema> { TODO("#14168") }
    },
    FHIR_MAPPINGS {
        override val jsonSchema: JsonSchema by lazy<JsonSchema> { TODO("#14169") }
    }, ;

    abstract val jsonSchema: JsonSchema

    protected fun getSchema(path: String): JsonSchema {
        val rawSchema = File(path).inputStream()
        return JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(rawSchema)
    }
}