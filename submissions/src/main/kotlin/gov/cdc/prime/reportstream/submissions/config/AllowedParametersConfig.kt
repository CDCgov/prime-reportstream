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
 * These properties will be automatically injected into the `headers` and `queryParameters` lists when the
 * Spring application context is initialized.
 *
 * @property headers A list of allowed HTTP header names that are expected in incoming requests.
 * @property queryParameters A list of allowed query parameter names that can be used in incoming requests.
 */
@Component
@ConfigurationProperties(prefix = "allowed")
class AllowedParametersConfig {
    /**
     * A list of allowed HTTP headers that can be accepted by the API.
     * Each entry in the list represents a header name expected in the incoming request.
     */
    var headers: List<String> = emptyList()

    /**
     * A list of allowed query parameters that the API can accept in incoming requests.
     * Each entry in the list represents a query parameter name expected in the incoming request.
     */
    var queryParameters: List<String> = emptyList()
}