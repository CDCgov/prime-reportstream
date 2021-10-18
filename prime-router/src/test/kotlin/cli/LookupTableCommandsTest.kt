package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jooq.JSONB
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LookupTableCommandsTest {
    @Test
    fun `test extract headers from json`() {
        var headers = LookupTableCommands
            .extractTableHeadersFromJson(JSONB.jsonb("{\"a\": \"value1\", \"b\": \"value2\"}"))
        assertThat(headers).isEqualTo(listOf("a", "b"))

        headers = LookupTableCommands
            .extractTableHeadersFromJson(JSONB.jsonb("{}"))
        assertThat(headers.isEmpty()).isTrue()
    }

    @Test
    fun `test extract row data from json`() {
        val colNames = listOf("a", "b")
        var data = LookupTableCommands
            .extractTableRowFromJson(JSONB.jsonb("{\"a\": \"value1\", \"b\": \"value2\"}"), colNames)
        assertThat(data).isEqualTo(listOf("value1", "value2"))

        data = LookupTableCommands
            .extractTableRowFromJson(JSONB.jsonb("{\"a\": \"value1\"}"), colNames)
        assertThat(data).isEqualTo(listOf("value1", ""))

        data = LookupTableCommands
            .extractTableRowFromJson(JSONB.jsonb("{}"), colNames)
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
    fun `test rows to table`() {
        val data = listOf(LookupTableRow())
        val colNames = listOf("a", "b")
        data[0].data = JSONB.jsonb("{\"a\": \"value1\", \"b\": \"value2\"}")
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
}