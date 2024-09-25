package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.annotation.BlobTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import org.apache.logging.log4j.kotlin.Logging

/**
 * Functions in this class are part of the workflow to sync translation schemas between azure blob stores.  There exists
 * one for each kind of schema (FHIR->FHIR, FHIR->HL7) and each function is triggered when a validating.txt blob is
 * added to the respective directory.
 */
class ValidateSchemasFunctions : Logging {

    private val translationSchemaManager = TranslationSchemaManager()

    /**
     * Validates the FHIR -> FHIR schemas when validating.txt is added to the store
     *
     * @param content the trigger for the function being executed
     */
    @FunctionName("validateFHIRToFHIRSchemas")
    @StorageAccount("AzureWebJobsStorage")
    fun validateFHIRToFHIRSchemas(
        @BlobTrigger(
            name = "fhirValidatingFile",
            path = "metadata/fhir_transforms/validating.txt"
        ) @Suppress("UNUSED_PARAMETER") content: Array<Byte>,
    ) {
        val blobConnectionString = Environment.get().storageEnvVar
        val blobContainerMetadata: BlobAccess.BlobContainerMetadata =
            BlobAccess.BlobContainerMetadata.build("metadata", blobConnectionString)
        validateSchemaChanges(TranslationSchemaManager.SchemaType.FHIR, blobContainerMetadata)
    }

    /**
     * Validates the FHIR -> HL7 schemas when validating.txt is added to the store
     *
     * @param content the trigger for the function being executed
     */
    @FunctionName("validateFHIRToHL7Schemas")
    @StorageAccount("AzureWebJobsStorage")
    fun validateFHIRToHL7Schemas(
        @BlobTrigger(
            name = "validatingFile",
            path = "metadata/hl7_mapping/validating.txt"
        ) @Suppress("UNUSED_PARAMETER") content: Array<Byte>,
    ) {
        val blobConnectionString = Environment.get().storageEnvVar
        val blobContainerMetadata: BlobAccess.BlobContainerMetadata =
            BlobAccess.BlobContainerMetadata.build("metadata", blobConnectionString)
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
    internal fun validateSchemaChanges(
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
            logger.info("Successfully validated schema changes.")
        } else {
            translationSchemaManager.handleValidationFailure(schemaType, validationState, blobContainerMetadata)
            logger.error("Schemas did not pass validation and changes were rolled back.")
        }
    }
}