package gov.cdc.prime.router.fhirengine.translation

import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import javax.ws.rs.InternalServerErrorException

class TranslationSchemaManager {
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
    ): Boolean {
        val sourceContainer = BlobAccess.getBlobContainer(blobContainerInfo)
        val blobs =
            BlobAccess.listBlobs(schemaType.directory, blobContainerInfo, false)
        val inputs = blobs.filter { it.currentBlobItem.name.startsWith("input.") }

        inputs.forEach { currentInput ->
            val inputDirectoryPath = Regex("(.*/).*").find(currentInput.currentBlobItem.name)!!.groups[1]!!.value
            val input = sourceContainer.getBlobClient(inputDirectoryPath + "input.fhir")
            val inputBundle = FhirTranscoder.decode(input.toString())

            val transformBlob = blobs.filter {
                it.currentBlobItem.name.startsWith(inputDirectoryPath) &&
                it.currentBlobItem.name.endsWith(".yml")
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
                        BlobAccess.logger.error("Validation failed for transform $transformBlobName")
                        return false
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
                        BlobAccess.logger.error("Validation failed for transform $transformBlobName")
                        return false
                    }
                }
            }
        }

        return true
    }
}