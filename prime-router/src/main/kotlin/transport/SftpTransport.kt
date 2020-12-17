package gov.cdc.prime.router.transport

import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.DatabaseAccess
import java.io.InputStream
import java.util.Properties

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile

class SftpTransport : ITransport {

    override fun send(
        orgName: String,
        transportType: TransportType,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean {

        val sftpTransportType = transportType as SFTPTransportType

        val (user, pass) = lookupCredentials(orgName)

        val fileDir = sftpTransportType.filePath.removeSuffix("/")

        val path = "$fileDir/${orgName.replace('.', '-')}-${header.task.reportId}.csv"
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port

        var success: Boolean

            uploadFile(host, port, user, pass, path, contents )
            success = true

        return success
    }

    private fun lookupCredentials(orgName: String): Pair<String, String> {

        val envVarLabel = orgName.replace(".", "__").replace('-', '_').toUpperCase()

        val user = System.getenv("${envVarLabel}__USER") ?: ""
        val pass = System.getenv("${envVarLabel}__PASS") ?: ""

        if (user.isNullOrBlank() || pass.isNullOrBlank())
            error("Unable to find SFTP credentials for $orgName")

        return Pair(user, pass)
    }

    private fun setupSshj(
        host: String,
        port: String,
        user: String,
        pass: String
    ): SSHClient {

        val client:SSHClient = SSHClient();
        client.addHostKeyVerifier(PromiscuousVerifier());
        client.connect(host,port.toInt());
        client.authPassword(user, pass);
        return client;
    }

    private fun uploadFile(
        host: String,
        port: String,
        user: String,
        pass: String,
        path: String,
        contents: ByteArray
    ) {
        val sshClient: SSHClient = setupSshj(host, port, user, pass);
        val sftpClient:SFTPClient = sshClient.newSFTPClient();

        val inMemoryFile = object : InMemorySourceFile() {
            override fun getName(): String { return "test.csv" }
            override fun getLength() : Long { return contents.size.toLong() }
            override fun getInputStream() : InputStream {return contents.inputStream()}
        }
 
        sftpClient.put(inMemoryFile, path);
 
         sftpClient.close();
        sshClient.disconnect();
    }
}