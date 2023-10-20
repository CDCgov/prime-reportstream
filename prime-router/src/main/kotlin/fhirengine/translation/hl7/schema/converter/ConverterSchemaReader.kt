package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader

/**
 * Read a converter schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun converterSchemaFromFile(schemaName: String, folder: String? = null): ConverterSchema {
    val schemaUri = "classpath:/$folder/$schemaName.yml"
    val schema =
        ConfigSchemaReader.fromFile(schemaUri, folder, schemaClass = ConverterSchema::class.java)
    if (schema is ConverterSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a ConverterSchema")
    }
}