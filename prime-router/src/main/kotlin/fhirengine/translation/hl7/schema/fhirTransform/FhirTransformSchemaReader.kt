package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader

/**
 * Read a fhirTransform schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun fhirTransformSchemaFromFile(schemaName: String, folder: String? = null): FhirTransformSchema {
    val schemaUri = "classpath:/$folder/$schemaName.yml"
    val schema = ConfigSchemaReader.fromFile(
        schemaUri,
        folder,
        schemaClass = FhirTransformSchema::class.java
    )
    if (schema is FhirTransformSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a FHIRTransformSchema")
    }
}