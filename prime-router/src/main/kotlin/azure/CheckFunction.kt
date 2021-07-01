package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.transport.SftpTransport
import net.schmizz.sshj.sftp.RemoteResourceFilter
import net.schmizz.sshj.sftp.RemoteResourceInfo
import org.apache.logging.log4j.kotlin.Logging
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

    class TestFileFilter(val fileName: String) : RemoteResourceFilter {
        override fun accept(resource: RemoteResourceInfo?): Boolean {
            resource?. let {
                return resource.isRegularFile && resource.name == fileName
            }
            return false
        }
    }

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
                    request?.body ?: ""
                )
                else -> SftpFile("hello-${UUID.randomUUID()}.txt", "")
            }
            // size check on the sftp file contents, fails if more than 100K chars in length
            sftpFile?. let {
                if (sftpFile.contents.length > 100000) {
                    return HttpUtilities.badRequestResponse(request, "Test upload file exceeds 100K size limit")
                }
            }
            val settings = WorkflowEngine.settings
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
                // Like Gen 18:31, if even one good sftp is found, that saves the overall run.
                overallPass = true
            }
            responseBody.add("") // This will add a newline when the strings are returned.
        }
        return overallPass
    }

    /**
     * Returns true on success, false on fail.
     */
    fun testTransport(receiver: Receiver, sftpFile: SftpFile?, responseBody: MutableList<String>): Boolean {
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
    fun testSftp(
        sftpTransportType: SFTPTransportType,
        receiver: Receiver,
        sftpFile: SftpFile?,
        responseBody: MutableList<String>
    ) {
        val host = sftpTransportType.host
        val port = sftpTransportType.port
        val path = sftpTransportType.filePath
        logger.info("SFTP Transport $sftpTransportType")
        responseBody.add("${receiver.fullName}: SFTP Transport: $sftpTransportType")
        val credential = SftpTransport.lookupCredentials(receiver.fullName)
        var sshClient = SftpTransport.connect(host, port, credential)
        responseBody.add("${receiver.fullName}: Able to Connect to sftp site")
        sftpFile?. let {
            logger.info("Attempting to upload ${it.name} to $sftpTransportType")
            if (SftpTransport.ls(sshClient, path, TestFileFilter(it.name)).isNotEmpty()) {
                throw Exception("File ${sftpFile.name} already exists on SFTP server. Aborting upload.")
            }
            // the client connection is closed in the SftpTransport methods
            sshClient = SftpTransport.connect(host, port, credential)
            SftpTransport.uploadFile(sshClient, path, it.name, it.contents.toByteArray())
            responseBody.add("${receiver.fullName}: Uploaded file '${sftpFile.name}' to SFTP transport")
            sshClient = SftpTransport.connect(host, port, credential)
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
            sshClient = SftpTransport.connect(host, port, credential)
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
            SftpTransport.rm(SftpTransport.connect(host, port, credential), path, sftpFile.name)
            msg = "${receiver.fullName}: Success: removed '${sftpFile.name}' from SFTP Transport"
            logger.info(msg)
            responseBody.add(msg)
        }
    }
}