package gov.cdc.prime.router.transport

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPLegacyTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserPassCredential
import org.apache.logging.log4j.kotlin.KotlinLogger
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream

class SftpLegacyTransport : ITransport, Logging {
    class SftpLegacySession(
        val user: String,
        val pass: String,
        val host: String,
        val port: String,
        timeoutMs: Int? = null
        ): TransportSession, Logging {
        val session: Session

        init {
            val jsch = JSch()
            logger.info("--->>> LEGACY SFTP: created jsch object")
            val session = jsch.getSession(user, host, port.toInt())
            logger.info("--->>> LEGACY SFTP: got jsch session")
            session.setPassword(pass)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(timeoutMs ?: DEFAULT_TIMEOUT_IN_MS)
            this.session = session
        }

        override fun close() {
            session.disconnect()
        }
    }

    override fun startSession(receiver: Receiver): TransportSession? {
        val transport = receiver.transport as SFTPLegacyTransportType
        val (user, pass) = lookupCredentials(receiver.fullName)
        return SftpLegacySession(user = user, pass = pass, host = transport.host, port = transport.port)
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: TransportSession?,
        actionHistory: ActionHistory
    ): RetryItems? {
        val sftpTransportType = header.receiver?.transport as SFTPLegacyTransportType
        val legacySession = session as SftpLegacySession
        return try {
            if (header.content == null)
                error("No content to sftp for report ${header.reportFile.reportId}")
            val receiver = header.receiver
            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            logger.info("Successfully connected to $sftpTransportType, ready to upload $fileName")
            uploadFile(legacySession.session, sftpTransportType.filePath, fileName, header.content, logger = logger)
            val msg = "Success: sftp legacy upload of $fileName to $sftpTransportType"
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
                "FAILED Sftp Legacy upload of inputReportId ${header.reportFile.reportId} to " +
                    "$sftpTransportType (orgService = ${header.receiver.fullName})" +
                    ", Exception: ${t.localizedMessage}"
            logger.warn(msg, t)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_IN_MS: Int = 120_000

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
            session: Session,
            path: String,
            fileName: String,
            contents: ByteArray,
            timeoutMs: Int? = null,
            logger: KotlinLogger? = null
        ) {
            val sftp = session.openChannel("sftp")
            logger?.info("--->>> LEGACY SFTP: opening sftp channel")
            sftp.connect(timeoutMs ?: DEFAULT_TIMEOUT_IN_MS)
            val sftpChannel = sftp as ChannelSftp
            sftpChannel.cd(path)
            val byteArrayStream = ByteArrayInputStream(contents)
            sftp.put(byteArrayStream, fileName, 777)
        }

        fun ls(
            session: Session,
            path: String? = null,
            timeoutMs: Int? = null,
            logger: KotlinLogger? = null
        ): List<String> {
            val sftp = session.openChannel("sftp")
            logger?.info("--->>> LEGACY SFTP: opening sftp channel")
            sftp.connect(timeoutMs ?: DEFAULT_TIMEOUT_IN_MS)
            val sftpChannel = sftp as ChannelSftp
            // if we don't pass in a path, use PWD
            val dir = if (path.isNullOrEmpty()) {
                logger?.info("--->>> LEGACY SFTP: calling pwd")
                sftpChannel.pwd()
            } else {
                logger?.info("--->>> LEGACY SFTP: using path $path")
                path
            }
            logger?.info("--->>> LEGACY SFTP: calling ls")
            val ls = sftpChannel.ls(dir)
            return ls.map {
                it.toString()
            }
        }

        fun pwd(session: Session, timeoutMs: Int? = null, logger: KotlinLogger? = null): String {
            val sftp = session.openChannel("sftp")
            logger?.info("--->>> LEGACY SFTP: opening sftp channel")
            sftp.connect(timeoutMs ?: DEFAULT_TIMEOUT_IN_MS)
            val sftpChannel = sftp as ChannelSftp
            return sftpChannel.pwd()
        }
    }
}