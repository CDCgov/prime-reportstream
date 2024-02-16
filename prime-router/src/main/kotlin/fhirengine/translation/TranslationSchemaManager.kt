package gov.cdc.prime.router.fhirengine.translation

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.apache.logging.log4j.kotlin.Logging

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

        private val hL7Reader = HL7Reader(ActionLogger())

        data class ValidationResult(
            val path: String,
            val passes: Boolean,
            val didError: Boolean = false,
        )

        data class ValidationContainer(val input: String, val output: String, val schemaUri: String)

        class TranslationValidationException(override val message: String, override val cause: Throwable? = null) :
            RuntimeException(cause)

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
                "${blobContainerInfo.getBlobEndpoint()}/$transformBlobName"
            return ValidationContainer(input, output, schemaUri)
        }
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
                            blobContainerInfo
                        ).validate(
                            inputBundle,
                            hL7Reader.getMessages(rawValidationInput.output)[0]
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