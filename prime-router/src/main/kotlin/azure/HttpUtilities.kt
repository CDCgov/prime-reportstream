package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.PAYLOAD_MAX_BYTES
import java.time.OffsetDateTime

class HttpUtilities {
    companion object {
        const val jsonMediaType = "application/json"

        fun okResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.OK)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun createdResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.CREATED)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        /**
         * This alllows the validator to figure out specific failure, and pass it in here.
         * Can be used for any failed response code.
         & todo other generic failure response methods here could be removed, and replaced with this
         *      generic method, instead of having to create a new method for every HttpStatus code.
         *
         *
         */
        fun notOKResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
            httpStatus: HttpStatus,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(httpStatus)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun badRequestResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun unauthorizedResponse(
            request: HttpRequestMessage<String?>
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .build()
        }

        fun unauthorizedResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun notFoundResponse(
            request: HttpRequestMessage<String?>
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.NOT_FOUND)
                .build()
        }

        fun internalErrorResponse(
            request: HttpRequestMessage<String?>
        ): HttpResponseMessage {
            val body = """{"error": "Internal error at ${OffsetDateTime.now()}"}"""
            return request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun errorJson(message: String): String {
            return """{"error": "$message"}"""
        }

        /**
         * Do a variety of checks on payload size.
         * Returns a Pair (http error code, human readable error message)
         */
        fun payloadSizeCheck(request: HttpRequestMessage<String?>): Pair<HttpStatus, String> {
            val contentLengthStr = request.headers["content-length"]
                ?: return HttpStatus.LENGTH_REQUIRED to "ERROR: No content-length header found.  Refusing this request."
            val contentLength = try {
                contentLengthStr.toLong()
            } catch (e: NumberFormatException) {
                return HttpStatus.LENGTH_REQUIRED to "ERROR: content-length header is not a number"
            }
            when {
                contentLength < 0 -> {
                    return HttpStatus.LENGTH_REQUIRED to "ERROR: negative content-length $contentLength"
                }
                contentLength > PAYLOAD_MAX_BYTES -> {
                    return HttpStatus.PAYLOAD_TOO_LARGE to
                        "ERROR: content-length $contentLength is larger than max $PAYLOAD_MAX_BYTES"
                }
            }
            // content-length header is ok.  Now check size of actual body as well
            val content = request.body
            if (content != null && content.length > PAYLOAD_MAX_BYTES) {
                return HttpStatus.PAYLOAD_TOO_LARGE to
                    "ERROR: body size ${content.length} is larger than max $PAYLOAD_MAX_BYTES " +
                    "(content-length header = $contentLength"
            }
            return HttpStatus.OK to ""
        }
    }
}