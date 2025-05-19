package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import org.apache.logging.log4j.kotlin.Logging

class ValidateTranslationSchemaCommand :
    CliktCommand(
        name = "validateSchemas",
    ),
    Logging {
    override fun help(
        context: Context,
    ): String = """"Commands to validate translation schemas in a particular environment
        | It works by taking the kind of schema to validate and the azure connection details/
        | 
        | Each schema is accompanied with an input and output file and is validated by running the schema
        | against the input and checking that it matches the output.
        | 
        | The schemaType parameter maps to an enum which contains details on which directory
        | in the container will be validated
        | FHIR -> fhir_transforms
        | HL7 -> hl7_mapping
        | 
        | If an unexpected error occurs please confirm that the files in directory are correct; i.e.
        | adding a FHIR -> FHIR transform into the hl7_mapping directory will cause an exception.
    """.trimMargin()

    private val translationSchemaManager = TranslationSchemaManager()

    /**
     * The schema type to validate
     */
    private val schemaType: TranslationSchemaManager.SchemaType by option(
        "--schema-type",
        "-s",
        help = "The schema type to validate against"
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
        if (validationResults.isNotEmpty()) {
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
            // This will cause the command to exit with code 1 to indicate it failed
            if (!allPassed) {
                throw CliktError("Validation failed")
            }
        } else {
            echo(
                "No schemas were found in ${schemaType.directory}," +
                    " please make sure that you have the correct configuration",
                err = true
            )
        }
    }
}