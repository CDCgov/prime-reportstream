package gov.cdc.prime.router.fhirengine.azure

import gov.cdc.prime.fhirconverter.translation.hl7.SchemaException
import gov.cdc.prime.fhirconverter.translation.hl7.schema.providers.SchemaServiceProvider
import gov.cdc.prime.router.azure.BlobAccess
import java.io.InputStream
import java.net.URI

class AzureSchemaServiceProvider(val blobConnectionInfo: BlobAccess.BlobContainerMetadata) : SchemaServiceProvider {
    override fun getProviderType(): String = "azure"

    override fun getInputStream(schemaUri: URI): InputStream {
        try {
            // Note: the schema URIs will not include the container name i.e.
            // azure:/hl7_mapping/receivers/STLTs/CA/CA.yml
            val blob = BlobAccess.downloadBlobAsByteArray(
                "${blobConnectionInfo.getBlobEndpoint()}${schemaUri.path}",
                blobConnectionInfo
            )
            return blob.inputStream()
        } catch (e: Exception) {
            throw SchemaException("Cannot read $schemaUri")
        }
    }
}