package gov.cdc.prime.router.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.GAENTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import khttp.post
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream

/**
 * The GAEN (Google/Apple Exposure Notification) transport was built for issuing notification to the
 * Exposure Verification Server (ENCV). The basic flow is as follows:
 *
 *   1. ReportStream receives a positive test for a patient in WA.
 *   2. ReportStream notifies the WA ENCV server that a patient tested positive
 *   3. The ENCV sends a text message to the patient with a link to allow them to notify their contacts
 *
 * This flow will be implemented as a receiver that has immediate timing and that filters for positive results.
 *
 * See [Google API for Exposure Notification](https://https://developers.google.com/android/exposure-notifications/exposure-notifications-api) for details on GAEN.
 * See [Issue API](https://https://github.com/google/exposure-notifications-verification-server/blob/main/docs/api.md#apiissue) for the details on the issue API ReportStream calls.
 */
class GAENTransport : ITransport, Logging {
    /**
     * Information helpful for the sending, logging and recording history all bundled together
     * to avoid long parameter lists in functions
     */
    data class SendParams(
        val gaenTransportInfo: GAENTransportType,
        val credential: UserApiKeyCredential,
        val reportId: ReportId,
        val sentReportId: ReportId,
        val receiver: Receiver,
        val header: WorkflowEngine.Header,
        val pastRetryItems: RetryItems?,
        val context: ExecutionContext,
        val actionHistory: ActionHistory,
    ) {
        val comboId: String get() = "$reportId($sentReportId)"
        val itemCount: Int get() = header.reportFile.itemCount
    }

    /**
     * Send a Report defined by [header] and [sentReportId]. [header] will pass in the
     * receiver to look up the API key in the key vault. Record the actions taken in [actionHistory].
     * If this report has partially or completely failed in the past, [retryItems] are
     * passed in. If this attempt has any items that should be retried, a retryItem list
     * is generated.
     */
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val gaenTransportInfo = transportType as GAENTransportType
        val reportId = header.reportFile.reportId
        return try {
            // Create a big bag of parameters to pass around
            if (header.content == null) error("No content to send for report $reportId")
            val receiver = header.receiver ?: error("No receiver defined for report $reportId")
            val credential = lookupCredentials(receiver.fullName)
            val params = SendParams(
                gaenTransportInfo,
                credential,
                reportId,
                sentReportId,
                receiver,
                header,
                retryItems,
                context,
                actionHistory
            )

            // Log the useful correlations of ids before the work
            context.logger.info("${receiver.fullName}: Ready to notify on ${params.comboId}")

            // Do the work
            val postResult = processReport(params)

            // Record the work in history and logs
            when (postResult) {
                PostResult.SUCCESS -> recordFullSuccess(params)
                PostResult.RETRY -> recordFailureWithRetry(params)
                PostResult.FAIL -> recordFailure(params)
            }

            // Return a retry token if we need to retry
            if (postResult == PostResult.RETRY) RetryToken.allItems else null
        } catch (t: Throwable) {
            val receiverFullName = header.receiver?.fullName ?: ""
            recordFailureWithException(t, context, actionHistory, receiverFullName, reportId, gaenTransportInfo)
            null
        }
    }

    /**
     * Record in [ActionHistory] the full success of this notification. Log an info message as well.
     */
    private fun recordFullSuccess(params: SendParams) {
        val msg = "${params.receiver.fullName}: Successful exposure notifications of ${params.comboId}"
        val history = params.actionHistory
        params.context.logger.info(msg)
        history.trackActionResult(msg)
        history.trackSentReport(
            params.receiver,
            params.sentReportId,
            null,
            params.gaenTransportInfo.toString(),
            msg,
            params.itemCount
        )
        history.trackItemLineages(Report.createItemLineagesFromDb(params.header, params.sentReportId))
    }

    /**
     * Record failure into logs and [ActionHistory]
     */
    private fun recordFailure(params: SendParams) {
        val msg = "${params.receiver.fullName}: Failed to send ${params.comboId} report. Will not retry."
        params.context.logger.severe(msg)
        params.actionHistory.setActionType(TaskAction.send_error)
        params.actionHistory.trackActionResult(msg)
    }

    /**
     * Record failure into logs and [ActionHistory] with retry. Not logged as an error.
     */
    private fun recordFailureWithRetry(params: SendParams) {
        val msg = "${params.receiver.fullName}: Failed to send ${params.comboId} report. Will retry."
        params.context.logger.warning(msg)
        params.actionHistory.setActionType(TaskAction.send_warning)
        params.actionHistory.trackActionResult(msg)
    }

    /**
     * Record failure into logs and [ActionHistory]. No retries.
     */
    private fun recordFailureWithException(
        ex: Throwable,
        context: ExecutionContext,
        actionHistory: ActionHistory,
        receiverFullName: String,
        reportId: ReportId,
        gaenTransportInfo: GAENTransportType
    ) {
        val msg = "FAILED GAEN notification of inputReportId $reportId to $gaenTransportInfo " +
            "(receiver = $receiverFullName);" +
            "No retry; Exception: ${ex.javaClass.canonicalName} ${ex.localizedMessage}"
        // Dev note: Expecting severe level to trigger monitoring alerts
        context.logger.severe(msg)
        actionHistory.setActionType(TaskAction.send_error)
        actionHistory.trackActionResult(msg)
    }

    /**
     * [Notification] is the payload sent to the ENCR server as JSON payload.
     * The structure is defined in
     * https://github.com/google/exposure-notifications-verification-server/blob/main/docs/api.md#apiissue
     */
    internal class Notification(table: List<Map<String, String>>, reportId: ReportId /* = java.util.UUID */) {
        val symptomDate: String
        val testDate: String
        val testType: String = "confirmed"
        val tzOffset: Int = 0
        val phone: String
        val padding: String
        val uuid: String
        val externalIssuerID: String = "cdcprime"

        init {
            if (table.size != 1) error("Internal Error: Expected a single item GAEN report")
            testDate = table[0]["date_result_released"] ?: ""
            // As a backup for missing symptomDate, use the testDate per conversation with WA-PHD
            symptomDate = table[0][ "illness_onset_date"] ?: testDate
            phone = table[0]["patient_phone_number"] ?: ""
            padding = RandomStringUtils.randomAlphanumeric(16)
            uuid = "$reportId"
        }
    }

    enum class PostResult { SUCCESS, FAIL, RETRY }

    /**
     * Do the work of sending a notification for each item in the report.
     * Return a list of retry items and the number of items sent.
     */
    internal fun processReport(params: SendParams): PostResult {
        //
        if (params.gaenTransportInfo.apiUrl.isBlank()) return PostResult.SUCCESS

        // Read the CSV table
        val reportStream = ByteArrayInputStream(params.header.content)
        val table = csvReader().readAllWithHeader(reportStream)

        // Send the notification
        val notification = Notification(table, params.reportId)
        val payload = mapper.writeValueAsString(notification)
        val (_, response, result) = Fuel
            .post(params.gaenTransportInfo.apiUrl)
            .header(API_KEY, params.credential.apiKey)
            .header(Headers.ACCEPT, "application/json")
            .timeout(GAEN_TIMEOUT)
            .jsonBody(payload)
            .responseString()

        return when (result) {
            is Result.Success -> {
                PostResult.SUCCESS
            }
            is Result.Failure -> {
                // The following retry/fail table is taken from the API's documentation
                val blockResult = when (response.statusCode) {
                    400 -> PostResult.FAIL // Bad parameters
                    409 -> PostResult.SUCCESS // UUID already present (consider this a success)
                    412 -> PostResult.FAIL // Unsupported test type
                    429 -> PostResult.RETRY // Maintanence mode or quota limit
                    in 500..599 -> PostResult.RETRY // Server error
                    else -> PostResult.FAIL // Unexpected error code
                }
                if (blockResult != PostResult.SUCCESS) {
                    val warning = "${params.receiver.fullName}: Error from GAEN server for ${notification.uuid}:" +
                        " ${response.statusCode} ${response.responseMessage}"
                    params.context.logger.warning(warning)
                    params.actionHistory.trackActionResult(warning)
                }
                blockResult
            }
        }
    }

    /**
     * From the credential service get an API Key for [receiverFullName].
     */
    internal fun lookupCredentials(receiverFullName: String): UserApiKeyCredential {
        val credentialLabel = CredentialHelper.formCredentialLabel(fromReceiverName = receiverFullName)
        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel,
            "GAENTransport",
            CredentialRequestReason.GAEN_NOTIFICATION
        ) as? UserApiKeyCredential?
            ?: error("Unable to find GAEN credentials for $receiverFullName using $credentialLabel")
    }

    companion object {
        const val API_KEY = "x-api-key"
        const val GAEN_TIMEOUT = 1000

        private val mapper = ObjectMapper().registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
    }
}