package gov.cdc.prime.router.transport

import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.DatabaseAccess
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.InputStream

class SftpTransport : ITransport {

    override fun send(
        orgName: String,
        transportType: TransportType,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean {

        val sftpTransportType = transportType as SFTPTransportType

        val (user, pass) = lookupCredentials(orgName)

        val fileName = "${orgName.replace('.', '-')}-${header.task.reportId}.csv"
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port

        var success: Boolean
        try {
            uploadFile(host, port, user, pass, sftpTransportType.filePath, fileName, contents)
            success = true
        } catch (e: Exception) {

            success = false
            System.out.println(e)
        }

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

    private fun uploadFile(
        host: String,
        port: String,
        user: String,
        pass: String,
        path: String,
        fileName: String,
        contents: ByteArray
    ) {
        val sshClient = SSHClient()
        try {

            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connect(host, port.toInt())
            sshClient.authPassword(user, pass)

            var client = sshClient.newSFTPClient()
            try {
                client.put(
                    object : InMemorySourceFile() {
                        override fun getName(): String { return fileName }
                        override fun getLength(): Long { return contents.size.toLong() }
                        override fun getInputStream(): InputStream { return contents.inputStream() }
                    },
                    path
                )
            } finally {
                client.close()
            }
        } finally {
            sshClient.disconnect()
        }
    }
}