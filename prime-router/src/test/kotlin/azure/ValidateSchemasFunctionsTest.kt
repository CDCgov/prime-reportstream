package gov.cdc.prime.router.azure

import gov.cdc.prime.router.fhirengine.translation.TranslationSchemaManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test

class ValidateSchemasFunctionsTest {

    @Test
    fun `validateSchemaChanges success - FHIR`() {
        val mockedValidationState = mockk<TranslationSchemaManager.Companion.ValidationState>()
        val mockedBlobContainerMetadata = mockk<BlobAccess.BlobContainerMetadata>()
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true))
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
        } returns mockedValidationState
        every {
            anyConstructed<TranslationSchemaManager>().handleValidationSuccess(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        } returns Unit

        ValidateSchemasFunctions().validateSchemaChanges(
            TranslationSchemaManager.SchemaType.FHIR,
            mockedBlobContainerMetadata
        )
        verify(exactly = 1) {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().handleValidationSuccess(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        }
    }

    @Test
    fun `validateSchemaChanges failure - FHIR`() {
        val mockedValidationState = mockk<TranslationSchemaManager.Companion.ValidationState>()
        val mockedBlobContainerMetadata = mockk<BlobAccess.BlobContainerMetadata>()
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", false))
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
        } returns mockedValidationState
        every {
            anyConstructed<TranslationSchemaManager>().handleValidationFailure(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        } returns Unit

        ValidateSchemasFunctions().validateSchemaChanges(
            TranslationSchemaManager.SchemaType.FHIR,
            mockedBlobContainerMetadata
        )
        verify(exactly = 1) {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().handleValidationFailure(
                TranslationSchemaManager.SchemaType.FHIR,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        }
    }

    @Test
    fun `validateSchemaChanges success - HL7`() {
        val mockedValidationState = mockk<TranslationSchemaManager.Companion.ValidationState>()
        val mockedBlobContainerMetadata = mockk<BlobAccess.BlobContainerMetadata>()
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", true))
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
        } returns mockedValidationState
        every {
            anyConstructed<TranslationSchemaManager>().handleValidationSuccess(
                TranslationSchemaManager.SchemaType.HL7,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        } returns Unit

        ValidateSchemasFunctions().validateSchemaChanges(
            TranslationSchemaManager.SchemaType.HL7,
            mockedBlobContainerMetadata
        )
        verify(exactly = 1) {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().handleValidationSuccess(
                TranslationSchemaManager.SchemaType.HL7,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        }
    }

    @Test
    fun `validateSchemaChanges failure - HL7`() {
        val mockedValidationState = mockk<TranslationSchemaManager.Companion.ValidationState>()
        val mockedBlobContainerMetadata = mockk<BlobAccess.BlobContainerMetadata>()
        mockkConstructor(TranslationSchemaManager::class)
        every {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
        } returns listOf(TranslationSchemaManager.Companion.ValidationResult("foo/bar/transform.yml", false))
        every {
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
        } returns mockedValidationState
        every {
            anyConstructed<TranslationSchemaManager>().handleValidationFailure(
                TranslationSchemaManager.SchemaType.HL7,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        } returns Unit

        ValidateSchemasFunctions().validateSchemaChanges(
            TranslationSchemaManager.SchemaType.HL7,
            mockedBlobContainerMetadata
        )
        verify(exactly = 1) {
            anyConstructed<TranslationSchemaManager>().validateManagedSchemas(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().retrieveValidationState(
                TranslationSchemaManager.SchemaType.HL7,
                mockedBlobContainerMetadata
            )
            anyConstructed<TranslationSchemaManager>().handleValidationFailure(
                TranslationSchemaManager.SchemaType.HL7,
                mockedValidationState,
                mockedBlobContainerMetadata
            )
        }
    }
}