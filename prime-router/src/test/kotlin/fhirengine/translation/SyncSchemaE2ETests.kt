package gov.cdc.prime.router.fhirengine.translation

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isNull
import com.github.ajalt.clikt.testing.test
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.ValidateSchemasFunctions
import gov.cdc.prime.router.cli.SyncTranslationSchemaCommand
import gov.cdc.prime.router.common.TestcontainersUtils
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class SyncSchemaE2ETests {

    @Container
    private val azuriteContainer1 = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_syncschemae2e1",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    @Container
    private val azuriteContainer2 = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_syncschemae2e2",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    @BeforeEach
    fun beforeEach() {
        unmockkAll()
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `test end-to-end sync schemas workflow`() {
        val sourceBlobMetadata = TranslationSchemaManagerTests.createBlobMetadata(azuriteContainer1)
        val destinationBlobMetadata = TranslationSchemaManagerTests.createBlobMetadata(azuriteContainer2)
        TranslationSchemaManagerTests.setupValidState(destinationBlobMetadata)
        TranslationSchemaManagerTests.setupValidState(sourceBlobMetadata)
        TranslationSchemaManagerTests.setupSchemaInDir(
            TranslationSchemaManager.SchemaType.FHIR, "sender/foo",
            sourceBlobMetadata,
            "/src/test/resources/fhirengine/translation/FHIR_to_FHIR"
        )

        val destinationStateBefore = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobMetadata
        )

        assertThat(destinationStateBefore.schemaBlobs).isEmpty()
        assertThat(destinationStateBefore.validating).isNull()

        val syncSchemasCommand = SyncTranslationSchemaCommand()

        val result = syncSchemasCommand.test(
            "-s",
            "FHIR",
            "-sb",
            sourceBlobMetadata.connectionString,
            "-sc",
            "metadata",
            "-db",
            destinationBlobMetadata.connectionString,
            "-dc",
            "metadata"
        )

        assertThat(result.stderr).isEmpty()

        mockkObject(BlobAccess.BlobContainerMetadata)
        every { BlobAccess.BlobContainerMetadata.build(any(), any()) } returns destinationBlobMetadata
        // Manually trigger the validation function
        // In a real environment, this would be triggered by the validating.txt blob being added
        ValidateSchemasFunctions().validateFHIRToFHIRSchemas(emptyArray())

        val destinationStateAfter = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobMetadata
        )

        assertThat(destinationStateAfter.schemaBlobs).hasSize(3)
        assertThat(destinationStateAfter.validating).isNull()
    }
}