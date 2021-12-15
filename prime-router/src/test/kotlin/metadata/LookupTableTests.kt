package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
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
    private val table: LookupTable
    private val csv = """
            a,b
            1,2
            3,4
            5,6
    """.trimIndent()

    init {
        table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
    }

    @Test
    fun `test read table`() {
        assertThat(table.rowCount).isEqualTo(3)
    }

    @Test
    fun `test lookup`() {
        assertThat(table.hasColumn("a")).isTrue()
        assertThat(table.lookupValue("b", mapOf("a" to "3"))).isEqualTo("4")
    }

    @Test
    fun `test lookup second column`() {
        assertThat(table.lookupValue("a", mapOf("b" to "4"))).isEqualTo("3")
    }

    @Test
    fun `test bad lookup`() {
        assertThat(table.hasColumn("c")).isFalse()
        assertThat(table.lookupValue("c", mapOf("a" to "3"))).isNull()
    }

    @Test
    fun `test table filter`() {
        val listOfValues = table.lookupValues("b", mapOf("a" to "1"))
        assertThat(listOfValues.isEmpty()).isFalse()
        assertThat(listOfValues[0]).isEqualTo("2")
    }

    @Test
    fun `test table filter ignoreCase`() {
        val csv = """
            a,b,c
            1,2,A
            3,4,B
            5,6,C
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val listOfValues = table.lookupValues("A", mapOf("c" to "A"))
        assertThat(listOfValues.isEmpty())
        assertThat(listOfValues[0]).isEqualTo("1")
    }

    @Test
    fun `test table filter but don't ignoreCase`() {
        val csv = """
            a,b,c
            1,2,A
            3,4,B
            5,6,C
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val listOfValues = table.lookupValues("A", mapOf("c" to "a"), false)
        assertThat(listOfValues.isEmpty()).isTrue()
    }

    @Test
    fun `test table lookup but don't ignoreCase`() {
        val csv = """
            a,b,c
            1,2,A
            3,4,B
            5,6,C
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val value = table.lookupValue("a", mapOf("c" to "c"), false)
        assertThat(value.isNullOrEmpty()).isTrue()
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
    fun `lookup values test`() {
        val tableData = listOf(
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
        val table = LookupTable(table = tableData)

        // Simple search
        var result = table.lookupValues("a", mapOf("b" to "valueB2"))
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo("valueA2")

        // This search will have two matches
        result = table.lookupValues("a", mapOf("b" to "repeatedIndex"))
        assertThat(result.size).isEqualTo(2)

        // This search has multiple matches, but they are all the same value
        result = table.lookupValues("a", mapOf("b" to "repeatedIndex2"))
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualTo("repeatedValue")

        // This search has multiple matches, but one value is repeated
        result = table.lookupValues("a", mapOf("b" to "repeatedIndex3"))
        assertThat(result.size).isEqualTo(2)

        // Nothing found
        result = table.lookupValues("a", mapOf("b" to "dummy"))
        assertThat(result.size).isEqualTo(0)

        // Error tests
        assertThat(table.lookupValues("a", mapOf("b" to "dummy"))).isEmpty()
        assertThat(table.lookupValues("dummmy", mapOf("b" to "repeatedIndex3"))).isEmpty()
        assertThat(table.lookupValues("a", mapOf("dummy" to "repeatedIndex3"))).isEmpty()

        val table2 = LookupTable.read("./metadata/tables/LIVD-SARS-CoV-2-2021-09-29.csv")
        assertThat(table2.rowCount).isGreaterThan(0)
    }

    @Test
    fun `lookup prefix values test`() {
        val tableData = listOf(
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
        val table = LookupTable(table = tableData)

        // Simple search
        var result = table.lookupPrefixValues("a", mapOf("b" to "valueB"))
        assertThat(result.size).isEqualTo(3)
        assertThat(result.contains("valueA1")).isTrue()
        assertThat(result.contains("valueA2")).isTrue()
        assertThat(result.contains("valueA3")).isTrue()

        // Ignore case
        result = table.lookupPrefixValues("a", mapOf("b" to "VALUEB"), ignoreCase = true)
        assertThat(result.size).isEqualTo(3)
        assertThat(result.contains("valueA1")).isTrue()
        assertThat(result.contains("valueA2")).isTrue()
        assertThat(result.contains("valueA3")).isTrue()

        // Multiple returns
        result = table.lookupPrefixValues("a", mapOf("b" to "repeatedIndex"))
        assertThat(result.size).isEqualTo(5)
        result = table.lookupPrefixValues("a", mapOf("b" to "repeatedIndex2"))
        assertThat(result.size).isEqualTo(1)

        // Error cases
        assertThat(table.lookupPrefixValues("a", mapOf("b" to "dummy"))).isEmpty()
        assertThat(table.lookupPrefixValues("dummmy", mapOf("b" to "repeatedIndex3"))).isEmpty()
        assertThat(table.lookupPrefixValues("a", mapOf("dummy" to "repeatedIndex3"))).isEmpty()
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
        assertThat(table.lookupValue("colB", mapOf("colA" to "value3"))).isEqualTo("value4")
    }
}