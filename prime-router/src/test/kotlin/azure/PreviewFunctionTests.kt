package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.messages.PreviewMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

const val SENDER_NAME = "ignore.ignore-simple-report"
const val RECEIVER_NAME = "ignore.CSV"
const val INPUT_FILE = "./src/testIntegration/resources/datatests/CSV_to_HL7/sample-single-pdi-20210608-0002.csv"
const val OUTPUT_FILE = "./src/test/csv_test_files/expected/pima-az-covid-19.csv"

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

    private fun buildPreviewMessage(): PreviewMessage {
        return PreviewMessage(
            senderName = SENDER_NAME,
            receiverName = RECEIVER_NAME,
            inputContent = Files.readString(Path.of(INPUT_FILE))
        )
    }

    private fun buildPreviewMessageJson(): String {
        return mapper.writeValueAsString(buildPreviewMessage())
    }

    @Test
    fun `test checkRequest`() {
        // Happy path
        val body = buildPreviewMessageJson()
        val mockRequest = buildRequest(body)
        val previewFunction = buildPreviewFunction()
        val functionParams = previewFunction.checkRequest(mockRequest)
        assertThat(functionParams.sender.fullName).isEqualTo(SENDER_NAME)
    }

    @Test
    fun `test processRequest`() {
        val previewFunction = buildPreviewFunction()
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val previewParameters = PreviewFunction.FunctionParameters(
            previewMessage = buildPreviewMessage(),
            sender = settings.findSender(SENDER_NAME)!!,
            receiver = settings.findReceiver(RECEIVER_NAME)!!
        )
        val response = previewFunction.processRequest(previewParameters)
        assertThat(response.receiverName).isEqualTo(RECEIVER_NAME)
        val expectedOutput = Files.readString(Path.of(OUTPUT_FILE))
        assertThat(response.content).isEqualTo(expectedOutput)
    }
}