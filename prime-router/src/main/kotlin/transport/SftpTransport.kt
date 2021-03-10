package gov.cdc.prime.router.transport

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import org.apache.logging.log4j.kotlin.Logging
import java.io.Closeable
import java.io.IOException
import java.io.InputStream

class SftpTransport : ITransport, Logging {
    override fun startSession(receiver: Receiver): Closeable? {
        val transport = receiver.transport as? SFTPTransportType ?: error("Internal Error: expected SFTPTransport")
        val (user, password) = lookupCredentials(receiver.organizationName)
        return SftpSession(transport.host, transport.port, user, password)
    }

    class SftpSession(host: String, port: String, user: String, pass: String) : Closeable {
        private val sshClient: SSHClient = SSHClient()
        val client: SFTPClient

        init {
            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connect(host, port.toInt())
            sshClient.authPassword(user, pass)
            client = sshClient.newSFTPClient()
            client.fileTransfer.preserveAttributes = false
        }

        override fun close() {
            client.close()
            sshClient.disconnect()
        }
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: Any?,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val sftpTransportType = receiver.transport as SFTPTransportType
        val sftpSession = session as? SftpSession ?: error("Internal Error: Expected a SFTP Session")
        if (header.content == null) error("No content to sftp for report $sentReportId")

        return try {
            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            uploadFile(sftpSession, sftpTransportType.filePath, fileName, header.content)
            val msg = "Success: sftp upload of $fileName to $sftpTransportType"
            logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                fileName,
                sftpTransportType.toString(),
                msg,
                header.reportFile.itemCount
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (ioException: IOException) {
            val msg = "FAILED Sftp upload of inputReportId $sentReportId to $sftpTransportType " +
                "(orgService = ${receiver.fullName})"
            logger.warn(msg, ioException)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
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
        session: SftpSession,
        path: String,
        fileName: String,
        contents: ByteArray
    ) {
        session.client.put(makeSourceFile(contents, fileName), "$path/$fileName")
    }

    private fun makeSourceFile(contents: ByteArray, fileName: String): LocalSourceFile {
        return object : InMemorySourceFile() {
            override fun getName(): String { return fileName }
            override fun getLength(): Long { return contents.size.toLong() }
            override fun getInputStream(): InputStream { return contents.inputStream() }
            override fun isDirectory(): Boolean { return false }
            override fun isFile(): Boolean { return true }
            override fun getPermissions(): Int { return 777 }
        }
    }
}