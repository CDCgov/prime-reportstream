package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jooq.JSONB
import org.junit.jupiter.api.Test

class DatabaseLookupTableAccessTests {
    @Test
    fun `test extract headers from json`() {
        var headers = DatabaseLookupTableAccess
            .extractTableHeadersFromJson(JSONB.jsonb("{\"a\": \"value1\", \"b\": \"value2\"}"))
        assertThat(headers).isEqualTo(listOf("a", "b"))

        headers = DatabaseLookupTableAccess
            .extractTableHeadersFromJson(JSONB.jsonb("{}"))
        assertThat(headers.isEmpty()).isTrue()
    }

    @Test
    fun `test extract row data from json`() {
        val colNames = listOf("a", "b")
        var data = DatabaseLookupTableAccess
            .extractTableRowFromJson(JSONB.jsonb("{\"a\": \"value1\", \"b\": \"value2\"}"), colNames)
        assertThat(data).isEqualTo(listOf("value1", "value2"))

        data = DatabaseLookupTableAccess
            .extractTableRowFromJson(JSONB.jsonb("{\"a\": \"value1\"}"), colNames)
        assertThat(data).isEqualTo(listOf("value1", ""))

        data = DatabaseLookupTableAccess
            .extractTableRowFromJson(JSONB.jsonb("{}"), colNames)
        assertThat(data).isEqualTo(listOf("", ""))
    }

    @Test
    fun `test set table row to json`() {
        var json = DatabaseLookupTableAccess.setTableRowToJson(mapOf("a" to "value1", "b" to "value2"))
        var jsonObject = Json.parseToJsonElement(json.data()) as JsonObject
        assertThat((jsonObject["a"] as JsonPrimitive).contentOrNull).isEqualTo("value1")
        assertThat((jsonObject["b"] as JsonPrimitive).contentOrNull).isEqualTo("value2")

        json = DatabaseLookupTableAccess.setTableRowToJson(emptyMap())
        jsonObject = Json.parseToJsonElement(json.data()) as JsonObject
        assertThat(jsonObject.keys.isEmpty()).isTrue()
    }
}