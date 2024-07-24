package gov.cdc.prime.router.common

import junit.framework.TestCase.assertEquals
import kotlin.test.Test

class HttpClientUtilsTests {

    @Test
    fun `test httpClient is a singleton`() {
        // create the httpClient singleton
        val httpClient1 = HttpClientUtils.getDefaultHttpClient()
        val httpClientObjRef1 = System.identityHashCode(httpClient1)

        // request the httpClient obj again
        val httpClient2 = HttpClientUtils.getDefaultHttpClient()
        val httpClientObjRef2 = System.identityHashCode(httpClient2)

        // should still be the same object
        assertEquals(httpClientObjRef1, httpClientObjRef2)
    }
}