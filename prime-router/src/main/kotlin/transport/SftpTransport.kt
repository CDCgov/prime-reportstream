package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.SftpCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.credentials.UserPpkCredential
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.StatefulSFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import java.io.InputStream
import java.io.StringReader
import java.util.logging.Level

class SftpTransport : ITransport {
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
            val credential = lookupCredentials(receiver.fullName)
            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            val sshClient = connect(host, port, credential)
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
        } catch (t: Throwable) {
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to " +
                    "$sftpTransportType (orgService = ${header.receiver?.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.log(Level.WARNING, msg, t)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object {
        fun lookupCredentials(receiverFullName: String): SftpCredential {
            val credentialLabel = receiverFullName
                .replace(".", "--")
                .replace("_", "-")
                .toUpperCase()

            // Assumes credential will be cast as SftpCredential, if not return null, and thus the error case
            return CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "SftpTransport", CredentialRequestReason.SFTP_UPLOAD
            ) as? SftpCredential?
                ?: error("Unable to find SFTP credentials for $receiverFullName connectionId($credentialLabel)")
        }

        fun connect(
            host: String,
            port: String,
            credential: SftpCredential,
        ): SSHClient {
            // create our client
            val sshClient = SSHClient()
            try {
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(host, port.toInt())
                when (credential) {
                    is UserPassCredential -> sshClient.authPassword(credential.user, credential.pass)
                    is UserPpkCredential -> PuTTYKeyFile().init(StringReader(credential.key))
                    else -> error("Unknown SftpCredential ${credential::class.simpleName}")
                }
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
                client.use {
                    val lsResponse = it.ls(path)
                    return lsResponse.map { l -> l.toString() }.toList()
                }
            } finally {
                sshClient.disconnect()
            }
        }

        fun pwd(sshClient: SSHClient): String {
            try {
                val client = sshClient.newStatefulSFTPClient() as StatefulSFTPClient
                sshClient.timeout = 120000
                client.use {
                    return it.pwd()
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