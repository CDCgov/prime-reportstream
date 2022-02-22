package gov.cdc.prime.router.azure.http.extensions

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpResponseMessage

fun HttpResponseMessage.Builder.contentType(contentType: String) {
    this.header(HttpHeaders.CONTENT_TYPE, contentType)
}