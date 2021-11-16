package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.jooq.JSONB
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DatabaseLookupTableAccessTests {
    @Test
    fun `test extract headers from json`() {
        val headers = DatabaseLookupTableAccess
            .extractTableHeadersFromJson(JSONB.jsonb("""{"a": "value1", "b": "value2"}"""))
        assertThat(headers).isEqualTo(listOf("a", "b"))

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTableAccess
                    .extractTableHeadersFromJson(JSONB.jsonb("{}"))
            }
        )

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTableAccess
                    .extractTableHeadersFromJson(JSONB.jsonb("[]"))
            }
        )

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTableAccess
                    .extractTableHeadersFromJson(JSONB.jsonb(""))
            }
        )
    }

    @Test
    fun `get data batch test`() {
        val dummyRow = JSONB.jsonb("""{"a":"1"}""")
        val uniqueRow = JSONB.jsonb("""{"b":"2"}""")

        val tableData = MutableList(21) { dummyRow }
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 1, 10)).isNotNull()
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 2, 10)).isNotNull()
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 3, 10)).isNotNull()
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 4, 10)).isNull()

        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 1, 10)!!.size).isEqualTo(10)
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 2, 10)!!.size).isEqualTo(10)
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 3, 10)!!.size).isEqualTo(1)
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 4, 10)).isNull()

        // Let's check that each row is in the correct location in the batch.
        tableData[11] = uniqueRow
        assertThat(DatabaseLookupTableAccess.getDataBatch(tableData, 2, 10)!![1]).isEqualTo(uniqueRow)

        // Empty lists returns a null
        assertThat(DatabaseLookupTableAccess.getDataBatch(emptyList(), 1, 10)).isNull()
    }
}