package gov.cdc.prime.router.transport

import com.google.common.base.Preconditions
import com.hierynomus.sshj.key.KeyAlgorithms
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.SftpCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.credentials.UserPemCredential
import gov.cdc.prime.router.credentials.UserPpkCredential
import gov.cdc.prime.router.report.ReportService
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceFilter
import net.schmizz.sshj.sftp.StatefulSFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.userauth.method.AuthMethod
import net.schmizz.sshj.userauth.method.AuthPassword
import net.schmizz.sshj.userauth.method.AuthPublickey
import net.schmizz.sshj.userauth.password.PasswordUtils
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.InputStream
import java.io.StringReader

class SftpTransport :
    ITransport,
    Logging {

    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        externalFileName: String,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
        reportEventService: IReportStreamEventService,
        reportService: ReportService,
        lineages: List<ItemLineage>?,
        queueMessage: String,
    ): RetryItems? {
        val sftpTransportType = transportType as SFTPTransportType

        return try {
            if (header.content == null) {
                error("No content to sftp for report ${header.reportFile.reportId}")
            }
            val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
            val sshClient = connect(receiver)

            // Dev note:  db table requires body_url to be unique, but not external_name
            context.logger.info("Successfully connected to $sftpTransportType, ready to upload $externalFileName")
            uploadFile(sshClient, sftpTransportType.filePath, externalFileName, header.content)
            val msg = "Success: sftp upload of $externalFileName to $sftpTransportType"
            context.logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                externalFileName,
                sftpTransportType.toString(),
                msg,
                header,
                reportEventService,
                reportService,
                this::class.java.simpleName,
                lineages,
                queueMessage
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (t: Throwable) {
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to " +
                    "$sftpTransportType (orgService = ${header.receiver?.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.warning(msg)
            actionHistory.setActionType(TaskAction.send_warning)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object : Logging {

        const val SFTP_HOST_OVERRIDE = "SFTP_HOST_OVERRIDE"
        const val SFTP_PORT_OVERRIDE = "SFTP_PORT_OVERRIDE"
        const val SFTP_HOST = "SFTP_HOST"
        const val SFTP_PORT = "SFTP_PORT"
        const val SFTP_CONNECTION_TIMEOUT = "SFTP_CONNECTION_TIMEOUT"
        const val SFTP_READ_TIMEOUT = "SFTP_READ_TIMEOUT"

        /**
         * Connect to a [receiver].  If the [credential] is not specified then it is fetched from the vault.
         * @return the SFTP client connection
         */
        fun connect(receiver: Receiver, credential: SftpCredential? = null): SSHClient {
            Preconditions.checkNotNull(receiver.transport)
            Preconditions.checkArgument(receiver.transport is SFTPTransportType)
            val sftpTransportInfo = receiver.transport as SFTPTransportType

            // Override the SFTP host and port only if provided and in the local environment.
            val host: String = if (Environment.isLocal() &&
                !System.getenv(SFTP_HOST_OVERRIDE).isNullOrBlank()
            ) {
                System.getenv(SFTP_HOST_OVERRIDE)
            } else {
                sftpTransportInfo.host
            }
            val port: String = if (Environment.isLocal() &&
                !System.getenv(SFTP_PORT_OVERRIDE).isNullOrBlank()
            ) {
                System.getenv(SFTP_PORT_OVERRIDE)
            } else {
                sftpTransportInfo.port
            }
            val properties = HashMap<String, String>()
            properties[SFTP_HOST] = host
            properties[SFTP_PORT] = port
            if (!sftpTransportInfo.connectionTimeout.isNullOrBlank()) {
                properties[SFTP_CONNECTION_TIMEOUT] = sftpTransportInfo.connectionTimeout
            }
            if (!sftpTransportInfo.readTimeout.isNullOrBlank()) {
                properties[SFTP_READ_TIMEOUT] = sftpTransportInfo.readTimeout
            }
            return connect(properties, credential ?: lookupCredentials(receiver))
        }

        /**
         * Fetch the credentials for a give [receiver].
         * @return the SFTP credential
         */
        fun lookupCredentials(receiver: Receiver): SftpCredential {
            Preconditions.checkNotNull(receiver.transport)
            Preconditions.checkArgument(receiver.transport is SFTPTransportType)
            val sftpTransportInfo = receiver.transport as SFTPTransportType

            // if the transport definition has defined default
            // credentials use them, otherwise go with the
            // standard way by using the receiver full name
            val credentialName = sftpTransportInfo.credentialName ?: receiver.fullName
            val credentialLabel = CredentialHelper.formCredentialLabel(credentialName)

            // Assumes credential will be cast as SftpCredential, if not return null, and thus the error case
            return CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "SftpTransport", CredentialRequestReason.SFTP_UPLOAD
            ) as? SftpCredential?
                ?: error("Unable to find SFTP credentials for $credentialName connectionId($credentialLabel)")
        }

        private fun connect(
            properties: Map<String, String>,
            credential: SftpCredential,
        ): SSHClient {
            val host = properties[SFTP_HOST]
            val port = properties[SFTP_PORT]?.toInt() ?: 22
            val sshClient = createDefaultSSHClient()
            if (!properties[SFTP_CONNECTION_TIMEOUT].isNullOrBlank()) {
                sshClient.connectTimeout = properties[SFTP_CONNECTION_TIMEOUT]?.toInt()!!
            }
            if (!properties[SFTP_READ_TIMEOUT].isNullOrBlank()) {
                sshClient.timeout = properties[SFTP_READ_TIMEOUT]?.toInt()!!
            }
            try {
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(host, port)
                when (credential) {
                    is UserPassCredential -> sshClient.authPassword(credential.user, credential.pass)
                    is UserPemCredential -> {
                        val key = OpenSSHKeyFile()
                        val keyContents = StringReader(credential.key)
                        when (StringUtils.isBlank(credential.keyPass)) {
                            true -> key.init(keyContents)
                            false -> key.init(keyContents, PasswordUtils.createOneOff(credential.keyPass.toCharArray()))
                        }
                        val authProviders = mutableListOf<AuthMethod>(AuthPublickey(key))
                        if (StringUtils.isNotBlank(credential.pass) && credential.pass != null) {
                            authProviders.add(AuthPassword(PasswordUtils.createOneOff(credential.pass.toCharArray())))
                        }
                        sshClient.auth(credential.user, authProviders)
                    }

                    is UserPpkCredential -> {
                        val key = PuTTYKeyFile()
                        val keyContents = StringReader(credential.key)
                        when (StringUtils.isBlank(credential.keyPass)) {
                            true -> key.init(keyContents)
                            false -> key.init(keyContents, PasswordUtils.createOneOff(credential.keyPass.toCharArray()))
                        }
                        val authProviders = mutableListOf<AuthMethod>(AuthPublickey(key))
                        if (StringUtils.isNotBlank(credential.pass) && credential.pass != null) {
                            authProviders.add(AuthPassword(PasswordUtils.createOneOff(credential.pass.toCharArray())))
                        }
                        sshClient.auth(credential.user, authProviders)
                    }

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
            contents: ByteArray,
        ) {
            try {
                try {
                    sshClient.newSFTPClient().use { client ->
                        client.fileTransfer.preserveAttributes = false
                        client.use {
                            it.put(makeSourceFile(contents, fileName), "$path/$fileName")
                        }
                    }
                } finally {
                    sshClient.disconnect()
                }
            } catch (ce: net.schmizz.sshj.connection.ConnectionException) {
                // if the timeout happens on disconnect it gets wrapped up in the connectException
                // and we need to check the root cause
                if (ce.cause is java.util.concurrent.TimeoutException) {
                    // do nothing. some servers just take a long time to disconnect
                    logger.warn("Connection exception during ls: ${ce.localizedMessage}")
                } else {
                    throw ce
                }
            }
        }

        fun ls(sshClient: SSHClient, path: String): List<String> = ls(sshClient, path, null)

        fun ls(sshClient: SSHClient, path: String, resourceFilter: RemoteResourceFilter?): List<String> {
            val lsResults = mutableListOf<String>()
            try {
                try {
                    sshClient.use { ssh_Client ->
                        ssh_Client.newSFTPClient().use {
                            it.ls(path, resourceFilter).map { l -> lsResults.add(l.toString()) }
                        }
                    }
                } finally {
                    sshClient.disconnect()
                }
            } catch (ce: net.schmizz.sshj.connection.ConnectionException) {
                // if the timeout happens on disconnect it gets wrapped up in the connectException
                // and we need to check the root cause
                if (ce.cause is java.util.concurrent.TimeoutException) {
                    // do nothing. some servers just take a long time to disconnect
                    logger.warn("Connection exception during ls: ${ce.localizedMessage}")
                } else {
                    throw ce
                }
            }

            return lsResults
        }

        fun rm(sshClient: SSHClient, path: String, fileName: String) {
            try {
                try {
                    sshClient.use { ssh_Client ->
                        ssh_Client.newSFTPClient().use {
                            it.rm("$path/$fileName")
                        }
                    }
                } finally {
                    sshClient.disconnect()
                }
            } catch (ce: net.schmizz.sshj.connection.ConnectionException) {
                // if the timeout happens on disconnect it gets wrapped up in the connectException
                // and we need to check the root cause
                if (ce.cause is java.util.concurrent.TimeoutException) {
                    // do nothing. some servers just take a long time to disconnect
                    logger.warn("Connection exception during ls: ${ce.localizedMessage}")
                } else {
                    throw ce
                }
            }
        }

        fun pwd(sshClient: SSHClient): String {
            var pwd: String
            try {
                sshClient.newStatefulSFTPClient().use { client ->
                    val statefulClient = client as StatefulSFTPClient
                    sshClient.timeout = 120000
                    statefulClient.use {
                        pwd = it.pwd()
                    }
                }
            } finally {
                sshClient.disconnect()
            }

            return pwd
        }

        private fun makeSourceFile(contents: ByteArray, fileName: String): LocalSourceFile =
            object : InMemorySourceFile() {
                override fun getName(): String = fileName

                override fun getLength(): Long = contents.size.toLong()

                override fun getInputStream(): InputStream = contents.inputStream()

                override fun isDirectory(): Boolean = false

                override fun isFile(): Boolean = true

                override fun getPermissions(): Int = 777
            }

        // allow us to mock SSHClient because there is no dependency injection in this class
        fun createDefaultSSHClient(): SSHClient {
            val sshConfig = DefaultConfig()

            // Started from version 0.33.0, SSHJ doesn't try to determine RSA-SHA2-* support on fly.
            // Instead, it looks only config.getKeyAlgorithms(), which may or may not contain ssh-rsa
            // and rsa-sha2-* in any order.  The default config stops working with old servers like
            // Apache SSHD that doesn't rsa-sha2-* signatures.  To make it works with old servers,
            // we need to include the KeyAlgorithms.SSHRSA at the top of the list or have higher
            // priority than other as below.
            sshConfig.keyAlgorithms = listOf(
                KeyAlgorithms.SSHRSA(),
                KeyAlgorithms.EdDSA25519CertV01(),
                KeyAlgorithms.EdDSA25519(),
                KeyAlgorithms.ECDSASHANistp521CertV01(),
                KeyAlgorithms.ECDSASHANistp521(),
                KeyAlgorithms.ECDSASHANistp384CertV01(),
                KeyAlgorithms.ECDSASHANistp384(),
                KeyAlgorithms.ECDSASHANistp256CertV01(),
                KeyAlgorithms.ECDSASHANistp256(),
                KeyAlgorithms.RSASHA512(),
                KeyAlgorithms.RSASHA256(),
                KeyAlgorithms.SSHRSACertV01(),
                KeyAlgorithms.SSHDSSCertV01(),
                KeyAlgorithms.SSHDSA()
            )
            return SSHClient(sshConfig)
        }
    }
}