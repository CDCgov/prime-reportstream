package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.common.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URL

/**
 * Utilities for commands.
 */
class CommandUtilities {
    companion object {
        /**
         * The API endpoint to check for.  This needs to be a simple operation.
         */
        private const val waitForApiEndpointPath = "api/lookuptables/list"

        /**
         * Waits for the endpoint at [environment] to become available. This function will retry [retries] number of
         * times waiting [pollIntervalSecs] seconds between retries.
         * @throws IOException if a connection was not made
         */
        internal fun waitForApi(environment: Environment, retries: Int = 30, pollIntervalSecs: Long = 1) {
            val url = environment.formUrl(waitForApiEndpointPath)
            val accessToken = OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: error("Unable to obtain Okta access token for environment $environment")
            var retryCount = 0
            while (!isEndpointAvailable(url, accessToken)) {
                retryCount++
                if (retryCount > retries)
                    throw IOException("Unable to connect to the API at $url")
                runBlocking {
                    delay(pollIntervalSecs * 1000)
                }
            }
        }

        /**
         * Is the service running the environment
         */
        internal fun isApiAvailable(environment: Environment): Boolean {
            val url = environment.formUrl(waitForApiEndpointPath)
            val accessToken = OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: error("Unable to obtain Okta access token for environment $environment")
            return isEndpointAvailable(url, accessToken)
        }

        /**
         * Checks if the API can be connected to.
         * @return true is the API is available, false otherwise
         */
        private fun isEndpointAvailable(url: URL, accessToken: String): Boolean {
            val (_, _, result) = Fuel.head(url.toString())
                .authentication()
                .bearer(accessToken)
                .response()
            return when (result) {
                is Result.Success -> true
                else -> false
            }
        }

        private val jsonMapper = jacksonObjectMapper()

        data class DiffRow(val name: String, val baseValue: String, val toValue: String)

        /**
         * Create a list of differences between two JSON strings.
         */
        fun diffJson(base: String, compareTo: String): List<DiffRow> {

            /**
             * Given the [node] call [visitor] all descendant value nodes
             */
            fun walkTree(node: JsonNode, path: String = "", visitor: (name: String, value: String) -> Unit) {
                when {
                    node.isNull -> visitor(path, "null")
                    node.isTextual -> visitor(path, "\"${node.textValue()}\"")
                    node.isBoolean -> visitor(path, node.asText())
                    node.isNumber -> visitor(path, node.asText())
                    node.isArray -> {
                        node.iterator().asSequence().forEachIndexed { index, element ->
                            walkTree(element, "$path[$index]", visitor)
                        }
                    }
                    node.isObject -> {
                        val parentPath = if (path.isBlank()) "" else "$path."
                        node.fields().forEach { entry ->
                            walkTree(entry.value, "$parentPath${entry.key}", visitor)
                        }
                    }
                }
            }

            /**
             * Flatten the [json] structure and create a map of name-value pairs
             */
            fun createMaps(json: String): Map<String, String> {
                val tree: JsonNode = jsonMapper.readTree(json) ?: return emptyMap()
                val resultMap = mutableMapOf<String, String>()
                walkTree(tree) { name, value ->
                    resultMap[name] = value
                }
                return resultMap
            }

            /**
             * Merge the [base] and [compareTo] maps together to create a list of [DiffRow]
             */
            fun mergeMaps(base: Map<String, String>, compareTo: Map<String, String>): List<DiffRow> {
                val commonRows = base
                    .filter { (name, _) -> compareTo.containsKey(name) }
                    .map { (name, baseValue) -> DiffRow(name, baseValue, compareTo[name]!!) }
                val extraBaseRows = base
                    .filter { (name, _) -> !compareTo.containsKey(name) }
                    .map { (name, baseValue) -> DiffRow(name, baseValue, "") }
                val extraCompareToRows = compareTo
                    .filter { (name, _) -> !base.containsKey(name) }
                    .map { (name, compareToValue) -> DiffRow(name, "", compareToValue) }
                return commonRows + extraBaseRows + extraCompareToRows
            }

            val baseMap = createMaps(base)
            val compareToMap = createMaps(compareTo)
            val mergedRows = mergeMaps(baseMap, compareToMap)
            return mergedRows.filter { it.baseValue != it.toValue }.sortedBy { it.name }
        }

        /**
         * Nice way to abort a command
         */
        fun abort(message: String): Nothing {
            throw PrintMessage(message, error = true)
        }
    }
}