package gov.cdc.prime.router

import assertk.Assert
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import assertk.assertions.support.show
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import kotlin.test.Test

class ReportStreamFilterDefinitionTests {

    private val rcvr = Receiver("name", "org", Topic.TEST, CustomerStatus.INACTIVE, "schema", Report.Format.CSV)

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
    fun `test Matches with multiple regexi`() {
        val filter = Matches()
        val table = Table.create(
            StringColumn.create("colA", listOf("A long list of items here", "items", "A short list here")),
            StringColumn.create("colB", listOf("B1", "B2", "B3"))
        )
        val args1 = listOf("colA", "items")
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertThat(filteredTable1).hasRowCount(1)
        assertThat(filteredTable1.getString(0, "colB")).isEqualTo("B2")

        val args2 = listOf("colA", ".*items.*", ".*short.*") // test multiple regexi
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertThat(filteredTable2).hasRowCount(3)
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
    fun `test DoesNotMatch`() {
        val filter = DoesNotMatch()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "a2", "X3")),
            StringColumn.create("colB", listOf("B1", "B2", "B3"))
        )
        val args1 = listOf("colA", "A1")
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertThat(filteredTable1).hasRowCount(2)
        assertThat(filteredTable1.getString(0, "colB")).isEqualTo("B2")
        assertThat(filteredTable1.getString(1, "colB")).isEqualTo("B3")

        val args2 = listOf("colA", "(?i)A.*") // test a regex
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertThat(filteredTable2).hasRowCount(1)
        assertThat(filteredTable2.getString(0, "colB")).isEqualTo("B3")

        val args3 = listOf("colA", "(?i)A.*", "X.*") // test multiple regexi
        val selection3 = filter.getSelection(args3, table, rcvr)
        val filteredTable3 = table.where(selection3)
        assertThat(filteredTable3).hasRowCount(0)
    }

    @Test
    fun `test empty DoesNotMatch`() {
        val filter = DoesNotMatch()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", null, "")),
            StringColumn.create("colB", listOf("B1", "B2", "B3"))
        )

        val args1 = listOf("x", "y") // no such column
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertThat(filteredTable1).hasRowCount(3)

        val args2 = listOf("colA", "A1")
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertThat(filteredTable2).hasRowCount(2)
        assertThat(filteredTable2.getString(0, "colB")).isEqualTo("B2")
        assertThat(filteredTable2.getString(1, "colB")).isEqualTo("B3")
    }

    @Test
    fun `test real world DoesNotMatch`() {
        val filter = DoesNotMatch()

        val table = Table.create(
            StringColumn.create("processing_mode_code", listOf("T", "P", "D", "")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4"))
        )
        val args1 = listOf("processing_mode_code", "T", "D") // no such column
        val selection1 = filter.getSelection(args1, table, rcvr)
        val filteredTable1 = table.where(selection1)
        assertThat(filteredTable1).hasRowCount(2)
        assertThat(filteredTable1.getString(0, "colB")).isEqualTo("B2")
        assertThat(filteredTable1.getString(1, "colB")).isEqualTo("B4")

        val table2 = Table.create(
            StringColumn.create("processing_toad_code", listOf("T", "P", "D", "")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4"))
        )
        val args2 = listOf("processing_mode_code", "T", "D") // no such column
        val selection2 = filter.getSelection(args2, table2, rcvr)
        val filteredTable2 = table2.where(selection2)
        assertThat(filteredTable2).hasRowCount(4)
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
        assertThat(filteredTable3).hasRowCount(2)
        assertThat(filteredTable3.getString(0, "patient_county")).isEqualTo("Baltimore City")
        assertThat(filteredTable3.getString(1, "patient_county")).isEqualTo("Pima")

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
        assertThat(filteredTable).hasRowCount(0)
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
        assertThat(filteredTable1).hasRowCount(2)
        assertThat(filteredTable1.getString(0, "colB")).isEqualTo("B1")
        assertThat(filteredTable1.getString(1, "colB")).isEqualTo("B3")

        val args2 = listOf("colA", "(?i)A.*", "colB", ".*4") // test a regex
        val selection2 = filter.getSelection(args2, table, rcvr)
        val filteredTable2 = table.where(selection2)
        assertThat(filteredTable2).hasRowCount(3)
        assertThat(filteredTable2.getString(0, "colB")).isEqualTo("B1")
        assertThat(filteredTable2.getString(1, "colB")).isEqualTo("B2")
        assertThat(filteredTable2.getString(2, "colB")).isEqualTo("B4")

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
        assertThat(filteredTable).hasRowCount(4)

        val junkColName = listOf("quux")
        selection = filter.getSelection(junkColName, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(0)

        val oneGoodoneBadColName = listOf("quux", "colB")
        selection = filter.getSelection(oneGoodoneBadColName, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(0)

        val oneGoodColName = listOf("colB")
        selection = filter.getSelection(oneGoodColName, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(4)

        val colWithEmpty = listOf("colA")
        selection = filter.getSelection(colWithEmpty, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(3)
        assertThat(filteredTable.getString(0, "colA")).isEqualTo("A1")
        assertThat(filteredTable.getString(1, "colA")).isEqualTo("A2")
        assertThat(filteredTable.getString(2, "colA")).isEqualTo("A4")

        val colWithNull = listOf("colC")
        selection = filter.getSelection(colWithNull, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(3)
        assertThat(filteredTable.getString(0, "colC")).isEqualTo("C1")
        assertThat(filteredTable.getString(1, "colC")).isEqualTo("C2")
        assertThat(filteredTable.getString(2, "colC")).isEqualTo("C3")

        val allCols = listOf("colA", "colB", "colC")
        selection = filter.getSelection(allCols, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(2)
        assertThat(filteredTable.getString(0, "colC")).isEqualTo("C1")
        assertThat(filteredTable.getString(1, "colC")).isEqualTo("C2")
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
        assertThat(filteredTable).hasRowCount(0)

        val oneGoodColName = listOf("foo", "bar", "colB")
        selection = filter.getSelection(oneGoodColName, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(4)

        val colWithEmptyString = listOf("foo", "colA")
        selection = filter.getSelection(colWithEmptyString, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(3)
        assertThat(filteredTable.getString(0, "colA")).isEqualTo("A1")
        assertThat(filteredTable.getString(1, "colA")).isEqualTo("A2")
        assertThat(filteredTable.getString(2, "colA")).isEqualTo("A4")

        val colWithNull = listOf("colC")
        selection = filter.getSelection(colWithNull, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(3)
        assertThat(filteredTable.getString(0, "colC")).isEqualTo("C1")
        assertThat(filteredTable.getString(1, "colC")).isEqualTo("C2")
        assertThat(filteredTable.getString(2, "colC")).isEqualTo("C3")

        val allCols = listOf("colA", "colB", "colC")
        selection = filter.getSelection(allCols, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(4)

        // First row does not exist for colA nor colB. No colC at all.
        val table2 = Table.create(
            StringColumn.create("colA", listOf("", "A2")),
            StringColumn.create("colB", listOf(null, "B2")),
        )
        selection = filter.getSelection(allCols, table2, rcvr)
        filteredTable = table2.where(selection)
        assertThat(filteredTable).hasRowCount(1)
        assertThat(filteredTable.getString(0, "colA")).isEqualTo("A2")
    }

    @Test
    fun `test AtLeastOneHasValue`() {
        val filter = AtLeastOneHasValue()
        val table = Table.create(
            StringColumn.create("colA", listOf("Y", "N", "", null)),
            StringColumn.create("colB", listOf("N", "N", "N", "Y")),
            StringColumn.create("colC", listOf("Y", "N", "", "N"))
        )

        val emptyArgs = listOf<String>()
        assertThat { filter.getSelection(emptyArgs, table, rcvr) }.isFailure()

        val junkColNames = listOf("Y", "foo", "bar", "baz")
        var selection = filter.getSelection(junkColNames, table, rcvr)
        var filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(0)

        val oneGoodColName = listOf("Y", "bar", "colB")
        selection = filter.getSelection(oneGoodColName, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(1)

        val colWithEmptyString = listOf("Y", "colC")
        selection = filter.getSelection(colWithEmptyString, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(1)

        val colWithNull = listOf("N", "colA")
        selection = filter.getSelection(colWithNull, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(1)

        val allCols = listOf("Y", "colA", "colB", "colC")
        selection = filter.getSelection(allCols, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(2)

        // First row does not exist for colA nor colB. No colC at all.
        val table2 = Table.create(
            StringColumn.create("colA", listOf("", "N")),
            StringColumn.create("colB", listOf(null, "Y")),
        )
        selection = filter.getSelection(allCols, table2, rcvr)
        filteredTable = table2.where(selection)
        assertThat(filteredTable).hasRowCount(1)
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
        assertThat(filteredTable).hasRowCount(4)
    }

    @Test
    fun `test allowNone`() {
        val filter = AllowNone()
        val table = Table.create(
            StringColumn.create("colA", listOf("A1", "A2", "", "A4")),
            StringColumn.create("colB", listOf("B1", "B2", "B3", "B4")),
            StringColumn.create("colC", listOf("C1", "C2", "C3", null))
        )

        // AllowNone takes no args, so passing args is an exception
        val colName = listOf("colA", "colB")
        assertThat { filter.getSelection(colName, table, rcvr) }.isFailure()

        val emptyArgs = listOf<String>()
        val selection = filter.getSelection(emptyArgs, table, rcvr)
        val filteredTable = table.where(selection)
        // And Then There Were None
        assertThat(filteredTable).hasRowCount(0)

        val emptyChairsAndEmptyTables = Table.create(
            StringColumn.create("colA"),
            StringColumn.create("colB"),
            StringColumn.create("colC")
        )
        val selection2 = filter.getSelection(emptyArgs, emptyChairsAndEmptyTables, rcvr)
        val filteredTable2 = table.where(selection2)
        // Nothing will come of nothing
        assertThat(filteredTable2).hasRowCount(0)
    }

    @Test
    fun `test IsValidCLIA`() {
        val filter = IsValidCLIA()
        val table = Table.create(
            StringColumn.create("colA", listOf("12D4567890", "12d4567890", "", "1A2B3C4D5E")),
            StringColumn.create("colB", listOf("12D4567890", "12d4567890", "1a2b3c4d5e", "1A2B3C4D5E")),
            StringColumn.create("colC", listOf("12D4567890", "12d4567890", "1a2b3c4d5e", null))
        )

        val emptyArgs = listOf<String>()
        assertThat { filter.getSelection(emptyArgs, table, rcvr) }.isFailure()

        val junkColNames = listOf("foo", "bar", "baz")
        var selection = filter.getSelection(junkColNames, table, rcvr)
        var filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(0)

        val oneGoodColName = listOf("foo", "bar", "colB")
        selection = filter.getSelection(oneGoodColName, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(4)

        val colWithEmptyString = listOf("foo", "colA")
        selection = filter.getSelection(colWithEmptyString, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(3)
        assertThat(filteredTable.getString(0, "colA")).isEqualTo("12D4567890")
        assertThat(filteredTable.getString(1, "colA")).isEqualTo("12d4567890")
        assertThat(filteredTable.getString(2, "colA")).isEqualTo("1A2B3C4D5E")

        val colWithNull = listOf("colC")
        selection = filter.getSelection(colWithNull, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(3)
        assertThat(filteredTable.getString(0, "colC")).isEqualTo("12D4567890")
        assertThat(filteredTable.getString(1, "colC")).isEqualTo("12d4567890")
        assertThat(filteredTable.getString(2, "colC")).isEqualTo("1a2b3c4d5e")

        val allCols = listOf("colA", "colB", "colC")
        selection = filter.getSelection(allCols, table, rcvr)
        filteredTable = table.where(selection)
        assertThat(filteredTable).hasRowCount(4)

        // First row and third rows are bad data.  No colC at all.
        val table2 = Table.create(
            StringColumn.create("colA", listOf("", "12D4567890", "abc")),
            StringColumn.create("colB", listOf(null, "12D4567890", "spaces bad")),
        )
        selection = filter.getSelection(allCols, table2, rcvr)
        filteredTable = table2.where(selection)
        assertThat(filteredTable).hasRowCount(1)
        assertThat(filteredTable.getString(0, "colA")).isEqualTo("12D4567890")
    }

    @Test
    fun `Test InDateInterval Filter`() {
        // Setup test data
        val lowerDateTime = "202107030000-0700"
        val middleDateTime = "202108030000-0500"
        val upperDateTime = "202109030000-0900"

        val filter = InDateInterval()
        val table = Table.create(
            StringColumn.create("colA", listOf(lowerDateTime, middleDateTime, upperDateTime, "")),
        )

        // Test bad args
        val emptyArgs = listOf<String>()
        assertThat { filter.getSelection(emptyArgs, table, rcvr) }.isFailure()
        val emptyColArgs = listOf("", "", "")
        assertThat { filter.getSelection(emptyColArgs, table, rcvr) }.isFailure()
        val badPeriodArgs = listOf("colA", "now", "20D")
        assertThat { filter.getSelection(badPeriodArgs, table, rcvr) }.isFailure()

        // Test with 1 Month
        val oneMonth = filter.getSelection(
            listOf("colA", middleDateTime, "P1M"),
            table,
            rcvr
        )
        assertThat(oneMonth.toArray()).containsExactly(1)

        // Test with 1 Month and 1 Day
        val oneMonthOneDay = filter.getSelection(
            listOf("colA", middleDateTime, "P1M1D"),
            table,
            rcvr
        )
        assertThat(oneMonthOneDay.toArray()).containsExactly(1, 2)

        // Test with a negative month
        val negativePeriod = filter.getSelection(
            listOf("colA", middleDateTime, "-P1M"),
            table,
            rcvr
        )
        assertThat(negativePeriod.toArray()).containsExactly(0)

        // Test with now and a small offset
        val nowSmall = filter.getSelection(
            listOf("colA", "now", "-P1M"),
            table,
            rcvr
        )
        assertThat(nowSmall.isEmpty).isTrue()

        // Test with now and a very large offset which should cover all dates
        val nowLarge = filter.getSelection(
            listOf("colA", "now", "-P10000Y"),
            table,
            rcvr
        )
        assertThat(nowLarge.toArray()).containsExactly(0, 1, 2)
    }

    @Test
    fun `Test InDateInterval Filter on Date values`() {
        // Setup test data
        val lowerDate = "20210703"
        val middleDate = "20210803"
        val upperDate = "20210903"

        val filter = InDateInterval()
        val table = Table.create(
            StringColumn.create("colA", listOf(lowerDate, middleDate, upperDate, "")),
        )

        // Test with 1 Month
        val oneMonth = filter.getSelection(
            listOf("colA", middleDate, "P1M"),
            table,
            rcvr
        )
        assertThat(oneMonth.toArray()).containsExactly(1)

        // Test with 1 Month and 1 Day
        val oneMonthOneDay = filter.getSelection(
            listOf("colA", middleDate, "P1M1D"),
            table,
            rcvr
        )
        assertThat(oneMonthOneDay.toArray()).containsExactly(1, 2)

        // Test with a negative month
        val negativePeriod = filter.getSelection(
            listOf("colA", middleDate, "-P1M"),
            table,
            rcvr
        )
        assertThat(negativePeriod.toArray()).containsExactly(0)

        // Test with now and a small offset
        val nowSmall = filter.getSelection(
            listOf("colA", "now", "-P1M"),
            table,
            rcvr
        )
        assertThat(nowSmall.isEmpty).isTrue()

        // Test with now and a very large offset which should cover all dates
        val nowLarge = filter.getSelection(
            listOf("colA", "now", "-P10000Y"),
            table,
            rcvr
        )
        assertThat(nowLarge.toArray()).containsExactly(0, 1, 2)
    }

    @Test
    fun `Test InDateInterval Filter on invalid date values`() {
        // Setup random data
        val lowerDate = "20210703"
        val middleDate = "20210803"
        val upperDate = "20210903"
        val lowerBadDate = "2333-9090-90-90"
        val middleBadDate = "asdfadfsasfd"
        val upperBadDate = "2234390-0-90-0"

        val filter = InDateInterval()
        val table = Table.create(
            StringColumn.create("colA", listOf(lowerDate, middleDate, upperDate, "")),
            StringColumn.create("colBad", listOf(lowerBadDate, middleBadDate, upperBadDate, "")),
        )

        // Test with 1 Month and good data
        val oneMonth = filter.getSelection(
            listOf("colA", middleDate, "P1M"),
            table,
            rcvr
        )
        assertThat(oneMonth.toArray()).containsExactly(1)

        // Test with 1 Month and bad data
        val oneBadMonth = filter.getSelection(
            listOf("colBad", middleDate, "P1M"),
            table,
            rcvr
        )
        assertThat(oneBadMonth.toArray()).isEmpty()

        // Test with bad argument and bad data
        assertThat {
            filter.getSelection(
                listOf("colBad", middleBadDate, "P1M"),
                table,
                rcvr
            )
        }.isFailure()

        // Test with bad data and negative argument
        val nowLarge = filter.getSelection(
            listOf("colBad", "now", "-P10000Y"),
            table,
            rcvr
        )
        assertThat(nowLarge.isEmpty).isTrue()
    }

    companion object {
        private fun Assert<Table>.hasRowCount(expected: Int) = given { actual ->
            if (actual.rowCount() == expected) return
            expected("rowCount:${show(expected)} but was rowCount:${show(actual.rowCount())}")
        }
    }
}