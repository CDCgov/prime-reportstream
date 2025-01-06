package gov.cdc.prime.router.azure.service

import ca.uhn.hl7v2.model.Message
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.HttpUtilities.Companion.isSuccessful
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7ACKUtils
import gov.cdc.prime.router.fhirengine.utils.HL7MessageHelpers
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import org.apache.logging.log4j.kotlin.Logging

/**
 * Builder class to create either JSON or HL7 response types based on the contents of the
 * submitted reports
 */
class SubmissionResponseBuilder(
    private val hL7ACKUtils: HL7ACKUtils = HL7ACKUtils(),
) : Logging {

    /**
     * Builds a response to send to the client after submitting a report
     *
     * This will be an HL7 ACK response given the client has enabled it and requested it. It will otherwise
     * default to our default JSON response
     */
    fun buildResponse(
        sender: Sender,
        responseStatus: HttpStatus,
        request: HttpRequestMessage<String?>,
        submission: DetailedSubmissionHistory?,
    ): HttpResponseMessage {
        // Azure handles all headers as lowercase
        val contentType = request.headers[HttpHeaders.CONTENT_TYPE.lowercase()]
        val requestBody = request.body
        return when (val responseType = determineResponseType(sender, responseStatus, contentType, requestBody)) {
            is HL7ResponseType -> {
                logger.info("Returning ACK response")
                val responseBody = hL7ACKUtils.generateOutgoingACKMessage(responseType.message)
                request.createResponseBuilder(responseStatus)
                    .header(HttpHeaders.CONTENT_TYPE, HttpUtilities.hl7V2MediaType)
                    .body(responseBody)
                    .build()
            }
            is JsonResponseType -> {
                val responseBody = JacksonMapperUtilities
                    .allowUnknownsMapper
                    .writeValueAsString(submission)
                request
                    .createResponseBuilder(responseStatus)
                    .header(HttpHeaders.CONTENT_TYPE, HttpUtilities.jsonMediaType)
                    .body(responseBody)
                    .header(
                        HttpHeaders.LOCATION,
                        request.uri.resolve(
                            "/api/waters/report/${submission?.reportId}/history"
                        ).toString()
                    )
                    .build()
            }
        }
    }

    /**
     * Figures out in what format we should respond to a submission with
     *
     * @return SubmissionResponseType the response type defined in this file
     */
    private fun determineResponseType(
        sender: Sender,
        responseStatus: HttpStatus,
        contentType: String?,
        requestBody: String?,
    ): SubmissionResponseType {
        val maybeACKMessage = hl7SuccessResponseRequired(sender, responseStatus, contentType, requestBody)
        return when {
            maybeACKMessage != null -> HL7ResponseType(maybeACKMessage)
            else -> JsonResponseType
        }
    }

    /**
     * This function will return true if the following conditions are met:
     * - The sender has the "hl7AcknowledgementEnabled" field set to true
     * - The HL7 message has been processed successfully
     * - The submitted HL7 contains MSH.15 == "AL"
     * - The submitted HL7 is not a batch message
     *
     * @return HL7 message if ACK required or null otherwise
     */
    private fun hl7SuccessResponseRequired(
        sender: Sender,
        responseStatus: HttpStatus,
        contentType: String?,
        requestBody: String?,
    ): Message? {
        val acceptAcknowledgmentTypeRespondValues = setOf("AL") // AL means "Always"
        return if (
            sender.hl7AcknowledgementEnabled &&
            responseStatus.isSuccessful() &&
            contentType == HttpUtilities.hl7V2MediaType &&
            requestBody != null
        ) {
            val messageCount = HL7MessageHelpers.messageCount(requestBody)
            val isBatch = HL7Reader.isBatch(requestBody, messageCount)

            if (!isBatch && messageCount == 1) {
                val message = HL7Reader.parseHL7Message(requestBody)
                val acceptAcknowledgementType = HL7Reader.getAcceptAcknowledgmentType(message)
                val ackResponseRequired = acceptAcknowledgmentTypeRespondValues.contains(acceptAcknowledgementType)
                if (ackResponseRequired) {
                    message
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }
}

/**
 * Rather than an enum, we have used a hierarchy that allows an HL7 response type to hold onto the already
 * parsed message during our check of MSH.15 to avoid doing that work twice.
 */
private sealed interface SubmissionResponseType
private data class HL7ResponseType(val message: Message) : SubmissionResponseType
private data object JsonResponseType : SubmissionResponseType