package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import junit.framework.TestCase.assertEquals
import java.lang.reflect.Field
import kotlin.test.Test

class HttpClientUtilsTests {

    @Test
    fun `test default values for clients and auth hash are null`() {
        // httpClient should be initialized as null
        val httpClientField: Field = HttpClientUtils::class.java.getDeclaredField("httpClient")
        httpClientField.trySetAccessible()
        assertEquals(null, httpClientField.get(HttpClient::class))

        // httpClientWithAuth should be initialized as null
        val httpClientWithAuth: Field = HttpClientUtils::class.java.getDeclaredField("httpClientWithAuth")
        httpClientWithAuth.trySetAccessible()
        assertEquals(null, httpClientWithAuth.get(HttpClient::class))

        // accessTokenHash should be initialized as -1
        val accessTokenHash: Field = HttpClientUtils::class.java.getDeclaredField("accessTokenHash")
        accessTokenHash.trySetAccessible()
        assertEquals(-1, accessTokenHash.get(Int::class))
    }

    @Test
    fun `test httpClient is a singleton and instantiating it does not impact value of field accessTokenHash`() {
        // create the httpClient singleton
        val httpClient1 = HttpClientUtils.getDefaultHttpClient(null)
        val httpClientObjRef1 = System.identityHashCode(httpClient1)

        // request the httpClient obj again
        val httpClient2 = HttpClientUtils.getDefaultHttpClient(null)
        val httpClientObjRef2 = System.identityHashCode(httpClient2)

        // should still be the same object
        assertEquals(httpClientObjRef1, httpClientObjRef2)

        // httpClientWithAuth should still be null
        val httpClientWithAuth: Field = HttpClientUtils::class.java.getDeclaredField("httpClientWithAuth")
        httpClientWithAuth.trySetAccessible()
        assertEquals(null, httpClientWithAuth.get(HttpClient::class))

        // accessTokenHash should still be -1
        val accessTokenHash: Field = HttpClientUtils::class.java.getDeclaredField("accessTokenHash")
        accessTokenHash.trySetAccessible()
        assertEquals(-1, accessTokenHash.get(Int::class))
    }
}