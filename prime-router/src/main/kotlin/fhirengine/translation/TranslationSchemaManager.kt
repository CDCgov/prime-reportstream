package gov.cdc.prime.router.fhirengine.translation

import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import com.azure.storage.blob.models.BlobItem
import fhirengine.engine.CustomFhirPathFunctions
import fhirengine.engine.CustomTranslationFunctions
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.config.HL7TranslationConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.apache.logging.log4j.kotlin.Logging
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Instance of this manager can be used to validate and update translation schemas in various environments
 */
class TranslationSchemaManager : Logging {

    /**
     * This enum contains the kinds of schemas that currently can be managed
     */
    enum class SchemaType(val directory: String, val outputExtension: String) {
        FHIR("fhir_transforms", "fhir"),
        HL7("hl7_mapping", "hl7"),
    }

    companion object {

        private val timestampRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d+Z.txt\$"
        private val validatingBlobName = "validating"
        private val validBlobName = "valid"
        private val previousValidBlobName = "previous-valid"
        private val previousPreviousValidBlobName = "previous-previous-valid"
        private val validBlobNameRegex = Regex("/$validBlobName-$timestampRegex")
        private val previousValidBlobNameRegex =
            Regex("/$previousValidBlobName-$timestampRegex")
        private val previousPreviousValidBlobNameRegex =
            Regex("/$previousPreviousValidBlobName-$timestampRegex")

        /**
         * Container class that holds the current state for a schema type in a particular azure store.
         * @param valid - a blob with the name "valid-{TIMESTAMP}.txt"
         * @param previousValid a blob with the name "previous-valid-{TIMESTAMP}.txt"
         * @param previousPreviousValid an optional blob with the name "previous-previous-valid-{TIMESTAMP}.txt", will only exist when the schemas are in the process of being validated
         * @param validating an optional blob with the name "validating.txt", will only exist when the schemas are in the process of being validated
         * @param schemaBlobs the list of all schemas, inputs and outputs
         */
        data class ValidationState(
            val valid: BlobItem?,
            val previousValid: BlobItem,
            val previousPreviousValid: BlobItem?,
            val validating: BlobItem?,
            val schemaBlobs: List<BlobAccess.Companion.BlobItemAndPreviousVersions>,
        ) {
            /**
             * Helper function that checks if the passed state has been synced more recently
             */
            fun isOutOfDate(otherValidationState: ValidationState): Boolean {
                if (this.valid == null || otherValidationState.valid == null) {
                    throw TranslationSyncException("Cannot compare validation states while a validation is in progress")
                }
                return this.valid.name < otherValidationState.valid.name
            }
        }

        data class ValidationResult(
            val path: String,
            val passes: Boolean,
            val didError: Boolean = false,
        )

        data class ValidationContainer(val input: String, val output: String, val schemaUri: String)

        class TranslationValidationException(override val message: String, override val cause: Throwable? = null) :
            RuntimeException(cause)

        class TranslationSyncException(override val message: String, override val cause: Throwable? = null) :
            RuntimeException(cause)

        class TranslationStateUninitalized() : RuntimeException() {
            override val message = "The azure account and container have not been initialized"
        }

        /**
         * Internal helper function that fetches the raw input, output file and the URI for the schema to be validated
         *
         * @property blobs the list of blobs to search through to find the schema
         * @property blobContainerInfo the information on the blob container to download from
         * @property inputDirectoryPath the path to the directory currently being processed
         * @property schemaType the kind of transform being validated
         * @return [ValidationContainer]
         * @throws [TranslationValidationException] if an error occurred gathering the input/output or finding the schema
         */
        internal fun getRawInputOutputAndSchemaUri(
            blobs: List<BlobAccess.Companion.BlobItemAndPreviousVersions>,
            blobContainerInfo: BlobAccess.BlobContainerMetadata,
            inputDirectoryPath: String,
            schemaType: SchemaType,
        ): ValidationContainer {
            val inputBlobUrl = "${blobContainerInfo.getBlobEndpoint()}/${inputDirectoryPath}input.fhir"
            val input = try {
                BlobAccess.downloadBlobAsBinaryData(
                    inputBlobUrl,
                    blobContainerInfo
                ).toString()
            } catch (e: Exception) {
                throw TranslationValidationException("Unable to download the input file: $inputBlobUrl", e)
            }

            val outputBlobUrl =
                "${blobContainerInfo.getBlobEndpoint()}/${inputDirectoryPath}output.${schemaType.outputExtension}"
            val output = try {
                BlobAccess.downloadBlobAsBinaryData(
                    outputBlobUrl,
                    blobContainerInfo
                ).toString()
            } catch (e: Exception) {
                throw TranslationValidationException("Unable to download the output file: $outputBlobUrl", e)
            }

            val transformBlob = blobs.filter {
                val find = Regex("$inputDirectoryPath[^/]+.yml").find(it.currentBlobItem.name)
                find != null && find.groups.isNotEmpty()
            }
            // This is a sanity check that multiple schemas were not added to the same schema directory
            if (transformBlob.size != 1) {
                throw TranslationValidationException(
                    """
                    ${transformBlob.size} schemas were found in $inputDirectoryPath, please check the configuration as only one should exist
                    """.trimIndent()
                )
            }
            val transformBlobName = transformBlob.single().currentBlobItem.name
            val schemaUri =
                "azure:/$transformBlobName"
            return ValidationContainer(input, output, schemaUri)
        }
    }

    /**
     * Function that is invoked by [ValidateSchemasFunctions] when the schemas are valid. The success path
     * will delete the previous-previous-valid-{TIMESTAMP}.txt and validating.txt file, create a new previous-valid-{TIMESTAMP}.txt
     * where the timestamp is taken from the valid-{TIMESTAMP}.txt and finally will create a new valid-{TIMESTAMP}.txt with the current time
     *
     * @param schemaType the [SchemaType] being processed
     * @param validationState the state of the [schemaType] in the [blobContainerMetadata] that needs to be updated
     * @param blobContainerMetadata the azure blob store connection details
     */
    fun handleValidationSuccess(
        schemaType: SchemaType,
        validationState: ValidationState,
        blobContainerMetadata: BlobAccess.BlobContainerMetadata,
    ) {
        // Delete the previous-previous-valid blob
        if (validationState.previousPreviousValid != null) {
            BlobAccess.deleteBlob(validationState.previousPreviousValid, blobContainerMetadata)
        } else {
            logger.warn(
                """The previous-previous-valid file was not unexpectedly not present. 
                |This indicates there might be a bug, but does not cause any execution issues.
                """.trimMargin()
            )
        }

        // Upload a valid blob with the current timestamp
        BlobAccess.uploadBlob(
            "${schemaType.directory}/$validBlobName-${Instant.now()}.txt",
            "".toByteArray(),
            blobContainerMetadata
        )

        // Delete the validating blob
        if (validationState.validating != null) {
            BlobAccess.deleteBlob(validationState.validating, blobContainerMetadata)
        } else {
            logger.warn(
                """The validating.txt was found after successfully syncing and validating. 
                |This indicates there might be a bug, but does not cause any execution issues.
                """.trimMargin()
            )
        }
    }

    /**
     * Function handles the case where validation of the schemas failed and need to get rolled back.  Handles
     * resetting all schema blobs to their previous version and then rolling back replacing valid-{TIMESTAMP}.txt with
     * the timestamp in previous-valid-{TIMESTAMP}.txt (the timestamp of the last valid sync).  Finally, the validating.txt
     * blob is removed
     *
     * @param validationState the state to get updated as part of handling the failure
     * @param blobContainerMetadata the azure connection info
     */
    fun handleValidationFailure(
        schemaType: SchemaType,
        validationState: ValidationState,
        blobContainerMetadata: BlobAccess.BlobContainerMetadata,
    ) {
        // Restore the most recent version of each schema, input and output
        validationState.schemaBlobs.forEach { BlobAccess.restorePreviousVersion(it, blobContainerMetadata) }

        // Restore the valid blob using the current previous-valid blob
        BlobAccess.uploadBlob(
            validationState.previousValid.name.replace(previousValidBlobName, validBlobName),
            "".toByteArray(),
            blobContainerMetadata
        )

        // Rename previous-previous-valid blob to previous-valid blob and delete the previous-valid blob
        BlobAccess.deleteBlob(validationState.previousValid, blobContainerMetadata)
        if (validationState.previousPreviousValid != null) {
            BlobAccess.uploadBlob(
                validationState.previousPreviousValid.name.replace(
                    previousPreviousValidBlobName,
                    previousValidBlobName
                ),
                "".toByteArray(), blobContainerMetadata
            )
            BlobAccess.deleteBlob(validationState.previousPreviousValid, blobContainerMetadata)
        } else {
            logger.error(
                """No previous-previous-valid file found while rolling back from a validation error. 
                |Creating a new previous-valid from five minutes ago.  
                |This likely indicates that there is a bug that needs to be resolved
                """.trimMargin()
            )
            BlobAccess.uploadBlob(
                "${schemaType.directory}/$previousValidBlobName-${Instant.now().minus(15, ChronoUnit.MINUTES)}",
                "".toByteArray(),
                blobContainerMetadata
            )
        }

        // Delete the validating blob
        if (validationState.validating != null) {
            BlobAccess.deleteBlob(validationState.validating, blobContainerMetadata)
        } else {
            logger.warn("Validating.txt was unexpectedly missing while rolling update back")
        }
    }

    /**
     * Retrieves the validation state for a particular schema type.
     *
     * @param schemaType which schema to retrieve the state for
     * @param blobContainerInfo the azure connection info to retrieve the state
     * @return [ValidationState]
     */
    fun retrieveValidationState(
        schemaType: SchemaType,
        blobContainerInfo: BlobAccess.BlobContainerMetadata,
    ): ValidationState {
        val allBlobs = BlobAccess.listBlobs(schemaType.directory, blobContainerInfo)
        if (allBlobs.isEmpty()) {
            throw TranslationStateUninitalized()
        }
        val valid = allBlobs.singleOrNull { it.blobName.contains(validBlobNameRegex) }
        val previousValid = allBlobs.singleOrNull { it.blobName.contains(previousValidBlobNameRegex) }
            ?: throw TranslationSyncException("Validation state was invalid, the previous-valid blob is misconfigured")
        val previousPreviousValid = allBlobs.filter { it.blobName.contains(previousPreviousValidBlobNameRegex) }.let {
            if (it.isEmpty()) {
                null
            } else if (it.size > 1) {
                throw TranslationSyncException(
                    "Validation state was invalid, there are multiple previous-previous-valid blobs"
                )
            } else {
                it.first()
            }
        }
        val validating = allBlobs.find { it.blobName.contains(validatingBlobName) }

        if (validating == null && valid == null) {
            throw TranslationSyncException(
                """Validation state was invalid, the valid blob is misconfigured. 
                    |It is either duplicated or not present when the state is not being validated
""".trimMargin()
            )
        }

        return ValidationState(
            valid?.currentBlobItem,
            previousValid.currentBlobItem,
            previousPreviousValid?.currentBlobItem,
            validating?.currentBlobItem,
            allBlobs.filter {
                it.blobName.endsWith(".yml") ||
                    it.blobName.endsWith(".hl7") ||
                    it.blobName.endsWith(".fhir")
            }
        )
    }

    /**
     * Copies the schemas for [schemaType] from [sourceBlobContainerMetadata] to [destinationBlobContainerMetadata] and then creates
     * a validating.txt blob in [destinationBlobContainerMetadata] to trigger validation.
     *
     * @param schemaType the type of schemas to be synced
     * @param destinationValidationState the validation state in the destination
     * @param sourceBlobContainerMetadata the azure connection info for the source
     * @param destinationBlobContainerMetadata  the azure connection info for the destination
     */
    fun syncSchemas(
        schemaType: SchemaType,
        sourceValidationState: ValidationState,
        destinationValidationState: ValidationState?,
        sourceBlobContainerMetadata: BlobAccess.BlobContainerMetadata,
        destinationBlobContainerMetadata: BlobAccess.BlobContainerMetadata,
    ) {
        // If the destinationValidationState is null, it means that the destination is being initialized for the first
        // time.  In that case, copy and create the previous-valid and previous-valid-valid files from the source
        if (destinationValidationState == null) {
            logger.info("Initializing a new translation schema validation state.")
            // Copy all the files between the two azure stores
            BlobAccess.copyDir(
                schemaType.directory,
                sourceBlobContainerMetadata,
                destinationBlobContainerMetadata
            ) { blob -> !blob.blobName.endsWith(".txt") }
            if (sourceValidationState.valid == null) {
                throw TranslationSyncException("Valid blob is unexpectedly missing, aborting sync")
            }
            BlobAccess.uploadBlob(
                sourceValidationState.valid.name.replace(validBlobName, previousValidBlobName),
                "".toByteArray(),
                destinationBlobContainerMetadata
            )
            BlobAccess.uploadBlob(
                sourceValidationState.previousValid.name.replace(previousValidBlobName, previousPreviousValidBlobName),
                "".toByteArray(),
                destinationBlobContainerMetadata
            )
        } else {
            // Upload a new previous-valid blob with time of the valid blob and delete the old one
            if (destinationValidationState.valid == null) {
                throw TranslationSyncException("Valid blob is unexpectedly missing, aborting sync")
            }
            BlobAccess.deleteBlob(destinationValidationState.previousValid, destinationBlobContainerMetadata)
            BlobAccess.uploadBlob(
                destinationValidationState.valid.name.replace(
                    validBlobName,
                    previousValidBlobName
                ),
                "".toByteArray(), destinationBlobContainerMetadata
            )

            // Create a previous-previous-valid blob with the timestamp from the previous-valid blob
            BlobAccess.uploadBlob(
                destinationValidationState.previousValid.name.replace(
                    previousValidBlobName,
                    previousPreviousValidBlobName
                ),
                "".toByteArray(), destinationBlobContainerMetadata
            )

            // Delete the valid blob
            BlobAccess.deleteBlob(destinationValidationState.valid, destinationBlobContainerMetadata)

            // Copy all the files between the two azure stores
            BlobAccess.copyDir(
                schemaType.directory,
                sourceBlobContainerMetadata,
                destinationBlobContainerMetadata
            ) { blob -> !blob.blobName.endsWith(".txt") }
            val sourceSchemaBlobNames = sourceValidationState.schemaBlobs.map { it.blobName }.toSet()
            // Delete all the blobs present in the destination but no longer present in the soruce
            val blobsToDelete =
                destinationValidationState.schemaBlobs.filterNot { sourceSchemaBlobNames.contains(it.blobName) }
            blobsToDelete.forEach { BlobAccess.deleteBlob(it.currentBlobItem, destinationBlobContainerMetadata) }
        }
        // Upload the validating.txt to trigger the validation azure function for the schema type
        BlobAccess.uploadBlob(
            "${schemaType.directory}/validating.txt",
            "".toByteArray(),
            destinationBlobContainerMetadata
        )
    }

    /**
     * This function processes a specific kind of transform and validates that each translation schema when applied to
     * the sample input exactly matches the sample output.
     *
     * @property schemaType the type of transform getting validated
     * @property blobContainerInfo the connection info for where the schemas getting validated are stored
     * @return [List[ValidationResult]] a list of the results from validating all valid schemas
     */
    fun validateManagedSchemas(
        schemaType: SchemaType,
        blobContainerInfo: BlobAccess.BlobContainerMetadata,
    ): List<ValidationResult> {
        val blobs =
            BlobAccess.listBlobs(schemaType.directory, blobContainerInfo, false)
        val inputs = blobs.filter { it.currentBlobItem.name.contains("/input.") }

        return inputs.map { currentInput ->
            val inputDirectoryPath =
                Regex("(?<inputDirectory>.*/).*")
                    .find(currentInput.currentBlobItem.name)!!.groups.get("inputDirectory")!!.value

            val rawValidationInput = try {
                getRawInputOutputAndSchemaUri(blobs, blobContainerInfo, inputDirectoryPath, schemaType)
            } catch (e: RuntimeException) {
                logger.error("Failed to get input or output to validate the schema", e)
                return@map ValidationResult(inputDirectoryPath, passes = false, didError = true)
            }

            val inputBundle = FhirTranscoder.decode(rawValidationInput.input)

            val isSchemaValid = try {
                when (schemaType) {
                    SchemaType.FHIR -> {
                        FhirTransformer(
                            rawValidationInput.schemaUri,
                            blobContainerInfo
                        ).validate(inputBundle, FhirTranscoder.decode(rawValidationInput.output))
                    }
                    SchemaType.HL7 -> {
                        FhirToHl7Converter(
                            rawValidationInput.schemaUri,
                            blobContainerInfo,
                            context = FhirToHl7Context(
                                CustomFhirPathFunctions(),
                                config = HL7TranslationConfig(
                                    Hl7Configuration(
                                        receivingApplicationOID = null,
                                        receivingFacilityOID = null,
                                        messageProfileId = null,
                                        receivingApplicationName = null,
                                        receivingFacilityName = null,
                                        receivingOrganization = null,
                                    ),
                                    null
                                ),
                                translationFunctions = CustomTranslationFunctions(),
                            )
                        ).validate(
                            inputBundle,
                            HL7Reader.parseHL7Message(
                                Hl7InputStreamMessageStringIterator(rawValidationInput.output.byteInputStream())
                                    .asSequence()
                                    .first(),
                                null
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error(
                    "An exception was encountered while trying to validate the schema: ${rawValidationInput.schemaUri}",
                    e
                )
                return@map ValidationResult(inputDirectoryPath, passes = false, didError = true)
            }

            ValidationResult(
                rawValidationInput.schemaUri,
                isSchemaValid
            )
        }
    }
}