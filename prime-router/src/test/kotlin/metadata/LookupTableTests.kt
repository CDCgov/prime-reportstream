package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import io.mockk.every
import io.mockk.mockk
import org.jooq.JSONB
import org.jooq.exception.DataAccessException
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LookupTableTests {
    private val tableData2 = listOf(
        listOf("a", "b", "c"),
        listOf("valueA1", "valueB1", "valueC1"),
        listOf("valueA2", "valueB2", "valueC2"),
        listOf("valueA3", "valueB3", "valueC3"),
        listOf("valueA4", "repeatedIndex", "valueC4"),
        listOf("valueA5", "repeatedIndex", "valueC5"),
        listOf("repeatedValue", "repeatedIndex2", "valueC5"),
        listOf("repeatedValue", "repeatedIndex2", "valueC5"),
        listOf("repeatedValue2", "repeatedIndex3", "valueC5"),
        listOf("repeatedValue2", "repeatedIndex3", "valueC5"),
        listOf("repeatedValue2", "repeatedIndex3", "valueC5"),
        listOf("repeatedValue3", "repeatedIndex3", "valueC5")
    )

    @Test
    fun `test read table`() {
        val table: LookupTable
        val csv = """
            a,b
            1,2
            3,4
            5,6
        """.trimIndent()
        table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        assertThat(table.rowCount).isEqualTo(3)
    }

    @Test
    fun `test lookupBestMatch`() {
        val csv = """
            a,b
            1,A BX CX
            2,D EX FX
            3,X EX GX 
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val canonicalize = { s: String -> s }

        // Match with one word
        val oneResult = table.lookupBestMatch(
            "b",
            "BX",
            "a",
            canonicalize,
            listOf("A", "D", "X")
        )
        assertThat(oneResult).isEqualTo("1")

        // Match with two words
        val twoResult = table.lookupBestMatch(
            "b",
            "EX GX",
            "a",
            canonicalize,
            listOf("A", "D", "X")
        )
        assertThat(twoResult).isEqualTo("3")

        // No match
        val noResult = table.lookupBestMatch(
            "b",
            "NO",
            "a",
            canonicalize,
            listOf("A", "D", "X")
        )
        assertThat(noResult).isNull()

        // Match with only a common word
        val commonResult = table.lookupBestMatch(
            "b",
            "D",
            "a",
            { it },
            listOf("A", "D", "X")
        )
        assertThat(commonResult).isNull()

        // Match with only a common word and an uncommon word
        val uncommonResult = table.lookupBestMatch(
            "b",
            "D EX",
            "a",
            canonicalize,
            listOf("A", "D", "X")
        )
        assertThat(uncommonResult).isEqualTo("2")
    }

    @Test
    fun `test filtered lookupBestMatch`() {
        val csv = """
            a,b,c
            1,W,A BX CX
            2,W,D EX FX
            3,V,X EX GX 
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val canonicalize = { s: String -> s }

        // Match with one word
        val oneResult = table.lookupBestMatch(
            "c",
            "BX",
            "a",
            canonicalize,
            listOf("A", "D", "X"),
            filterColumn = "b",
            filterValue = "W"
        )
        assertThat(oneResult).isEqualTo("1")

        // Do not match with filter
        val noMatch = table.lookupBestMatch(
            "c",
            "BX",
            "a",
            canonicalize,
            listOf("A", "D", "X"),
            filterColumn = "b",
            filterValue = "V"
        )
        assertThat(noMatch).isNull()
    }

    @Test
    fun `lookup exact match test`() {
        val table = LookupTable(table = tableData2)

        // Simple search
        var result = table.FilterBuilder().equals(tableData2[0][1], tableData2[3][1]).findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo(tableData2[3][0])

        // This search will have two matches
        result = table.FilterBuilder().equals(tableData2[0][1], "repeatedIndex").findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(2)

        // This search has multiple matches, but they are all the same value
        result = table.FilterBuilder().equals(tableData2[0][1], "repeatedIndex2").findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo("repeatedValue")

        // This search has multiple matches, but one value is repeated
        result = table.FilterBuilder().equals(tableData2[0][1], "repeatedIndex3").findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(2)

        // Nothing found
        result = table.FilterBuilder().equals(tableData2[0][1], "dummy").findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(0)

        // Lookup single value
        assertThat(table.FilterBuilder().equals(tableData2[0][1], tableData2[3][1]).findSingleResult(tableData2[0][0]))
            .isNotNull()
        assertThat(
            table.FilterBuilder().equals(tableData2[0][1], "repeatedIndex")
                .findSingleResult(tableData2[0][0])
        ).isNull()

        // Multiple matches
        result = table.FilterBuilder()
            .equals(mapOf(tableData2[0][0] to tableData2[1][0], tableData2[0][1] to tableData2[1][1]))
            .findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(1)

        result = table.FilterBuilder()
            .equals(mapOf(tableData2[0][0] to tableData2[1][0], tableData2[0][1] to tableData2[2][1]))
            .findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(0)

        // Error tests
        assertThat(
            table.FilterBuilder().equals(tableData2[0][1], "dummy")
                .findAllUnique(tableData2[0][0])
        ).isEmpty()
        assertThat(
            table.FilterBuilder().equals(tableData2[0][1], "repeatedIndex3")
                .findAllUnique("dummy")
        ).isEmpty()
        assertThat(
            table.FilterBuilder().equals("dummy", "repeatedIndex3")
                .findAllUnique(tableData2[0][0])
        ).isEmpty()
    }

    @Test
    fun `lookup prefix match test`() {
        val table = LookupTable(table = tableData2)

        // Simple search
        var result = table.FilterBuilder().startsWith(tableData2[0][1], "valueB").findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(3)
        assertThat(result.contains(tableData2[1][0])).isTrue()
        assertThat(result.contains(tableData2[2][0])).isTrue()
        assertThat(result.contains(tableData2[3][0])).isTrue()

        // Ignore case
        result = table.FilterBuilder().startsWithIgnoreCase(tableData2[0][1], "VALUEB")
            .findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(3)
        assertThat(result.contains(tableData2[1][0])).isTrue()
        assertThat(result.contains(tableData2[2][0])).isTrue()
        assertThat(result.contains(tableData2[3][0])).isTrue()

        // Multiple returns
        assertThat(
            table.FilterBuilder().startsWith(tableData2[0][1], "repeatedIndex")
                .findAllUnique(tableData2[0][0]).size
        ).isEqualTo(5)
        assertThat(
            table.FilterBuilder().startsWith(tableData2[0][1], "repeatedIndex2")
                .findAllUnique(tableData2[0][0]).size
        ).isEqualTo(1)

        // Lookup one value
        assertThat(
            table.FilterBuilder().startsWith(tableData2[0][1], "repeatedIndex")
                .findSingleResult(tableData2[0][0])
        ).isNull()
        assertThat(
            table.FilterBuilder().startsWith(tableData2[0][1], "repeatedIndex2")
                .findSingleResult(tableData2[0][0])
        ).isNotNull()

        // With exact matches
        result = table.FilterBuilder().startsWith(tableData2[0][1], "value")
            .equals(tableData2[0][2], tableData2[3][2]).findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(1)
        assertThat(result.contains(tableData2[3][0])).isTrue()

        // Multiple matches
        result = table.FilterBuilder()
            .startsWith(mapOf(tableData2[0][0] to "valueA", tableData2[0][1] to "valueB"))
            .findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(3)

        result = table.FilterBuilder()
            .startsWith(mapOf(tableData2[0][0] to "valueA", tableData2[0][1] to "repeatedIndex2"))
            .findAllUnique(tableData2[0][0])
        assertThat(result.size).isEqualTo(0)

        // Error cases
        assertThat(
            table.FilterBuilder().startsWith(tableData2[0][1], "dummy")
                .findAllUnique(tableData2[0][0])
        ).isEmpty()
        assertThat(
            table.FilterBuilder().startsWith(tableData2[0][1], "repeatedIndex3")
                .findAllUnique("dummy")
        ).isEmpty()
        assertThat(
            table.FilterBuilder().startsWith("dummy", "repeatedIndex3")
                .findAllUnique(tableData2[0][0])
        ).isEmpty()
    }

    @org.junit.jupiter.api.Test
    fun `load table test`() {
        val mockDbTableAccess = mockk<DatabaseLookupTableAccess>()
        val tableData = listOf(LookupTableRow(), LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"colA": "value1", "colB": "value2"}""")
        tableData[1].data = JSONB.jsonb("""{"colA": "value3", "colB": "value4"}""")
        val tableName = "name"
        val tableVersion = 1

        assertFailsWith<IllegalArgumentException>(
            block = {
                LookupTable(tableName, emptyList(), mockDbTableAccess).loadTable(-1)
            }
        )

        every { mockDbTableAccess.fetchTable(any(), any()) } throws DataAccessException("error")
        assertFailsWith<DataAccessException>(
            block = {
                LookupTable(tableName, emptyList(), mockDbTableAccess).loadTable(1)
            }
        )

        every { mockDbTableAccess.fetchTable(any(), any()) } returns tableData
        val table = LookupTable(tableName, emptyList(), mockDbTableAccess)
        table.loadTable(tableVersion)
        assertThat(table.rowCount).isEqualTo(2)
        assertThat(table.version).isEqualTo(tableVersion)
        assertThat(table.name).isEqualTo(tableName)
        assertThat(table.hasColumn("colA")).isTrue()
        assertThat(table.hasColumn("colB")).isTrue()
        assertThat(table.FilterBuilder().equals("colA", "value3").findSingleResult("colB"))
            .isEqualTo("value4")
    }

    @Test
    fun `get data rows test`() {
        val dataRows = LookupTable(table = tableData2).dataRows
        assertThat(dataRows).isNotEmpty()
        assertThat(dataRows.size).isEqualTo(tableData2.size - 1)
        assertThat(dataRows.last()).isEqualTo(tableData2.last())

        assertThat(LookupTable().dataRows).isEmpty()
    }

    @Test
    fun `get distinct values test`() {
        val table = LookupTable(table = tableData2)
        val values = table.FilterBuilder().findAllUnique(tableData2[0][0])
        assertThat(values).isNotEmpty()
        assertThat(values.size).isEqualTo(8)
        tableData2.forEachIndexed { index, list ->
            if (index > 0) assertThat(values).contains(list[0]) // Skip the header
        }

        assertThat(table.FilterBuilder().findAllUnique("dummy")).isEmpty()
    }

    @Test
    fun `filter test`() {
        val table = LookupTable(table = tableData2)
        var filteredTable = table.FilterBuilder().equals(tableData2[0][0], tableData2[1][0]).filter()
        assertThat(filteredTable).isNotNull()
        assertThat(filteredTable.rowCount).isEqualTo(1)

        filteredTable = table.FilterBuilder().equals(tableData2[0][1], tableData2[4][1]).filter()
        assertThat(filteredTable).isNotNull()
        assertThat(filteredTable.rowCount).isEqualTo(2)

        assertThat(table.FilterBuilder().equals("dummy", "dummy").filter().rowCount).isEqualTo(0)
    }
}