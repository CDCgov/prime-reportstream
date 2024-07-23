package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.BeforeEach
import java.lang.reflect.Field
import kotlin.test.Test

class HttpClientUtilsTests {

    @BeforeEach
    fun setup() {
        HttpClientUtils.reset()
    }

    @Test
    fun `test default values for client is null`() {
        // httpClient should be initialized as null
        val httpClientField: Field = HttpClientUtils::class.java.getDeclaredField("httpClient")
        httpClientField.trySetAccessible()
        assertEquals(null, httpClientField.get(HttpClient::class))
    }

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