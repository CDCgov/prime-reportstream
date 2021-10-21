package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LookupTableFunctionsTests {
    /**
     * The mock request.
     */
    private val mockRequest = mockk<HttpRequestMessage<String?>>()

    /**
     * The mock execution context.
     */
    private val mockContext = mockk<ExecutionContext>()

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
        val version1 = LookupTableVersion()
        version1.tableName = "name1"
        version1.tableVersion = 1
        version1.isActive = false
        version1.createdBy = "author1"
        version1.createdAt = OffsetDateTime.now()

        val version2 = LookupTableVersion()
        version2.tableName = "name1"
        version2.tableVersion = 2
        version2.isActive = true
        version2.createdBy = "author2"
        version2.createdAt = OffsetDateTime.now()

        // Only shows active
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.fetchTableList(eq(false)) } returns listOf(version2)
        every { mockRequest.httpMethod } returns HttpMethod.GET
        every { mockRequest.queryParameters } returns emptyMap()
        var mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableList(mockRequest, mockContext)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonArray)
                    assertTrue(json.size == 1)
                    assertTrue(json[0] is JsonObject)
                    assertTrue((json[0] as JsonObject).containsKey("tableName"))
                    assertEquals(
                        ((json[0] as JsonObject)["tableName"]!! as JsonPrimitive).contentOrNull,
                        version1.tableName
                    )
                }
            )
        }

        // Show all
        every { lookupTableAccess.fetchTableList(eq(true)) } returns listOf(version1, version2)
        every { mockRequest.queryParameters } returns mapOf(LookupTableFunctions.showInactiveParamName to "true")
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableList(mockRequest, mockContext)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonArray)
                    assertTrue(json.size == 2)
                }
            )
        }

        // Database error
        every { lookupTableAccess.fetchTableList(any()) }.throws(DataAccessException("error"))
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableList(mockRequest, mockContext)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `get lookup table data by name and version test`() {
        val tableName = "dummyTable"
        val tableVersionNum = 1
        every { mockRequest.httpMethod } returns HttpMethod.GET

        // Table does not exist
        var mockResponseBuilder = createResponseBuilder()
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns false
        every { mockRequest.createResponseBuilder(HttpStatus.NOT_FOUND) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum, mockContext)
        verifyError(mockResponseBuilder)

        // Get a table
        val tableData = listOf(LookupTableRow(), LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"a": "11", "b": "21"}""")
        tableData[1].data = JSONB.jsonb("""{"a": "12", "b": "22"}""")
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns true
        every { lookupTableAccess.fetchTable(eq(tableName), eq(tableVersionNum)) } returns tableData
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum, mockContext)
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
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum, mockContext)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `get lookup table info by name and version test`() {
        val tableName = "dummyTable"
        val tableVersionNum = 1
        every { mockRequest.httpMethod } returns HttpMethod.GET

        // Table does not exist
        var mockResponseBuilder = createResponseBuilder()
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(tableVersionNum)) } returns null
        every { mockRequest.createResponseBuilder(HttpStatus.NOT_FOUND) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableInfo(mockRequest, tableName, tableVersionNum, mockContext)
        verifyError(mockResponseBuilder)

        // Get a table info
        val tableInfo = LookupTableVersion()
        tableInfo.tableName = tableName
        tableInfo.tableVersion = tableVersionNum
        tableInfo.isActive = true
        tableInfo.createdBy = "dummyUser"
        tableInfo.createdAt = OffsetDateTime.now()
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(tableVersionNum)) } returns tableInfo
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableInfo(mockRequest, tableName, tableVersionNum, mockContext)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonObject)
                    assertTrue(json.containsKey("tableName"))
                    assertEquals(tableName, (json["tableName"] as JsonPrimitive).contentOrNull)
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(any(), any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum, mockContext)
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

    @Test
    fun `convert table to data to json test`() {
        val tableData = listOf(LookupTableRow(), LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"a": "11", "b": "21"}""")
        tableData[1].data = JSONB.jsonb("""{"a": "12", "b": "22"}""")

        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.fetchTable(any(), any()) } returns tableData

        val data = LookupTableFunctions(lookupTableAccess).convertTableDataToJsonString("dummy", 1)
        assertThat(data).isNotEmpty()
        val jsonData = Json.parseToJsonElement(data)
        assertThat(jsonData is JsonArray).isTrue()
        assertThat((jsonData as JsonArray)[0] is JsonObject)
        assertThat(jsonData[1] is JsonObject)
        assertThat((jsonData[0] as JsonObject).containsKey("a"))
    }

    @Test
    fun `create error message test`() {
        val message = "dummy message"
        val jsonError = LookupTableFunctions.createErrorMsg(message)
        assertThat(jsonError).isNotEmpty()
        assertThat((Json.parseToJsonElement(jsonError) as JsonObject).containsKey("error")).isTrue()
        assertThat(((Json.parseToJsonElement(jsonError) as JsonObject)["error"] as JsonPrimitive).contentOrNull)
            .isEqualTo(message)
    }

    @Test
    fun `create table test`() {
        val tableName = "dummy"
        val latestVersion = 1
        every { mockRequest.httpMethod } returns HttpMethod.POST
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()

        // Empty payload
        var mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        every { mockRequest.body } returns ""
        every { lookupTableAccess.fetchLatestVersion(tableName) } returns latestVersion
        LookupTableFunctions(lookupTableAccess).createLookupTable(mockRequest, tableName, mockContext)
        verifyError(mockResponseBuilder)

        // Payload is not consistent
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        every { mockRequest.body } returns """[{"a": "11", "b": "21"},{"a": "12"}]"""
        LookupTableFunctions(lookupTableAccess).createLookupTable(mockRequest, tableName, mockContext)
        verifyError(mockResponseBuilder)

        // Create a new version of an existing table
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        every { mockRequest.body } returns """[{"a": "11", "b": "21"},{"a": "12", "b": "22"}]"""
        val versionInfo = LookupTableVersion()
        versionInfo.tableName = tableName
        versionInfo.tableVersion = latestVersion + 1
        versionInfo.isActive = false
        versionInfo.createdBy = "author1"
        versionInfo.createdAt = OffsetDateTime.now()
        every { lookupTableAccess.createTable(eq(tableName), eq(latestVersion + 1), any()) } returns Unit
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(latestVersion + 1)) } returns versionInfo
        LookupTableFunctions(lookupTableAccess).createLookupTable(mockRequest, tableName, mockContext)
        verify(exactly = 1) {
            lookupTableAccess.createTable(
                any(), any(),
                withArg {
                    assertEquals(2, it.size)
                    val json = Json.parseToJsonElement(it[0].data())
                    assertTrue(json is JsonObject)
                    assertEquals("11", (json["a"] as JsonPrimitive).contentOrNull)
                }
            )
            mockResponseBuilder.body(
                withArg {
                    assertTrue(it is String)
                    assertTrue(it.isNotBlank())
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonObject)
                    assertNotNull(json["tableName"])
                    assertEquals(tableName, (((json["tableName"]) as JsonPrimitive).contentOrNull))
                }
            )
        }

        // Create a new table
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        every { lookupTableAccess.fetchLatestVersion(tableName) } returns null
        every { lookupTableAccess.createTable(eq(tableName), eq(1), any()) } returns Unit
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(1)) } returns versionInfo
        LookupTableFunctions(lookupTableAccess).createLookupTable(mockRequest, tableName, mockContext)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    assertTrue(it is String)
                    assertTrue(it.isNotBlank())
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonObject)
                    assertNotNull(json["tableVersion"])
                    assertEquals(
                        (latestVersion + 1).toString(),
                        (((json["tableVersion"]) as JsonPrimitive).contentOrNull)
                    )
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.fetchLatestVersion(tableName) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).createLookupTable(mockRequest, tableName, mockContext)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `activate table test`() {
        val tableName = "dummyTable"
        val tableVersionNum = 1
        every { mockRequest.httpMethod } returns HttpMethod.PUT

        // Table does not exist
        var mockResponseBuilder = createResponseBuilder()
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns false
        every { mockRequest.createResponseBuilder(HttpStatus.NOT_FOUND) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).activateLookupTable(
            mockRequest, tableName, tableVersionNum,
            mockContext
        )
        verifyError(mockResponseBuilder)

        // Activate a table
        val versionInfo = LookupTableVersion()
        versionInfo.tableName = tableName
        versionInfo.tableVersion = tableVersionNum
        versionInfo.isActive = false
        versionInfo.createdBy = "author1"
        versionInfo.createdAt = OffsetDateTime.now()
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns true
        every { lookupTableAccess.activateTable(eq(tableName), eq(tableVersionNum)) } returns true
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(tableVersionNum)) } returns versionInfo
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).activateLookupTable(
            mockRequest, tableName, tableVersionNum,
            mockContext
        )
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val json = Json.parseToJsonElement(it)
                    assertTrue(json is JsonObject)
                    assertEquals(tableName, ((json["tableName"]) as JsonPrimitive).contentOrNull)
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(any(), any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        LookupTableFunctions(lookupTableAccess).getLookupTableData(mockRequest, tableName, tableVersionNum, mockContext)
        verifyError(mockResponseBuilder)
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