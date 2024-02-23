package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.annotation.BlobTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager

class ValidateSchemasFunctions {

    private val translationSchemaManager = TranslationSchemaManager()

    /**
     * Validates the FHIR -> FHIR schemas when validating.txt is added to the store
     *
     * @param validatingFile the trigger for the function being executed
     * @param blobContainerMetadata the connection info for the blob store
     */
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

    /**
     * Validates the FHIR -> HL7 schemas when validating.txt is added to the store
     *
     * @param validatingFile the trigger for the function being executed
     * @param blobContainerMetadata the connection info for the blob store
     */
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

    /**
     * Function that validates the schemas for the passed schema.  If the validation passes, [TranslationSchemaManager.handleValidationSuccess]
     * success is invoked to update the blob store state.  If the validation fails, [TranslationSchemaManager.handleValidationFailure] is invoked
     * and the schema changes are rolled back.
     *
     * The most likely cause of a validation error is that there is code present in a lower environment or locally that
     * has yet to be deployed
     *
     *
     * @param schemaType which schema type to validate
     * @param blobContainerMetadata the connection info where schemas will be pulled from
     */
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
            translationSchemaManager.handleValidationFailure(schemaType, validationState, blobContainerMetadata)
        }
    }
}