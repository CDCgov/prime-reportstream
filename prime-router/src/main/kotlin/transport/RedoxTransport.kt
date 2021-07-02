package gov.cdc.prime.router.transport

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.AUTHORIZATION
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.secrets.SecretManagement
import java.util.logging.Level

class RedoxTransport() : ITransport, SecretManagement {
    private val secretEnvName = "REDOX_SECRET"
    private val redoxTimeout = 1000
    private val redoxBaseUrl = "https://api.redoxengine.com"
    private val redoxAuthPath = "/auth/authenticate"
    private val redoxEndpointPath = "/endpoint"
    private val redoxKey = "apiKey"
    private val redoxSecret = "secret"
    private val redoxAccessToken = "accessToken"
    private val jsonMimeType = "application/json"
    private val redoxMessageId = "messageId"

    enum class ResultStatus { SUCCESS, FAILURE, NOT_SENT, NEVER_ATTEMPTED }

    data class SendResult(val itemId: String, val status: ResultStatus, val redoxId: Int? = null)

    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val redoxTransportType = transportType as RedoxTransportType
        if (header.content == null)
            error("No content to send to redox for report ${header.reportFile.reportId}")
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val messages = String(header.content).split("\n") // NDJSON content
        // All of these are needed in the large finally block below
        var nextRetryItems = mutableListOf<String>()
        var attemptedCount: Int = 0
        var successCount: Int = 0
        val sendUrl = "${getBaseUrl(redoxTransportType)}$redoxEndpointPath"
        var resultMsg = ""
        var results = mutableListOf<RedoxTransport.SendResult>()
        context.logger.info(
            "The incoming retry item list for ${header.reportFile.reportId} is: " +
                (retryItems?.joinToString(",") ?: "null")
        )
        try {
            val (key, secret) = getKeyAndSecret(redoxTransportType)
            // DevNote: Redox access tokens live for many days
            val token = fetchToken(redoxTransportType, key, secret, context)
            if (token == null) {
                actionHistory.trackActionResult("Failure: fetch redox token failed.  Requesting retry.")
                return retryItems ?: RetryToken.allItems // finally block below will still execute.
            }
            messages.forEachIndexed() { index, message ->
                val itemId = "${header.reportFile.reportId}-$index"
                val sendResult = when {
                    (retryItems == null) ||
                        RetryToken.isAllItems(retryItems)
                        || retryItems.contains(index.toString())
                    -> {
                        attemptedCount++
                        try {
                            sendItem(sendUrl, token, message, itemId)
                        } catch (t: Throwable) {
                            context.logger.log(
                                Level.SEVERE, "Redox Exception for item $itemId, ${header.reportFile.reportId}", t
                            )
                            SendResult(itemId, ResultStatus.FAILURE)
                        }
                    }
                    else ->
                        SendResult(itemId, ResultStatus.NOT_SENT)
                }
                when (sendResult.status) {
                    ResultStatus.SUCCESS -> successCount++
                    ResultStatus.FAILURE -> nextRetryItems.add(index.toString())
                    else -> { /* do nothing */
                    }
                }
                results.add(sendResult)
            }
        } catch (t: Throwable) {
            resultMsg =
                "Exception during REDOX send of reportId ${header.reportFile.reportId}: " +
                "  ${t.localizedMessage}.  "
            context.logger.log(Level.WARNING, resultMsg, t)
            if (successCount == 0) {
                nextRetryItems = (retryItems ?: RetryToken.allItems) as MutableList<String>
                // If even one redox message got through, we'll call that 'send' rather than send_error
                actionHistory.setActionType(TaskAction.send_error)
            }
        } finally {
            val statusStr = when {
                attemptedCount == 0 -> "Failure prior to first send"
                successCount == attemptedCount -> "Success"
                successCount == 0 -> "Failure"
                else -> "Partial Failure"
            }
            resultMsg = resultMsg + "$statusStr: $successCount of $attemptedCount items successfully sent to $sendUrl"
            actionHistory.trackActionResult(resultMsg)
            context.logger.info(resultMsg)
            if (successCount > 0) {
                // only create a child report in the history, if something actually worked.
                actionHistory.trackSentReport(receiver, sentReportId, null, sendUrl, resultMsg, successCount)
                val itemLineages = Report.createItemLineagesFromDb(header, sentReportId)
                if (itemLineages != null) {
                    Report.decorateItemLineagesWithTransportResults(
                        itemLineages,
                        createTransportResults(results, retryItems != null)
                    )
                    actionHistory.trackItemLineages(itemLineages)
                }
            }
        }
        if (nextRetryItems.isNotEmpty()) {
            context.logger.info(
                "The outgoing retry item list for ${header.reportFile.reportId} is: " +
                    nextRetryItems.joinToString(",")
            )
            return nextRetryItems
        } else {
            return null
        }
    }

    /**
     * Map redox send results onto strings that we can store in the item_lineage table.
     */
    private fun createTransportResults(results: List<SendResult>, isRetry: Boolean): List<String> {
        return results.map {
            when (it.status) {
                ResultStatus.SUCCESS -> it.redoxId.toString()
                ResultStatus.FAILURE -> if (isRetry) "RETRY_FAILURE" else "FAILURE"
                ResultStatus.NOT_SENT -> "NOT_SENT" + if (isRetry) " (likely sent in prior attempt)" else "(bug!!)"
                ResultStatus.NEVER_ATTEMPTED -> error("Should not reach here if redox tried to send this item")
            }
        }
    }

    private fun getBaseUrl(redox: RedoxTransportType): String {
        return redox.baseUrl ?: redoxBaseUrl
    }

    private fun getKeyAndSecret(redox: RedoxTransportType): Pair<String, String> {
        // Dev Note: The Redox API key doesn't change, while the secret can
        val secret = secretService.fetchSecret(secretEnvName) ?: ""
        if (secret.isBlank()) error("Unable to find $secretEnvName")
        return Pair(redox.apiKey, secret)
    }

    fun fetchToken(
        redox: RedoxTransportType,
        key: String,
        secret: String,
        context: ExecutionContext
    ): String? {
        val url = "${getBaseUrl(redox)}$redoxAuthPath"
        val (_, _, result) = Fuel
            .post(url)
            .header(CONTENT_TYPE to jsonMimeType)
            .timeout(redoxTimeout)
            .body("""{"$redoxKey": "$key", "$redoxSecret": "$secret"}""")
            .responseJson()
        return when (result) {
            is Result.Success -> {
                if (result.value.obj().has(redoxAccessToken)) {
                    result.value.obj().getString(redoxAccessToken)
                } else {
                    context.logger.log(Level.SEVERE, "Redox Auth call succeeded but token parsing failed")
                    null
                }
            }
            is Result.Failure -> {
                context.logger.log(Level.SEVERE, "Redox Auth call failed: ${result.error.message}")
                null
            }
            else -> {
                context.logger.log(Level.SEVERE, "Redox Auth called failed")
                null
            }
        }
    }

    fun sendItem(
        sendUrl: String,
        token: String,
        message: String,
        id: String,
    ): SendResult {
        val (_, _, result) = Fuel
            .post(sendUrl)
            .header(CONTENT_TYPE to jsonMimeType, AUTHORIZATION to "Bearer $token")
            .timeout(redoxTimeout)
            .body(message)
            .responseJson()
        return when (result) {
            is Result.Success -> {
                val messageId = result.value.obj()
                    .getJSONObject("Meta")
                    .getJSONObject("Message")
                    .getInt("ID")
                SendResult(id, ResultStatus.SUCCESS, messageId)
            }
            is Result.Failure -> {
                SendResult(id, ResultStatus.FAILURE)
            }
            else -> SendResult(id, ResultStatus.FAILURE)
        }
    }
}