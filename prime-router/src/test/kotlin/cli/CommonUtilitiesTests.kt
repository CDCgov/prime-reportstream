package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import gov.cdc.prime.router.cli.CommandUtilities.Companion.DiffRow
import gov.cdc.prime.router.cli.CommandUtilities.Companion.diffJson
import gov.cdc.prime.router.cli.FileUtilities.saveTableAsCSV
import gov.cdc.prime.router.transport.TokenInfo
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.mockk.Runs
import io.mockk.clearConstructorMockk
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.verify
import java.io.File
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CommonUtilitiesTests {
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

        val result = CommandUtilities.getWithStringResponse(
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

        val result2 = CommandUtilities.getWithStringResponse(
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

        val result = CommandUtilities.putWithStringResponse(
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

        val result2 = CommandUtilities.putWithStringResponse(
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

        val result = CommandUtilities.postWithStringResponse(
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

        val result2 = CommandUtilities.postWithStringResponse(
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

        // when expect success set to true
        assertFailsWith<ServerResponseException>(
            block = {
                CommandUtilities.postWithStringResponse(
                    url = "fakeEndpoint/post_bad_payload",
                    expSuccess = true,
                    httpClient = clientWithMockEngine,
                    jsonPayload = """{"lookupTableVersionId" ---- 6}"""
                )
            }
        )
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

        val result = CommandUtilities.deleteWithStringResponse(
                url = "fakeEndpoint/delete_resource",
                expSuccess = true,
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

        val result = CommandUtilities.submitFormT<TokenInfo>(
            url = "fakeEndpoint/submit_form",
            expSuccess = true,
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
                CommandUtilities.submitFormT<TokenInfo>(
                    url = "fakeEndpoint/submit_form",
                    expSuccess = true,
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

        val (response, respStr) = CommandUtilities.headWithStringResponse(
            url = "fakeEndpoint/head_operation",
            expSuccess = true,
            httpClient = clientWithMockEngine,
        )

        assertEquals(response.status.value, HttpStatusCode.OK.value)
        assertEquals(respStr, "place-holder")
    }

    @Test
    fun `test diffJson with simple json`() {
        val base = """
            {
                "a": "1",
                "b": "2"
            }
        """.trimIndent()

        val compare1 = """
            {
                "b": "2"
            }
        """.trimIndent()
        val diff1 = diffJson(base, compare1)
        assertThat(diff1).isEqualTo(listOf(DiffRow("a", "\"1\"", "")))

        val compare2 = """
            {
                "a": "1",
                "b": "2"
            }
        """.trimIndent()
        val diff2 = diffJson(base, compare2)
        assertThat(diff2).isEqualTo(listOf())

        val compare3 = """
            {
                "a": "1",
                "b": "2",
                "c": 1.1
            }
        """.trimIndent()
        val diff3 = diffJson(base, compare3)
        assertThat(diff3).isEqualTo(listOf(DiffRow("c", "", "1.1")))

        val compare4 = """
            {
                "a": "1",
                "b": false
            }
        """.trimIndent()
        val diff4 = diffJson(base, compare4)
        assertThat(diff4).isEqualTo(listOf(DiffRow("b", "\"2\"", "false")))
    }

    @Test
    fun `test diffJson with complex json`() {
        val base = """
            {
                "a": ["1", "2"],
                "b": {
                    "x": 1,
                    "y": true
                }
            }
        """.trimIndent()

        val compare1 = """
            {
                "a": ["1", "2"],
                "b": null
            }
        """.trimIndent()
        val diff1 = diffJson(base, compare1)
        assertThat(diff1).isEqualTo(
            listOf(
                DiffRow("b", "", "null"),
                DiffRow("b.x", "1", ""),
                DiffRow("b.y", "true", "")
            )
        )

        val compare2 = """
            {
                "a": ["1", "2", "3"],
                "b": {
                    "x": 1,
                    "y": true
                }
            }
        """.trimIndent()
        val diff2 = diffJson(base, compare2)
        assertThat(diff2).isEqualTo(
            listOf(
                DiffRow("a[2]", "", "\"3\""),
            )
        )
    }

    @Test
    fun `test saving table as csv`() {
        val table = listOf(
            mapOf("col1" to "x", "col2" to "y", "col3" to "z"),
            mapOf("col1" to "xx", "col2" to "yy", "col3" to "zz")
        )
        val expectedCSVRows = listOf(
            listOf("col1", "col2", "col3"),
            listOf("x", "y", "z"),
            listOf("xx", "yy", "zz")
        )
        val someFile = File("/dev/null")
        mockkConstructor(CsvWriter::class)
        every { anyConstructed<CsvWriter>().writeAll(any(), any<OutputStream>()) } just Runs
        saveTableAsCSV(someFile, table)
        verify(exactly = 1) { anyConstructed<CsvWriter>().writeAll(expectedCSVRows, any<OutputStream>()) }
        clearConstructorMockk(CsvWriter::class)
    }
}