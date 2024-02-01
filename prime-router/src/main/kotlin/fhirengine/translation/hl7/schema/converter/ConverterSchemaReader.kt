package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.URIScheme
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.getURI
import java.net.URI

/**
 * Read a converter schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun getConvertSchema(schemaName: String, folder: String? = null): ConverterSchema {
    val schemaUri = getURI(URIScheme.CLASSPATH, folder, schemaName)
    val schema =
        ConfigSchemaReader.fromFile(schemaUri, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}

/**
 * Read a converter schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun converterSchemaFromFile(schemaName: String, folder: String? = null): ConverterSchema {
    val schema = ConfigSchemaReader.fromFile(schemaName, folder, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}

fun converterSchemaFromURI(schemaURI: URI, folder: String? = null): ConverterSchema {
    val schema = ConfigSchemaReader.fromFile(schemaURI.toString(), folder, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}