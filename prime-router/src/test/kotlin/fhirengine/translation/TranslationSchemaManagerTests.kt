package gov.cdc.prime.router.fhirengine.translation

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.BlobAccess
import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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

        BlobAccess.uploadBlob(
            "dev/bar/input.fhir",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/azure/resources/validationTests/FHIR_to_FHIR/input.fhir"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "dev/bar/output.fhir",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/azure/resources/validationTests/FHIR_to_FHIR/output.fhir"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "dev/bar/simple-transform.yml",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/azure/resources/validationTests/FHIR_to_FHIR/simple-transform.yml"
            )
                .inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        assertThat(
            TranslationSchemaManager().validateSchemas(
                "dev/bar",
                sourceBlobContainerMetadata,
                Report.Format.FHIR,
                blobEndpoint,
            )
        ).isEqualTo(true)
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

        BlobAccess.uploadBlob(
            "dev/foo/input.fhir",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/azure/resources/validationTests/FHIR_to_HL7/input.fhir"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "dev/foo/output.hl7",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/azure/resources/validationTests/FHIR_to_HL7/output.hl7"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        BlobAccess.uploadBlob(
            "dev/foo/sender-transform.yml",
            File(
                Paths.get("").toAbsolutePath().toString() +
                    "/src/test/kotlin/azure/resources/validationTests/FHIR_to_HL7/sender-transform.yml"
            ).inputStream().readAllBytes(),
            sourceBlobContainerMetadata
        )

        assertThat(
            TranslationSchemaManager().validateSchemas(
                "dev/foo",
                sourceBlobContainerMetadata,
                Report.Format.HL7,
                blobEndpoint
            )
        ).isEqualTo(true)
    }
}