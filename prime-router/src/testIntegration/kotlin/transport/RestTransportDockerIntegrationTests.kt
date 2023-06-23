package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.startsWith
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.io.path.Path

@Testcontainers
class RestTransportDockerIntegrationTests : TransportIntegrationTests() {

    @Container
    var server = GenericContainer(
        ImageFromDockerfile().withDockerfile(Path("./src/testIntegration/resources/resttransport/rest.Dockerfile"))
    ).withExposedPorts(80)

    private inner class Fixture(private val host: String, private val port: Int) {
        val restTransport = spyk(RESTTransport())
        val metadata = Metadata.getInstance()
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)

        val actionHistory = ActionHistory(TaskAction.send)
        val transportType = RESTTransportType(
            "http://$host:$port/report",
            "http://$host:$port/auth",
            null,
            mapOf("mock-h1" to "value-h1", "mock-h2" to "value-h2")
        )
        val task = Task(
            reportId,
            TaskAction.send,
            null,
            "standard.standard-covid-19",
            "az-phd.elr-test",
            4,
            "",
            "",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )

        val content = "HL7|Stuff"

        val receiver = settings
            .findReceiver("ignore.REST_TEST")!!
            .copy(
                transport = RESTTransportType(
                    "http://$host:$port/report",
                    "http://$host:$port/auth",
                    headers = emptyMap()
                )
            )

        val header = WorkflowEngine.Header(
            task,
            reportFile,
            null,
            settings.findOrganization("ignore"),
            receiver,
            metadata.findSchema("covid-19"),
            content = content.toByteArray(),
            true
        )
    }

    @Test
    fun `jamie is cool`() {
        val f = Fixture(server.host, server.firstMappedPort)

        every { f.restTransport.lookupDefaultCredential(any()) }.returns(
            UserApiKeyCredential(
                "test-user",
                "test-key"
            )
        )

        val retry = f.restTransport.send(
            f.transportType,
            f.header,
            reportId,
            null,
            context,
            f.actionHistory
        )

        assertThat(f.actionHistory.action.httpStatus).isEqualTo(200)
        assertThat(f.actionHistory.action.actionResult).startsWith("Success")
        assertThat(retry).isNull()
    }
}