package gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform

import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader

/**
 * Read a fhirTransform schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun fhirTransformSchemaFromFile(
    schemaName: String,
    blobConnectionInfo: BlobAccess.BlobContainerMetadata,
): FhirTransformSchema = ConfigSchemaReader.fromFile(
        schemaName,
        schemaClass = FhirTransformSchema::class.java,
        blobConnectionInfo
    )