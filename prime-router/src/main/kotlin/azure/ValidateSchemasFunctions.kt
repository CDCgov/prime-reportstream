package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.annotation.BlobTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import org.apache.logging.log4j.kotlin.Logging

class ValidateSchemasFunctions : Logging {

    private val translationSchemaManager = TranslationSchemaManager()

    @FunctionName("validateFHIRToFHIRSchemas")
    @StorageAccount("AzureWebJobsStorage")
    fun validateFHIRToFHIRSchemas(
        @BlobTrigger(
            name = "validating.txt",
            path = "metadata/fhir_transforms/{name}"
        ) @Suppress("UNUSED_PARAMETER") validatingFile: Array<Byte>,
        blobContainerMetadata: BlobAccess.BlobContainerMetadata = BlobAccess.defaultBlobMetadata,
    ) {
        validateSchemaChanges(TranslationSchemaManager.SchemaType.FHIR, blobContainerMetadata)
    }

    @FunctionName("validateFHIRToHL7Schemas")
    @StorageAccount("AzureWebJobsStorage")
    fun validateFHIRToHL7Schemas(
        @BlobTrigger(
            name = "validating.txt",
            path = "metadata/hl7_mappings/{name}"
        ) @Suppress("UNUSED_PARAMETER") validatingFile: Array<Byte>,
        blobContainerMetadata: BlobAccess.BlobContainerMetadata = BlobAccess.defaultBlobMetadata,
    ) {
        validateSchemaChanges(TranslationSchemaManager.SchemaType.HL7, blobContainerMetadata)
    }

    private fun validateSchemaChanges(
        schemaType: TranslationSchemaManager.SchemaType,
        blobContainerMetadata: BlobAccess.BlobContainerMetadata,
    ) {
        val results = translationSchemaManager.validateManagedSchemas(
            schemaType,
            blobContainerMetadata
        )
        val validationState = translationSchemaManager.retrieveValidationState(schemaType, blobContainerMetadata)
        if (results.all { it.passes }) {
            translationSchemaManager.handleValidationSuccess(schemaType, validationState, blobContainerMetadata)
        } else {
            translationSchemaManager.handleValidationFailure(validationState, blobContainerMetadata)
        }
    }
}