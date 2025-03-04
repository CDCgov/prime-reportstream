package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import org.junit.jupiter.api.Nested
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.test.Test

class ReportFileFunctionTests {
    @Nested
    inner class HistoryApiParameters {
        @Test
        fun `test fileName gets encoded`() {
            val httpRequestMessage = MockHttpRequestMessage()
            val fileName = "InterPartner~ELIMS_ELR~CDC~KY~Test~Test~20240315150045113~STOP~13_72321_05227632_40060.hl7"
            httpRequestMessage.parameters["fileName"] = fileName

            val historyApiParameters = ReportFileFunction.HistoryApiParameters(httpRequestMessage.parameters)
            assertThat(historyApiParameters.fileName).isEqualTo(URLEncoder.encode(fileName, Charset.defaultCharset()))
        }

        @Test
        fun `test fileName returns null`() {
            val httpRequestMessage = MockHttpRequestMessage()

            val historyApiParameters = ReportFileFunction.HistoryApiParameters(httpRequestMessage.parameters)
            assertThat(historyApiParameters.fileName).isEqualTo(null)
        }
    }
}