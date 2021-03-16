package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.transport.SftpTransport
import org.apache.logging.log4j.kotlin.Logging

/*
 * HealthCheck API
 */

class HealthCheckFunction : Logging {

    @FunctionName("healthcheck")
    fun run(
        @HttpTrigger(
            name = "healthcheck",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION,
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        logger.info("Entering healthcheck")
        val responseBody = mutableListOf<String>()
        try {
            if (request.queryParameters.size != 1) {
                return HttpUtilities.badRequestResponse(request, "Must send exactly one option")
            }
            val receiverFullName = request.queryParameters["sftpcheck"]
                ?: return HttpUtilities.badRequestResponse(request, "Missing option sftpcheck")
            val settings = WorkflowEngine.settings
            val receiver = settings.findReceiver(receiverFullName)
                ?: return HttpUtilities.badRequestResponse(request, "Unable to find receiver $receiverFullName")
            responseBody.add("Found ${receiver.fullName} as a valid receiver")
            if (receiver.transport == null) {
                return HttpUtilities.badRequestResponse(request, "$receiverFullName has no transport defined")
            }
            when (receiver.transport) {
                is SFTPTransportType -> {
                    testSftp(receiver.transport, receiverFullName, responseBody)
                }
                // todo add other types of transports as needed.
                else ->
                    return HttpUtilities.badRequestResponse(
                        request,
                        "Test of ${receiver.transport.type} type not implemented."
                    )
            }
        } catch (t: Throwable) {
            responseBody.add(t.localizedMessage)
        }
        return HttpUtilities.okResponse(request, responseBody.joinToString("\n") + "\n")
    }

    fun testSftp(sftpTransportType: SFTPTransportType, receiverFullName: String, responseBody: MutableList<String>) {
        val host = sftpTransportType.host
        val port = sftpTransportType.port
        val path = sftpTransportType.filePath
        val (user, pass) = SftpTransport.lookupCredentials(receiverFullName)
        val sshClient = SftpTransport.connect(host, port, user, pass)
        responseBody.add("Able to Connect to sftp site.  Now trying an `ls`.")
        val lsList: List<String> = SftpTransport.ls(sshClient, path)
        // Log what we found from ls, but don't return it.
        logger.info("What we got back from ls (first few lines): ")
        lsList.filterIndexed { index, _ -> index <= 5 }.forEach { logger.info(it) }
        val msg = "Success: ls returned ${lsList.size} rows of info from $sftpTransportType"
        logger.info(msg)
        responseBody.add(msg)
    }
}