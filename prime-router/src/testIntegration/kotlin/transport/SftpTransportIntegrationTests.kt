package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.startsWith
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.UserPassCredential
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.xfer.LocalSourceFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SftpTransportIntegrationTests : TransportIntegrationTests() {

    private inner class Fixture {
        val transport = spyk(SftpTransport())
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val metadata = Metadata.getInstance()
        val reportId = UUID.randomUUID()

        val transportType = SFTPTransportType(
            host = "sftp",
            port = "22",
            filePath = "./upload",
            credentialName = "DEFAULT-SFTP"
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
        val contents = "HL7|Stuff"
        val header = WorkflowEngine.Header(
            task,
            reportFile,
            null,
            settings.findOrganization("ignore"),
            settings.findReceiver("ignore.SFTP_TEST"),
            metadata.findSchema("covid-19"),
            content = contents.toByteArray(),
            true
        )
        val fileName = Report.formExternalFilename(header)
        val actionHistory = ActionHistory(TaskAction.send)

        val mockSSHClient = mockk<SSHClient>()
        val mockSFTPClient = mockk<SFTPClient>()
    }

    @BeforeAll
    fun setUp() {
        // set up creds in memory
        CredentialHelper.getCredentialService().saveCredential(
            "DEFAULT-SFTP",
            UserPassCredential("foo", "pass"),
            "SftpTransportIntegrationTests"
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SftpTransport.Companion)
    }

    @Test
    fun `happy path`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { SftpTransport.createDefaultSSHClient() } returns f.mockSSHClient
        every { f.mockSSHClient.addHostKeyVerifier(any<HostKeyVerifier>()) } just runs
        every { f.mockSSHClient.connect(f.transportType.host, f.transportType.port.toInt()) } just runs
        every { f.mockSSHClient.authPassword("foo", "pass") } just runs
        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        every { f.mockSFTPClient.fileTransfer.preserveAttributes = false } just runs
        every { f.mockSFTPClient.put(any<LocalSourceFile>(), "${f.transportType.filePath}/${f.fileName}") } just runs
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        val retry = f.transport.send(
            f.transportType,
            f.header,
            f.reportId,
            null,
            context,
            f.actionHistory
        )

        assertThat(f.actionHistory.action.actionResult).startsWith("Success: sftp upload")
        assertThat(retry).isNull()
    }
}