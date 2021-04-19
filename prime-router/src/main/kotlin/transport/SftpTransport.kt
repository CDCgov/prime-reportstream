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
import gov.cdc.prime.router.credentials.SftpCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.credentials.UserPpkCredential
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.StatefulSFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.userauth.password.PasswordUtils
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.InputStream
import java.io.StringReader

class SftpTransport : ITransport, Logging {
    class SftpSession(
        host: String,
        port: String,
        credential: SftpCredential,
    ): TransportSession, Logging {
        val sshClient: SSHClient = SSHClient()

        init {
            // create our client
            try {
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(host, port.toInt())
                when (credential) {
                    is UserPassCredential -> sshClient.authPassword(credential.user, credential.pass)
                    is UserPpkCredential -> {
                        val key = PuTTYKeyFile()
                        val keyContents = StringReader(credential.key)
                        when (StringUtils.isBlank(credential.keyPass)) {
                            true -> key.init(keyContents)
                            false -> key.init(keyContents, PasswordUtils.createOneOff(credential.keyPass.toCharArray()))
                        }
                        sshClient.authPublickey(credential.user, key)
                    }
                    else -> error("Unknown SftpCredential ${credential::class.simpleName}")
                }
            } catch (t: Throwable) {
                sshClient.disconnect()
                throw t
            }
        }

        override fun close() {
            sshClient.disconnect()
            sshClient.close()
        }
    }

    override fun startSession(receiver: Receiver): TransportSession? {
        val credential = lookupCredentials(receiver.fullName)
        val sftpTransportType = receiver.transport as SFTPTransportType
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port
        return SftpSession(host, port, credential)
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: TransportSession?,
        actionHistory: ActionHistory
    ): RetryItems? {
        val sftpTransportType = header.receiver?.transport as SFTPTransportType
        val sftpSession = session as SftpSession
        return try {
            if (header.content == null)
                error("No content to sftp for report ${header.reportFile.reportId}")
            val receiver = header.receiver

            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            logger.info( "Successfully connected to $sftpTransportType, ready to upload $fileName")
            uploadFile(sftpSession.sshClient, sftpTransportType.filePath, fileName, header.content)
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
                    "$sftpTransportType (orgService = ${header.receiver.fullName})" +
                    ", Exception: ${t.localizedMessage}"
            logger.warn(msg, t)
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

        /**
         * Call this after you have already successfully connect()ed.
         */
        fun uploadFile(
            sshClient: SSHClient,
            path: String,
            fileName: String,
            contents: ByteArray
        ) {
            val client = sshClient.newSFTPClient()
            client.fileTransfer.preserveAttributes = false
            client.use {
                it.put(makeSourceFile(contents, fileName), "$path/$fileName")
            }
        }

        fun ls(sshClient: SSHClient, path: String): List<String> {
            val client = sshClient.newSFTPClient()
            client.use {
                val lsResponse = it.ls(path)
                return lsResponse.map { l -> l.toString() }.toList()
            }
        }

        fun pwd(sshClient: SSHClient): String {
            val client = sshClient.newStatefulSFTPClient() as StatefulSFTPClient
            sshClient.timeout = 120000
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