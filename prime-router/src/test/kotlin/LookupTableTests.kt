package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.io.ByteArrayInputStream
import kotlin.test.Test

class LookupTableTests {
    private val table: LookupTable
    private val csv = """
            a,b
            1,2
            3,4
            5,6
    """.trimIndent()

    init {
        table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
    }

    @Test
    fun `test read table`() {
        assertThat(table.rowCount).isEqualTo(3)
    }

    @Test
    fun `test lookup`() {
        assertThat(table.hasColumn("a")).isTrue()
        assertThat(table.lookupValue("a", "3", "b")).isEqualTo("4")
    }

    @Test
    fun `test lookup second column`() {
        assertThat(table.lookupValue("b", "4", "a")).isEqualTo("3")
    }

    @Test
    fun `test bad lookup`() {
        assertThat(table.hasColumn("c")).isFalse()
        assertThat(table.lookupValue("a", "3", "c")).isNull()
    }

    @Test
    fun `test lookupValues`() {
        val csv = """
            a,b,c
            1,2,A
            3,4,B
            5,6,C
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        assertThat(table.lookupValues(listOf("a" to "3", "b" to "4"), "c")).isEqualTo("B")
        assertThat(table.lookupValues(listOf("a" to "1", "b" to "2"), "c")).isEqualTo("A")
        assertThat(table.lookupValues(listOf("a" to "1", "b" to "6"), "c")).isNull()
    }

    @Test
    fun `test table filter`() {
        val listOfValues = table.filter("a", "1", "b")
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
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        val listOfValues = table.filter("c", "A", "A")
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
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        val listOfValues = table.filter("c", "a", "A", false)
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
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
        val value = table.lookupValue("c", "c", "a", false)
        assertThat(value.isNullOrEmpty()).isTrue()
    }

    @Test
    fun `test zip code lookup`() {
        // arrange
        val metadata = Metadata("./metadata")
        val zipCodeTable = metadata.findLookupTable("zip-code-data")
        val state = "VT"
        val county = "Rutland"
        val zipCode = "05701"
        // act
        val matchingRows = zipCodeTable?.filter(
            "city",
            mapOf(
                "state_abbr" to state,
                "county" to county,
                "zipcode" to zipCode
            )
        )
        // assert
        assertThat(matchingRows?.isNotEmpty() == true).isTrue()
        assertThat(matchingRows?.getOrElse(0) { null } == "Rutland").isTrue()
    }

    @Test
    fun `test lookupBestMatch`() {
        val csv = """
            a,b
            1,A BX CX
            2,D EX FX
            3,X EX GX 
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
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
        assertThat(uncommonResult).equals("2")
    }

    @Test
    fun `test filtered lookupBestMatch`() {
        val csv = """
            a,b,c
            1,W,A BX CX
            2,W,D EX FX
            3,V,X EX GX 
        """.trimIndent()
        val table = LookupTable.read(ByteArrayInputStream(csv.toByteArray()))
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
}