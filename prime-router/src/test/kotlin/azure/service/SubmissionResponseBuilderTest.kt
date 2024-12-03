package gov.cdc.prime.router.azure.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import java.time.OffsetDateTime
import kotlin.test.Test

class SubmissionResponseBuilderTest {

    inner class Fixture {
        val sender = UniversalPipelineSender(
            "test",
            "phd",
            MimeFormat.HL7,
            CustomerStatus.ACTIVE,
            topic = Topic.FULL_ELR,
            hl7AcknowledgementEnabled = true
        )
        val submission = DetailedSubmissionHistory(
            1,
            TaskAction.receive,
            OffsetDateTime.now(),
            reports = mutableListOf(),
            logs = emptyList()
        )

        @Suppress("ktlint:standard:max-line-length")
        val ackHL7 = "MSH|^~\\&|Epic|Hospital|LIMS|StatePHL|20241003000000||ORM^O01^ORM_O01|4AFA57FE-D41D-4631-9500-286AAAF797E4|T|2.5.1|||AL|NE"

        @Suppress("ktlint:standard:max-line-length")
        val noAckHL7 = "MSH|^~\\&|Epic|Hospital|LIMS|StatePHL|20241003000000||ORM^O01^ORM_O01|4AFA57FE-D41D-4631-9500-286AAAF797E4|T|2.5.1|||NE|NE"

        val batchHL7 = """
            FHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202201042030-0800
            BHS|^~\&|||0.0.0.0.1|0.0.0.0.1|202201042030-0800
            $ackHL7
        """.trimIndent()

        // default clock since we are not comparing HL7 in this test file
        val builder = SubmissionResponseBuilder()

        fun buildRequest(
            contentType: String = HttpUtilities.hl7V2MediaType,
            content: String,
        ): HttpRequestMessage<String?> {
            val request = MockHttpRequestMessage(
                content = content
            )
            request.httpHeaders[HttpHeaders.CONTENT_TYPE.lowercase()] = contentType
            return request
        }
    }

    @Test
    fun `build ACK response when enabled for sender and correct conditions `() {
        val f = Fixture()

        val request = f.buildRequest(content = f.ackHL7)

        val response = f.builder.buildResponse(f.sender, HttpStatus.CREATED, request, f.submission)

        assertThat(response.status).isEqualTo(HttpStatus.CREATED)
        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(HttpUtilities.hl7V2MediaType)
    }

    @Test
    fun `build JSON response when enabled for sender but HL7 does not request it`() {
        val f = Fixture()

        val request = f.buildRequest(content = f.noAckHL7)

        val response = f.builder.buildResponse(f.sender, HttpStatus.CREATED, request, f.submission)

        assertThat(response.status).isEqualTo(HttpStatus.CREATED)
        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(HttpUtilities.jsonMediaType)
    }

    @Test
    fun `build JSON response when enabled for sender but is a batch message`() {
        val f = Fixture()

        val request = f.buildRequest(content = f.batchHL7)

        val response = f.builder.buildResponse(f.sender, HttpStatus.CREATED, request, f.submission)

        assertThat(response.status).isEqualTo(HttpStatus.CREATED)
        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(HttpUtilities.jsonMediaType)
    }

    @Test
    fun `build JSON response when processing was unsuccessful`() {
        val f = Fixture()

        val request = f.buildRequest(content = f.batchHL7)

        val response = f.builder.buildResponse(f.sender, HttpStatus.BAD_REQUEST, request, f.submission)

        assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(HttpUtilities.jsonMediaType)
    }
}