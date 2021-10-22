package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.json.FuelJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.apache.http.HttpStatus
import org.jooq.JSONB
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LookupTableCommandsTest {
    @Test
    fun `test rows to table`() {
        val data = listOf(LookupTableRow())
        val colNames = listOf("a", "b")
        data[0].data = JSONB.jsonb("""{"a": "value1", "b": "value2"}""")
        val output = LookupTableCommands.rowsToPrintableTable(data, colNames)
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
        var data = LookupTableCommands
            .extractTableRowFromJson(JSONB.jsonb("""{"a": "value1", "b": "value2"}"""), colNames)
        assertThat(data).isEqualTo(listOf("value1", "value2"))

        data = LookupTableCommands.extractTableRowFromJson(JSONB.jsonb("""{"a": "value1"}"""), colNames)
        assertThat(data).isEqualTo(listOf("value1", ""))

        data = LookupTableCommands.extractTableRowFromJson(JSONB.jsonb("{}"), colNames)
        assertThat(data).isEqualTo(listOf("", ""))
    }

    @Test
    fun `test set table row to json`() {
        var json = LookupTableCommands.setTableRowToJson(mapOf("a" to "value1", "b" to "value2"))
        var jsonObject = Json.parseToJsonElement(json.data()) as JsonObject
        assertThat((jsonObject["a"] as JsonPrimitive).contentOrNull).isEqualTo("value1")
        assertThat((jsonObject["b"] as JsonPrimitive).contentOrNull).isEqualTo("value2")

        json = LookupTableCommands.setTableRowToJson(emptyMap())
        jsonObject = Json.parseToJsonElement(json.data()) as JsonObject
        assertThat(jsonObject.keys.isEmpty()).isTrue()
    }

    @Test
    fun `get error from response test`() {
        val mockResult = mockk<Result.Success<FuelJson>>()
        val mockResultFailure = mockk<Result.Failure<FuelError>>()
        val mockGenericErrorMessage = "Some dummy message"

        // Not a failure
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResult)).isEqualTo("")

        // No response from the API
        every { mockResultFailure.error.response.body().isEmpty() } returns true
        every { mockResultFailure.error.toString() } returns mockGenericErrorMessage
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo(mockGenericErrorMessage)

        // The response from the API is just a string, not JSON
        every { mockResultFailure.error.response.body().isEmpty() } returns false
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns "not Json"
        every { mockResultFailure.error.toString() } returns mockGenericErrorMessage
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo(mockGenericErrorMessage)

        // Response is JSON, but has no error field
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"dummy": "dummy"}"""
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo(mockGenericErrorMessage)

        // A response with an error
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"error": "some error"}"""
        assertThat(LookupTableEndpointUtilities.getErrorFromResponse(mockResultFailure))
            .isEqualTo("some error")
    }

    @Test
    fun `check common errors from response test`() {
        val mockResult = mockk<Result.Success<FuelJson>>()
        val mockResultFailure = mockk<Result.Failure<FuelError>>()
        val mockResponse = mockk<Response>()

        // API Not found
        every { mockResponse.statusCode } returns HttpStatus.SC_NOT_FOUND
        every { mockResultFailure.error.response.body().isEmpty() } returns false
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns "not Json"
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // API Not found with unexpected JSON response
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"dummy": "dummy"}"""
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // Table not found
        every { mockResultFailure.error.response.body().asString(HttpUtilities.jsonMediaType) } returns
            """{"error": "some error"}"""
        assertFailsWith<LookupTableEndpointUtilities.Companion.TableNotFoundException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // Nome other error
        every { mockResponse.statusCode } returns HttpStatus.SC_BAD_REQUEST
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )

        // Good response, but no body returned
        every { mockResponse.statusCode } returns HttpStatus.SC_OK
        every { mockResult.get().content.isBlank() } returns true
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.checkCommonErrorsFromResponse(mockResultFailure, mockResponse)
            }
        )
    }

    @Test
    fun `get array from response test`() {
        val mockResult = mockk<Result<FuelJson, FuelError>>()
        val mockResponse = mockk<Response>()

        // Empty content
        every { mockResponse.statusCode } returns HttpStatus.SC_OK
        every { mockResult.get().content } returns ""

        // Not a JSON array
        every { mockResult.get().content } returns """{}"""
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.getArrayFromResponse(mockResult, mockResponse)
            }
        )

        // Not an array of JSON objects
        every { mockResult.get().content } returns """["dummy": "dummy"]"""
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.getArrayFromResponse(mockResult, mockResponse)
            }
        )

        // Good data
        every { mockResult.get().content } returns """[{"dummy": "dummy"}]"""
        val array = LookupTableEndpointUtilities.getArrayFromResponse(mockResult, mockResponse)
        assertThat(array.size == 1).isTrue()
    }

    @Test
    fun `convert json to table version test`() {
        // Bad object
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.convertJsonToInfo(Json.parseToJsonElement("""{}""") as JsonObject)
            }
        )

        // Missing properties
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.convertJsonToInfo(
                    Json.parseToJsonElement(
                        """{"tableName": "name", "tableVersion": "notNum"}"""
                    ) as JsonObject
                )
            }
        )

        // Bad time
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.convertJsonToInfo(
                    Json.parseToJsonElement(
                        """{"tableName": "name", "tableVersion": 1, "isActive": true, 
                        "createdBy": "developer", "createdAt": "some bad time"}""".trimMargin()
                    ) as JsonObject
                )
            }
        )

        // Good data
        val info = LookupTableEndpointUtilities.convertJsonToInfo(
            Json.parseToJsonElement(
                """{"tableName": "name", "tableVersion": 1, "isActive": true, 
                        "createdBy": "developer", "createdAt": "2018-12-30T06:00:00Z"}""".trimMargin()
            ) as JsonObject
        )
        assertThat(info.tableVersion).isEqualTo(1)
    }

    @Test
    fun `get table info from response test`() {
        val mockResult = mockk<Result<FuelJson, FuelError>>()
        val mockResponse = mockk<Response>()

        // Empty content
        every { mockResponse.statusCode } returns HttpStatus.SC_OK
        every { mockResult.get().content } returns ""

        // Not a JSON array
        every { mockResult.get().content } returns """{}"""
        assertFailsWith<IOException>(
            block = {
                LookupTableEndpointUtilities.getTableInfoFromResponse(mockResult, mockResponse)
            }
        )

        // Good data
        every { mockResult.get().content } returns
            """{"tableName": "name", "tableVersion": 1, "isActive": true, 
                        "createdBy": "developer", "createdAt": "2018-12-30T06:00:00Z"}"""
        val info = LookupTableEndpointUtilities.getTableInfoFromResponse(mockResult, mockResponse)
        assertThat(info.tableName).isEqualTo("name")
    }
}