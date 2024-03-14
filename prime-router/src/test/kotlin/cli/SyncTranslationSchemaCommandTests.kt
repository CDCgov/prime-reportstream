package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import com.azure.storage.blob.models.BlobItem
import com.github.ajalt.clikt.testing.test
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SyncTranslationSchemaCommandTests {

    @Test
    fun `successfully syncs schemas`() {
        val sourceValidBlob = BlobItem()
        sourceValidBlob.name = "valid-${Instant.now()}.txt"
        val destinationValidBlob = BlobItem()
        destinationValidBlob.name = "valid-${Instant.now().minus(5, ChronoUnit.MINUTES)}.txt"
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true))
        every { anyConstructed<TranslationSchemaManager>().syncSchemas(any(), any(), any(), any(), any()) } returns Unit
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "source")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            sourceValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "destination")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            destinationValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )

        val command = SyncTranslationSchemaCommand()
        val result =
            command.test("-s", "FHIR", "-sb", "source", "-sc", "container1", "-db", "destination", "-dc", "container1")
        assertThat(result.output)
            .contains("Source and destination are both in a valid state to sync schemas, beginning sync...")
        assertThat(result.output)
            .contains("Successfully synced source to destination, validation will now be triggered")
    }

    @Test
    fun `does not sync if destination has a more recent update`() {
        val sourceValidBlob = BlobItem()
        sourceValidBlob.name = "valid-${Instant.now()}.txt"
        val destinationValidBlob = BlobItem()
        destinationValidBlob.name = "valid-${Instant.now().plus(5, ChronoUnit.MINUTES)}.txt"
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true))
        every { anyConstructed<TranslationSchemaManager>().syncSchemas(any(), any(), any(), any(), any()) } returns Unit
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "source")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            sourceValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "destination")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            destinationValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )

        val command = SyncTranslationSchemaCommand()
        val result =
            command.test("-s", "FHIR", "-sb", "source", "-sc", "container1", "-db", "destination", "-dc", "container1")
        assertThat(result.stderr).contains("Destination has been updated more recently than source copy of schemas")
    }

    @Test
    fun `does not sync if destination is being validated`() {
        val sourceValidBlob = BlobItem()
        sourceValidBlob.name = "valid-${Instant.now()}.txt"
        val destinationValidBlob = BlobItem()
        destinationValidBlob.name = "valid-${Instant.now().minus(5, ChronoUnit.MINUTES)}.txt"
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true))
        every { anyConstructed<TranslationSchemaManager>().syncSchemas(any(), any(), any(), any(), any()) } returns Unit
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "source")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            sourceValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "destination")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            destinationValidBlob,
            BlobItem(),
            null,
            BlobItem(),
            emptyList()
        )

        val command = SyncTranslationSchemaCommand()
        val result =
            command.test("-s", "FHIR", "-sb", "source", "-sc", "container1", "-db", "destination", "-dc", "container1")
        assertThat(result.stderr).contains("Cannot sync to destination, schemas are currently being validated")
    }

    @Test
    fun `does not sync if source is not valid`() {
        val sourceValidBlob = BlobItem()
        sourceValidBlob.name = "valid-${Instant.now()}.txt"
        val destinationValidBlob = BlobItem()
        destinationValidBlob.name = "valid-${Instant.now().minus(5, ChronoUnit.MINUTES)}.txt"
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", false))
        every { anyConstructed<TranslationSchemaManager>().syncSchemas(any(), any(), any(), any(), any()) } returns Unit
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "source")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            sourceValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "destination")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            destinationValidBlob,
            BlobItem(),
            null,
            BlobItem(),
            emptyList()
        )

        val command = SyncTranslationSchemaCommand()
        val result =
            command.test("-s", "FHIR", "-sb", "source", "-sc", "container1", "-db", "destination", "-dc", "container1")
        assertThat(result.stderr).contains("Source is not valid and schemas will not be synced")
    }

    @Test
    fun `syncs to a destination that has not been initialized`() {
        val sourceValidBlob = BlobItem()
        sourceValidBlob.name = "valid-${Instant.now()}.txt"
        val destinationValidBlob = BlobItem()
        destinationValidBlob.name = "valid-${Instant.now().minus(5, ChronoUnit.MINUTES)}.txt"
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                any()
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true))
        every { anyConstructed<TranslationSchemaManager>().syncSchemas(any(), any(), any(), any(), any()) } returns Unit
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "source")
            )
        } returns TranslationSchemaManager.Companion.ValidationState(
            sourceValidBlob,
            BlobItem(),
            null,
            null,
            emptyList()
        )
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                BlobAccess.BlobContainerMetadata("container1", "destination")
            )
        } throws TranslationSchemaManager.Companion.TranslationStateUninitalized()

        val command = SyncTranslationSchemaCommand()
        val result =
            command.test("-s", "FHIR", "-sb", "source", "-sc", "container1", "-db", "destination", "-dc", "container1")
        assertThat(result.output)
            .contains("Source and destination are both in a valid state to sync schemas, beginning sync...")
        assertThat(result.output)
            .contains("Successfully synced source to destination, validation will now be triggered")
    }
}