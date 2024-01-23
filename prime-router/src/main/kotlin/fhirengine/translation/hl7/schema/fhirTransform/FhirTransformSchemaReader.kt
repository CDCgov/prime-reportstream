package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader

/**
 * Read a fhirTransform schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun fhirTransformSchemaFromFile(schemaName: String, folder: String? = null): FhirTransformSchema {
    val schema = ConfigSchemaReader.fromFile(
        getURI(folder, schemaName),
        schemaClass = FhirTransformSchema::class.java
    )

    if (schema is FhirTransformSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a FHIRTransformSchema")
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