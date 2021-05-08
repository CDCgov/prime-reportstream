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
import org.apache.logging.log4j.kotlin.Logging

/*
 * Check API
 */

class CheckFunction : Logging {

    @FunctionName("check")
    fun run(
        @HttpTrigger(
            name = "check",
            methods = [HttpMethod.GET],
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
            val settings = WorkflowEngine.settings
            if (receiverFullName == "all") {
                if (!testAllTransports(settings.receivers, responseBody)) {
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR // everything bombed.
                }
            } else {
                val receiver = settings.findReceiver(receiverFullName)
                    ?: return HttpUtilities.badRequestResponse(request, "Unable to find receiver $receiverFullName")
                responseBody.add("$receiverFullName: is a valid receiver")
                if (receiver.transport == null) {
                    return HttpUtilities.badRequestResponse(request, "$receiverFullName: no transport defined")
                }
                httpStatus = if (testTransport(receiver, responseBody)) HttpStatus.OK else HttpStatus.BAD_REQUEST
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
    private fun testAllTransports(receivers: Collection<Receiver>, responseBody: MutableList<String>): Boolean {
        var overallPass = false
        receivers.forEach { receiver ->
            if (testTransport(receiver, responseBody)) {
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
    fun testTransport(receiver: Receiver, responseBody: MutableList<String>): Boolean {
        if (receiver.transport == null) {
            responseBody.add("**** ${receiver.fullName}:  no transport defined.")
            return false
        }
        try {
            return when (receiver.transport) {
                is SFTPTransportType -> {
                    testSftp(receiver.transport, receiver, responseBody)
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
    fun testSftp(sftpTransportType: SFTPTransportType, receiver: Receiver, responseBody: MutableList<String>) {
        val host = sftpTransportType.host
        val port = sftpTransportType.port
        val path = sftpTransportType.filePath
        val credential = SftpTransport.lookupCredentials(receiver.fullName)
        val sshClient = SftpTransport.connect(host, port, credential)
        responseBody.add("${receiver.fullName}: Able to Connect to sftp site.  Now trying an `ls`...")
        val lsList: List<String> = SftpTransport.ls(sshClient, path)
        // Log what we found from ls, but don't return it.
        logger.info("What we got back from ls (first few lines): ")
        lsList.filterIndexed { index, _ -> index <= 5 }.forEach { logger.info(it) }
        val msg = "${receiver.fullName}: Success: ls returned ${lsList.size} rows of info from $sftpTransportType"
        logger.info(msg)
        responseBody.add(msg)
    }
}