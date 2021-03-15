package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialManagement
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserPassCredential
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level

class SftpTransport : ITransport, CredentialManagement {
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val sftpTransportType = transportType as SFTPTransportType
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port
        return try {
            if (header.content == null)
                error("No content to sftp for report ${header.reportFile.reportId}")
            val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
            val (user, pass) = lookupCredentials(receiver.fullName)
            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            val sshClient = connect(host, port, user, pass)
            context.logger.log(Level.INFO, "Successfully connected to $sftpTransportType, ready to upload $fileName")
            uploadFile(sshClient, sftpTransportType.filePath, fileName, header.content)
            val msg = "Success: sftp upload of $fileName to $sftpTransportType"
            context.logger.log(Level.INFO, msg)
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
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to " +
                    "$sftpTransportType (orgService = ${header.receiver?.fullName ?: "null"})"
            context.logger.log(
                Level.WARNING, msg, ioException
            )
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        } // let non-IO exceptions be caught by the caller
    }

    companion object {
        fun lookupCredentials(receiverFullName: String): Pair<String, String> {

            val envVarLabel = orgName.replace(".", "__").replace('-', '_').toUpperCase()

            // Assumes credential will be cast as UserPassCredential, if not return null, and thus the error case
            val credential = credentialService.fetchCredential(
                envVarLabel, "SftpTransport", CredentialRequestReason.SFTP_UPLOAD
            ) as? UserPassCredential?
                ?: error("Unable to find SFTP credentials for $orgName connectionId($envVarLabel)")

            return Pair(credential.user, credential.pass)
        }

        fun connect(
            host: String,
            port: String,
            user: String,
            pass: String,
        ): SSHClient {
            val sshClient = SSHClient()
            try {
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(host, port.toInt())
                sshClient.authPassword(user, pass)
                return sshClient
            } catch (t: Throwable) {
                sshClient.disconnect()
                throw t
            }
        }

        /**
         * Call this after you have already successfully connect()ed.
         */
        fun uploadFile(
            sshClient: SSHClient,
            path: String,
            fileName: String,
            contents: ByteArray
        ) {
            try {
                val client = sshClient.newSFTPClient()
                client.fileTransfer.preserveAttributes = false
                client.use {
                    it.put(makeSourceFile(contents, fileName), "$path/$fileName")
                }
            } finally {
                sshClient.disconnect()
            }
        }

        fun ls(sshClient: SSHClient, path: String): List<String> {
            try {
                val client = sshClient.newSFTPClient()
                client.fileTransfer.preserveAttributes = false
                client.use {
                    val lsResponse = it.ls(path)
                    return lsResponse.map { it.toString() }.toList()
                }
            } finally {
                sshClient.disconnect()
            }
        }

        private fun makeSourceFile(contents: ByteArray, fileName: String): LocalSourceFile {
            return object : InMemorySourceFile() {
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
            }
        }
    }
}