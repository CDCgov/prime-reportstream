package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level

class SftpTransport : ITransport {
    override fun send(
        orgService: OrganizationService,
        transportType: TransportType,
        contents: ByteArray,
        reportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext
    ): RetryItems? {
        val sftpTransportType = transportType as SFTPTransportType
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port
        return try {
            val (user, pass) = lookupCredentials(orgService.fullName)
            val extension = orgService.format.toExt()
            val fileName = "${orgService.fullName.replace('.', '-')}-$reportId.$extension"

            context.logger.log(Level.INFO, "About to sftp upload $fileName to $user at $host:$port (orgService = ${orgService.fullName})")
            uploadFile(host, port, user, pass, sftpTransportType.filePath, fileName, contents)
            context.logger.log(Level.INFO, "Successful sftp upload of $fileName")
            null
        } catch (ioException: IOException) {
            context.logger.log(Level.WARNING, "FAILED Sftp upload of reportId $reportId to $host:$port  (orgService = ${orgService.fullName})\"")
            RetryToken.allItems
        } // let non-IO exceptions be caught by the caller
    }

    private fun lookupCredentials(orgName: String): Pair<String, String> {

        val envVarLabel = orgName.replace(".", "__").replace('-', '_').toUpperCase()

        val user = System.getenv("${envVarLabel}__USER") ?: ""
        val pass = System.getenv("${envVarLabel}__PASS") ?: ""

        if (user.isBlank() || pass.isBlank())
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