package gov.cdc.prime.router.fhirengine.translation.hl7.utils.helpers

import gov.cdc.prime.router.azure.BlobAccess
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaReferenceResolverHelperTests {

    private val HL7_PATH = "classpath:/fhirengine/translation/hl7/schema/schema-read-test-01/ORU_R01.yml"
    private val FHIR_PATH = "classpath:/fhir_sender_transforms/sample_schema.yml"

    @Test
    fun `test blobInfo`() {
        val blobInfo = SchemaReferenceResolverHelper.getBlobConnectionInfo()
        assertNotNull(blobInfo)
        assertEquals("metadata", blobInfo.containerName)
        // This is set in build.gradle.kts
        assertEquals("test-AzureWebJobsStorage", blobInfo.connectionString)
    }

    @Test
    fun `test retrieveHl7SchemaReference`() {
        val schemaRef = SchemaReferenceResolverHelper.retrieveHl7SchemaReference(HL7_PATH)
        assertNotNull(schemaRef)
        assertTrue(schemaRef.isValid())
    }

    @Test
    fun `test retrieveHl7SchemaReference with blobInfo`() {
        val schemaRef = SchemaReferenceResolverHelper.retrieveHl7SchemaReference(
            HL7_PATH,
            mockk<BlobAccess.BlobContainerMetadata>()
        )
        assertNotNull(schemaRef)
        assertTrue(schemaRef.isValid())
    }

    @Test
    fun `test retrieveFhirSchemaReference`() {
        val schemaRef = SchemaReferenceResolverHelper.retrieveFhirSchemaReference(FHIR_PATH)
        assertNotNull(schemaRef)
        assertTrue(schemaRef.isValid())
    }

    @Test
    fun `test retrieveFhirSchemaReference with blobInfo`() {
        val schemaRef = SchemaReferenceResolverHelper.retrieveFhirSchemaReference(
            FHIR_PATH,
            mockk<BlobAccess.BlobContainerMetadata>()
        )
        assertNotNull(schemaRef)
        assertTrue(schemaRef.isValid())
    }

    @Test
    fun `test getSchemaServiceProviders`() {
        val providers = SchemaReferenceResolverHelper.getSchemaServiceProviders()
        assertNotNull(providers)
        assertEquals(3, providers.size)
        assertNotNull(providers.get("file"))
        assertNotNull(providers.get("classpath"))
        assertNotNull(providers.get("azure"))
    }

    @Test
    fun `test getSchemaServiceProviders with blobInfo`() {
        val providers =
            SchemaReferenceResolverHelper.getSchemaServiceProviders(
                mockk<BlobAccess.BlobContainerMetadata>()
            )
        assertNotNull(providers)
        assertEquals(3, providers.size)
        assertNotNull(providers.get("file"))
        assertNotNull(providers.get("classpath"))
        assertNotNull(providers.get("azure"))
    }
}