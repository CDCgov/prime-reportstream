package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.transport.SftpTransport
import net.schmizz.sshj.sftp.RemoteResourceFilter
import net.schmizz.sshj.sftp.RemoteResourceInfo
import org.apache.logging.log4j.kotlin.Logging
import java.time.Instant
import java.util.UUID

/*
 * Check API
 */

class CheckFunction : Logging {
    // data structure for sftp file
    data class SftpFile(
        val name: String,
        val contents: String,
    )

    /**
     * A class to wrap the connection check event
     */
    data class RemoteConnectionCheck(
        val organizationId: Int,
        val receiverId: Int,
        val checkSuccessful: Boolean,
        val initiatedOn: Instant,
        val completedAt: Instant,
        val checkResult: String
    )

    class TestFileFilter(val fileName: String) : RemoteResourceFilter {
        override fun accept(resource: RemoteResourceInfo?): Boolean {
            resource?. let {
                return resource.isRegularFile && resource.name == fileName
            }
            return false
        }
    }

    /**
     * Checks a single remote connection
     */
    @FunctionName("check")
    fun run(
        @HttpTrigger(
            name = "check",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION,
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        logger.info("Entering check api")
        val responseBody = mutableListOf<String>()
        var httpStatus = HttpStatus.OK
        try {
            if (request.queryParameters.size != 1) {
                return HttpUtilities.badRequestResponse(request, "Must send exactly one option")
            }
            val receiverFullName = request.queryParameters["sftpcheck"]
                ?: return HttpUtilities.badRequestResponse(request, "Missing option sftpcheck")
            /**
             * The query parameter is not added unless it has a value, treating
             * sendfile as a flag in the URI. When the sendfile flag is present,
             * an empty file is created on a GET request but POST will use the
             * request body as the file contents. The file name is generated as
             * hello-{UUID}.txt with details in as StfpFile instance.
             */
            val sftpFile: SftpFile? = when {
                "&sendfile" !in request.uri.query -> null
                request.httpMethod == HttpMethod.POST -> SftpFile(
                    "hello-${UUID.randomUUID()}.txt",
                    request.body ?: ""
                )
                else -> SftpFile("hello-${UUID.randomUUID()}.txt", "")
            }
            // size check on the sftp file contents, fails if more than 100K chars in length
            sftpFile?. let {
                if (sftpFile.contents.length > 100000) {
                    return HttpUtilities.badRequestResponse(request, "Test upload file exceeds 100K size limit")
                }
            }
            val settings = BaseEngine.settingsProviderSingleton
            if (receiverFullName == "all") {
                if (!testAllTransports(settings.receivers, sftpFile, responseBody)) {
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR // everything bombed.
                }
            } else {
                val receiver = settings.findReceiver(receiverFullName)
                    ?: return HttpUtilities.badRequestResponse(request, "Unable to find receiver $receiverFullName")
                responseBody.add("$receiverFullName: is a valid receiver")
                if (receiver.transport == null) {
                    return HttpUtilities.badRequestResponse(request, "$receiverFullName: no transport defined")
                }
                httpStatus = if (testTransport(receiver, sftpFile, responseBody)) {
                    HttpStatus.OK
                } else {
                    HttpStatus.BAD_REQUEST
                }
            }
        } catch (t: Throwable) {
            responseBody.add(t.localizedMessage)
            httpStatus = HttpStatus.BAD_REQUEST
        }
        return HttpUtilities.httpResponse(request, responseBody.joinToString("\n") + "\n", httpStatus)
    }

    /**
     * Runs a check of all remote connections for all receivers and stores the results in a table.
     * This is set up to run as a cron job in Azure.
     * The current cron set up is 0 0 *12 * * * which means to run every two hours
     */
    @FunctionName("scheduled-remote-connection-check")
    fun scheduledRun(
        @TimerTrigger(
            name = "scheduledRemoteConnectionCheckTrigger",
            schedule = "%REMOTE_CONNECTION_CHECK_SCHEDULE%"
        ) timerInfo: String,
    ) {
        // Each setting is checked against this logic to see if it should run.
        fun checkShouldRun(receiverSetting: Receiver): Boolean {
            if (receiverSetting.customerStatus != CustomerStatus.ACTIVE)
                return false
            // note: right now ONLY SFTP is supported, but we should expand!
            return when (receiverSetting.transport) {
                is SFTPTransportType -> true
                else -> false
            }
        }

        logger.info("Staring scheduled check of remote receiver connections. Schedule is set to $timerInfo")
        val settings = BaseEngine.settingsProviderSingleton
        val db = BaseEngine.databaseAccessSingleton
        settings.receivers.forEach {
            if (!checkShouldRun(it)) return@forEach // skip
            logger.info("Checking connection for ${it.organizationName}-${it.name}")
            // create the response body
            val responseBody: MutableList<String> = mutableListOf()
            // test the transport
            val initiatedAt = Instant.now()
            val successful = try {
                testTransport(it, null, responseBody)
            } catch (ex: Throwable) {
                responseBody.add(ex.localizedMessage)
                responseBody.add(ex.stackTraceToString())
                false
            }

            val completedOn = Instant.now()
            db.transact { txn ->
                // get the id for the organization
                val organizationId = db.fetchSetting(
                    SettingType.ORGANIZATION,
                    it.organizationName,
                    null,
                    txn
                ).let { setting ->
                    setting?.settingId
                }
                // get the id for the receiver
                val receiverId = db.fetchSetting(SettingType.RECEIVER, it.name, organizationId, txn).let { receiver ->
                    receiver?.settingId
                }
                // save the record
                if (organizationId != null && receiverId != null) {
                    // update and move on
                    val connectionCheck = RemoteConnectionCheck(
                        organizationId,
                        receiverId,
                        successful,
                        initiatedAt,
                        completedOn,
                        responseBody.joinToString("\n"),
                    )
                    db.saveRemoteConnectionCheck(txn, connectionCheck)
                } else {
                    logger.info(
                        "Unable to save connection check for ${it.organizationName}-${it.name}" +
                            " because organizationId ($organizationId) or receiverId ($receiverId) is null"
                    )
                }
            }
        }
        logger.info("Done checking remote receiver connections")
        return
    }

    /**
     * Return true if even one sftp worked; return false if all failed.
     */
    private fun testAllTransports(
        receivers: Collection<Receiver>,
        sftpFile: SftpFile?,
        responseBody: MutableList<String>
    ): Boolean {
        var overallPass = false
        receivers.forEach { receiver ->
            if (testTransport(receiver, sftpFile, responseBody)) {
                // if even one good sftp is found, that saves the overall run.
                overallPass = true
            }
            responseBody.add("") // This will add a newline when the strings are returned.
        }
        return overallPass
    }

    /**
     * Returns true on success, false on fail.
     */
    private fun testTransport(receiver: Receiver, sftpFile: SftpFile?, responseBody: MutableList<String>): Boolean {
        if (receiver.transport == null) {
            responseBody.add("**** ${receiver.fullName}:  no transport defined.")
            return false
        }
        try {
            return when (receiver.transport) {
                is SFTPTransportType -> {
                    testSftp(receiver.transport, receiver, sftpFile, responseBody)
                    responseBody.add("**** ${receiver.fullName}: OK")
                    true
                }
                // todo add other types of transports as needed.
                else -> {
                    responseBody.add(
                        "**** ${receiver.fullName}: No test implemented for transport type ${receiver.transport.type}"
                    )
                    false
                }
            }
        } catch (t: Throwable) {
            logger.info("Exception in health check: ${t.message}: ${t.cause?.message ?: "No root cause"}")
            logger.info(t.stackTraceToString())
            responseBody.add("${receiver.fullName}: ${t.localizedMessage}: ${t.cause?.message ?: "No root cause"}")
            responseBody.add(t.stackTraceToString())
            responseBody.add("**** ${receiver.fullName}: FAILED")
            return false
        }
    }

    /**
     * Any normal return is success.  Any exception thrown is failure.
     */
    private fun testSftp(
        sftpTransportType: SFTPTransportType,
        receiver: Receiver,
        sftpFile: SftpFile?,
        responseBody: MutableList<String>
    ) {
        val path = sftpTransportType.filePath
        logger.info("SFTP Transport $sftpTransportType")
        responseBody.add("${receiver.fullName}: SFTP Transport: $sftpTransportType")
        val credential = SftpTransport.lookupCredentials(receiver)
        var sshClient = SftpTransport.connect(receiver, credential)
        responseBody.add("${receiver.fullName}: Able to Connect to sftp site")
        sftpFile?. let {
            logger.info("Attempting to upload ${it.name} to $sftpTransportType")
            if (SftpTransport.ls(sshClient, path, TestFileFilter(it.name)).isNotEmpty()) {
                throw Exception("File ${sftpFile.name} already exists on SFTP server. Aborting upload.")
            }
            // the client connection is closed in the SftpTransport methods
            sshClient = SftpTransport.connect(receiver, credential)
            SftpTransport.uploadFile(sshClient, path, it.name, it.contents.toByteArray())
            responseBody.add("${receiver.fullName}: Uploaded file '${sftpFile.name}' to SFTP transport")
            sshClient = SftpTransport.connect(receiver, credential)
        }
        logger.info("Now trying an `ls` on $path")
        val lsList: List<String> = SftpTransport.ls(sshClient, path)
        // Log what we found from ls, but don't return it.
        logger.info("What we got back from ls (first few lines): ")
        lsList.filterIndexed { index, _ -> index <= 5 }.forEach { logger.info(it) }
        var msg = "${receiver.fullName}: Success: ls returned ${lsList.size} rows of info from SFTP Transport"
        logger.info(msg)
        responseBody.add(msg)
        sftpFile?. let {
            logger.info("Checking for uploaded file on SFTP Transport")
            sshClient = SftpTransport.connect(receiver, credential)
            msg = if (SftpTransport.ls(sshClient, path, TestFileFilter(it.name)).isEmpty()) {
                "${receiver.fullName}: Couldn't find file '${sftpFile.name}' on SFTP Transport"
            } else {
                "${receiver.fullName}: Found uploaded file '${sftpFile.name}' on SFTP Transport"
            }
            logger.info(msg)
            responseBody.add(msg)
            msg = "${receiver.fullName}: Removing '${sftpFile.name}' from SFTP transport"
            logger.info(msg)
            responseBody.add(msg)
            SftpTransport.rm(SftpTransport.connect(receiver, credential), path, sftpFile.name)
            msg = "${receiver.fullName}: Success: removed '${sftpFile.name}' from SFTP Transport"
            logger.info(msg)
            responseBody.add(msg)
        }
    }
}