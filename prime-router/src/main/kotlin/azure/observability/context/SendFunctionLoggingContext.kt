package gov.cdc.prime.router.azure.observability.context

import gov.cdc.prime.router.ReportId

/**
 * Fields added to the MDC during Send function calls
 */
data class SendFunctionLoggingContext(val reportId: ReportId, val receiver: String) : AzureLoggingContext