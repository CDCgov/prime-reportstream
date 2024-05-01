// TestcontainersUtils.kt
package gov.cdc.prime.router.common

import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.io.File
import java.nio.file.Path

object TestcontainersUtils {
    private val AZURITE_DOCKERFILE: Path = File("../.environment/docker/docker-compose/Dockerfile.azurite").toPath()

    private val azuriteContainers: MutableMap<Pair<String, Map<String, String>>, GenericContainer<*>> = mutableMapOf()

    fun createAzuriteContainer(
        customImageName: String,
        customEnv: Map<String, String> = emptyMap(),
    ): GenericContainer<*> {
        val version = getVersionFromDockerfile(AZURITE_DOCKERFILE)
        val imageName = "$customImageName:$version"
        val newContainer = GenericContainer(
            ImageFromDockerfile(imageName)
                .withDockerfile(AZURITE_DOCKERFILE)
        )
            .withExposedPorts(10000, 10001, 10002)

        // Apply custom environment variables
        customEnv.forEach { (key, value) ->
            newContainer.withEnv(key, value)
        }
        return newContainer
    }

    private const val POSTGRES_IMAGE_NAME = "postgres"
    private val POSTGRES_DOCKERFILE: Path = File("../.environment/docker/docker-compose/Dockerfile.postgres").toPath()

    fun createPostgresContainer(): String {
        val version = getVersionFromDockerfile(POSTGRES_DOCKERFILE)
        return "$POSTGRES_IMAGE_NAME:$version"
    }

    private fun getVersionFromDockerfile(dockerfilePath: Path): String {
        val dockerfile = dockerfilePath.toFile().readText()
        val versionRegex = Regex("FROM .*:([\\w.-]+)")
        val matchResult = versionRegex.find(dockerfile)
        return matchResult?.groupValues?.get(1) ?: "latest"
    }
}