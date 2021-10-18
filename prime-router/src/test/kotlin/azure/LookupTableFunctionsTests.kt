package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jooq.JSONB
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LookupTableFunctionsTests {
    /**
     * The mock request.
     */
    private val mockRequest = mockk<HttpRequestMessage<String?>>()

    @BeforeAll
    fun initDependencies() {
        every { mockRequest.headers } returns mapOf(HttpHeaders.AUTHORIZATION.lowercase() to "Bearer dummy")
        every { mockRequest.uri } returns URI.create("http://localhost:7071/api/lookuptables")
    }

    /**
     * Create a new response builder.  Useful to reset the verification count.
     */
    private fun createResponseBuilder(): HttpResponseMessage.Builder {
        val mockResponseBuilder = mockk<HttpResponseMessage.Builder>()
        every { mockResponseBuilder.body(any()) } returns mockResponseBuilder
        every { mockResponseBuilder.header(any(), any()) } returns mockResponseBuilder
        every { mockResponseBuilder.build() } returns mockk()
        return mockResponseBuilder
    }

    @Test
    fun `get lookup table list test`() {
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

        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.fetchTableList() } returns versionList

        every { mockRequest.httpMethod } returns HttpMethod.GET
        every { mockRequest.queryParameters } returns emptyMap()
        var mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder

        LookupTableFunctions(lookupTableAccess).getLookupTableList(mockRequest)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
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

        // Database error
        every { lookupTableAccess.fetchTableList() }.throws(DataAccessException("error"))
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableList(mockRequest)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `get lookup table data by name and version test`() {
        val tableName = "dummyTable"
        val tableVersionNum = 1
        every { mockRequest.httpMethod } returns HttpMethod.GET

        val tableData = listOf(LookupTableRow(), LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"a": "11", "b": "21"}""")
        tableData[1].data = JSONB.jsonb("""{"a": "12", "b": "22"}""")

        // Table does not exist
        var mockResponseBuilder = createResponseBuilder()
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.fetchTable(eq(tableName), eq(tableVersionNum)) } returns tableData
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns false
        every { mockRequest.createResponseBuilder(HttpStatus.NOT_FOUND) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum)
        verifyError(mockResponseBuilder)

        // Create a table
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns true
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonArray)
                    assertTrue(json.size == 2)
                    assertTrue(json[0] is JsonObject)
                    assertTrue((json[0] as JsonObject).containsKey("a"))
                    assertTrue((json[0] as JsonObject).containsKey("b"))
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(any(), any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `check create request test`() {
        // Null body
        var mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        var requestBody: JsonElement? = null
        every { mockRequest.body } returns null
        var response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNotNull()
        verifyError(mockResponseBuilder)

        // Empty body
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        every { mockRequest.body } returns ""
        response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNotNull()
        verifyError(mockResponseBuilder)

        // Not an array
        every { mockRequest.body } returns "dummy body" // Anything here works for the rest of the checks
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        requestBody = Json.parseToJsonElement("""{}""") // Not an array
        response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNotNull()
        verifyError(mockResponseBuilder)

        // An empty array
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        requestBody = Json.parseToJsonElement("""[]""") // Empty array
        response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNotNull()
        verifyError(mockResponseBuilder)

        // Not an array of objects
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        requestBody = Json.parseToJsonElement("""["a","b"]""") // Array of primitives
        response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNotNull()
        verifyError(mockResponseBuilder)

        // Array of objects, but with no data
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        requestBody = Json.parseToJsonElement("""[{}]""") // Array with empty object
        response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNotNull()
        verifyError(mockResponseBuilder)

        // A good row
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        requestBody = Json.parseToJsonElement("""[{"a":"1"}]""") // Array with object
        response = LookupTableFunctions.checkCreateRequest(mockRequest, requestBody)
        assertThat(response).isNull()
    }

    /**
     * Verify an error response is created.
     */
    private fun verifyError(mockResponseBuilder: HttpResponseMessage.Builder) {
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonObject)
                    assertTrue(json.containsKey("error"))
                }
            )
        }
        // Sanity check.  Force a clear, so we don't make a mistake to not reset the verification count
        clearMocks(mockResponseBuilder)
    }
}