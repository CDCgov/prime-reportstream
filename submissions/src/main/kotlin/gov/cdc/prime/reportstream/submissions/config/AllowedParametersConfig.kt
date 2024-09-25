package gov.cdc.prime.reportstream.submissions.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration class to load allowed headers and query parameters from the application properties.
 *
 * This class is used to read properties prefixed with "allowed" from the `application.properties` or `application.yml`
 * file and bind them to the `headers` and `queryParameters` fields.
 *
 * Example of properties in the `application.properties` file:
 *
 * ```
 * allowed.headers.client_id=client_id
 * allowed.headers.content_type=content-type
 * allowed.queryParameters.processing=processing
 * allowed.queryParameters.another_param=another
 * ```
 *
 * These properties will be automatically injected into the `headers` and `queryParameters` maps when the
 * Spring application context is initialized.
 *
 * @property headers A map of allowed HTTP header names that are expected in incoming requests.
 * @property queryParameters A map of allowed query parameter names that can be used in incoming requests.
 */
@Component
@ConfigurationProperties(prefix = "allowed")
class AllowedParametersConfig {
    /**
     * A map of allowed HTTP headers that can be accepted by the API.
     * The keys in this map correspond to internal property names, and the values represent the actual header names
     * expected in the incoming request.
     *
     * For example, if the property is defined as `allowed.headers.client_id=client_id`, it will expect a header with
     * the name `client_id` in the incoming HTTP request.
     *
     * Defaults to an empty map if not provided in the configuration.
     */
    var headers: Map<String, String> = emptyMap()

    /**
     * A map of allowed query parameters that the API can accept in incoming requests.
     * The keys in this map correspond to internal property names, and the values represent the actual query parameter
     * names expected in the incoming request.
     *
     * For example, if the property is defined as `allowed.queryParameters.param=test1`, it will allow the
     * query parameter `param` to be used in the request, and multiple values can be passed (e.g., `?param=test1&param=test2`).
     *
     * Defaults to an empty map if not provided in the configuration.
     */
    var queryParameters: Map<String, String> = emptyMap()
}