package gov.cdc.prime.router.common

import io.ktor.client.HttpClient
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import java.lang.reflect.Field
import kotlin.test.Test

class HttpClientUtilsTests {

    @BeforeEach
    fun setup() {
        HttpClientUtils.reset()
    }

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

        // accessTokenHash should be initialized as 0
        val accessTokenHash: Field = HttpClientUtils::class.java.getDeclaredField("accessTokenHash")
        accessTokenHash.trySetAccessible()
        assertEquals(0, accessTokenHash.get(Int::class))
    }

    @Test
    fun `test httpClient is a singleton and instantiating it does not impact value of other fields`() {
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
        assertEquals(0, accessTokenHash.get(Int::class))
    }

    @Test
    fun `test httpClientWithAuth is a singleton when arg access token is same and other fields are not impacted`() {
        val httpClientWithAuth1 = HttpClientUtils.getDefaultHttpClient("mr_token_face")
        val httpClientWithAuthObjRef1 = System.identityHashCode(httpClientWithAuth1)

        // request the httpClient obj again
        val httpClientWithAuth2 = HttpClientUtils.getDefaultHttpClient("mr_token_face")
        val httpClientWithAuthObjRef2 = System.identityHashCode(httpClientWithAuth2)

        // should still be the same object
        assertEquals(httpClientWithAuthObjRef1, httpClientWithAuthObjRef2)

        // httpClient should still be null
        val httpClient: Field = HttpClientUtils::class.java.getDeclaredField("httpClient")
        httpClient.trySetAccessible()
        assertEquals(null, httpClient.get(HttpClient::class))

        // accessTokenHash should be the hashCode for the auth token argument we gave
        val expectedAccessTokenHash = "mr_token_face".hashCode()
        val accessTokenHash: Field = HttpClientUtils::class.java.getDeclaredField("accessTokenHash")
        accessTokenHash.trySetAccessible()
        assertEquals(expectedAccessTokenHash, accessTokenHash.get(Int::class))
    }

    @Test
    fun `test httpClientWithAuth is replaced when arg access token is new and other fields are not impacted`() {
        val httpClientWithAuth1 = HttpClientUtils.getDefaultHttpClient("mr_token_face")
        val httpClientWithAuthObjRef1 = System.identityHashCode(httpClientWithAuth1)

        // request the httpClient obj again
        val httpClientWithAuth2 = HttpClientUtils.getDefaultHttpClient("uncle_token_face")
        val httpClientWithAuthObjRef2 = System.identityHashCode(httpClientWithAuth2)

        // should no longer be the same object
        assertNotEquals(httpClientWithAuthObjRef1, httpClientWithAuthObjRef2)

        // httpClient should still be null
        val httpClient: Field = HttpClientUtils::class.java.getDeclaredField("httpClient")
        httpClient.trySetAccessible()
        assertEquals(null, httpClient.get(HttpClient::class))

        // accessTokenHash should be the hashCode for last the auth token argument we gave
        val expectedAccessTokenHash = "uncle_token_face".hashCode()
        val accessTokenHash: Field = HttpClientUtils::class.java.getDeclaredField("accessTokenHash")
        accessTokenHash.trySetAccessible()
        assertEquals(expectedAccessTokenHash, accessTokenHash.get(Int::class))
    }
}