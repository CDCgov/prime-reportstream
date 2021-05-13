package gov.cdc.prime.router

import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class JurisdictionalFilterTests {
    @Test
    fun `test Matches`() {
        val filter = Matches()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "a2", "X3")),
            StringColumn.create("colB", listOf("B1", "B2", "B3"))
        )

        val args1 = listOf("colA", "A1")
        val selection1 = filter.getSelection(args1, table)
        val filteredTable1 = table.where(selection1)
        assertEquals(1, filteredTable1.rowCount())
        assertEquals("B1", filteredTable1.getString(0, "colB"))

        val args2 = listOf("colA", "(?i)A.*") // test a regex
        val selection2 = filter.getSelection(args2, table)
        val filteredTable2 = table.where(selection2)
        assertEquals(2, filteredTable2.rowCount())
        assertEquals("B1", filteredTable2.getString(0, "colB"))
        assertEquals("B2", filteredTable2.getString(1, "colB"))
    }

    @Test
    fun `test empty Matches`() {
        val filter = Matches()
        val table = Table.create(
            StringColumn.create("BOGUS"),
        )

        val args1 = listOf("a", "b") // correct # args.
        // However, table doesn't have the expected columns, so an empty selection
        assertTrue(filter.getSelection(args1, table).isEmpty)
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
        val selection1 = filter.getSelection(args1, table)
        val filteredTable1 = table.where(selection1)
        assertEquals(1, filteredTable1.rowCount())
        assertEquals("Prince George's", filteredTable1.getString(0, "patient_county"))

        // Both Baltimore and Baltimore City should match
        val args2 = listOf("MD", "Baltimore")
        val selection2 = filter.getSelection(args2, table)
        val filteredTable2 = table.where(selection2)
        assertEquals(2, filteredTable2.rowCount())
        assertEquals("Baltimore", filteredTable2.getString(0, "patient_county"))
        assertEquals("Baltimore City", filteredTable2.getString(1, "patient_county"))

        // Test the 'OR':   result is selected if the patient OR the facility are in the location:
        val args3 = listOf("AZ", "Pima")
        val selection3 = filter.getSelection(args3, table)
        val filteredTable3 = table.where(selection3)
        assertEquals(2, filteredTable3.rowCount())
        assertEquals("Baltimore City", filteredTable3.getString(0, "patient_county"))
        assertEquals("Pima", filteredTable3.getString(1, "patient_county"))

        val args4 = listOf("MD") // wrong num args
        assertFails { filter.getSelection(args4, table) }
    }

    @Test
    fun `test filterByCountyWithMissingColumns`() {
        val filter = FilterByCounty()
        val table = Table.create(
            StringColumn.create("BOGUS", listOf("a", "b", "c", "d")),
            StringColumn.create("BOGUS2", listOf("a", "b", "c", "d")),
        )

        val args1 = listOf("a", "a") // correct # args.
        val selection = filter.getSelection(args1, table)
        val filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())
    }

    @Test
    fun `test OrEquals`() {
        val filter = OrEquals()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "a2", "X3", "X4")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4"))
        )

        val emptyArgs = listOf<String>()
        assertFails { filter.getSelection(emptyArgs, table) }

        val missingArgs = listOf("colA", "A1", "colB") // must have even number of args
        assertFails { filter.getSelection(missingArgs, table) }

        val args1 = listOf("colA", "A1", "colB", "B3")
        val selection1 = filter.getSelection(args1, table)
        val filteredTable1 = table.where(selection1)
        assertEquals(2, filteredTable1.rowCount())
        assertEquals("B1", filteredTable1.getString(0, "colB"))
        assertEquals("B3", filteredTable1.getString(1, "colB"))

        val args2 = listOf("colA", "(?i)A.*", "colB", ".*4") // test a regex
        val selection2 = filter.getSelection(args2, table)
        val filteredTable2 = table.where(selection2)
        assertEquals(3, filteredTable2.rowCount())
        assertEquals("B1", filteredTable2.getString(0, "colB"))
        assertEquals("B2", filteredTable2.getString(1, "colB"))
        assertEquals("B4", filteredTable2.getString(2, "colB"))

        val args4 = listOf("colA", "([?:|") // malformed regex
        assertFails { val selection4 = filter.getSelection(args4, table) }
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
        var selection = filter.getSelection(emptyArgs, table)
        var filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        val junkColName = listOf("quux")
        selection = filter.getSelection(junkColName, table)
        filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())

        val oneGoodoneBadColName = listOf("quux", "colB")
        selection = filter.getSelection(oneGoodoneBadColName, table)
        filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())

        val oneGoodColName = listOf("colB")
        selection = filter.getSelection(oneGoodColName, table)
        filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        val colWithEmpty = listOf("colA")
        selection = filter.getSelection(colWithEmpty, table)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("A1", filteredTable.getString(0, "colA"))
        assertEquals("A2", filteredTable.getString(1, "colA"))
        assertEquals("A4", filteredTable.getString(2, "colA"))

        val colWithNull = listOf("colC")
        selection = filter.getSelection(colWithNull, table)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("C1", filteredTable.getString(0, "colC"))
        assertEquals("C2", filteredTable.getString(1, "colC"))
        assertEquals("C3", filteredTable.getString(2, "colC"))

        val allCols = listOf("colA", "colB", "colC")
        selection =  filter.getSelection(allCols, table)
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
        assertFails{ filter.getSelection(emptyArgs, table) }

        val junkColNames = listOf("foo", "bar", "baz")
        var selection =  filter.getSelection(junkColNames, table)
        var filteredTable = table.where(selection)
        assertEquals(0, filteredTable.rowCount())

        val oneGoodColName = listOf("foo", "bar", "colB")
        selection =  filter.getSelection(oneGoodColName, table)
        filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        val colWithEmptyString = listOf("foo", "colA")
        selection =  filter.getSelection(colWithEmptyString, table)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("A1", filteredTable.getString(0, "colA"))
        assertEquals("A2", filteredTable.getString(1, "colA"))
        assertEquals("A4", filteredTable.getString(2, "colA"))

        val colWithNull = listOf("colC")
        selection =  filter.getSelection(colWithNull, table)
        filteredTable = table.where(selection)
        assertEquals(3, filteredTable.rowCount())
        assertEquals("C1", filteredTable.getString(0, "colC"))
        assertEquals("C2", filteredTable.getString(1, "colC"))
        assertEquals("C3", filteredTable.getString(2, "colC"))

        val allCols = listOf("colA", "colB", "colC")
        selection =  filter.getSelection(allCols, table)
        filteredTable = table.where(selection)
        assertEquals(4, filteredTable.rowCount())

        // First row does not exist for colA nor colB. No colC at all.
        val table2 = Table.create(
            StringColumn.create("colA", listOf("", "A2")),
            StringColumn.create("colB", listOf(null, "B2")),
        )
        selection =  filter.getSelection(allCols, table2)
        filteredTable = table.where(selection)
        assertEquals(1, filteredTable.rowCount())
        assertEquals("A2", filteredTable.getString(0, "colA"))
    }
}
