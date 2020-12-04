package gov.cdc.prime.router.azure


import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.transport.SftpTransport
import gov.cdc.prime.router.transport.EmailTransport
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
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata.loadAll("$baseDir/metadata")
            val workflowEngine = WorkflowEngine()

            val event = Event.parse(message) as ReportEvent
            workflowEngine.handleReportEvent(event) { header, _ ->
                val service = Metadata.findService(header.task.receiverName)
                    ?: error("Internal Error: could not find ${header.task.receiverName}")

                val content = workflowEngine.readBody(header)
                service.transports.forEach {
                    when( it ) {
                        is OrganizationService.Transport.SFTP -> SftpTransport().send(service.fullName, it, header, content)
                        is OrganizationService.Transport.Email -> EmailTransport().send( service.fullName, it, header, content) 
                    }
                }

                val transportSuccessful = true;
                /*
                var transportSuccessful = when (service.transport.type) {
                    OrganizationService.Transport.TransportType.SFTP -> {
                        val content = workflowEngine.readBody(header)
                        // TODO:  look up the correct class to call based on the transport metadata
                        val transport = SftpTransport()
                        transport.send(service, header, content)
                    }
                    OrganizationService.Transport.TransportType.DEFAULT -> false
                } */
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

}
