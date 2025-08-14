package gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers

import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema

object SchemaReferenceResolverHelper {

    fun retrieveHl7SchemaReference(schema: String): HL7ConverterSchema =
        retrieveHl7SchemaReference(schema, getBlobConnectionInfo())

    fun retrieveHl7SchemaReference(schema: String, blobInfo: BlobAccess.BlobContainerMetadata): HL7ConverterSchema =
        ConfigSchemaReader.fromFile(
            schema,
            HL7ConverterSchema::class.java,
            blobInfo
        )

    fun retrieveFhirSchemaReference(schema: String): FhirTransformSchema =
        retrieveFhirSchemaReference(schema, getBlobConnectionInfo())

    fun retrieveFhirSchemaReference(schema: String, blobInfo: BlobAccess.BlobContainerMetadata): FhirTransformSchema =
        ConfigSchemaReader.fromFile(
            schema,
            schemaClass = FhirTransformSchema::class.java,
            blobInfo
        )

    fun getBlobConnectionInfo(): BlobAccess.BlobContainerMetadata =
        BlobAccess.BlobContainerMetadata.build(
            "metadata",
            Environment.get().storageEnvVar
        )
}