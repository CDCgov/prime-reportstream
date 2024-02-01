package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.URIScheme
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.getURI

/**
 * Read a fhirTransform schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun getTransformSchema(schemaName: String, folder: String? = null): FhirTransformSchema {
    val schema = ConfigSchemaReader.fromFile(
        getURI(URIScheme.CLASSPATH, folder, schemaName),
        schemaClass = FhirTransformSchema::class.java
    )

    if (schema is FhirTransformSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a FHIRTransformSchema")
    }
}

/**
 * Read a fhirTransform schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun fhirTransformSchemaFromFile(schemaName: String, folder: String? = null): FhirTransformSchema {
    val schema = ConfigSchemaReader.fromFile(schemaName, folder, schemaClass = FhirTransformSchema::class.java)
    if (schema is FhirTransformSchema) {
        return schema
    } else {
        throw SchemaException("Schema ${schema.name} is not a FHIRTransformSchema")
    }
}