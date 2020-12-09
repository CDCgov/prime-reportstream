package gov.cdc.prime.router

import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `test FilterByCounty`() {
        val filter = FilterByCounty()
        val table = Table.create(
            StringColumn.create("standard.patient_state", listOf("MD", "MD", "MD", "AZ")),
            StringColumn.create("standard.patient_county", listOf("Prince George's", "Baltimore", "Baltimore City", "Pima")),
            StringColumn.create("standard.ordering_facility_state", listOf("MD", "MD", "AZ", "AZ")),
            StringColumn.create("standard.ordering_facility_county", listOf("Prince George's", "Montgomery", "Pima", "Pima Cty")),
        )

        val args1 = listOf("MD", "Prince George's")
        val selection1 = filter.getSelection(args1, table)
        val filteredTable1 = table.where(selection1)
        assertEquals(1, filteredTable1.rowCount())
        assertEquals("Prince George's", filteredTable1.getString(0, "standard.patient_county"))

        // Both Baltimore and Baltimore City should match
        val args2 = listOf("MD", "Baltimore")
        val selection2 = filter.getSelection(args2, table)
        val filteredTable2 = table.where(selection2)
        assertEquals(2, filteredTable2.rowCount())
        assertEquals("Baltimore", filteredTable2.getString(0, "standard.patient_county"))
        assertEquals("Baltimore City", filteredTable2.getString(1, "standard.patient_county"))

        // Test the 'OR':   result is selected if the patient OR the facility are in the location:
        val args3 = listOf("AZ", "Pima")
        val selection3 = filter.getSelection(args3, table)
        val filteredTable3 = table.where(selection3)
        assertEquals(2, filteredTable3.rowCount())
        assertEquals("Baltimore City", filteredTable3.getString(0, "standard.patient_county"))
        assertEquals("Pima", filteredTable3.getString(1, "standard.patient_county"))
    }
}