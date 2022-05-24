package gov.cdc.prime.router.fhirengine.http.extensions

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpResponseMessage

/**
 * Set the content-type http header
 */
fun HttpResponseMessage.Builder.contentType(contentType: String) {
    this.header(HttpHeaders.CONTENT_TYPE, contentType)
}