package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.isEqualTo
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
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.CredentialService
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.credentials.UserPemCredential
import gov.cdc.prime.router.credentials.UserPpkCredential
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.ConnectionException
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.method.AuthMethod
import net.schmizz.sshj.xfer.LocalSourceFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.TimeoutException

class SftpTransportIntegrationTests : TransportIntegrationTests() {

    /**
     * The collection of objects and values required for each test.
     *
     * Wrapping them in a class allows us to have a fresh set per test
     * in case there are issues with mutability.
     */
    private inner class Fixture {
        val transport = spyk(SftpTransport())
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val metadata = Metadata.getInstance()
        val reportId = UUID.randomUUID()

        val credentialName = "DEFAULT-SFTP"
        val transportType = SFTPTransportType(
            host = "sftp",
            port = "22",
            filePath = "./upload",
            credentialName = credentialName
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

        val lsPath = "./path"
        val remoteResourceInfos = listOf("a", "b", "c").map {
            mockk<RemoteResourceInfo> {
                every { this@mockk.toString() } returns "$it.txt"
            }
        }
        val remoteFiles = listOf("a.txt", "b.txt", "c.txt")

        val rmPath = "./path"
        val rmFile = "a.txt"

        val successSFTPUpload = "Success: sftp upload"
        val failedSFTPUpload = "FAILED Sftp upload"

        val mockSSHClient = mockk<SSHClient>()
        val mockSFTPClient = mockk<SFTPClient>()
        val mockCredentialService = mockk<CredentialService>()
    }

    @BeforeEach
    fun setUp() {
        mockkObject(CredentialHelper)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `happy path with user pass credentials`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { CredentialHelper.getCredentialService() } returns f.mockCredentialService
        // return user/pass credentials for this call to authenticate with user/pass
        every {
            f.mockCredentialService.fetchCredential(
                f.credentialName,
                "SftpTransport",
                CredentialRequestReason.SFTP_UPLOAD
            )
        } returns UserPassCredential("foo", "pass")
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

        // successful SFTP upload
        assertThat(f.actionHistory.action.actionResult).startsWith(f.successSFTPUpload)
        assertThat(retry).isNull()
    }

    @Test
    fun `happy path with PEM credentials`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { CredentialHelper.getCredentialService() } returns f.mockCredentialService
        // return PEM credentials for this call to authenticate with PEM
        every {
            f.mockCredentialService.fetchCredential(
                f.credentialName,
                "SftpTransport",
                CredentialRequestReason.SFTP_UPLOAD
            )
        } returns UserPemCredential("user", "key", "keyPass", "pass")
        every { SftpTransport.createDefaultSSHClient() } returns f.mockSSHClient
        every { f.mockSSHClient.addHostKeyVerifier(any<HostKeyVerifier>()) } just runs
        every { f.mockSSHClient.connect(f.transportType.host, f.transportType.port.toInt()) } just runs
        every { f.mockSSHClient.auth("user", any<List<AuthMethod>>()) } just runs
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

        // successful SFTP upload
        assertThat(f.actionHistory.action.actionResult).startsWith(f.successSFTPUpload)
        assertThat(retry).isNull()
    }

    @Test
    fun `happy path with PPK credentials`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { CredentialHelper.getCredentialService() } returns f.mockCredentialService
        // return PKP credentials for this call to authenticate with PKP
        every {
            f.mockCredentialService.fetchCredential(
                f.credentialName,
                "SftpTransport",
                CredentialRequestReason.SFTP_UPLOAD
            )
        } returns UserPpkCredential("user", "key", "keyPass", "pass")
        every { SftpTransport.createDefaultSSHClient() } returns f.mockSSHClient
        every { f.mockSSHClient.addHostKeyVerifier(any<HostKeyVerifier>()) } just runs
        every { f.mockSSHClient.connect(f.transportType.host, f.transportType.port.toInt()) } just runs
        every { f.mockSSHClient.auth("user", any<List<AuthMethod>>()) } just runs
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

        // successful SFTP upload
        assertThat(f.actionHistory.action.actionResult).startsWith(f.successSFTPUpload)
        assertThat(retry).isNull()
    }

    @Test
    fun `no content in header error`() {
        val f = Fixture()

        val headerWithNullContent = f.header.copy(
            content = null
        )

        val retry = f.transport.send(
            f.transportType,
            headerWithNullContent,
            f.reportId,
            null,
            context,
            f.actionHistory
        )

        // asserts that the initial null check works
        assertThat(f.actionHistory.action.actionResult).startsWith(f.failedSFTPUpload)
        assertThat(retry).isEqualTo(RetryToken.allItems)
    }

    @Test
    fun `no credentials found for reciever error`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { CredentialHelper.getCredentialService() } returns f.mockCredentialService
        every {
            f.mockCredentialService.fetchCredential(
                f.credentialName,
                "SftpTransport",
                CredentialRequestReason.SFTP_UPLOAD
            )
        } returns null

        val retry = f.transport.send(
            f.transportType,
            f.header,
            f.reportId,
            null,
            context,
            f.actionHistory
        )

        // asserts that missing credentials will fail SFTP
        assertThat(f.actionHistory.action.actionResult).startsWith(f.failedSFTPUpload)
        assertThat(retry).isEqualTo(RetryToken.allItems)
    }

    @Test
    fun `authentication error`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { CredentialHelper.getCredentialService() } returns f.mockCredentialService
        // returns basic username/pass word
        every {
            f.mockCredentialService.fetchCredential(
                f.credentialName,
                "SftpTransport",
                CredentialRequestReason.SFTP_UPLOAD
            )
        } returns UserPassCredential("foo", "pass")
        every { SftpTransport.createDefaultSSHClient() } returns f.mockSSHClient
        every { f.mockSSHClient.addHostKeyVerifier(any<HostKeyVerifier>()) } just runs
        // throws authentication exception
        every {
            f.mockSSHClient.connect(f.transportType.host, f.transportType.port.toInt())
        } throws UserAuthException("bad password")
        every { f.mockSSHClient.disconnect() } just runs

        val retry = f.transport.send(
            f.transportType,
            f.header,
            f.reportId,
            null,
            context,
            f.actionHistory
        )

        // asserts that authentication error will result in error
        assertThat(f.actionHistory.action.actionResult).startsWith(f.failedSFTPUpload)
        assertThat(retry).isEqualTo(RetryToken.allItems)
    }

    @Test
    fun `invalid credential type error`() {
        val f = Fixture()

        mockkObject(SftpTransport.Companion)

        every { CredentialHelper.getCredentialService() } returns f.mockCredentialService
        // returns a credential type that does not apply to SFTP servers
        every {
            f.mockCredentialService.fetchCredential(
                f.credentialName,
                "SftpTransport",
                CredentialRequestReason.SFTP_UPLOAD
            )
        } returns UserApiKeyCredential("user", "apiKey")

        val retry = f.transport.send(
            f.transportType,
            f.header,
            f.reportId,
            null,
            context,
            f.actionHistory
        )

        // asserts that invalid credential types will result in error
        assertThat(f.actionHistory.action.actionResult).startsWith(f.failedSFTPUpload)
        assertThat(retry).isEqualTo(RetryToken.allItems)
    }

    @Test
    fun `uploadFile happy path`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        every { f.mockSFTPClient.fileTransfer.preserveAttributes = false } just runs
        // mock a successful file upload
        every { f.mockSFTPClient.put(any<LocalSourceFile>(), "${f.transportType.filePath}/${f.fileName}") } just runs
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // this function returns Unit so a successful run means no exception was thrown
        assertDoesNotThrow {
            SftpTransport.uploadFile(f.mockSSHClient, f.transportType.filePath, f.fileName, f.contents.toByteArray())
        }
    }

    @Test
    fun `uploadFile connection error`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        every { f.mockSFTPClient.fileTransfer.preserveAttributes = false } just runs
        // throw a connection exception on upload
        every {
            f.mockSFTPClient.put(any<LocalSourceFile>(), "${f.transportType.filePath}/${f.fileName}")
        } throws ConnectionException("oops")
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // throw the exception coming from the SSHClient library
        assertThrows<ConnectionException> {
            SftpTransport.uploadFile(f.mockSSHClient, f.transportType.filePath, f.fileName, f.contents.toByteArray())
        }
    }

    @Test
    fun `uploadFile ignore timeout error`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        every { f.mockSFTPClient.fileTransfer.preserveAttributes = false } just runs
        // throw a ConnectionException with TimeoutException as a cause
        every {
            f.mockSFTPClient.put(any<LocalSourceFile>(), "${f.transportType.filePath}/${f.fileName}")
        } throws ConnectionException("oops", TimeoutException())
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // TimeoutException is expected with some slower SFTP servers so consider this OK
        assertDoesNotThrow {
            SftpTransport.uploadFile(f.mockSSHClient, f.transportType.filePath, f.fileName, f.contents.toByteArray())
        }
    }

    @Test
    fun `ls happy path`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        // mock a successful ls on the remote SFTP server
        every { f.mockSFTPClient.ls(f.lsPath, null) } returns f.remoteResourceInfos
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        val results = SftpTransport.ls(f.mockSSHClient, f.lsPath)

        // assert the mapping to filenames worked as expected
        assertThat(results).isEqualTo(f.remoteFiles)
    }

    @Test
    fun `ls connection error`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        // throw a connection exception on ls
        every { f.mockSFTPClient.ls(f.lsPath, null) } throws ConnectionException("oops")
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // throw the exception coming from the SSHClient library
        assertThrows<ConnectionException> {
            SftpTransport.ls(f.mockSSHClient, f.lsPath)
        }
    }

    @Test
    fun `ls ignore timeout error`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        // throw a ConnectionException on ls with TimeoutException as a cause
        every { f.mockSFTPClient.ls(f.lsPath, null) } throws ConnectionException("oops", TimeoutException())
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // TimeoutException is expected with some slower SFTP servers so consider this OK
        // the return value may or may not be set if the exception was thrown while trying to close the connection
        assertDoesNotThrow {
            SftpTransport.ls(f.mockSSHClient, f.lsPath)
        }
    }

    @Test
    fun `rm happy path`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        // mock a successful rm on the remote SFTP server
        every { f.mockSFTPClient.rm("${f.rmPath}/${f.rmFile}") } just runs
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // assert the call throws no exceptions
        assertDoesNotThrow {
            SftpTransport.rm(f.mockSSHClient, f.rmPath, f.rmFile)
        }
    }

    @Test
    fun `rm connection error`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        // throw a connection exception on rm
        every { f.mockSFTPClient.rm("${f.rmPath}/${f.rmFile}") } throws ConnectionException("oops")
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // throw the exception coming from the SSHClient library
        assertThrows<ConnectionException> {
            SftpTransport.rm(f.mockSSHClient, f.rmPath, f.rmFile)
        }
    }

    @Test
    fun `rm ignore timeout error`() {
        val f = Fixture()

        every { f.mockSSHClient.newSFTPClient() } returns f.mockSFTPClient
        // throw a ConnectionException on rm with TimeoutException as a cause
        every { f.mockSFTPClient.rm("${f.rmPath}/${f.rmFile}") } throws ConnectionException("oops", TimeoutException())
        every { f.mockSFTPClient.close() } just runs
        every { f.mockSSHClient.close() } just runs
        every { f.mockSSHClient.disconnect() } just runs

        // TimeoutException is expected with some slower SFTP servers so consider this OK
        assertDoesNotThrow {
            SftpTransport.rm(f.mockSSHClient, f.rmPath, f.rmFile)
        }
    }
}