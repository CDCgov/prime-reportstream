package gov.cdc.prime.router.transport

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserPassCredential
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.StatefulSFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import org.apache.logging.log4j.kotlin.Logging
import java.io.InputStream


private const val SSH_CLIENT_TIMEOUT = 120000 // milliseconds

class SftpTransport : ITransport, Logging {
    override fun startSession(receiver: Receiver): TransportSession? {
        val transport = receiver.transport as? SFTPTransportType ?: error("Internal Error: expected SFTPTransport")
        val (user, password) = lookupCredentials(receiver.fullName)
        val session = SftpSession(transport.host, transport.port, user, password)
        logger.info("Successfully connected to $transport, ready to upload")
        return session
    }

    class SftpSession(val host: String, val port: String, val user: String, val pass: String) : TransportSession {
        val sshClient: SSHClient = SSHClient()

        fun connect() {
            try {
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(host, port.toInt())
                sshClient.authPassword(user, pass)
                sshClient.timeout = SSH_CLIENT_TIMEOUT
            } catch (t: Throwable) {
                sshClient.disconnect()
                throw t
            }
        }

        override fun close() {
            sshClient.close()
            sshClient.disconnect()
        }
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: TransportSession?,
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
        } catch (t: Throwable) {
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to " +
                    "$sftpTransportType (orgService = ${header.receiver.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            logger.warn(msg, t)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object {
        fun lookupCredentials(receiverFullName: String): Pair<String, String> {
            val credentialLabel = receiverFullName
                .replace(".", "--")
                .replace("_", "-")
                .toUpperCase()

            // Assumes credential will be cast as UserPassCredential, if not return null, and thus the error case
            val credential = CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "SftpTransport", CredentialRequestReason.SFTP_UPLOAD
            ) as? UserPassCredential?
                ?: error("Unable to find SFTP credentials for $receiverFullName connectionId($credentialLabel)")

            return Pair(credential.user, credential.pass)
        }

        fun uploadFile(
            session: SftpSession,
            path: String,
            fileName: String,
            contents: ByteArray
        ) {
            val client = session.sshClient.newSFTPClient()
            client.fileTransfer.preserveAttributes = false
            client.use {
                it.put(makeSourceFile(contents, fileName), "$path/$fileName")
            }
        }

        fun ls(session: SftpSession, path: String): List<String> {
            val client = session.sshClient.newSFTPClient()
            client.use {
                val lsResponse = it.ls(path)
                return lsResponse.map { l -> l.toString() }.toList()
            }
        }

        fun pwd(session: SftpSession): String {
            val client = session.sshClient.newStatefulSFTPClient() as StatefulSFTPClient
            client.use {
                return it.pwd()
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