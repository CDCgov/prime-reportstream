package gov.cdc.prime.router.azure


import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.transport.SftpTransport
import java.util.logging.Level


/**
 * Azure Functions with HTTP Trigger. Write to blob.
 */
class SendFunction {

    @FunctionName("send")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = "merged")
        message: String,
        context: ExecutionContext
    ) {
        try {
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata.loadAll("$baseDir/metadata")

            val (header, content) = ReportQueue.receiveHeaderAndBody(ReportQueue.Name.VALIDATED, message)
            context.logger.info("Sending report: ${header.id}")

            val service = Metadata.findService(header.destination)

            if( service == null )
                error( "Unable to find a service handler for ${header.destination}")

            context.logger.info( "Transport found for ${service.fullName} = ${service.transport.type}")

            var transportSuccessful = when( service.transport.type ){
                    OrganizationService.Transport.TransportType.SFTP -> {
                        val transport = SftpTransport() // TODO:  look up the correct class to call based on the transport metadata
                        transport.send( service, header, content)
                    }
                    OrganizationService.Transport.TransportType.DEFAULT -> true
            }

            if( transportSuccessful ){
                ReportQueue.sendHeaderAndBody(ReportQueue.Name.SENT, header, content)
            }

        } catch (t: Throwable) {
            context.logger.log(Level.SEVERE, "send exception", t)
        }

    }

    private fun lookupTransportMetadata(): OrganizationService.Transport {
        return OrganizationService.Transport()  // TODO: actually lookup the Transport here - for now use the default
    }

}
