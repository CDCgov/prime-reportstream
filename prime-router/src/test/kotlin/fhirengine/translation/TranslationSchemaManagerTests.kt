package gov.cdc.prime.router.fhirengine.translation

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.nio.file.Paths

class TranslationSchemaManagerTests {
    private val azuriteContainer1 =
        GenericContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite"))
            .withEnv("AZURITE_ACCOUNTS", "devstoreaccount1:keydevstoreaccount1")
            .withExposedPorts(10000, 10001, 10002)

    init {
        azuriteContainer1.start()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `validateSchemas - fhir to fhir`() {
        val blobEndpoint = "http://${azuriteContainer1.host}:${
            azuriteContainer1.getMappedPort(
                10000
            )
        }/devstoreaccount1"
        val containerName = "container1"
        val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
            containerName,
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=$blobEndpoint;QueueEndpoint=http://${azuriteContainer1.host}:${
                azuriteContainer1.getMappedPort(
                    10001
                )
            }/devstoreaccount1;"""
        )

        val inputFilePath = "fhir_transforms/dev/bar/input.fhir"
        val outputFilePath = "fhir_transforms/dev/bar/output.fhir"
        val transformFilePath = "fhir_transforms/dev/bar/simple-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_FHIR/input.fhir"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_FHIR/output.fhir"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_FHIR/simple-transform.yml"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.FHIR,
            sourceBlobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(1)

        assertThat(
            validationResults[0].path
        ).contains(transformFilePath)
    }

    @Test
    fun `validateSchemas - fhir to hl7`() {
        val blobEndpoint = "http://${azuriteContainer1.host}:${
            azuriteContainer1.getMappedPort(
                10000
            )
        }/devstoreaccount1"
        val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;""" +
                """AccountKey=keydevstoreaccount1;BlobEndpoint=$blobEndpoint;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
        )

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "hl7_mapping/dev/foo/distraction/sender-transform.yml",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            sourceBlobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(1)

        assertThat(
            validationResults[0].path
        ).contains(transformFilePath)
    }

    @Test
    fun `test validateSchemas - multiple to verify`() {
        val blobEndpoint = "http://${azuriteContainer1.host}:${
            azuriteContainer1.getMappedPort(
                10000
            )
        }/devstoreaccount1"
        val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;""" +
                """AccountKey=keydevstoreaccount1;BlobEndpoint=$blobEndpoint;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
        )

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
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath1,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath1,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            inputFilePath2,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath2,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath2,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            sourceBlobContainerMetadata,
        )

        assertThat(
            validationResults.size
        ).isEqualTo(2)
    }

    @Test
    fun `test validateSchemas - error encountered`() {
        val blobEndpoint = "http://${azuriteContainer1.host}:${
            azuriteContainer1.getMappedPort(
                10000
            )
        }/devstoreaccount1"
        val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;""" +
                """AccountKey=keydevstoreaccount1;BlobEndpoint=$blobEndpoint;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
        )

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "hl7_mapping/dev/foo/sender-transform2.yml",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            sourceBlobContainerMetadata,
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
        val blobEndpoint = "http://${azuriteContainer1.host}:${
            azuriteContainer1.getMappedPort(
                10000
            )
        }/devstoreaccount1"
        val sourceBlobContainerMetadata = BlobAccess.BlobContainerMetadata(
            "container1",
            """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;""" +
                """AccountKey=keydevstoreaccount1;BlobEndpoint=$blobEndpoint;QueueEndpoint=http://${azuriteContainer1.host}:${
                    azuriteContainer1.getMappedPort(
                        10001
                    )
                }/devstoreaccount1;"""
        )

        val inputFilePath = "hl7_mapping/dev/foo/input.fhir"
        val outputFilePath = "hl7_mapping/dev/foo/output.hl7"
        val transformFilePath = "hl7_mapping/dev/foo/sender-transform.yml"
        BlobAccess.uploadBlob(
            inputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            outputFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            transformFilePath,
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/fhirengine/translation/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        mockkConstructor(FhirToHl7Converter::class)
        every { anyConstructed<FhirToHl7Converter>().validate(any(), any()) } throws RuntimeException("Convert fail")

        val validationResults = TranslationSchemaManager().validateManagedSchemas(
            TranslationSchemaManager.SchemaType.HL7,
            sourceBlobContainerMetadata,
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