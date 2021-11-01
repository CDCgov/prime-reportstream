package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import io.mockk.every
import io.mockk.mockk
import org.jooq.JSONB
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DatabaseLookupTableTests {
    @Test
    fun `load table test`() {
        val mockDbTableAccess = mockk<DatabaseLookupTableAccess>()
        val tableData = listOf(LookupTableRow(), LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"colA": "value1", "colB": "value2"}""")
        tableData[1].data = JSONB.jsonb("""{"colA": "value3", "colB": "value4"}""")
        val tableName = "name"
        val tableVersion = 1

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTable(tableName, mockDbTableAccess).loadTable(-1)
            }
        )

        every { mockDbTableAccess.fetchTable(any(), any()) } throws DataAccessException("error")
        assertFailsWith<DataAccessException>(
            block = {
                DatabaseLookupTable(tableName, mockDbTableAccess).loadTable(1)
            }
        )

        every { mockDbTableAccess.fetchTable(any(), any()) } returns tableData
        val table = DatabaseLookupTable(tableName, mockDbTableAccess)
        table.loadTable(tableVersion)
        assertThat(table.rowCount).isEqualTo(2)
        assertThat(table.version).isEqualTo(tableVersion)
        assertThat(table.name).isEqualTo(tableName)
        assertThat(table.hasColumn("colA")).isTrue()
        assertThat(table.hasColumn("colB")).isTrue()
        assertThat(table.lookupValue("colA", "value3", "colB")).isEqualTo("value4")
    }
}