package gov.cdc.prime.router.fhirengine.translation

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.azure.core.util.BinaryData
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.BlobStorageException
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit

@Testcontainers(parallel = true)
class TranslationSchemaManagerTests {
    @Container
    private val azuriteContainer1 =
        GenericContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite"))
            .withEnv("AZURITE_ACCOUNTS", "devstoreaccount1:keydevstoreaccount1")
            .withExposedPorts(10000, 10001, 10002)

    @Container
    private val azuriteContainer2 =
        GenericContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite"))
            .withEnv("AZURITE_ACCOUNTS", "devstoreaccount1:keydevstoreaccount1")
            .withExposedPorts(10000, 10001, 10002)

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    companion object {
        private fun createBlobMetadata(container: GenericContainer<*>): BlobAccess.BlobContainerMetadata {
            val blobEndpoint = "http://${container.host}:${
                container.getMappedPort(
                    10000
                )
            }/devstoreaccount1"
            val containerName = "container1"
            return BlobAccess.BlobContainerMetadata(
                containerName,
                """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=$blobEndpoint;QueueEndpoint=http://${container.host}:${
                    container.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
            )
        }

        private fun setupValidSchema(blobContainerMetadata: BlobAccess.BlobContainerMetadata) {
            val inputFilePath = "${TranslationSchemaManager.SchemaType.FHIR.directory}/dev/bar/input.fhir"
            val outputFilePath = "${TranslationSchemaManager.SchemaType.FHIR.directory}/dev/bar/output.fhir"
            val transformFilePath = "${TranslationSchemaManager.SchemaType.FHIR.directory}/dev/bar/simple-transform.yml"
            BlobAccess.uploadBlob(
                inputFilePath,
                File(
                    Paths.get("").toAbsolutePath().toString() +
                        "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/input.fhir"
                )
                    .inputStream().readAllBytes(),
                blobContainerMetadata
            )

            BlobAccess.uploadBlob(
                outputFilePath,
                File(
                    Paths.get("").toAbsolutePath().toString() +
                        "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/output.fhir"
                )
                    .inputStream().readAllBytes(),
                blobContainerMetadata
            )

            BlobAccess.uploadBlob(
                transformFilePath,
                File(
                    Paths.get("").toAbsolutePath().toString() +
                        "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/simple-transform.yml"
                )
                    .inputStream().readAllBytes(),
                blobContainerMetadata
            )
        }

        private fun setupValidatingState(
            blobContainerMetadata: BlobAccess.BlobContainerMetadata,
        ): Triple<String, String, String> {
            val validBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
            BlobAccess.uploadBlob(
                validBlobName,
                "".toByteArray(),
                blobContainerMetadata
            )
            val previousValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
                Instant.now().minus(5, ChronoUnit.MINUTES)
            }.txt"
            BlobAccess.uploadBlob(
                previousValidBlobName,
                "".toByteArray(),
                blobContainerMetadata
            )
            val previousPreviousValidBlobName =
                "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-previous-valid-${
                    Instant.now().minus(5, ChronoUnit.MINUTES)
                }.txt"
            BlobAccess.uploadBlob(
                previousPreviousValidBlobName,
                "".toByteArray(),
                blobContainerMetadata
            )
            BlobAccess.uploadBlob(
                "${TranslationSchemaManager.SchemaType.FHIR.directory}/validating.txt",
                "".toByteArray(),
                blobContainerMetadata
            )
            return Triple(validBlobName, previousValidBlobName, previousPreviousValidBlobName)
        }
    }

    @Test
    fun `syncSchemas - overwrite destination`() {
        val sourceBlobContainerMetadata = createBlobMetadata(azuriteContainer1)
        val destinationBlobContainerMetadata = createBlobMetadata(azuriteContainer2)

        val sourceValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
        BlobAccess.uploadBlob(
            sourceValidBlobName,
            "".toByteArray(),
            sourceBlobContainerMetadata
        )
        val sourcePreviousValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
            Instant.now().minus(5, ChronoUnit.MINUTES)
        }.txt"
        BlobAccess.uploadBlob(
            sourcePreviousValidBlobName,
            "".toByteArray(),
            sourceBlobContainerMetadata
        )
        setupValidSchema(sourceBlobContainerMetadata)

        val destinationValidBlobName =
            "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
        BlobAccess.uploadBlob(
            destinationValidBlobName,
            "".toByteArray(),
            destinationBlobContainerMetadata
        )
        val destinationPreviousValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
            Instant.now().minus(5, ChronoUnit.MINUTES)
        }.txt"
        BlobAccess.uploadBlob(
            destinationPreviousValidBlobName,
            "".toByteArray(),
            destinationBlobContainerMetadata
        )
        setupValidSchema(destinationBlobContainerMetadata)

        val destinationValidationStateBefore = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobContainerMetadata
        )

        assertThat(destinationValidationStateBefore.validating).isNull()
        assertThat(destinationValidationStateBefore.previousPreviousValid).isNull()
        assertThat(destinationValidationStateBefore.previousValid).isNotNull()
        assertThat(destinationValidationStateBefore.valid).isNotNull()
        assertThat(destinationValidationStateBefore.schemaBlobs).hasSize(3)

        TranslationSchemaManager().syncSchemas(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationValidationStateBefore,
            sourceBlobContainerMetadata,
            destinationBlobContainerMetadata
        )

        val destinationValidationStateAfter = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobContainerMetadata
        )

        assertThat(destinationValidationStateAfter.validating).isNotNull()
        assertThat(destinationValidationStateAfter.previousPreviousValid).isNotNull()
        assertThat(destinationValidationStateAfter.previousValid).isNotNull()
        assertThat(destinationValidationStateAfter.valid).isNotNull()
        // TODO better assertion that the file has been overwritten
        assertThat(destinationValidationStateAfter.schemaBlobs).hasSize(3)
    }

    @Test
    fun `syncSchemas - destination empty`() {
        val sourceBlobContainerMetadata = createBlobMetadata(azuriteContainer1)
        val destinationBlobContainerMetadata = createBlobMetadata(azuriteContainer2)

        val sourceValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
        BlobAccess.uploadBlob(
            sourceValidBlobName,
            "".toByteArray(),
            sourceBlobContainerMetadata
        )
        val sourcePreviousValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
            Instant.now().minus(5, ChronoUnit.MINUTES)
        }.txt"
        BlobAccess.uploadBlob(
            sourcePreviousValidBlobName,
            "".toByteArray(),
            sourceBlobContainerMetadata
        )
        setupValidSchema(sourceBlobContainerMetadata)

        val destinationValidBlobName =
            "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
        BlobAccess.uploadBlob(
            destinationValidBlobName,
            "".toByteArray(),
            destinationBlobContainerMetadata
        )
        val destinationPreviousValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
            Instant.now().minus(5, ChronoUnit.MINUTES)
        }.txt"
        BlobAccess.uploadBlob(
            destinationPreviousValidBlobName,
            "".toByteArray(),
            destinationBlobContainerMetadata
        )

        val destinationValidationStateBefore = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobContainerMetadata
        )

        assertThat(destinationValidationStateBefore.validating).isNull()
        assertThat(destinationValidationStateBefore.previousPreviousValid).isNull()
        assertThat(destinationValidationStateBefore.previousValid).isNotNull()
        assertThat(destinationValidationStateBefore.valid).isNotNull()
        assertThat(destinationValidationStateBefore.schemaBlobs).hasSize(0)

        TranslationSchemaManager().syncSchemas(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationValidationStateBefore,
            sourceBlobContainerMetadata,
            destinationBlobContainerMetadata
        )

        val destinationValidationStateAfter = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            destinationBlobContainerMetadata
        )

        assertThat(destinationValidationStateAfter.validating).isNotNull()
        assertThat(destinationValidationStateAfter.previousPreviousValid).isNotNull()
        assertThat(destinationValidationStateAfter.previousValid).isNotNull()
        assertThat(destinationValidationStateAfter.valid).isNotNull()
        assertThat(destinationValidationStateAfter.schemaBlobs).hasSize(3)
    }

    @Test
    fun `handleValidationSuccess - all files present`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)
        val (validBlobName, _, _) = setupValidatingState(
            blobContainerMetadata
        )
        val validBlobNameTimestamp =
            validBlobName.removePrefix("${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-")
        setupValidSchema(
            blobContainerMetadata
        )
        val validationStateBefore = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata
        )
        TranslationSchemaManager().handleValidationSuccess(
            TranslationSchemaManager.SchemaType.FHIR,
            validationStateBefore,
            blobContainerMetadata
        )
        val validationStateAfter = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata
        )

        assertThat(validationStateAfter.previousPreviousValid).isNull()
        assertThat(validationStateAfter.validating).isNull()
        assertThat(validationStateAfter.valid).isNotNull().transform { it.name }
            .doesNotContain(validBlobNameTimestamp)
        assertThat(validationStateAfter.previousValid).isNotNull().transform { it.name }
            .contains(validBlobNameTimestamp)
    }

    @Test
    fun `handleValidationFailure - all files present`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)
        val (_, previousValidBlobName, previousPreviousValidBlobName) = setupValidatingState(
            blobContainerMetadata
        )
        setupValidSchema(
            blobContainerMetadata
        )
        val previousValidTimestamp =
            previousValidBlobName.removePrefix("${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-")
        val previousPreviousValidTimestamp =
            previousPreviousValidBlobName.removePrefix(
                "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-previous-valid-"
            )

        mockkObject(BlobAccess)
        // azurite does not support blob versions, so this must be mocked
        every { BlobAccess.restorePreviousVersion(any(), blobContainerMetadata) } answers { }

        val validationStateBeforeRollback = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata
        )

        TranslationSchemaManager().handleValidationFailure(validationStateBeforeRollback, blobContainerMetadata)
        val validationStateAfterRollback = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata
        )

        assertThat(validationStateAfterRollback.previousPreviousValid).isNull()
        assertThat(validationStateAfterRollback.validating).isNull()
        assertThat(validationStateAfterRollback.valid).isNotNull().transform { it.name }
            .contains(previousValidTimestamp)
        assertThat(validationStateAfterRollback.previousValid).isNotNull().transform { it.name }
            .contains(previousPreviousValidTimestamp)
        verify(exactly = 1) {
            BlobAccess.restorePreviousVersion(validationStateBeforeRollback.schemaBlobs[0], blobContainerMetadata)
            BlobAccess.restorePreviousVersion(validationStateBeforeRollback.schemaBlobs[1], blobContainerMetadata)
            BlobAccess.restorePreviousVersion(validationStateBeforeRollback.schemaBlobs[2], blobContainerMetadata)
        }
    }

    @Test
    fun `retrieveValidationState - previous-valid blob missing`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val validBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
        BlobAccess.uploadBlob(
            validBlobName,
            "".toByteArray(),
            blobContainerMetadata
        )

        val exception = assertThrows<TranslationSchemaManager.Companion.TranslationSyncException> {
            TranslationSchemaManager().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                blobContainerMetadata
            )
        }

        assertThat(exception.message).isEqualTo("Validation state was invalid, the previous-valid blob was missing")
    }

    @Test
    fun `retrieveValidationState - valid blob missing`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)
        val exception = assertThrows<TranslationSchemaManager.Companion.TranslationSyncException> {
            TranslationSchemaManager().retrieveValidationState(
                TranslationSchemaManager.SchemaType.FHIR,
                blobContainerMetadata
            )
        }

        assertThat(exception.message).isEqualTo("Validation state was invalid, the valid blob was missing")
    }

    @Test
    fun `retrieveValidationState - validating and previous-previous missing`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val validBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/valid-${Instant.now()}.txt"
        BlobAccess.uploadBlob(
            validBlobName,
            "".toByteArray(),
            blobContainerMetadata
        )
        val previousValidBlobName = "${TranslationSchemaManager.SchemaType.FHIR.directory}/previous-valid-${
            Instant.now().minus(5, ChronoUnit.MINUTES)
        }.txt"
        BlobAccess.uploadBlob(
            previousValidBlobName,
            "".toByteArray(),
            blobContainerMetadata
        )
        setupValidSchema(blobContainerMetadata)

        val validationState = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata
        )

        assertThat(validationState.valid).isNotNull().transform { validationState.valid.name }.contains(validBlobName)
        assertThat(validationState.previousValid).isNotNull().transform { validationState.previousValid.name }
            .contains(previousValidBlobName)
        assertThat(validationState.previousPreviousValid).isNull()
        assertThat(validationState.validating).isNull()
        assertThat(validationState.schemaBlobs).hasSize(3)
    }

    @Test
    fun `retrieveValidationState - all files present`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val (validBlobName, previousValidBlobName, previousPreviousValidBlobName) = setupValidatingState(
            blobContainerMetadata
        )
        setupValidSchema(blobContainerMetadata)

        val validationState = TranslationSchemaManager().retrieveValidationState(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata
        )

        assertThat(validationState.valid).isNotNull().transform { validationState.valid.name }.contains(validBlobName)
        assertThat(validationState.previousValid).isNotNull().transform { validationState.previousValid.name }
            .contains(previousValidBlobName)
        assertThat(validationState.previousPreviousValid).isNotNull()
            .transform { validationState.previousPreviousValid!!.name }.contains(previousPreviousValidBlobName)
        assertThat(validationState.validating).isNotNull().transform { validationState.validating!!.name }
            .contains("validating.txt")
        assertThat(validationState.schemaBlobs).hasSize(3)
    }

    @Test
    fun `validateSchemas - fhir to fhir`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val inputFilePath = "fhir_transforms/dev/bar/input.fhir"
        val outputFilePath = "fhir_transforms/dev/bar/output.fhir"
        val transformFilePath = "fhir_transforms/dev/bar/simple-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/input.fhir"
            )
                .inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/output.fhir"
            )
                .inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_FHIR/simple-transform.yml"
            )
                .inputStream().readAllBytes(),
            blobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.FHIR,
            blobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(1)

        assertThat(validationResults[0].passes).isTrue()
        assertThat(validationResults[0].didError).isFalse()

        assertThat(
            validationResults[0].path
        ).contains(transformFilePath)
    }

    @Test
    fun `validateSchemas - fhir to hl7`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            blobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(1)

        assertThat(validationResults[0].passes).isTrue()
        assertThat(validationResults[0].didError).isFalse()

        assertThat(
            validationResults[0].path
        ).contains(transformFilePath)
    }

    @Test
    fun `test validateSchemas - validation fails`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/output-invalid.hl7"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            blobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(1)

        assertThat(validationResults[0].passes).isFalse()

        assertThat(validationResults[0].didError).isFalse()

        assertThat(
            validationResults[0].path
        ).contains(transformFilePath)
    }

    @Test
    fun `test validateSchemas - multiple to verify`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val inputFilePath1 = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath1 = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath1 = "hl7_mapping/dev/foo/sender-transform.yml"
        val inputFilePath2 = "hl7_mapping/dev/bar/input.fhir"
        val outputFilePath2 = "hl7_mapping/dev/bar/output.hl7"
        val transformFilePath2 = "hl7_mapping/dev/bar/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath1,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath1,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath1,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            inputFilePath2,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath2,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath2,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            blobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(2)

        assertThat(validationResults.all { result -> result.passes }).isTrue()
    }

    @Test
    fun `test validateSchemas - error encountered`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "hl7_mapping/dev/foo/sender-transform2.yml",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            blobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(1)

        assertThat(
            validationResults[0].path
        ).contains("hl7_mapping/dev/foo/")
    }

    @Test
    fun `test conversion error`() {
        val blobContainerMetadata = createBlobMetadata(azuriteContainer1)

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/resources/fhirengine/translation/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            blobContainerMetadata
        )

        mockkConstructor(FhirToHl7Converter::class)
        every { anyConstructed<FhirToHl7Converter>().validate(any(), any()) } throws RuntimeException("Convert fail")

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            blobContainerMetadata,
        )

        assertThat(validationResults).hasSize(1)
        assertThat(validationResults[0].passes).isFalse()
        assertThat(validationResults[0].didError).isTrue()
    }

    @Nested
    inner class TranslateSchemaManagerGetInputOutputAndSchemaTests {

        private val inputDirectory = "fhir_transforms/test_transform/"
        private val blobContainerMetadata = BlobAccess.BlobContainerMetadata("test", ";BlobEndpoint=mock;")
        private val schema1Blob = mockk<BlobItem>()
        private val schema2Blob = mockk<BlobItem>()

        @Test
        fun `test all required values present`() {
            every { schema1Blob.name } returns "${inputDirectory}transform.yml"
            mockkObject(BlobAccess)
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/input.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("MSH")
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/output.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("{}")

            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/transform.yml",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("{}")

            val results = TranslationSchemaManager.getRawInputOutputAndSchemaUri(
                listOf(
                    BlobAccess.Companion.BlobItemAndPreviousVersions(
                        schema1Blob,
                        emptyList()
                    )
                ),
                blobContainerMetadata, inputDirectory, TranslationSchemaManager.SchemaType.FHIR
            )

            assertThat(results.input).isEqualTo("MSH")
        }

        @Test
        fun `test input missing`() {
            every { schema1Blob.name } returns "${inputDirectory}transform.yml"
            mockkObject(BlobAccess)
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/input.fhir",
                    blobContainerMetadata
                )
            } throws BlobStorageException("Error", null, null)

            val exception = assertThrows<TranslationSchemaManager.Companion.TranslationValidationException> {
                TranslationSchemaManager.getRawInputOutputAndSchemaUri(
                    listOf(
                        BlobAccess.Companion.BlobItemAndPreviousVersions(
                            schema1Blob,
                            emptyList()
                        )
                    ),
                    blobContainerMetadata, inputDirectory, TranslationSchemaManager.SchemaType.FHIR
                )
            }

            assertThat(exception.message)
                .isEqualTo(
                    "Unable to download the input file: mock/test/fhir_transforms/test_transform/input.fhir"
                )
        }

        @Test
        fun `test output missing`() {
            mockkObject(BlobAccess)
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/input.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("MSH")
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/output.fhir",
                    blobContainerMetadata
                )
            } throws BlobStorageException("Error", null, null)

            val exception = assertThrows<TranslationSchemaManager.Companion.TranslationValidationException> {
                TranslationSchemaManager.getRawInputOutputAndSchemaUri(
                    listOf(
                        BlobAccess.Companion.BlobItemAndPreviousVersions(
                            schema1Blob,
                            emptyList()
                        )
                    ),
                    blobContainerMetadata, inputDirectory, TranslationSchemaManager.SchemaType.FHIR
                )
            }

            assertThat(exception.message)
                .isEqualTo(
                    "Unable to download the output file: mock/test/fhir_transforms/test_transform/output.fhir"
                )
        }

        @Test
        fun `test schema missing`() {
            mockkObject(BlobAccess)
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/input.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("MSH")
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/output.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("{}")

            val exception = assertThrows<TranslationSchemaManager.Companion.TranslationValidationException> {
                TranslationSchemaManager.getRawInputOutputAndSchemaUri(
                    emptyList(),
                    blobContainerMetadata,
                    inputDirectory,
                    TranslationSchemaManager.SchemaType.FHIR
                )
            }

            assertThat(exception.message)
                .isEqualTo(
                    """
                    0 schemas were found in fhir_transforms/test_transform/, please check the configuration as only one should exist
                    """.trimIndent()
                )
        }

        @Test
        fun `test too many schemas`() {
            every { schema1Blob.name } returns "${inputDirectory}transform.yml"
            every { schema2Blob.name } returns "${inputDirectory}transforms.yml"
            mockkObject(BlobAccess)
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/input.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("MSH")
            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/output.fhir",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("{}")

            every {
                BlobAccess.downloadBlobAsBinaryData(
                    "mock/test/fhir_transforms/test_transform/transform.yml",
                    blobContainerMetadata
                )
            } returns BinaryData.fromString("{}")

            val exception = assertThrows<TranslationSchemaManager.Companion.TranslationValidationException> {
                TranslationSchemaManager.getRawInputOutputAndSchemaUri(
                    listOf(
                        BlobAccess.Companion.BlobItemAndPreviousVersions(
                            schema1Blob,
                            emptyList()
                        ),
                        BlobAccess.Companion.BlobItemAndPreviousVersions(
                            schema2Blob,
                            emptyList()
                        )
                    ),
                    blobContainerMetadata, inputDirectory, TranslationSchemaManager.SchemaType.FHIR
                )
            }

            assertThat(exception.message)
                .isEqualTo(
                    """
                    2 schemas were found in fhir_transforms/test_transform/, please check the configuration as only one should exist
                    """.trimIndent()
                )
        }
    }
}