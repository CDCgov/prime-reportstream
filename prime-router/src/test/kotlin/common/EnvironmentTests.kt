package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.net.URI
import kotlin.test.assertFailsWith

class EnvironmentTests {
    @Test
    fun `form path test`() {
        val path1 = "/api/reports"
        val path2 = "api/reports"
        val url = Environment.LOCAL.url

        assertThat(Environment.LOCAL.formUrl(path1).toString()).isEqualTo("$url$path1")
        assertThat(Environment.LOCAL.formUrl(path2).toString()).isEqualTo("$url/$path2")
    }

    @Test
    fun `get environment test`() {
        assertThat(Environment.get("local")).isEqualTo(Environment.LOCAL)
        assertThat(Environment.get("LOCAL")).isEqualTo(Environment.LOCAL)
        assertFailsWith<IllegalArgumentException>(
            block = {
                Environment.get("DUMMY")
            }
        )
    }

    @Test
    fun `get base url test`() {
        assertThat(Environment.getBaseUrl(URI("http://localhost"))).isEqualTo("localhost")
        assertThat(Environment.getBaseUrl(URI("http://localhost:7071"))).isEqualTo("localhost:7071")
        assertThat(Environment.getBaseUrl(URI("https://localhost"))).isEqualTo("localhost")
        assertThat(Environment.getBaseUrl(URI("https://localhost:8443"))).isEqualTo("localhost:8443")
    }
}