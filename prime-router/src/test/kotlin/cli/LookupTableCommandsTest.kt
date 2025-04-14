package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import io.ktor.http.HttpStatusCode
import org.jooq.JSONB
import java.io.IOException
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    ): LookupTableEndpointUtilities = LookupTableEndpointUtilities(
            Environment.LOCAL,
            useThisToken = null,
            ApiMockEngine(
                url,
                status,
                body
            ).client()
        )

    @Test
    fun `test lookuptable list invalid response body`() {
        val exception = assertFailsWith<IOException>(
            message = "Expected IOException does not happen",
            block = {
                getMockUtil(
                    "/api/lookuptables/list",
                    HttpStatusCode.OK,
                    """{"error": {"code": 999,"message": "Mock internal server error."}}"""
                ).fetchList()
            }
        )
        assertTrue(exception.message.toString().contains("Invalid response body found"))
    }

    @Test
    fun `test lookuptable list error response`() {
        val body = """{"error": {"code": 404,"message": "Something wrong when processing request."}}"""
        val exception = assertFailsWith<IOException>(
            message = "Error response: status code: ${HttpStatusCode.NotFound}, body: $body",
            block = {
                getMockUtil(
                    "/api/lookuptables/list",
                    HttpStatusCode.NotFound,
                    """{"error": {"code": 404,"message": "Something wrong when processing request."}}"""
                ).fetchList()
            }
        )
        assertTrue(exception.message.toString().contains("Something wrong when processing request"))
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
        }]"""
        val listOfTables = getMockUtil(
            "/api/lookuptables/list",
            HttpStatusCode.OK,
            body = tables
        ).fetchList()
        assertTrue(listOfTables.isNotEmpty() && listOfTables.size == 1)
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
    fun `get table info from response create table conflict`() {
        val exception = assertFailsWith<LookupTableEndpointUtilities.Companion.TableConflictException>(
            message = "Expect TableConflictException does not thrown.",
            block = {
                val respBody = """{"error": "New Lookup Table Table is identical to existing table version 1."}"""
                getMockUtil(
                    "${LookupTableEndpointUtilities.endpointRoot}/race",
                    HttpStatusCode.Conflict,
                    body = respBody
                ).createTable("race", listOf(mapOf()), true)
            }
        )
        assertTrue(
            exception.message.toString().contains(
                "New Lookup Table Table is identical to existing table version 1"
            )
        )
    }

    @Test
    fun `get table info from response create table end point not found`() {
        val exception = assertFailsWith<IOException>(
            message = "Expected 404 with empty response body not happening",
            block = {
                getMockUtil(
                    "${LookupTableEndpointUtilities.endpointRoot}/race",
                    HttpStatusCode.NotFound,
                    body = ""
                ).createTable("race", listOf(mapOf()), true)
            }
        )
        assertTrue(exception.message.toString().contains("Response status: 404, NOT FOUND, endpoint not found"))
    }

    @Test
    fun `get table info from response fetch table`() {
        val body = """[ {
            "code" : "Y",
            "name" : "yesno",
            "display" : "YES",
            "version" : ""
        }, {
            "code" : "Y",
            "name" : "yesno",
            "display" : "Y",
            "version" : ""
        }, {
            "code" : "N",
            "name" : "yesno",
            "display" : "NO",
            "version" : ""
        }]"""
        val table = getMockUtil(
            "${LookupTableEndpointUtilities.endpointRoot}/yesno/1/content",
            HttpStatusCode.OK,
            body = body
        ).fetchTableContent("yesno", 1)

        assertThat(table.size).isEqualTo(3)
    }

    @Test
    fun `get table info from response fetch table malformed body`() {
        val exception = assertFailsWith<IOException>(
            block = {
                val body = """"Just a bad json string"""
                getMockUtil(
                    "${LookupTableEndpointUtilities.endpointRoot}/yesno/1/content",
                    HttpStatusCode.OK,
                    body = body
                ).fetchTableContent("yesno", 1)
            }
        )
        assertTrue(exception.message.toString().contains("Unexpected end-of-input: was expecting closing quote"))
    }

    @Test
    fun `get table info from response activate table`() {
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