package gov.cdc.prime.router.fhirengine.translation

import gov.cdc.prime.router.Report
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
    /**
     * Downloads the content of each blob in the [directory] with [blobContainerInfo] takes the input file, runs
     * the transform on it, and compares it to the output validating that they are the same.
     */
    fun validateSchemas(
        directory: String,
        blobContainerInfo: BlobAccess.BlobContainerMetadata = BlobAccess.defaultBlobMetadata,
        schemaType: Report.Format,
    ): Boolean {
        val sourceContainer = BlobAccess.getBlobContainer(blobContainerInfo)
        val blobs =
            BlobAccess.listBlobs(directory, blobContainerInfo, false)
        val transforms = blobs.filter { it.currentBlobItem.name.endsWith(".yml") }
        transforms.forEach { currentTransform ->
            val currentTransformName = currentTransform.currentBlobItem.name

            val transformBlobClient = sourceContainer.getBlobClient(currentTransformName)
            val transform = transformBlobClient.downloadContent()

            val transformDirectoryPath = Regex("(.*/).*").find(currentTransformName)!!.groups[1]!!.value
            val inputBlobClient = sourceContainer.getBlobClient(transformDirectoryPath + "input.fhir")
            val input = inputBlobClient.downloadContent()
            val inputBundle = FhirTranscoder.decode(input.toString())

            if (schemaType == Report.Format.FHIR) {
                val configSchema = ConfigSchemaReader.readOneYamlSchema(
                    transform.toStream(),
                    FhirTransformSchema::class.java
                )
                val transformedInputBundle = if (configSchema is FhirTransformSchema) {
                    FhirTransformer(configSchema).transform(inputBundle)
                } else {
                    throw InternalServerErrorException("Transform schema $currentTransformName not a FhirTransform")
                }
                val output = sourceContainer.getBlobClient(transformDirectoryPath + "output.fhir").downloadContent()
                // Unfortunately need to do this to get it to be the same format.
                val expectedOutput = FhirTranscoder.decode(output.toString())
                // .equals is required here == does not compare it properly, despite what IntelliJ says
                if (transformedInputBundle.equals(expectedOutput)) {
                    BlobAccess.logger.error("Validation failed for transform $currentTransformName")
                    return false
                }
            } else {
                val converterSchema = ConfigSchemaReader.readOneYamlSchema(transform.toStream())
                val hl7Transform = FhirToHl7Converter(
                    converterSchema as ConverterSchema,
                    false,
                    null,
                    ConstantSubstitutor(),
                    null
                ).convert(inputBundle)
                val output = sourceContainer.getBlobClient(transformDirectoryPath + "output.hl7").downloadContent()
                if (!hl7Transform.toString().trim().equals(output.toString().trim())) {
                    BlobAccess.logger.error("Validation failed for transform $currentTransformName")
                    return false
                }
            }
        }

        return true
    }
}