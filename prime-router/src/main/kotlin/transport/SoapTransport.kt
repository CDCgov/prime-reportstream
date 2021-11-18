package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SoapTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine

/**
 * A SOAP transport that will connect to the endpoint and send a message in a serialized SOAP envelope
 */
class SoapTransport : ITransport {
    /**
     * Sends the actual message to the endpoint
     */
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory
    ): RetryItems? {
        val soapTransportType = transportType as? SoapTransportType
            ?: error("Transport type passed in not of SOAPTransportType")

        context.logger.info(
            "Preparing to sending ${header.reportFile.reportId} " +
                "to ${soapTransportType.soapAction} at ${soapTransportType.endpoint}"
        )

        return try {
            null
        } catch (t: Throwable) {
            context.logger.severe(t.localizedMessage)
            context.logger.severe(t.stackTraceToString())
            RetryToken.allItems
        }
    }
}