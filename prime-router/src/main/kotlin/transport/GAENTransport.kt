package gov.cdc.prime.router.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.GAENTransportType
import gov.cdc.prime.router.GAEN_SCHEMA
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
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.ByteArrayInputStream
import java.util.UUID

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
     * receiver to look up the API key in the key vault. Record a log in [actionHistory].
     * Retries are tracked on an individual item basis.
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
            if (header.content == null) error("No content to send for report $reportId")
            val receiver = header.receiver ?: error("No receiver defined for report $reportId")
            val itemCount = header.reportFile.itemCount
            val credential = lookupCredentials(receiver.fullName)

            // Creat a big bag of parameters to pass around
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

            // Log the useful correlations of ids
            context.logger.info("${receiver.fullName}: Ready to notify on ${params.comboId}")

            // Do the work
            val (futureRetries, sentCount) = processReport(params)

            // Record the history of this transaction
            val retryCount = futureRetries?.size ?: 0
            when {
                sentCount == itemCount -> recordFullSuccess(params)
                sentCount + retryCount == itemCount -> recordSuccessWithRetries(params, sentCount)
                sentCount + retryCount < itemCount -> recordPartialFailure(params, retryCount, sentCount)
                sentCount == 0 -> recordFailure(params)
                else -> error("Logic error in: sentCount=$sentCount retryCount=$retryCount itemCount=$itemCount")
            }

            futureRetries
        } catch (t: Throwable) {
            val receiverFullName = header.receiver?.fullName ?: ""
            val msg = "FAILED GAEN notification of inputReportId $reportId to $gaenTransportInfo " +
                "(receiver = $receiverFullName);" +
                "No retry; Exception: ${t.javaClass.canonicalName} ${t.localizedMessage}"
            // Dev note: Expecting severe level to trigger monitoring alerts
            context.logger.severe(msg)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            null
        }
    }

    /**
     * Record in [ActionHistory] the full success of this notification. Log an info message as well.
     */
    fun recordFullSuccess(params: SendParams) {
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
     * Record in [ActionHistory] the partial success of this action with retries. Log an info message as well.
     */
    private fun recordSuccessWithRetries(params: SendParams, sentCount: Int) {
        val msg = "${params.receiver.fullName}: Partial send of $sentCount item of the ${params.comboId} report. " +
            "Retries queued for ${params.itemCount - sentCount} items"
        val history = params.actionHistory
        params.context.logger.info(msg)
        history.trackActionResult(msg)
        history.trackSentReport(
            params.receiver,
            params.sentReportId,
            null,
            params.gaenTransportInfo.toString(),
            msg,
            sentCount
        )
        history.trackItemLineages(Report.createItemLineagesFromDb(params.header, params.sentReportId))
    }

    /**
     * Record in [ActionHistory] the partial success of this notification. Log a warning message.
     */
    private fun recordPartialFailure(params: SendParams, retryCount: Int, sentCount: Int) {
        val msg = "${params.receiver.fullName}: Partial send of $sentCount item of the ${params.comboId} report. " +
            "Retries queued for $retryCount items. " +
            "Errors for ${params.itemCount - sentCount - retryCount} items."
        val history = params.actionHistory
        params.context.logger.warning(msg)
        history.trackActionResult(msg)
        history.trackSentReport(
            params.receiver,
            params.sentReportId,
            null,
            params.gaenTransportInfo.toString(),
            msg,
            sentCount
        )
        history.trackItemLineages(Report.createItemLineagesFromDb(params.header, params.sentReportId))
    }

    fun recordFailure(params: SendParams) {
        val msg = "${params.receiver.fullName}: Failed to send all items in ${params.comboId} report. " +
            "Errors for ${params.itemCount} items."
        val history = params.actionHistory
        params.context.logger.severe(msg)
        history.setActionType(TaskAction.send_error)
        history.trackActionResult(msg)
    }

    /**
     * [Notification] is the payload sent to the ENCR server as JSON payload.
     * The structure is defined in
     * https://github.com/google/exposure-notifications-verification-server/blob/main/docs/api.md#apiissue
     */
    class Notification(report: Report, row: Int, transmitId: UUID) {
        val symptomDate: String
        val testDate: String
        val testType: String = "confirmed"
        val tzOffset: Int = 0
        val phone: String
        val padding: String
        val uuid: String
        val externalIssuerID: String = "cdcprime"

        init {
            symptomDate = report.getString(row, "patient_symptom_onset")
                ?: error("Internal Error: expected patient_symptom_onset")
            testDate = report.getString(row, "date_result_released")
                ?: error("Internal Error: expected date_result_released")
            phone = report.getString(row, "patient_phone_numer")
                ?: error("Internal Error: expecte patient_phone_number")
            padding = RandomStringUtils.random(10)
            uuid = "${report.id}($transmitId)-$row"
        }
    }

    /**
     * Do the work of sending a notification for each result in the report
     */
    internal fun processReport(params: SendParams): Pair<RetryItems?, Int> {
        // Get the report
        val report = WorkflowEngine.csvSerializer.readInternal(
            GAEN_SCHEMA,
            ByteArrayInputStream(params.header.content),
            emptyList(),
            params.receiver,
            params.header.reportFile.reportId
        )

        // For each message create a notification
        val notifications = report.itemIndices.map { Notification(report, it, params.sentReportId) }

        // Post each notification that needs to be retried
        return handleIndexedRetryItems(notifications, params.pastRetryItems) { notification ->
            postNotification(notification, params)
        }
    }

    enum class BlockResult { SUCCESS, FAIL, RETRY }

    /**
     * Handler for retry and retryItems. Takes a [pastRetryItems] and generates a future RetryItems list.
     * The [block] is called to preform the operation.
     */
    internal fun <T> handleIndexedRetryItems(
        items: List<T>,
        pastRetryItems: RetryItems?, /* = kotlin.collections.List<kotlin.String>? */
        block: (item: T) -> BlockResult
    ): Pair<RetryItems?, Int> {
        val futureRetryItems = mutableListOf<String>()
        var sentCount = 0
        items.forEachIndexed { index, item ->
            // Only send items in that are in the retry list
            if ((pastRetryItems != null) &&
                !RetryToken.isAllItems(pastRetryItems) &&
                !pastRetryItems.contains(index.toString())
            ) return@forEachIndexed

            // Send
            val result = block(item)

            // Add to retry list
            when (result) {
                BlockResult.SUCCESS -> sentCount += 1
                BlockResult.RETRY -> futureRetryItems += index.toString()
                BlockResult.FAIL -> {}
            }
        }
        return Pair(futureRetryItems.ifEmpty { null }, sentCount)
    }

    /**
     * Post the [notification] payload to the server.
     */
    internal fun postNotification(notification: Notification, params: SendParams): BlockResult {
        val payload = mapper.writeValueAsString(notification)
        val (_, response, result) = Fuel
            .post(params.gaenTransportInfo.apiUrl)
            .header(API_KEY, params.credential.apiKey)
            .timeout(GAEN_TIMEOUT)
            .jsonBody(payload)
            .responseJson()
        return when (result) {
            // Only retry if there is a server error
            is Result.Failure -> {
                return when (response.statusCode) {
                    in 500..509 -> BlockResult.RETRY // retry
                    else -> {
                        // Record something about the failure
                        BlockResult.FAIL
                    }
                }
            }
            is Result.Success -> {
                BlockResult.SUCCESS
            }
        }
    }

    /**
     * From the credential service get a JKS for [receiverFullName].
     */
    internal fun lookupCredentials(receiverFullName: String): UserApiKeyCredential {
        // Covert to the upper case naming convention of the Client KeyVault
        val credentialLabel = receiverFullName
            .replace(".", "--")
            .replace("_", "-")
            .uppercase()

        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel,
            "GAENTransport",
            CredentialRequestReason.GAEN_NOTIFICATION
        ) as? UserApiKeyCredential?
            ?: error("Unable to find GAEN credentials for $receiverFullName connectionId($credentialLabel)")
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