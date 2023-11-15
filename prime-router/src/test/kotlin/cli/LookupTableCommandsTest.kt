package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.jooq.JSONB
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LookupTableCommandsTest {
    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper = JacksonMapperUtilities.defaultMapper

    /**
     * Helper
     */
    private fun getMockUtil(
        url: String,
                            status: HttpStatusCode,
                            body: String,
    ): LookupTableEndpointUtilities {
        return LookupTableEndpointUtilities(
            Environment.LOCAL,
            null,
            ApiMockEngine(
                url,
                status,
                body
            ).client()
        )
    }

    @Test
    fun `test lookuptable list invalid response body`() {
        assertFailsWith<IOException>(
            message = "Invalid response body found.",
            block = {
                getMockUtil(
                    "/api/lookuptables/list",
                    HttpStatusCode.OK,
                    """{"error": {"code": 999,"message": "Mock internal server error."}}"""
                ).fetchList()
            }
        )
    }

    @Test
    fun `test lookuptable list error response`() {
        val body = """{"error": {"code": 404,"message": "Something wrong when processing request."}}"""
        // note here a ClientRequestException thrown instead of IOException
        // due to the expectSuccess flag of the mock engine got override
        assertFailsWith<ClientRequestException>(
            message = "Error response: status code: ${HttpStatusCode.NotFound}, body: $body",
            block = {
                getMockUtil(
                    "/api/lookuptables/list",
                    HttpStatusCode.NotFound,
                    """{"error": {"code": 404,"message": "Something wrong when processing request."}}"""
                ).fetchList()
            }
        )
    }

    @Test
    fun `test lookuptable list OK`() {
        val tables = """[{
        "lookupTableVersionId" : 6,
        "tableName" : "ethnicity",
        "tableVersion" : 1,
        "isActive" : true,
        "createdBy" : "local@test.com",
        "createdAt" : "2023-11-13T15:38:50.495Z",
        "tableSha256Checksum" : "67a9db3bb62a79b4a9d22126f58eebb15dd99a2a2a81bdf4ff740fa884fd5635"
        }, {
            "lookupTableVersionId" : 9,
            "tableName" : "fhirpath_filter_shorthand",
            "tableVersion" : 1,
            "isActive" : true,
            "createdBy" : "local@test.com",
            "createdAt" : "2023-11-13T15:38:50.598Z",
            "tableSha256Checksum" : "4295f38f1e9bdb233d5086bdae3cf92024815883db3f0a96066580c4ba74fcde"
        }]"""
        val listOfTables = getMockUtil(
                    "/api/lookuptables/list",
                    HttpStatusCode.OK,
                    body = tables
                ).fetchList()
        assertThat { listOfTables.isNotEmpty() && listOfTables.size == 2 }
    }

        @Test
    fun `test rows to table`() {
        val colNames = listOf("a", "b")
        val data = mapOf(colNames[0] to "value1", colNames[1] to "value2")
        val output = LookupTableCommands.rowsToPrintableTable(listOf(data), colNames)
        assertThat(output.isNotEmpty()).isTrue()

        assertFailsWith<IllegalArgumentException>(
            block = {
                LookupTableCommands.rowsToPrintableTable(emptyList(), colNames)
            }
        )
    }

    @Test
    fun `test info to table`() {
        val data = listOf(LookupTableVersion())
        data[0].createdAt = OffsetDateTime.now()
        data[0].createdBy = "someone"
        data[0].isActive = false
        data[0].tableVersion = 1
        data[0].tableName = "name"
        data[0].tableSha256Checksum = "abc123"
        val output = LookupTableCommands.infoToPrintableTable(data)
        assertThat(output.isNotEmpty()).isTrue()

        assertFailsWith<IllegalArgumentException>(
            block = {
                LookupTableCommands.infoToPrintableTable(emptyList())
            }
        )
    }

    @Test
    fun `test extract row data from json`() {
        val colNames = listOf("a", "b")
        var data = LookupTableEndpointUtilities
            .extractTableRowFromJson(JSONB.jsonb("""{"a": "value1", "b": "value2"}"""), colNames)
        assertThat(data).isEqualTo(listOf("value1", "value2"))

        data = LookupTableEndpointUtilities.extractTableRowFromJson(JSONB.jsonb("""{"a": "value1"}"""), colNames)
        assertThat(data).isEqualTo(listOf("value1", ""))

        data = LookupTableEndpointUtilities.extractTableRowFromJson(JSONB.jsonb("{}"), colNames)
        assertThat(data).isEqualTo(listOf("", ""))
    }

    @Test
    fun `test set table row to json`() {
        var json = LookupTableEndpointUtilities.setTableRowToJson(mapOf("a" to "value1", "b" to "value2"))
        var row = mapper.readValue<Map<String, String>>(json.data())
        assertThat(row["a"]).isEqualTo("value1")
        assertThat(row["b"]).isEqualTo("value2")

        json = LookupTableEndpointUtilities.setTableRowToJson(emptyMap())
        row = mapper.readValue(json.data())
        assertThat(row.isEmpty()).isTrue()
    }

    @Test
    fun `get error from response test`() {
//        val response = apiMock.posts("fakeUrl")
//        println(response)
//        assertThat(LookupTableEndpointUtilities.getResponseError(response)).isEqualTo("")

//        val mockResult = mockk<Result.Success<FuelJson>>()
//        val mockResultFailure = mockk<Result.Failure<FuelError>>()
//        val mockGenericErrorMessage = "Some dummy message"
//
//        // No response from the API
//        every { mockResultFailure.error.response.body().isEmpty() } returns true
//        every { mockResultFailure.error.toString() } returns mockGenericErrorMessage
//        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
//            .isEqualTo(mockGenericErrorMessage)
//
//        // The response from the API is just a string, not JSON
//        every { mockResultFailure.error.response.body().isEmpty() } returns false
//        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns "not Json"
//        every { mockResultFailure.error.toString() } returns mockGenericErrorMessage
//        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
//            .isEqualTo(mockGenericErrorMessage)
//
//        // Response is JSON, but has no error field
//        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
//            """{"dummy": "dummy"}"""
//        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
//            .isEqualTo(mockGenericErrorMessage)
//
//        // A response with an error
//        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
//            """{"error": "some error"}"""
//        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
//            .isEqualTo("some error")
    }

    @Test
    fun `check common errors from response test`() {
//        val mockResult = mockk<Result.Success<FuelJson>>()
//        val mockResultFailure = mockk<Result.Failure<FuelError>>()
//        val mockResponse = mockk<Response>()
//
//        // API Not found
//        every { mockResponse.statusCode } returns HttpStatus.SC_NOT_FOUND
//        every { mockResultFailure.error.response.body().isEmpty() } returns false
//        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns "not Json"
//        assertFailsWith<IOException>(
//            block = {
//                LookupTableEndpointUtilities.checkResponse(call.response)
//            }
//        )

//        // API Not found with unexpected JSON response
//        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
//            """{"dummy": "dummy"}"""
//        assertFailsWith<IOException>(
//            block = {
//                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
//            }
//        )
//
//        // Table not found
//        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
//            """{"error": "some error"}"""
//        assertFailsWith<LookupTableEndpointUtilities.Companion.TableNotFoundException>(
//            block = {
//                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
//            }
//        )

//        // Nome other error
//        every { mockResponse.statusCode } returns HttpStatus.SC_BAD_REQUEST
//        assertFailsWith<IOException>(
//            block = {
//                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
//            }
//        )
//
//        // Good response, but no body returned
//        every { mockResponse.statusCode } returns HttpStatus.SC_OK
//        every { mockResult.get().content.isBlank() } returns true
//        assertFailsWith<IOException>(
//            block = {
//                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
//            }
//        )
    }

    @Test
    fun `get table info from response test`() {
//        val mockResult = mockk<Result<FuelJson, FuelError>>()
//        val mockResponse = mockk<Response>()
//
//        // Empty content
//        every { mockResponse.statusCode } returns HttpStatus.SC_OK
//        every { mockResult.get().content } returns ""
//
//        // Not a JSON array
//        every { mockResult.get().content } returns """{}"""
//        assertFailsWith<IOException>(
//            block = {
//                LookupTableEndpointUtilities.checkResponse(call.response)
//            }
//        )

        // Good data
//        every { mockResult.get().content } returns
//            """{"tableName": "name", "tableVersion": 1, "isActive": true,
//                        "createdBy": "developer", "createdAt": "2018-12-30T06:00:00Z"}
//            """
//        val info = LookupTableEndpointUtilities.getTableInfoResponse(call.response)
//        assertThat(info.tableName).isEqualTo("name")
    }

    @Test
    fun `compare and annotate sender compendium with lookup table`() {
        val tableMap: Map<String?, Map<String, String>> = mapOf(
            "12345" to mapOf("Code" to "12345", "Descriptor" to "some descriptor", "Code System" to "SYSTEM1"),
            "54321" to mapOf("Code" to "54321", "Descriptor" to "some descriptor", "Code System" to "SYSTEM2")
        )
        val compendium = listOf(
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM1"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM2"),
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM2"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM1"),
            mapOf("test code" to "56789", "test description" to "test", "coding system" to "SYSTEM1")
        )
        val expectedOutput = listOf(
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM1", "mapped?" to "Y"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM2", "mapped?" to "Y"),
            mapOf("test code" to "12345", "test description" to "test", "coding system" to "SYSTEM2", "mapped?" to "N"),
            mapOf("test code" to "54321", "test description" to "test", "coding system" to "SYSTEM1", "mapped?" to "N"),
            mapOf("test code" to "56789", "test description" to "test", "coding system" to "SYSTEM1", "mapped?" to "N")
        )
        val output = LookupTableCompareMappingCommand.compareMappings(compendium, tableMap)
        assertThat(output).isEqualTo(expectedOutput)
    }
}