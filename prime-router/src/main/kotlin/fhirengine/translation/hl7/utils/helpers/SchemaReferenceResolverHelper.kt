package gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers

import gov.cdc.prime.fhirconverter.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.fhirconverter.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.fhirconverter.translation.hl7.schema.providers.ClasspathSchemaServiceProvider
import gov.cdc.prime.fhirconverter.translation.hl7.schema.providers.FileSchemaServiceProvider
import gov.cdc.prime.fhirconverter.translation.hl7.schema.providers.SchemaServiceProvider
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.azure.AzureSchemaServiceProvider
import gov.cdc.prime.router.fhirengine.engine.LookupTableValueSet

object RouterSchemaReferenceResolverHelper {

    fun retrieveHl7SchemaReference(schema: String): HL7ConverterSchema =
        retrieveHl7SchemaReference(schema, getBlobConnectionInfo())

    fun retrieveHl7SchemaReference(schema: String, blobInfo: BlobAccess.BlobContainerMetadata): HL7ConverterSchema =
        ConfigSchemaReader.fromFile(
            schema,
            HL7ConverterSchema::class.java,
            getSchemaServiceProviders(blobInfo)
        )

    fun retrieveFhirSchemaReference(schema: String): FhirTransformSchema =
        retrieveFhirSchemaReference(schema, getBlobConnectionInfo())

    fun retrieveFhirSchemaReference(schema: String, blobInfo: BlobAccess.BlobContainerMetadata): FhirTransformSchema {
        val myKlass = LookupTableValueSet::class.java
        ConfigSchemaReader.subtypeClass = myKlass

        return ConfigSchemaReader.fromFile(
            schema,
            schemaClass = FhirTransformSchema::class.java,
            getSchemaServiceProviders(blobInfo)
        )
    }

    fun getBlobConnectionInfo(): BlobAccess.BlobContainerMetadata =
        BlobAccess.BlobContainerMetadata.build(
            "metadata",
            Environment.get().storageEnvVar
        )

    fun getSchemaServiceProviders(): Map<String, SchemaServiceProvider> =
        getSchemaServiceProviders(getBlobConnectionInfo())

    fun getSchemaServiceProviders(blobConnectionInfo: BlobAccess.BlobContainerMetadata):
        Map<String, SchemaServiceProvider> {
        val serviceProviders: MutableMap<String, SchemaServiceProvider> = mutableMapOf()
        serviceProviders["file"] = FileSchemaServiceProvider()
        serviceProviders["classpath"] = ClasspathSchemaServiceProvider()
        serviceProviders["azure"] = AzureSchemaServiceProvider(blobConnectionInfo)
        return serviceProviders
    }
}