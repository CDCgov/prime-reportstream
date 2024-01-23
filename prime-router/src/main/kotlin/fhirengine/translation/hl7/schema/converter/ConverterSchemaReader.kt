package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import java.net.URI

/**
 * Read a converter schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun converterSchemaFromFile(schemaName: String, folder: String? = null): ConverterSchema {
//    val schema = ConfigSchemaReader.fromFile(schemaName, pefolder, schemaClass = ConverterSchema::class.java)
    val schemaUri = getURI(folder, schemaName)
//    val schema =
//        ConfigSchemaReader.fromFile(schemaUri, folder, schemaClass = ConverterSchema::class.java)
    val schema =
        ConfigSchemaReader.fromFile(schemaUri, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}

fun converterSchemaFromURI(schemaUri: URI): ConverterSchema {
//    val schema = ConfigSchemaReader.fromFile(schemaName, pefolder, schemaClass = ConverterSchema::class.java)
//    val schemaUri = getURI(folder, schemaName)
//    val schema =
//        ConfigSchemaReader.fromFile(schemaUri, folder, schemaClass = ConverterSchema::class.java)
    val schema =
        ConfigSchemaReader.fromFile(schemaUri, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}

/**
 * helper
 */
fun getURI(folder: String?, schemaName: String): String {
    var path = if (folder.isNullOrBlank()) schemaName else "$folder/$schemaName"
    if (!path.startsWith("classpath:/")) {
        path = if (path.startsWith("/")) "classpath:$path" else "classpath:/$path"
    }
    if (!path.endsWith(".yml")) {
        path = "$path.yml"
    }
    return path
}