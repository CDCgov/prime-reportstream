package gov.cdc.prime.router.transport

import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType

interface ITransport {
    /**
     * Send the content on the specific transport. Return retry information, if needed. Null, if not.
     *
     * @param orgName the service name that is being sent.
     * @param transportType the type of the transport (should always match the class)
     * @param contents being sent
     * @param reportId from which the byte array sends
     * @param retryItems the retry items from the last effort, if it was unsuccessful
     * @return null, if successful. RetryItems if not successful.
     */
    fun send(
        orgName: String,
        transportType: TransportType,
        contents: ByteArray,
        reportId: ReportId,
        retryItems: RetryItems?,
    ): RetryItems?
}