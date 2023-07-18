package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.microsoft.azure.functions.HttpStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class OpenAPIDocsFunctionTest {
    @BeforeEach
    fun setup() {
        // no setup needed
    }

    @AfterEach
    fun reset() {
        // no tear down needed
    }

    @Nested
    inner class SwaggerUIResourcesTests {
        @Test
        fun `Test load swagger ui index html successfully`() {
            val httpRequestMessage = MockHttpRequestMessage()
            val response = OpenAPIDocsFunction().getApiDocs(
                httpRequestMessage
            )
            assertThat(response.status).isEqualTo(HttpStatus.OK)
            assertThat(response.getBody().toString()).contains("div id=\"swagger-ui\"")
        }

        @Test
        fun `Test referenced resources load successfully`() {
            val expected = listOf<String>(
                "api.yaml",
                "index.css",
                "swagger-ui.css",
                "swagger-initializer.js",
                "swagger-ui-bundle.js",
                "swagger-ui-standalone-preset.js",
                "favicon-16x16.png",
                "favicon-32x32.png",
            )
            expected.forEach {
                val httpRequestMessage = MockHttpRequestMessage()
                val response = OpenAPIDocsFunction().getSwaggerResources(httpRequestMessage, it)
                assertThat(response.status).isEqualTo(HttpStatus.OK)
            }
        }

        @Test
        fun `Test fails on attempt to load unexpected resources`() {
            val httpRequestMessage = MockHttpRequestMessage()
            val response = OpenAPIDocsFunction().getSwaggerResources(
                httpRequestMessage,
                "unexpected_scripts.php"
            )
            assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}