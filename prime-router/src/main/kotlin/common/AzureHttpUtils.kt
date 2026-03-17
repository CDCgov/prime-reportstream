package gov.cdc.prime.router.common

import com.microsoft.azure.functions.HttpRequestMessage

/**
 * Utility object that provides methods for handling Azure HTTP requests.
 * This object contains helper functions to extract the sender's IP address
 * from HTTP request headers.
 */
object AzureHttpUtils {

    /**
     * Retrieves the sender's IP address from an [HttpRequestMessage].
     *
     * @param request The HTTP request message from which to extract the sender's IP address.
     * @return The sender's IP address as a [String], or `null` if not found.
     */
    fun getSenderIP(request: HttpRequestMessage<*>): String? =
        (request.headers["x-forwarded-for"]?.split(",")?.firstOrNull())
            ?: request.headers["x-azure-clientip"]

    /**
     * Retrieves the sender's IP address from a map of HTTP headers.
     *
     * @param headers A map of HTTP headers from which to extract the sender's IP address.
     * @return The sender's IP address as a [String], or `null` if not found.
     */
    fun getSenderIP(headers: Map<String, String>): String? =
        (headers["x-forwarded-for"]?.split(",")?.firstOrNull())
            ?: headers["x-azure-clientip"]
}