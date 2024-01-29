package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.getURI
import java.net.URI

/**
 * Read a converter schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun getConvertSchema(schemaName: String, folder: String? = null): ConverterSchema {
    val schemaUri = getURI(folder, schemaName)
    val schema =
        ConfigSchemaReader.fromFile(schemaUri, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}

fun getConvertSchema(schemaUri: URI): ConverterSchema {
    val schema =
        ConfigSchemaReader.fromFile(schemaUri, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}