package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.transport.SftpTransport
import java.util.logging.Level

/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
const val dataRetentionDays = 7L
const val send = "send"

class SendFunction {

    @FunctionName(send)
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = send)
        message: String,
        context: ExecutionContext,
    ) {
        try {
            context.logger.info("Started Send Function: $message")
            val workflowEngine = WorkflowEngine()

            val event = Event.parse(message) as ReportEvent
            workflowEngine.handleReportEvent(event) { header, _ ->
                val service = workflowEngine.metadata.findService(header.task.receiverName)
                    ?: error("Internal Error: could not find ${header.task.receiverName}")

                context.logger.info("Transport found for ${service.fullName} = ${service.transport.type}")

                var transportSuccessful = when (service.transport.type) {
                    OrganizationService.Transport.TransportType.SFTP -> {
                        context.logger.info(
                            "trying to send to ${service.transport.host} " +
                                "${service.transport.port} ${service.transport.filePath}"
                        )
                        val content = workflowEngine.readBody(header)
                        // TODO:  look up the correct class to call based on the transport metadata
                        val transport = SftpTransport()
                        transport.send(service, header, content)
                    }
                    OrganizationService.Transport.TransportType.DEFAULT -> false
                }
                if (transportSuccessful) {
                    context.logger.info("Sent report: ${header.task.reportId} to ${service.fullName}")
                }
                // TODO: Next action should be WIPE when implemented
                ReportEvent(Event.Action.NONE, header.task.reportId)
            }
            // For debugging and auditing purposes
        } catch (t: Throwable) {
            context.logger.log(Level.SEVERE, "Send exception", t)
        }
    }

    private fun lookupTransportMetadata(): OrganizationService.Transport {
        return OrganizationService.Transport() // TODO: actually lookup the Transport here - for now use the default
    }
}