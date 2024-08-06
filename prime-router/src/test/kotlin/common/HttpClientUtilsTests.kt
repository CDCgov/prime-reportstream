package gov.cdc.prime.router.common

import junit.framework.TestCase.assertEquals
import kotlin.test.Test

class HttpClientUtilsTests {

    @Test
    fun `test httpClient is a singleton`() {
        // create the httpClient singleton
        val httpClient1 = HttpClientUtils.getDefaultHttpClient()

        // request the httpClient obj again
        val httpClient2 = HttpClientUtils.getDefaultHttpClient()

        // should still be the same object
        assertEquals(httpClient1, httpClient2)
    }
}