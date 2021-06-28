package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FTPSTransportType
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.SftpCredential
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.io.CopyStreamException
import org.apache.commons.net.util.TrustManagerUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory

/**
 * FTPSTransport
 *
 * TODO - create a credential that wraps certificates with username/password
 * TODO - verify permutations of SSL/TLS FTPS servers
 */
class FTPSTransport : ITransport, Logging {

    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val ftpsTransportType = transportType as FTPSTransportType
        return try {
            if (header.content == null)
                error("No content to sftp for report ${header.reportFile.reportId}")
            val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
            val fileName = Report.formExternalFilename(header)

            val ftpsClient = connect(
                ftpsTransportType,
                context
            )

            context.logger.info("Successfully connected to $ftpsTransportType, ready to upload $fileName")
            var msg: String
            if (uploadFile(ftpsClient, fileName, header.content)) {
                msg = "Success: FTPS upload of $fileName to $ftpsTransportType"
            } else {
                msg = "Failure: FTPS upload of $fileName to $ftpsTransportType failed"
            }
            context.logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                fileName,
                ftpsTransportType.toString(),
                msg,
                header.reportFile.itemCount
            )
            ftpsClient.logout()
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (t: Throwable) {
            val msg =
                "FAILED FTPS upload of inputReportId ${header.reportFile.reportId} to " +
                    "$ftpsTransportType (orgService = ${header.receiver?.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.warning(msg)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object {
        // TODO - this needs an FTPSCredential once its known what that entails
        fun lookupCredentials(receiverFullName: String): SftpCredential {
            val credentialLabel = receiverFullName
                .replace(".", "--")
                .replace("_", "-")
                .uppercase()

            // Assumes credential will be cast as SftpCredential, if not return null, and thus the error case
            return CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "FtpsTrasnport", CredentialRequestReason.FTPS_UPLOAD
            ) as? SftpCredential?
                ?: error("Unable to find FTPS credentials for $receiverFullName connectionId($credentialLabel)")
        }

        /**
         * Connect to the FTPS server
         *
         * @param ftpsTransportType
         * @param context
         */
        fun connect(
            ftpsTransportType: FTPSTransportType,
            context: ExecutionContext,
        ): FTPSClient {
            val port: Int = ftpsTransportType.port
            val username: String = ftpsTransportType.username
            val password: String = ftpsTransportType.password
            val protocol: String = ftpsTransportType.protocol
            val server: String = ftpsTransportType.host
            val binaryTransfer: Boolean = ftpsTransportType.binaryTransfer
            val acceptAllCerts: Boolean = ftpsTransportType.acceptAllCerts

            val ftpsClient = FTPSClient(protocol)
            ftpsClient.addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
            try {

                if (acceptAllCerts) {
                    ftpsClient.trustManager = TrustManagerUtils.getAcceptAllTrustManager()
                }

                // TODO - Upload client certs. Self-signed certs may need to be granted access specifically

                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(null, null)
                val km: KeyManager = kmf.keyManagers[0]
                ftpsClient.setKeyManager(km)

                ftpsClient.connect(server, port)
                context.logger.info("Connected to $server.")

                // After connection attempt, you should check the reply code to verify
                // success.
                val reply: Int = ftpsClient.replyCode
                if (!FTPReply.isPositiveCompletion(reply)) {
                    context.logger.finest("FTPS: entering active mode")
                    ftpsClient.execPBSZ(0) // Set protection buffer size
                    ftpsClient.execPROT("P") // Set data channel protection to private
                    ftpsClient.execCCC()
                    ftpsClient.enterLocalActiveMode()
                } else {
                    context.logger.finest("FTPS: entering passive mode")
                    ftpsClient.enterLocalPassiveMode()
                    ftpsClient.soTimeout = 10000
                    ftpsClient.setDataTimeout(10000)
                }

                ftpsClient.bufferSize = 1024 * 1024
                ftpsClient.connectTimeout = 100000
                ftpsClient.keepAlive = true

                if (!ftpsClient.login(username, password)) {
                    throw Exception("Could not log into FTPS server with user $username")
                }

                if (binaryTransfer) {
                    ftpsClient.setFileType(FTP.BINARY_FILE_TYPE)
                }
            } catch (e: FTPConnectionClosedException) {
                context.logger.info("FTPS connection died while exchanging credentials")
                e.printStackTrace()
            } catch (e: IOException) {
                if (ftpsClient.isConnected) {
                    try {
                        println("Closing connection due to exception")
                        ftpsClient.disconnect()
                    } catch (f: IOException) {
                        // do nothing
                    }
                }
                System.err.println("Could not connect to server.")
                e.printStackTrace()
            }
            return ftpsClient
        }

        /**
         * Upload a file through FTPS - Call this after you have already successfully connect()ed.
         */
        fun uploadFile(
            ftpsClient: FTPSClient,
            fileName: String,
            contents: ByteArray
        ): Boolean {
            try {
                val fileInputStream: InputStream = ByteArrayInputStream(contents)
                return ftpsClient.storeFile(fileName, fileInputStream)
            } catch (e: FTPConnectionClosedException) {
                println("Server closed connection.")
                e.printStackTrace()
            } catch (e: CopyStreamException) {
                println("Error while transferring file to FTPS server")
                e.printStackTrace()
            } catch (e: IOException) {
                println("IOException")
                e.printStackTrace()
            } catch (e: Exception) {
                println("General exception")
                e.printStackTrace()
            }
            return false
        }
    }
}