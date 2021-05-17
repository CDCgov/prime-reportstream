package gov.cdc.prime.router

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.support.expected
import assertk.assertions.support.show
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import kotlin.test.Test
import kotlin.test.assertEquals

class JurisdictionalFilterTests {

    private val rcvr = Receiver("name", "org", "topic", "schema", Report.Format.CSV)

    @Test
    fun `test Matches`() {
        val filter = Matches()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "a2", "X3")),
            StringColumn.create("colB", listOf("B1", "B2", "B3"))
        )
        val args1 = listOf("colA", "A1")
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertThat(filteredTable1).hasRowCount(1)
        assertThat(filteredTable1.getString(0, "colB")).isEqualTo("B1")

        val args2 = listOf("colA", "(?i)A.*") // test a regex
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertThat(filteredTable2).hasRowCount(2)
        assertThat(filteredTable2.getString(0, "colB")).isEqualTo("B1")
        assertThat(filteredTable2.getString(1, "colB")).isEqualTo("B2")
    }

    @Test
    fun `test empty Matches`() {
        val filter = Matches()
        val table = Table.create(
            StringColumn.create("BOGUS"),
        )

        val args1 = listOf("a", "b") // correct # args.
        // However, table doesn't have the expected columns, so an empty selection
        assertThat(filter.getSelection(args1, table, rcvr)).isEmpty()
    }

    @Test
    fun `test FilterByCounty`() {
        val filter = FilterByCounty()
        val table = Table.create(
            StringColumn.create(
                "patient_state", listOf("MD", "MD", "MD", "AZ")
            ),
            StringColumn.create(
                "patient_county", listOf("Prince George's", "Baltimore", "Baltimore City", "Pima")
            ),
            StringColumn.create(
                "ordering_facility_state", listOf("MD", "MD", "AZ", "AZ")
            ),
            StringColumn.create(
                "ordering_facility_county", listOf("Prince George's", "Montgomery", "Pima", "Pima Cty")
            ),
        )

        val args1 = listOf("MD", "Prince George's")
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertThat(filteredTable1).hasRowCount(1)
        assertThat(filteredTable1.getString(0, "patient_county")).isEqualTo("Prince George's")

        // Both Baltimore and Baltimore City should match
        val args2 = listOf("MD", "Baltimore")
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertThat(filteredTable2).hasRowCount(2)
        assertThat(filteredTable2.getString(0, "patient_county")).isEqualTo("Baltimore")
        assertThat(filteredTable2.getString(1, "patient_county")).isEqualTo("Baltimore City")

        // Test the 'OR':   result is selected if the patient OR the facility are in the location:
        val args3 = listOf("AZ", "Pima")
        val selection3 = filter.getSelection(args3, table, rcvr)
        val filteredTable3 = table.where(selection3)
        assertEquals(2, filteredTable3.rowCount())
        assertEquals("Baltimore City", filteredTable3.getString(0, "patient_county"))
        assertEquals("Pima", filteredTable3.getString(1, "patient_county"))

        val args4 = listOf("MD") // wrong num args
        assertThat { filter.getSelection(args4, table, rcvr) }.isFailure()
    }

    @Test
    fun `test filterByCountyWithMissingColumns`() {
        val filter = FilterByCounty()
        val table = Table.create(
            StringColumn.create("BOGUS", listOf("a", "b", "c", "d")),
            StringColumn.create("BOGUS2", listOf("a", "b", "c", "d")),
        )
        val args1 = listOf("a", "a") // correct # args.
        val selection = filter.getSelection(args1, table, rcvr)
        val filteredTable = table.where(selection)
        assertThat(filteredTable.rowCount()).isEqualTo(0)
    }

    @Test
    fun `test OrEquals`() {
        val filter = OrEquals()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "a2", "X3", "X4")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4"))
        )

        val emptyArgs = listOf<String>()
        assertThat { filter.getSelection(emptyArgs, table, rcvr) }.isFailure()

        val missingArgs = listOf("colA", "A1", "colB") // must have even number of args
        assertThat { filter.getSelection(missingArgs, table, rcvr) }.isFailure()

        val args1 = listOf("colA", "A1", "colB", "B3")
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertEquals(2, filteredTable1.rowCount())
        assertEquals("B1", filteredTable1.getString(0, "colB"))
        assertEquals("B3", filteredTable1.getString(1, "colB"))

        val args2 = listOf("colA", "(?i)A.*", "colB", ".*4") // test a regex
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertEquals(3, filteredTable2.rowCount())
        assertEquals("B1", filteredTable2.getString(0, "colB"))
        assertEquals("B2", filteredTable2.getString(1, "colB"))
        assertEquals("B4", filteredTable2.getString(2, "colB"))

        val args4 = listOf("colA", "([?:|") // malformed regex
        assertThat { filter.getSelection(args4, table, rcvr) }.isFailure()
    }

    @Test
    fun `test HasValidDataFor`() {
        val filter = HasValidDataFor()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "A2", "", "A4")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4")),
            StringColumn.create("colC", listOf("C1", "C2", "C3", null))
        )

        val emptyArgs = listOf<String>()
        var selection = filter.getSelection(emptyArgs, table, rcvr)
        var filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        val junkColName = listOf("quux")
        selection = filter.getSelection(junkColName, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())

        val oneGoodoneBadColName = listOf("quux", "colB")
        selection = filter.getSelection(oneGoodoneBadColName, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())

        val oneGoodColName = listOf("colB")
        selection = filter.getSelection(oneGoodColName, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        val colWithEmpty = listOf("colA")
        selection = filter.getSelection(colWithEmpty, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("A1", filteredTable.getString(0, "colA"))
        assertEquals("A2", filteredTable.getString(1, "colA"))
        assertEquals("A4", filteredTable.getString(2, "colA"))

        val colWithNull = listOf("colC")
        selection = filter.getSelection(colWithNull, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("C1", filteredTable.getString(0, "colC"))
        assertEquals("C2", filteredTable.getString(1, "colC"))
        assertEquals("C3", filteredTable.getString(2, "colC"))

        val allCols = listOf("colA", "colB", "colC")
        selection = filter.getSelection(allCols, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(2, filteredTable.rowCount())
        assertEquals("C1", filteredTable.getString(0, "colC"))
        assertEquals("C2", filteredTable.getString(1, "colC"))
    }

    @Test
    fun `test HasAtLeastOneOf`() {
        val filter = HasAtLeastOneOf()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "A2", "", "A4")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4")),
            StringColumn.create("colC", listOf("C1", "C2", "C3", null))
        )

        val emptyArgs = listOf<String>()
        assertThat { filter.getSelection(emptyArgs, table, rcvr) }.isFailure()

        val junkColNames = listOf("foo", "bar", "baz")
        var selection = filter.getSelection(junkColNames, table, rcvr)
        var filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())

        val oneGoodColName = listOf("foo", "bar", "colB")
        selection = filter.getSelection(oneGoodColName, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        val colWithEmptyString = listOf("foo", "colA")
        selection = filter.getSelection(colWithEmptyString, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("A1", filteredTable.getString(0, "colA"))
        assertEquals("A2", filteredTable.getString(1, "colA"))
        assertEquals("A4", filteredTable.getString(2, "colA"))

        val colWithNull = listOf("colC")
        selection = filter.getSelection(colWithNull, table, rcvr)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("C1", filteredTable.getString(0, "colC"))
        assertEquals("C2", filteredTable.getString(1, "colC"))
        assertEquals("C3", filteredTable.getString(2, "colC"))

        val allCols = listOf("colA", "colB", "colC")
        selection = filter.getSelection(allCols, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable.rowCount()).isEqualTo(4)

        // First row does not exist for colA nor colB. No colC at all.
        val table2 = Table.create(
            StringColumn.create("colA", listOf("", "A2")),
            StringColumn.create("colB", listOf(null, "B2")),
        )
        selection = filter.getSelection(allCols, table2, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable.rowCount()).isEqualTo(1)
        assertThat(filteredTable.getString(0, "colA")).isEqualTo("A2")
    }

    @Test
    fun `test allowAll`() {
        val filter = AllowAll()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "A2", "", "A4")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4")),
            StringColumn.create("colC", listOf("C1", "C2", "C3", null))
        )

        val colName = listOf("colA", "colB")
        assertThat { filter.getSelection(colName, table, rcvr) }.isFailure()

        val emptyArgs = listOf<String>()
        val selection = filter.getSelection(emptyArgs, table, rcvr)
        val filteredTable = table.where(selection)
        assertThat(filteredTable.rowCount()).isEqualTo(4)
    }

    companion object {
        private fun Assert<Table>.hasRowCount(expected: Int) = given { actual ->
            if (actual.rowCount() == expected) return
            expected("rowCount:${show(expected)} but was rowCount:${show(actual.rowCount())}")
        }
    }
}