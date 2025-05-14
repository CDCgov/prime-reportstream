package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import org.apache.logging.log4j.kotlin.Logging

class SyncTranslationSchemaCommand :
    CliktCommand(
        name = "syncSchemas",
    ),
    Logging {
    override fun help(context: Context): String {
        // TODO
        return """"
    """.trimMargin()
    }

    private val translationSchemaManager = TranslationSchemaManager()

    /**
     * The schema type to validate
     */
    private val schemaType: TranslationSchemaManager.SchemaType by option(
        "--schema-type",
        "-s",
        help = "The schema type to validate against"
    ).enum<TranslationSchemaManager.SchemaType>().required()

    private val sourceBlobStoreConnection: String by option(
        "--source-blob-store-connect",
        "-sb",
        help = "The blob store connection string where schemas will be validated"
    ).required()

    private val sourceBlobStoreContainer: String by option(
        "--source-blob-store-container",
        "-sc",
        help = "The container in azure where the schemas are stored"
    ).required()

    private val destinationBlobStoreConnection: String by option(
        "--destination-blob-store-connect",
        "-db",
        help = "The blob store connection string where schemas will be validated"
    ).required()

    private val destinationBlobStoreContainer: String by option(
        "--destination-blob-store-container",
        "-dc",
        help = "The container in azure where the schemas are stored"
    ).required()

    /**
     * CLI command that will attempt to sync schemas from a source blob store to a destination blob store.
     *
     * Schemas will only be synced if:
     * - the source is up-to-date with the changes in destination
     * - the schemas in the source are valid
     * - the schemas in the destination are not currently being checked for validity
     */
    override fun run() {
        val sourceContainerMetadata =
            BlobAccess.BlobContainerMetadata(sourceBlobStoreContainer, sourceBlobStoreConnection)
        val destinationContainerMetadata =
            BlobAccess.BlobContainerMetadata(destinationBlobStoreContainer, destinationBlobStoreConnection)

        val sourceValidationResults =
            translationSchemaManager.validateManagedSchemas(schemaType, sourceContainerMetadata)
        val sourceIsValid = sourceValidationResults.all { it.passes }

        if (!sourceIsValid) {
            echo("Source is not valid and schemas will not be synced", err = true)
        } else {
            // The destination state can be null if this is first time it is getting synced
            val destinationValidationState = try {
                translationSchemaManager.retrieveValidationState(schemaType, destinationContainerMetadata)
            } catch (e: TranslationSchemaManager.Companion.TranslationStateUninitalized) {
                null
            }

            if (destinationValidationState?.validating != null) {
                echo("Cannot sync to destination, schemas are currently being validated", err = true)
            } else {
                val sourceValidationState =
                    translationSchemaManager.retrieveValidationState(schemaType, sourceContainerMetadata)

                if (destinationValidationState != null &&
                    sourceValidationState.isOutOfDate(destinationValidationState)
                ) {
                    echo("Destination has been updated more recently than source copy of schemas", err = true)
                } else {
                    echo("Source and destination are both in a valid state to sync schemas, beginning sync...")
                    translationSchemaManager.syncSchemas(
                        schemaType,
                        sourceValidationState,
                        destinationValidationState,
                        sourceContainerMetadata,
                        destinationContainerMetadata
                    )
                    echo("Successfully synced source to destination, validation will now be triggered")
                }
            }
        }
    }
}