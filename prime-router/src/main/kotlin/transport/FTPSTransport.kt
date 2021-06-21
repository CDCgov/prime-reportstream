package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.*
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.*
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.apache.logging.log4j.kotlin.Logging
import java.io.*


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
        val host: String = ftpsTransportType.host
        val port: String = ftpsTransportType.port
        val username: String = ftpsTransportType.username
        val password: String = ftpsTransportType.password
        val protocol: String = ftpsTransportType.protocol
        val filePath: String = ftpsTransportType.filePath
        val binaryTransfer: Boolean = ftpsTransportType.binaryTransfer

        return try {
            if (header.content == null)
                error("No content to sftp for report ${header.reportFile.reportId}")
            val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
            val credential = lookupCredentials(receiver.fullName)
            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            val ftpsClient = connect(
                server = "$host:$port",
                username = username,
                password = password,
                protocol = protocol,
                binaryTransfer = binaryTransfer
            )

            context.logger.info("Successfully connected to $ftpsTransportType, ready to upload $fileName")
            uploadFile(ftpsClient, filePath, fileName, header.content)
            val msg = "Success: sftp upload of $fileName to $ftpsTransportType"
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
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))


            null
        } catch (t: Throwable) {
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to " +
                    "$ftpsTransportType (orgService = ${header.receiver?.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.warning(msg)
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
                .uppercase()

            // Assumes credential will be cast as SftpCredential, if not return null, and thus the error case
            return CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "FtpsTrasnport", CredentialRequestReason.FTPS_UPLODAD
            ) as? SftpCredential?
                ?: error("Unable to find FTPS credentials for $receiverFullName connectionId($credentialLabel)")
        }


        /**
         * Connect to the FTPS server
         *
         * [protocol] - TLS/SSL
         * [binaryTransfer] - true to enforce binary transfer
         */
        fun connect(
            server: String,
            username: String,
            password: String,
            protocol: String,
            binaryTransfer: Boolean
        ): FTPSClient {

            val ftpsClient = FTPSClient(protocol)
            ftpsClient.addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
            try {
                ftpsClient.connect(server)
                println("Connected to $server.")

                // After connection attempt, you should check the reply code to verify
                // success.
                val reply: Int = ftpsClient.replyCode
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpsClient.disconnect()
                    System.err.println("FTP server refused connection.")
                }

                ftpsClient.bufferSize = 1000
                if (!ftpsClient.login(username, password)) {
                    ftpsClient.logout()
                }
                println("Remote system is " + ftpsClient.systemType)
                if (binaryTransfer) {
                    ftpsClient.setFileType(FTP.BINARY_FILE_TYPE)
                }

                // Use passive mode as default because most of us are
                // behind firewalls these days.
                ftpsClient.enterLocalPassiveMode()

            } catch (e: IOException) {
                if (ftpsClient.isConnected) {
                    try {
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
            path: String,
            fileName: String,
            contents: ByteArray
        ) {
            try {
                val input: InputStream
                input = FileInputStream(path)
                ftpsClient.storeFile(fileName, input)
                input.close()
                ftpsClient.logout()
            } catch (e: FTPConnectionClosedException) {
                println("Server closed connection.")
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
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