package gov.cdc.prime.router.cli.tests

import com.github.ajalt.clikt.testing.test
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.cli.SyncTranslationSchemaCommand
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit

class SyncTranslationSchemasTest : CoolTest() {
    override val name: String = "sync-translation-schemas"
    override val description: String =
        "Verifies that the happy for syncing translation schemas between azure environments work as expected"
    override val status: TestStatus = TestStatus.SMOKE

    private val maxPollTime = 5000L

    private val syncSchemasCommand = SyncTranslationSchemaCommand()

    @Suppress("ktlint:standard:max-line-length")
    private val destinationConnectionString =
        "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
            "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
            "BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"

    @Suppress("ktlint:standard:max-line-length")
    private val sourceConnectionString =
        "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
            "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
            "BlobEndpoint=http://localhost:11000/devstoreaccount1;QueueEndpoint=http://localhost:11001/devstoreaccount1;"

    private fun setupCleanState(
        sourceBlobContainerMetadata: BlobAccess.BlobContainerMetadata,
        destinationBlobContainerMetadata: BlobAccess.BlobContainerMetadata,
    ) {
        // Delete all the blobs in the containers
        BlobAccess.listBlobs("", sourceBlobContainerMetadata)
            .forEach { BlobAccess.deleteBlob(it.currentBlobItem, sourceBlobContainerMetadata) }
        BlobAccess.listBlobs("", destinationBlobContainerMetadata)
            .forEach { BlobAccess.deleteBlob(it.currentBlobItem, destinationBlobContainerMetadata) }

        // add validation state to source
        BlobAccess.uploadBlob(
            "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt",
            "".toByteArray(),
            sourceBlobContainerMetadata
        )
        BlobAccess.uploadBlob(
            "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
                Instant.now().minus(5, ChronoUnit.MINUTES)
            }.txt",
            "".toByteArray(),
            sourceBlobContainerMetadata
        )
        // add schemas to source
        val inputFilePath = "${TranslationSchemaManager.SchemaType.FHIR.directory}/sender/test/input.fhir"
        val outputFilePath = "${TranslationSchemaManager.SchemaType.FHIR.directory}/sender/test/output.fhir"
        val transformFilePath = "${TranslationSchemaManager.SchemaType.FHIR.directory}/sender/test/simple-transform.yml"

        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/input.fhir"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/output.fhir"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/simple-transform.yml"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        // add validation state to destination
        BlobAccess.uploadBlob(
            "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${
                Instant.now().minus(5, ChronoUnit.MINUTES)
            }.txt",
            "".toByteArray(),
            destinationBlobContainerMetadata
        )
        BlobAccess.uploadBlob(
            "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
                Instant.now().minus(10, ChronoUnit.MINUTES)
            }.txt",
            "".toByteArray(),
            destinationBlobContainerMetadata
        )
    }

    private suspend fun waitForValidationToComplete(
        schemaType: TranslationSchemaManager.SchemaType,
        blobContainerMetadata: BlobAccess.BlobContainerMetadata,
    ): Deferred<TranslationSchemaManager.Companion.ValidationState> = coroutineScope {
        async {
            withTimeout(maxPollTime) {
                var validationState = TranslationSchemaManager().retrieveValidationState(
                    schemaType,
                    blobContainerMetadata
                )
                while (validationState.validating != null) {
                    try {
                        validationState = TranslationSchemaManager().retrieveValidationState(
                            schemaType,
                            blobContainerMetadata
                        )
                    } catch (ex: Exception) {
                        // An exception can occur if we fetch the validation state while
                        // the handler is occurring changing the state
                        continue
                    }
                    delay(500)
                }
                validationState
            }
        }
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val ci = System.getenv("CI")
        if (ci == null || ci != "CI") {
            echo("This test is only configured to run during CI to prevent deleting local work")
            return true
        }
        val containerName = "metadata"
        val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(containerName, sourceConnectionString)
        val destinationBlobContainerMetadata =
            BlobAccess.BlobContainerMetadata(containerName, destinationConnectionString)
        setupCleanState(sourceBlobContainerMetadata, destinationBlobContainerMetadata)
        val destinationValidationStateBefore = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobContainerMetadata
        )
        if (destinationValidationStateBefore.schemaBlobs.isNotEmpty()) {
            bad("Incorrect state, destination should contain no schemas")
        }
        syncSchemasCommand.test(
            "-s",
            "FHIR",
            "-sc",
            containerName,
            "-sb",
            sourceConnectionString,
            "-dc",
            containerName,
            "-db",
            destinationConnectionString
        )

        val validationPoll =
            waitForValidationToComplete(TranslationSchemaManager.SchemaType.FHIR, destinationBlobContainerMetadata)
        try {
            val destinationValidationStateAfter = validationPoll.await()
            if (destinationValidationStateAfter.previousPreviousValid != null) {
                bad("previous-previous-valid was present after validation was complete")
                return false
            }
            if (!destinationValidationStateBefore.isOutOfDate(destinationValidationStateAfter)) {
                bad("valid file did not get updated with a new timestamp")
                return false
            }
            if (destinationValidationStateAfter.schemaBlobs.isEmpty()) {
                bad("Destination should contain schemas")
                return false
            }

            good("Validation completed and all files were correct")
            return true
        } catch (e: TimeoutCancellationException) {
            bad("Validation never completed")
            return false
        }
    }
}