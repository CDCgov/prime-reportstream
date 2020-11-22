package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.*
import java.util.logging.Level


/**
 * Process will take a report and filter and transform it to the appropriate services in our list
 */
class ProcessFunction {
    @FunctionName("process")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @QueueTrigger(name = "msg", queueName = "validated")
        message: String,
        context: ExecutionContext,
    ) {
        try {
            context.logger.info("Queue trigger processed a request.")
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata.loadAll("$baseDir/metadata")

            val report = ReportQueue.receiveReport(ReportQueue.Name.VALIDATED, message)
            context.logger.info("Processing report: ${report.id}")

            OrganizationService
                .filterAndMapByService(report, Metadata.organizationServices)
                .forEach { (report, service) ->
                    //TODO send to processed queue when Merge function is written. Send to merged for now
                    val header = ReportQueue.sendReport(
                        ReportQueue.Name.MERGED,
                        report.copy(destination = service)
                    )
                    context.logger.info("Queued: $header")
                }
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, "process exception", e)
        }
    }
}
