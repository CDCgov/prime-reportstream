package gov.cdc.prime.router.transport

import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import java.io.Closeable

interface ITransport {
    /**
     * The session is an optional part of the protocol. Sessions are started before the first send call to
     * a particular receiver and ended after the last.
     * The transport defines the content of the session and returns an closeable token for the session.
     * There can be multiple sessions executing at the same time.
     */
    fun startSession(receiver: Receiver): Closeable?

    /**
     * Send the content on the specific transport. Return retry information, if needed. Null, if not.
     *
     * @param header container of all info needed about report being sent.
     * @param retryItems the retry items from the last effort, if it was unsuccessful
     * @param session for this receiver
     * @return null, if successful. RetryItems if not successful.
     */
    fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: Closeable?,
        actionHistory: ActionHistory,
    ): RetryItems?
}