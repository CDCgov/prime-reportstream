package gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter

import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader

/**
 * Read a converter schema [schemaName] from a file given the root [folder].
 * @return the validated schema
 * @throws Exception if the schema is invalid or is of the wrong type
 */
fun converterSchemaFromFile(
    schemaName: String,
    blobConnectionInfo: BlobAccess.BlobContainerMetadata,
): HL7ConverterSchema = ConfigSchemaReader.fromFile(
        schemaName,
        schemaClass = HL7ConverterSchema::class.java,
        blobConnectionInfo
    )