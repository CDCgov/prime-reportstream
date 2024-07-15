package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import junit.framework.TestCase.assertEquals
import java.lang.reflect.Field
import kotlin.test.Test


class HttpClientUtilsTests {

    @Test
    fun `test default values for clients and auth hash are null`() {
        // httpClient should be initialized as null
        var httpClientField: Field = HttpClientUtils::class.java.getDeclaredField("httpClient")
        httpClientField.trySetAccessible()
        assertEquals(null, httpClientField.get(HttpClient::class))

        // httpClientWithAuth should be initialized as null
        var httpClientWithAuth: Field = HttpClientUtils::class.java.getDeclaredField("httpClientWithAuth")
        httpClientWithAuth.trySetAccessible()
        assertEquals(null, httpClientWithAuth.get(HttpClient::class))

        // accessTokenHash should be initialized as null
        var accessTokenHash: Field = HttpClientUtils::class.java.getDeclaredField("accessTokenHash")
        accessTokenHash.trySetAccessible()
        assertEquals(-1, accessTokenHash.get(Int::class))
    }

}