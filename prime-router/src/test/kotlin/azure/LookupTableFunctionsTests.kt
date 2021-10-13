package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LookupTableFunctionsTests {

    @Test
    fun `get lookup table list test`() {
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        val versionList = listOf(LookupTableVersion(), LookupTableVersion())
        versionList[0].tableName = "name1"
        versionList[0].tableVersion = 1
        versionList[0].isActive = false
        versionList[0].createdBy = "author1"
        versionList[0].createdAt = OffsetDateTime.now()

        versionList[1].tableName = "name1"
        versionList[1].tableVersion = 2
        versionList[1].isActive = true
        versionList[1].createdBy = "author2"
        versionList[1].createdAt = OffsetDateTime.now()

        every { lookupTableAccess.fetchTableList() } returns versionList

        val mockResponse = mockk<HttpResponseMessage>()
        val mockResponseBuilder = mockk<HttpResponseMessage.Builder>()

        val mockRequest = mockk<HttpRequestMessage<String?>>()
        every { mockRequest.headers } returns mapOf(HttpHeaders.AUTHORIZATION.lowercase() to "Bearer dummy")
        every { mockRequest.uri } returns URI.create("http://localhost:7071/api/lookuptables")
        every { mockRequest.httpMethod } returns HttpMethod.GET
        every { mockRequest.queryParameters } returns emptyMap()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        every { mockResponseBuilder.body(any()) } returns mockResponseBuilder
        every { mockResponseBuilder.header(any(), any()) } returns mockResponseBuilder
        every { mockResponseBuilder.build() } returns mockResponse

        LookupTableFunctions(lookupTableAccess).getLookupTableList(mockRequest)

        verify {
            mockResponseBuilder.body(
                withArg {
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonArray)
                    assertTrue(json.size == 2)
                    assertTrue(json[0] is JsonObject)
                    assertTrue((json[0] as JsonObject).containsKey("tableName"))
                    assertEquals(
                        ((json[0] as JsonObject)["tableName"]!! as JsonPrimitive).contentOrNull,
                        versionList[0].tableName
                    )
                }
            )
        }
    }
}