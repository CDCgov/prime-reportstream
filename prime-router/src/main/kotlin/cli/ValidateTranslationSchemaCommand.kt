package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import org.apache.logging.log4j.kotlin.Logging

class ValidateTranslationSchemaCommand :
    CliktCommand(
    name = "validateSchemas",
    help = "Commands to validate translation schemas"
),
    Logging {

    private val translationSchemaManager = TranslationSchemaManager()

    /**
     * The schema type to validate
     */
    private val schemaType: TranslationSchemaManager.SchemaType by option(
        "--schema-type",
        "-s",
        help = "which schema type to process"
    ).enum<TranslationSchemaManager.SchemaType>().required()

    private val blobStoreConnection: String by option(
        "--blob-store-connect",
        "-c",
        help = "The blob store connection string where schemas will be validated"
    ).required()

    private val blobStoreContainer: String by option(
        "--blob-store-container",
        "-b",
        help = "The container in azure where the schemas are stored"
    ).required()

    override fun run() {
        val validationResults =
            translationSchemaManager.validateManagedSchemas(
                schemaType,
                BlobAccess.BlobContainerMetadata(blobStoreContainer, blobStoreConnection)
            )
        if (validationResults.size > 0) {
            val allPassed = validationResults.all { it.passes }

            val individualResults = validationResults.map { result ->
                """
            Path validated: ${result.path}
            Valid: ${result.passes}
            Errored while validating: ${result.didError}"""
            }.joinToString("\n")
            val output =
                """
            -----
            Results from validating $schemaType, schemas were ${if (allPassed) "valid" else "invalid"}
            
            ${validationResults.size} schemas were validated
            -----
            
            Individual Results:
            $individualResults
            """.trimIndent()

            echo(output)
        } else {
            echo("No schemas were found, please make sure that you have the correct configuration", err = true)
        }
    }
}