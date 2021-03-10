package gov.cdc.prime.router.transport

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.AUTHORIZATION
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import org.apache.logging.log4j.kotlin.Logging
import java.io.Closeable

class RedoxTransport : ITransport, Logging {
    private val secretEnvName = "REDOX_SECRET"
    private val redoxTimeout = 1000
    private val redoxBaseUrl = "https://api.redoxengine.com"
    private val redoxAuthPath = "/auth/authenticate"
    private val redoxEndpointPath = "/endpoint"
    private val redoxKey = "apiKey"
    private val redoxSecret = "secret"
    private val redoxAccessToken = "accessToken"
    private val jsonMimeType = "application/json"

    enum class ResultStatus { SUCCESS, FAILURE, NOT_SENT }
    data class SendResult(val itemId: String, val status: ResultStatus, val redoxId: Int? = null)

    override fun startSession(receiver: Receiver): Closeable? {
        return null
    }

    override fun send(
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        session: Any?,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val redoxTransportType = receiver.transport as RedoxTransportType
        val (key, secret) = getKeyAndSecret(redoxTransportType)
        if (header.content == null)
            error("No content to send to redox for report ${header.reportFile.reportId}")
        val messages = String(header.content).split("\n") // NDJSON content
        val token = fetchToken(redoxTransportType, key, secret)
        if (token == null) {
            actionHistory.trackActionResult("Failure: fetch redox token failed.  Requesting retry of allItems")
            return RetryToken.allItems
        }
        val sendUrl = "${getBaseUrl(redoxTransportType)}$redoxEndpointPath"
        // DevNote: Redox access tokens live for many days

        var attemptedCount = 0
        var successCount = 0
        val nextRetryItems = mutableListOf<String>()
        val results = messages.mapIndexed { index, message ->
            val itemId = "${header.reportFile.reportId}-$index"
            val sendResult = when {
                (retryItems == null) || RetryToken.isAllItems(retryItems) || retryItems.contains(index.toString()) -> {
                    attemptedCount++
                    sendItem(sendUrl, token, message, itemId)
                }
                else ->
                    SendResult(itemId, ResultStatus.NOT_SENT)
            }
            when (sendResult.status) {
                ResultStatus.SUCCESS -> successCount++
                ResultStatus.FAILURE -> nextRetryItems.add(index.toString())
                else -> { /* do nothing */ }
            }
            sendResult
        }
        val statusStr = when {
            attemptedCount == 0 -> "Weird"
            successCount == attemptedCount -> "Success"
            successCount == 0 -> "Failure"
            else -> "Partial Failure"
        }
        val resultMsg = "$statusStr: $successCount of $attemptedCount items successfully sent to $sendUrl"
        actionHistory.trackActionResult(resultMsg)
        logger.info(resultMsg)
        actionHistory.trackSentReport(receiver, sentReportId, null, sendUrl, resultMsg, successCount)
        val itemLineages = Report.createItemLineagesFromDb(header, sentReportId)
        if (itemLineages != null) {
            Report.decorateItemLineagesWithTransportResults(
                itemLineages,
                createTransportResults(results, retryItems != null)
            )
            actionHistory.trackItemLineages(itemLineages)
        }
        return if (nextRetryItems.isNotEmpty()) nextRetryItems else null
    }

    /**
     * Map redox results onto strings that we can store in the item_lineage table.
     */
    private fun createTransportResults(results: List<SendResult>, isRetry: Boolean): List<String> {
        return results.map {
            when (it.status) {
                ResultStatus.SUCCESS -> it.redoxId.toString()
                ResultStatus.FAILURE -> if (isRetry) "RETRY_FAILURE" else "FAILURE"
                ResultStatus.NOT_SENT -> "NOT_SENT" + if (isRetry) " (likely sent in prior attempt)" else "(bug!!)"
            }
        }
    }

    private fun getBaseUrl(redox: RedoxTransportType): String {
        return redox.baseUrl ?: redoxBaseUrl
    }

    private fun getKeyAndSecret(redox: RedoxTransportType): Pair<String, String> {
        // Dev Note: The Redox API key doesn't change, while the secret can
        val secret = System.getenv(secretEnvName) ?: ""
        if (secret.isBlank()) error("Unable to find $secretEnvName")
        return Pair(redox.apiKey, secret)
    }

    private fun fetchToken(
        redox: RedoxTransportType,
        key: String,
        secret: String
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
                    logger.error("Redox Auth call succeeded but token parsing failed")
                    null
                }
            }
            is Result.Failure -> {
                logger.error("Redox Auth call failed: ${result.error.message}")
                null
            }
            else -> {
                logger.error("Redox Auth called failed")
                null
            }
        }
    }

    private fun sendItem(
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