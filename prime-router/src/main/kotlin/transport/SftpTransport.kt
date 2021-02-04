package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level

class SftpTransport : ITransport {
    override fun send(
        transportType: TransportType,
        header: DatabaseAccess.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val sftpTransportType = transportType as SFTPTransportType
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port
        return try {
            if (header.content == null || header.orgSvc == null) error("No content or orgSvc to sftp, for report ${header.reportFile.reportId}")
            val (user, pass) = lookupCredentials(header.orgSvc.fullName)
            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            uploadFile(host, port, user, pass, sftpTransportType.filePath, fileName, header.content, context)
            val msg = "Success: sftp upload of $fileName to $sftpTransportType"
            context.logger.log(Level.INFO, msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                header.orgSvc,
                sentReportId,
                fileName,
                sftpTransportType.toString(),
                msg,
                header.reportFile.itemCount
            )
            null
        } catch (ioException: IOException) {
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to $sftpTransportType (orgService = ${header.orgSvc?.fullName ?: "null"})"
            context.logger.log(
                Level.WARNING, msg, ioException
            )
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
        host: String,
        port: String,
        user: String,
        pass: String,
        path: String,
        fileName: String,
        contents: ByteArray,
        context: ExecutionContext // TODO: temp fix to add logging
    ) {
        val sshClient = SSHClient()
        try {

            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connect(host, port.toInt())
            sshClient.authPassword(user, pass)

            var client = sshClient.newSFTPClient()
            client.getFileTransfer().setPreserveAttributes(false)
            try {

                client
                    .put(
                        object : InMemorySourceFile() {
                            override fun getName(): String {
                                return fileName
                            }

                            override fun getLength(): Long {
                                return contents.size.toLong()
                            }

                            override fun getInputStream(): InputStream {
                                return contents.inputStream()
                            }

                            override fun isDirectory(): Boolean {
                                return false
                            }

                            override fun isFile(): Boolean {
                                return true
                            }

                            override fun getPermissions(): Int {
                                return 777
                            }
                        },
                        path + "/" + fileName
                    )
                // TODO: remove this over logging when bug is fixed
                // context.logger.log(Level.INFO, "SFTP PUT succeeded: $fileName")
            } finally {
                client.close()
            }
        } finally {
            sshClient.disconnect()
            // context.logger.log(Level.INFO, "SFTP DISCONNECT succeeded: $fileName")
        }
    }
}