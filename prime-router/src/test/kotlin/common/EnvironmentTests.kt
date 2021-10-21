package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.cli.Environment
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.net.URL
import kotlin.test.assertFailsWith

class EnvironmentTests {
    @Test
    fun `form path test`() {
        val path1 = "/api/reports"
        val path2 = "api/reports"
        val url = Environment.LOCAL.baseUrl

        assertThat(Environment.LOCAL.formUrl(path1)).equals("$url$path1")
        assertThat(Environment.LOCAL.formUrl(path2)).equals("$url/$path2")
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
        assertThat(Environment.getBaseUrl(URL("http://localhost"))).equals("localhost")
        assertThat(Environment.getBaseUrl(URL("http://localhost:7071"))).equals("localhost:7071")
        assertThat(Environment.getBaseUrl(URL("https://localhost"))).equals("localhost")
        assertThat(Environment.getBaseUrl(URL("https://localhost:8443"))).equals("localhost:8443")
    }

    @Test
    fun `test local okta access token`() {
        assertThat(Environment.LOCAL.accessToken).isEqualTo(Environment.dummyOktaAccessToken)
    }
}