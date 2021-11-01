package gov.cdc.prime.router.metadata

import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import io.mockk.every
import io.mockk.mockk
import org.jooq.JSONB
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DatabaseLookupTableTests {
    @Test
    fun `load table test`() {
        val mockDbTableAccess = mockk<DatabaseLookupTableAccess>()
        val tableData = listOf(LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"colA": "valueA", "colb": "valueB"}""")

        assertFailsWith<IllegalArgumentException>(
            block = {
                DatabaseLookupTable("name", mockDbTableAccess).loadTable(-1)
            }
        )

        every { mockDbTableAccess.fetchTable(any(), any()) } returns tableData
        DatabaseLookupTable("name", mockDbTableAccess).loadTable(1)
    }
}