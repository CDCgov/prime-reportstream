package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isNotEmpty
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper = JacksonMapperUtilities.defaultMapper

    @BeforeAll
    fun initDependencies() {
        every { mockRequest.headers } returns mapOf(HttpHeaders.AUTHORIZATION.lowercase() to "Bearer dummy")
        every { mockRequest.uri } returns URI.create("http://localhost:7071/api/lookuptables")
        val mockAuthenticatedClaims = mockk<AuthenticatedClaims>()
        every { mockAuthenticatedClaims.userName } returns "dummy"
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
        val function = LookupTableFunctions(lookupTableAccess)
        function.getLookupTableList(mockRequest)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val versions = mapper.readValue<List<LookupTableVersion>>(it)
                    assertTrue(versions.size == 1)
                    assertEquals(versions[0].tableName, version1.tableName)
                }
            )
        }

        // Show all
        every { lookupTableAccess.fetchTableList(eq(true)) } returns listOf(version1, version2)
        every { mockRequest.queryParameters } returns mapOf(LookupTableFunctions.showInactiveParamName to "true")
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        function.getLookupTableList(mockRequest)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val versions = mapper.readValue<List<LookupTableVersion>>(it)
                    assertTrue(versions.size == 2)
                }
            )
        }

        // Database error
        every { lookupTableAccess.fetchTableList(any()) }.throws(DataAccessException("error"))
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        function.getLookupTableList(mockRequest)
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
        val function = LookupTableFunctions(lookupTableAccess)
        function.getLookupTableData(mockRequest, tableName, tableVersionNum)
        verifyError(mockResponseBuilder)

        // Get a table
        val tableData = listOf(
            LookupTableRow(),
            LookupTableRow()
        )
        tableData[0].data = JSONB.jsonb("""{"a": "11", "b": "21"}""")
        tableData[1].data = JSONB.jsonb("""{"a": "12", "b": "22"}""")
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(eq(tableName), eq(tableVersionNum)) } returns true
        every { lookupTableAccess.fetchTable(eq(tableName), eq(tableVersionNum)) } returns tableData
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        function.getLookupTableData(mockRequest, tableName, tableVersionNum)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val rows = mapper.readValue<List<Map<String, String>>>(it)
                    assertTrue(rows.size == 2)
                    assertTrue(rows[0].containsKey("a"))
                    assertTrue(rows[0].containsKey("b"))
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(any(), any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        function.getLookupTableData(mockRequest, tableName, tableVersionNum)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `get active lookup table data by name test`() {
        val tableName = "dummyTable"
        every { mockRequest.httpMethod } returns HttpMethod.GET

        // Table does not exist
        var mockResponseBuilder = createResponseBuilder()
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        every { lookupTableAccess.fetchActiveVersion(eq(tableName)) } returns null
        every { mockRequest.createResponseBuilder(HttpStatus.NOT_FOUND) } returns mockResponseBuilder
        val function = LookupTableFunctions(lookupTableAccess)
        function.getActiveLookupTableData(mockRequest, tableName)
        verifyError(mockResponseBuilder)

        // Get a table
        val tableData = listOf(
            LookupTableRow(),
            LookupTableRow()
        )
        tableData[0].data = JSONB.jsonb("""{"a": "11", "b": "21"}""")
        tableData[1].data = JSONB.jsonb("""{"a": "12", "b": "22"}""")
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.fetchActiveVersion(eq(tableName)) } returns 1
        every { lookupTableAccess.fetchTable(eq(tableName), eq(1)) } returns tableData
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        function.getActiveLookupTableData(mockRequest, tableName)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val rows = mapper.readValue<List<Map<String, String>>>(it)
                    assertTrue(rows.size == 2)
                    assertTrue(rows[0].containsKey("a"))
                    assertTrue(rows[0].containsKey("b"))
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.fetchActiveVersion(any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        function.getActiveLookupTableData(mockRequest, tableName)
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
        val function = LookupTableFunctions(lookupTableAccess)
        function.getLookupTableInfo(mockRequest, tableName, tableVersionNum)
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
        function.getLookupTableInfo(mockRequest, tableName, tableVersionNum)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val version = mapper.readValue<LookupTableVersion>(it)
                    assertEquals(tableName, version.tableName)
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(any(), any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        function.getLookupTableData(mockRequest, tableName, tableVersionNum)
        verifyError(mockResponseBuilder)
    }

    @Test
    fun `convert table to data to json test`() {
        val tableData = listOf(
            LookupTableRow(),
            LookupTableRow()
        )
        tableData[0].data = JSONB.jsonb("""{"a": "11", "b": "21"}""")
        tableData[1].data = JSONB.jsonb("""{"a": "12", "b": "22"}""")

        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()
        val data = LookupTableFunctions(lookupTableAccess)
            .convertTableDataToJsonString(tableData)
        assertThat(data).isNotEmpty()
        val rows = mapper.readValue<List<Map<String, String>>>(data)
        assertTrue(rows.size == 2)
        assertThat(rows[0].containsKey("a"))
    }

    @Test
    fun `create table test`() {
        val tableName = "dummy"
        val tableSha256 = "abc123"
        val force = true
        val latestVersion = 1
        every { mockRequest.httpMethod } returns HttpMethod.POST
        val lookupTableAccess = mockk<DatabaseLookupTableAccess>()

        // Empty payload
        var mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        every { mockRequest.body } returns ""
        every { mockRequest.queryParameters } returns emptyMap()
        every { lookupTableAccess.fetchLatestVersion(tableName) } returns latestVersion
        val function = LookupTableFunctions(lookupTableAccess)
        function.createLookupTable(mockRequest, tableName)
        verifyError(mockResponseBuilder)

        // Payload is not consistent
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        every { mockRequest.body } returns """[{"a": "11", "b": "21"},{"a": "12"}]"""
        function.createLookupTable(mockRequest, tableName)
        verifyError(mockResponseBuilder)

        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.BAD_REQUEST) } returns mockResponseBuilder
        every { mockRequest.body } returns """[{"a": "11", "b": "21"},{"a": "12", "b": "22", "c": "32"}]"""
        function.createLookupTable(mockRequest, tableName)
        verifyError(mockResponseBuilder)

        // Create a new version of an existing table
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        every { mockRequest.body } returns """[{"a": "11", "b": "21"},{"a": "12", "b": "22"}]"""
        every { mockRequest.queryParameters } returns mapOf(LookupTableFunctions.forceQueryParameter to "true")
        val versionInfo = LookupTableVersion()
        versionInfo.tableName = tableName
        versionInfo.tableSha256Checksum = tableSha256
        versionInfo.tableVersion = latestVersion + 1
        versionInfo.isActive = false
        versionInfo.createdBy = "author1"
        versionInfo.createdAt = OffsetDateTime.now()
        every {
            lookupTableAccess.createTable(
                eq(tableName), eq(latestVersion + 1), any(),
                any(),
                force
            )
        } returns Unit
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(latestVersion + 1)) } returns versionInfo
        function.createLookupTable(mockRequest, tableName)
        verify(exactly = 1) {
            lookupTableAccess.createTable(
                any(), any(),
                withArg {
                    assertEquals(2, it.size)
                    val row = mapper.readValue<Map<String, String>>(it[0].data())
                    assertEquals("11", row["a"])
                },
                any(),
                force
            )
            mockResponseBuilder.body(
                withArg {
                    assertTrue(it is String)
                    assertTrue(it.isNotBlank())
                    val version = mapper.readValue<LookupTableVersion>(it)
                    assertEquals(tableName, version.tableName)
                }
            )
        }

        // Create a new table
        mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        every { lookupTableAccess.fetchLatestVersion(tableName) } returns null
        every { lookupTableAccess.createTable(eq(tableName), eq(1), any(), any(), force) } returns Unit
        every { lookupTableAccess.fetchVersionInfo(eq(tableName), eq(1)) } returns versionInfo
        function.createLookupTable(mockRequest, tableName)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    assertTrue(it is String)
                    assertTrue(it.isNotBlank())
                    val version = mapper.readValue<LookupTableVersion>(it)
                    assertEquals(tableName, version.tableName)
                    assertEquals((latestVersion + 1).toString(), version.tableVersion.toString())
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.fetchLatestVersion(tableName) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        function.createLookupTable(mockRequest, tableName)
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
        val function = LookupTableFunctions(lookupTableAccess)
        function.activateLookupTable(mockRequest, tableName, tableVersionNum)
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
        function.activateLookupTable(mockRequest, tableName, tableVersionNum)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val version = mapper.readValue<LookupTableVersion>(it)
                    assertEquals(tableName, version.tableName)
                }
            )
        }

        // Database error
        mockResponseBuilder = createResponseBuilder()
        every { lookupTableAccess.doesTableExist(any(), any()) }.throws(DataAccessException("error"))
        every { mockRequest.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR) } returns mockResponseBuilder
        function.getLookupTableData(mockRequest, tableName, tableVersionNum)
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
                    val errorMsg = mapper.readValue<Map<String, String>>(it)
                    assertTrue(errorMsg.containsKey("error"))
                }
            )
        }
        // Sanity check.  Force a clear, so we don't make a mistake to not reset the verification count
        clearMocks(mockResponseBuilder)
    }
}