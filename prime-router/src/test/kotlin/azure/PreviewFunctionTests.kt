package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.messages.PreviewMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class PreviewFunctionTests {
    private val mapper = jacksonMapperBuilder().build()

    private fun buildPreviewFunction(): PreviewFunction {
        return PreviewFunction()
    }

    private fun buildRequest(body: String): HttpRequestMessage<String?> {
        val mockRequest = mockk<HttpRequestMessage<String?>>()
        every { mockRequest.headers } returns mapOf(HttpHeaders.AUTHORIZATION.lowercase() to "Bearer dummy")
        every { mockRequest.uri } returns URI.create("http://localhost:7071/api/sender-files")
        every { mockRequest.queryParameters } returns emptyMap()
        every { mockRequest.body } returns body
        return mockRequest
    }

    private fun buildPreviewMessage(): String {
        val content = Files.readString(
            Path.of(
                "./src/testIntegration/resources/datatests/CSV_to_HL7/sample-single-pdi-20210608-0002.csv"
            )
        )
        val previewMessage = PreviewMessage(
            senderName = "ignore.ignore-simple-report",
            receiverName = "ignore.CSV",
            inputContent = content
        )
        return mapper.writeValueAsString(previewMessage)
    }

    @Test
    fun `test checkRequest`() {
        // Happy path
        val body = buildPreviewMessage()
        val mockRequest = buildRequest(body)
        val previewFunction = buildPreviewFunction()
        val functionParams = previewFunction.checkRequest(mockRequest)
        assertThat(functionParams.sender.fullName).isEqualTo("ignore.ignore-simple-report")
    }
}