package gov.cdc.prime.router.fhirengine.translation

import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import org.apache.logging.log4j.kotlin.Logging
import javax.ws.rs.InternalServerErrorException

class TranslationSchemaManager : Logging {
    enum class SchemaType(val directory: String) {
        FHIR("fhir_transforms"),
        HL7("hl7_mapping"),
    }

    /**
     * Downloads the content of each blob in the [directory] with [blobContainerInfo] takes the input file, runs
     * the transform on it, and compares it to the output validating that they are the same.
     */
    fun validateManagedSchemas(
        schemaType: SchemaType,
        blobContainerInfo: BlobAccess.BlobContainerMetadata = BlobAccess.defaultBlobMetadata,
        blobEndpoint: String,
    ): MutableList<ValidationResults> {
        val sourceContainer = BlobAccess.getBlobContainer(blobContainerInfo)
        val blobs =
            BlobAccess.listBlobs(schemaType.directory, blobContainerInfo, false)
        val inputs = blobs.filter { it.currentBlobItem.name.contains("/input.") }
        val validationResults = mutableListOf<ValidationResults>()

        inputs.forEach { currentInput ->
            val inputDirectoryPath = Regex("(.*/).*").find(currentInput.currentBlobItem.name)!!.groups[1]!!.value
            val input = sourceContainer.getBlobClient(inputDirectoryPath + "input.fhir").downloadContent()
            val inputBundle = FhirTranscoder.decode(input.toString())

            val transformBlob = blobs.filter {
                val find = Regex("$inputDirectoryPath[^/]+.yml").find(it.currentBlobItem.name)
                find != null && find.groups.isNotEmpty()
            }
            val transformBlobName = transformBlob[0].currentBlobItem.name

            when (schemaType) {
                SchemaType.FHIR -> {
                    val configSchema = ConfigSchemaReader.fromFile(
                        blobEndpoint + "/" + blobContainerInfo.containerName + "/" + transformBlobName,
                        null,
                        FhirTransformSchema::class.java,
                        blobContainerInfo
                    )
                    val transformedInputBundle = if (configSchema is FhirTransformSchema) {
                        FhirTransformer(configSchema).transform(inputBundle)
                    } else {
                        throw InternalServerErrorException("Transform schema $transformBlob not a FhirTransform")
                    }
                    val output = sourceContainer.getBlobClient(inputDirectoryPath + "output.fhir").downloadContent()
                    // Unfortunately need to do this to get it to be the same format.
                    val expectedOutput = FhirTranscoder.decode(output.toString())
                    // .equals is required here == does not compare it properly, despite what IntelliJ says
                    if (transformedInputBundle.equals(expectedOutput)) {
                        logger.error("Validation failed for transform $transformBlobName")
                        validationResults.add(
                            ValidationResults(
                            currentInput.currentBlobItem.name,
                            inputDirectoryPath + "output.fhir",
                            transformBlobName,
                            false
                        )
                        )
                    } else {
                        validationResults.add(
                            ValidationResults(
                            currentInput.currentBlobItem.name,
                            inputDirectoryPath + "output.fhir",
                            transformBlobName,
                            true
                        )
                        )
                    }
                }
                SchemaType.HL7 -> {
                    val converterSchema = ConfigSchemaReader.fromFile(
                        blobEndpoint + "/" + blobContainerInfo.containerName + "/" + transformBlobName,
                        null,
                        ConverterSchema::class.java,
                        blobContainerInfo
                    )
                    val hl7Transform = FhirToHl7Converter(
                        converterSchema as ConverterSchema,
                        false,
                        null,
                        ConstantSubstitutor(),
                        null
                    ).convert(inputBundle)
                    val output = sourceContainer.getBlobClient(inputDirectoryPath + "output.hl7").downloadContent()
                    if (!hl7Transform.toString().trim().equals(output.toString().trim())) {
                        logger.error("Validation failed for transform $transformBlobName")
                        validationResults.add(
                            ValidationResults(
                            currentInput.currentBlobItem.name,
                            inputDirectoryPath + "output.hl7",
                            transformBlobName,
                            false
                        )
                        )
                    } else {
                        validationResults.add(
                            ValidationResults(
                            currentInput.currentBlobItem.name,
                            inputDirectoryPath + "output.hl7",
                            transformBlobName,
                            true
                        )
                        )
                    }
                }
            }
        }

        return validationResults
    }

    data class ValidationResults(
        val inputFilePath: String,
        val outputFilePath: String,
        val transformFilePath: String,
        val passes: Boolean,
    )
}