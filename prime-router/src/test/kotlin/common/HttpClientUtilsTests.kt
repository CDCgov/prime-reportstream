package gov.cdc.prime.router.common

import gov.cdc.prime.router.cli.ApiMockEngine
import gov.cdc.prime.router.transport.TokenInfo
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class HttpClientUtilsTests {
    private val sampleRespBodyJson = """[{
        "lookupTableVersionId" : 6,
        "tableName" : "ethnicity",
        "tableVersion" : 1,
        "isActive" : true,
        "createdBy" : "local@test.com",
        "createdAt" : "2023-11-13T15:38:50.495Z",
        "tableSha256Checksum" : "67a9db3bb62a79b4a9d22126f58eebb15dd99a2a2a81bdf4ff740fa884fd5635"
    }"""

    @Test
    fun `test get wrappers`() {
        val clientWithMockEngine = ApiMockEngine(
            "/fakeEndpoint/get_001",
            HttpStatusCode.OK,
            body = sampleRespBodyJson
        ) {
            assertEquals(it.method.value, HttpMethod.Get.value)
            assertEquals(it.url.encodedPath, "/fakeEndpoint/get_001")
        }.client()

        val result = HttpClientUtils.getWithStringResponse(
            url = "fakeEndpoint/get_001",
            httpClient = clientWithMockEngine
        )

        assertNotNull(result, "Expect a pair of response and response body as json string.")
        assertEquals(result.first.status, HttpStatusCode.OK, "Expect a OK response status.")

        val clientWithMockEngine2 = ApiMockEngine(
            "/fakeEndpoint/get_002",
            HttpStatusCode.BadRequest,
            body = sampleRespBodyJson
        ) {
            assertEquals(it.method.value, HttpMethod.Get.value)
            assertEquals(it.url.encodedPath, "/fakeEndpoint/get_002")
        }.client()

        val result2 = HttpClientUtils.getWithStringResponse(
            url = "fakeEndpoint/get_002",
            httpClient = clientWithMockEngine2
        )

        assertNotNull(result2, "Expect a pair of response and response body as json string.")
        assertEquals(result2.first.status, HttpStatusCode.BadRequest, "Expect a Bad Request response status.")
    }

    @Test
    fun `test put wrappers`() {
        val clientWithMockEngine = ApiMockEngine(
            "/fakeEndpoint/put_001",
            HttpStatusCode.OK,
            body = sampleRespBodyJson
        ) {
            assertEquals(it.method.value, HttpMethod.Put.value)
            assertEquals(it.url.encodedPath, "/fakeEndpoint/put_001")
        }.client()

        val result = HttpClientUtils.putWithStringResponse(
            url = "fakeEndpoint/put_001",
            httpClient = clientWithMockEngine,
            jsonPayload = sampleRespBodyJson
        )

        assertNotNull(result, "Expect a pair of response and response body as json string.")
        assertEquals(result.first.status, HttpStatusCode.OK, "Expect a OK response status.")

        val clientWithMockEngine2 = ApiMockEngine(
            "/fakeEndpoint/put_002",
            HttpStatusCode.BadRequest,
            body = sampleRespBodyJson
        ) {
            assertEquals(it.method.value, HttpMethod.Put.value)
            assertEquals(it.url.encodedPath, "/fakeEndpoint/put_002")
        }.client()

        val result2 = HttpClientUtils.putWithStringResponse(
            url = "fakeEndpoint/put_002",
            httpClient = clientWithMockEngine2,
            jsonPayload = sampleRespBodyJson
        )

        assertNotNull(result2, "Expect a pair of response and response body as json string.")
        assertEquals(result2.first.status, HttpStatusCode.BadRequest, "Expect a Bad Request response status.")
    }

    @Test
    fun `test post wrappers`() {
        val fakeUrlPath = "/fakeEndpoint/post_001"
        val clientWithMockEngine = ApiMockEngine(
            fakeUrlPath,
            HttpStatusCode.OK,
            body = sampleRespBodyJson
        ) {
            assertEquals(it.method.value, HttpMethod.Post.value)
            assertEquals(it.url.encodedPath, fakeUrlPath)
        }.client()

        val result = HttpClientUtils.postWithStringResponse(
            url = "fakeEndpoint/post_001",
            httpClient = clientWithMockEngine,
            jsonPayload = sampleRespBodyJson
        )

        assertNotNull(result, "Expect a pair of response and response body as json string.")
        assertEquals(result.first.status, HttpStatusCode.OK, "Expect a OK response status.")
        assertEquals(result.second, sampleRespBodyJson, "Expect a given payload in response.")

        val fakeUrlPath2 = "/fakeEndpoint/post_002"
        val clientWithMockEngine2 = ApiMockEngine(
            fakeUrlPath2,
            HttpStatusCode.BadRequest,
            body = sampleRespBodyJson
        ) {
            assertEquals(it.method.value, HttpMethod.Post.value)
            assertEquals(it.url.encodedPath, fakeUrlPath2)
        }.client()

        val result2 = HttpClientUtils.postWithStringResponse(
            url = fakeUrlPath2,
            httpClient = clientWithMockEngine2,
            jsonPayload = sampleRespBodyJson
        )

        assertNotNull(result2, "Expect a pair of response and response body as json string.")
        assertEquals(result2.first.status, HttpStatusCode.BadRequest, "Expect a Bad Request response status.")
    }

    @Test
    fun `test post wrappers on 5XX code`() {
        val fakeUrlPath = "/fakeEndpoint/post_bad_payload"
        val clientWithMockEngine = ApiMockEngine(
            fakeUrlPath,
            HttpStatusCode.BadGateway,
            body = "does not matter"
        ) {
            assertEquals(it.method.value, HttpMethod.Post.value)
            assertEquals(it.url.encodedPath, fakeUrlPath)
        }.client()

        val (response, _) = HttpClientUtils.postWithStringResponse(
            url = "fakeEndpoint/post_bad_payload",
            httpClient = clientWithMockEngine,
            jsonPayload = """{"lookupTableVersionId" ---- 6}"""
        )

        assertEquals(response.status, HttpStatusCode.BadGateway)
    }

    @Test
    fun `test delete wrappers response OK`() {
        val fakeUrlPath = "/fakeEndpoint/delete_resource"
        val clientWithMockEngine = ApiMockEngine(
            fakeUrlPath,
            HttpStatusCode.OK,
            body = "does not matter"
        ) {
            assertEquals(it.method.value, HttpMethod.Delete.value)
            assertEquals(it.url.encodedPath, fakeUrlPath)
        }.client()

        val result = HttpClientUtils.deleteWithStringResponse(
            url = "fakeEndpoint/delete_resource",
            httpClient = clientWithMockEngine,
        )
        assertNotNull(result, "Expect a pair of response and response body as json string.")
        assertEquals(result.first.status, HttpStatusCode.OK, "Expect a OK Request response status.")
    }

    @Test
    fun `test submitForm wrappers response OK`() {
        val fakeUrlPath = "/fakeEndpoint/submit_form"
        val clientWithMockEngine = ApiMockEngine(
            fakeUrlPath,
            HttpStatusCode.OK,
            body = """{"access_token": "AYjcyMzY3ZDhiNmJkNTY",
                 "refresh_token": "RjY2NjM5NzA2OWJjuE7c", 
                 "token_type": "Bearer", "expires_in": 3600}"""
        ) {
            assertEquals(it.method.value, HttpMethod.Post.value)
            assertEquals(it.url.encodedPath, fakeUrlPath)
        }.client()

        val result = HttpClientUtils.submitFormT<TokenInfo>(
            url = "fakeEndpoint/submit_form",
            formParams = mapOf(
                Pair("grant_type", "authorization_code"),
                Pair("redirect_uri", "fake-redirect-001"),
                Pair("client_id", "437ry35rfy4f5fh4"),
                Pair("code", "code001"),
                Pair("code_verifier", "754753977397")
            ),
            httpClient = clientWithMockEngine,
        )
        assertNotNull(result, "Expect a object if type <T> - TokenInfo.")
    }

    @Test
    fun `test submitForm wrappers malformed json response`() {
        val fakeUrlPath = "/fakeEndpoint/submit_form"
        val clientWithMockEngine = ApiMockEngine(
            fakeUrlPath,
            HttpStatusCode.OK,
            body = """{"access_token": "AYjcyMzY3ZDhiNmJkNTY",
                 "refresh_token": "RjY2NjM5NzA2OWJjuE7c", 
                 "token_type"::::"Bearer", "expires_in": 3600}"""
        ) {
            assertEquals(it.method.value, HttpMethod.Post.value)
            assertEquals(it.url.encodedPath, fakeUrlPath)
        }.client()

        assertFailsWith<JsonConvertException>(
            block = {
                HttpClientUtils.submitFormT<TokenInfo>(
                    url = "fakeEndpoint/submit_form",
                    formParams = mapOf(
                        Pair("grant_type", "authorization_code"),
                        Pair("redirect_uri", "fake-redirect-001"),
                        Pair("client_id", "437ry35rfy4f5fh4"),
                        Pair("code", "code001"),
                        Pair("code_verifier", "754753977397")
                    ),
                    httpClient = clientWithMockEngine,
                )
            }
        )
    }

    @Test
    fun `test head wrappers happy case`() {
        val fakeUrlPath = "/fakeEndpoint/head_operation"
        val clientWithMockEngine = ApiMockEngine(
            fakeUrlPath,
            HttpStatusCode.OK,
            body = """place-holder"""
        ) {
            assertEquals(it.method.value, HttpMethod.Head.value)
            assertEquals(it.url.encodedPath, fakeUrlPath)
        }.client()

        val (response, respStr) = HttpClientUtils.headWithStringResponse(
            url = "fakeEndpoint/head_operation",
            httpClient = clientWithMockEngine,
        )

        assertEquals(response.status.value, HttpStatusCode.OK.value)
        assertEquals(respStr, "place-holder")
    }
}