package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.net.URL
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
        assertThat(Environment.getBaseUrl(URL("http://localhost"))).isEqualTo("localhost")
        assertThat(Environment.getBaseUrl(URL("http://localhost:7071"))).isEqualTo("localhost:7071")
        assertThat(Environment.getBaseUrl(URL("https://localhost"))).isEqualTo("localhost")
        assertThat(Environment.getBaseUrl(URL("https://localhost:8443"))).isEqualTo("localhost:8443")
    }
}