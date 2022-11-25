package gov.cdc.prime.router

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameAs
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.isSameAs
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.support.appendName
import assertk.assertions.support.expected
import assertk.assertions.support.show
import gov.cdc.prime.router.azure.DatabaseLookupTableAccess
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableRow
import gov.cdc.prime.router.azure.db.tables.pojos.LookupTableVersion
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import org.jooq.JSONB
import org.jooq.exception.DataAccessException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MetadataTests {
    @Test
    fun `test findReportStreamFilterDefinitions`() {
        val metadata = UnitTestUtils.simpleMetadata
        assertThat(metadata.findReportStreamFilterDefinitions("matches")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("doesNotMatch")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("filterByCounty")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("orEquals")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("allowAll")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("allowNone")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("hasValidDataFor")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("isValidCLIA")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("hasAtLeastOneOf")).isNotNull()
        assertThat(metadata.findReportStreamFilterDefinitions("atLeastOneHasValue")).isNotNull()
    }

    @Test
    fun `test loading two schemas`() {
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(
            Schema(Element("a"), name = "one", topic = Topic.TEST),
            Schema(Element("a"), Element("b"), name = "two", topic = Topic.TEST)
        )
        assertThat(metadata.findSchema("one")).isNotNull()
    }

    @Test
    fun `test loading basedOn schemas`() {
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(
            Schema(Element("a", default = "foo"), name = "one", topic = Topic.TEST),
            Schema(Element("a"), Element("b"), name = "two", topic = Topic.TEST, basedOn = "one")
        )
        val two = metadata.findSchema("two")
        assertThat(two)
            .isNotNull()
            .hasElement("a")
            .hasDefaultEqualTo("foo")
    }

    @Test
    fun `test loading extends schemas`() {
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = Topic.TEST),
            Schema(Element("a"), name = "two", topic = Topic.TEST, extends = "one")
        )
        val two = metadata.findSchema("two")
        assertThat(two)
            .isNotNull()
            .hasElement("b")
        assertThat(two)
            .isNotNull()
            .hasElement("a")
            .hasDefaultEqualTo("foo")
    }

    @Test
    fun `test loading multi-level schemas`() {
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = Topic.TEST),
            Schema(Element("a"), Element("c"), name = "two", topic = Topic.TEST, basedOn = "one"),
            Schema(Element("a"), Element("d"), name = "three", topic = Topic.TEST, extends = "two")
        )
        val three = metadata.findSchema("three")
        assertThat(three?.findElement("b")).isNull()
        assertThat(three).isNotNull().hasElement("c")
        assertThat(three).isNotNull().hasElement("d")
        assertThat(three)
            .isNotNull()
            .hasElement("a")
            .hasDefaultEqualTo("foo")
    }

    @Test
    fun `load valueSets`() {
        val metadata = UnitTestUtils.simpleMetadata.loadValueSets(
            ValueSet("one", ValueSet.SetSystem.HL7),
            ValueSet("two", ValueSet.SetSystem.LOCAL)
        )
        assertThat(metadata.findValueSet("one")).isNotNull()
    }

    @Test
    fun `load value set directory`() {
        val metadata = UnitTestUtils.simpleMetadata.loadValueSetCatalog("./metadata/valuesets")
        assertThat(metadata.findValueSet("hl70136")).isNotNull()
    }

    @Test
    fun `test find schemas`() {
        val metadata = UnitTestUtils.simpleMetadata.loadSchemas(
            Schema(name = "One", topic = Topic.TEST, elements = listOf(Element("a"))),
            Schema(name = "Two", topic = Topic.TEST, elements = listOf(Element("a"), Element("b")))
        )
        assertThat(metadata.findSchema("one")).isNotNull()
    }

    @Test
    fun `test schema contamination`() {
        // arrange
        val valueSetA = ValueSet(
            "a_values",
            ValueSet.SetSystem.LOCAL,
            values = listOf(ValueSet.Value("Y", "Yes"), ValueSet.Value("N", "No"))
        )
        val elementA = Element("a", Element.Type.CODE, valueSet = "a_values", valueSetRef = valueSetA)
        val baseSchema = Schema(name = "base_schema", topic = Topic.TEST, elements = listOf(elementA))
        val childSchema = Schema(
            name = "child_schema",
            extends = "base_schema",
            topic = Topic.TEST,
            elements = listOf(
                Element(
                    "a",
                    altValues = listOf(
                        ValueSet.Value("J", "Ja"),
                        ValueSet.Value("N", "Nein")
                    ),
                    csvFields = listOf(Element.CsvField("Ja Oder Nein", format = "\$code"))
                )
            )
        )
        val siblingSchema = Schema(
            name = "sibling_schema",
            extends = "base_schema",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = listOf(Element.CsvField("yes/no", format = null)))
            )
        )
        val twinSchema = Schema(
            name = "twin_schema",
            basedOn = "base_schema",
            topic = Topic.TEST,
            elements = listOf(
                Element("a", csvFields = listOf(Element.CsvField("yes/no", format = null)))
            )
        )

        // act
        val metadata = UnitTestUtils.simpleMetadata
        metadata.loadValueSets(valueSetA)
        metadata.loadSchemas(
            baseSchema,
            childSchema,
            siblingSchema,
            twinSchema
        )

        // assert
        val elementName = "a"
        val parent = metadata.findSchema("base_schema")
        assertThat(parent).isNotNull()
            .hasElement(elementName)
            .prop("csvFields") { Element::csvFields.call(it) }
            .isNullOrEmpty()
        // the first child element
        val child = metadata.findSchema("child_schema")
        assertThat(child).isNotNull()
        val childElement = child!!.findElement(elementName)
        assertThat(childElement).isNotNull()
        assertEquals("\$code", childElement!!.csvFields?.first()?.format)
        // sibling uses extends
        val sibling = metadata.findSchema("sibling_schema")
        assertThat(sibling)
            .isNotNull()
            .hasElement(elementName)
            .isNotNull()
            .csvFieldsHasSize(1)
        val siblingElement = sibling!!.findElement(elementName)
        assertNull(siblingElement!!.csvFields?.first()?.format)
        // twin uses basedOn instead of extends
        val twin = metadata.findSchema("twin_schema")
        assertThat(twin).isNotNull()
        assertThat(twin!!.findElement(elementName)).all {
            isNotNull()
            prop("csvFields") { Element::csvFields.call(it) }.hasSize(1)
            prop("csvFields") { Element::csvFields.call(it) }.given {
                if (it?.first()?.format.isNullOrEmpty()) return@given
                expected("format expected to be null but was ${show(it?.first()?.format)}")
            }
        }
    }

    @Test
    fun `test valueset merging`() {
        // arrange
        val valueSet = ValueSet(
            "a", ValueSet.SetSystem.LOCAL,
            values = listOf(
                ValueSet.Value("Y", "Yes"),
                ValueSet.Value("N", "No"),
                ValueSet.Value("UNK", "Unknown"),
            )
        )

        val emptyAltValues = listOf<ValueSet.Value>()
        val replacementValues = listOf(
            ValueSet.Value("U", "Unknown", replaces = "UNK")
        )
        val additionalValues = listOf(
            ValueSet.Value("M", "Maybe")
        )
        // act
        val shouldBeSame = valueSet.mergeAltValues(emptyAltValues)
        val shouldBeDifferent = valueSet.mergeAltValues(replacementValues)
        val shouldBeExtended = valueSet.mergeAltValues(additionalValues)

        // assert
        assertThat(valueSet).isSameAs(shouldBeSame)
        assertThat(valueSet).isNotSameAs(shouldBeDifferent)

        assertThat(shouldBeSame.values.find { it.code.equals("UNK", ignoreCase = true) }).isNotNull()
        assertThat(shouldBeDifferent.values.find { it.code.equals("U", ignoreCase = true) }).isNotNull()
        assertThat(shouldBeDifferent.values.find { it.replaces.equals("UNK", ignoreCase = true) }).isNotNull()
        assertThat(shouldBeDifferent.values.find { it.code.equals("UNK", ignoreCase = true) }).isNull()

        assertThat(shouldBeExtended.values.find { it.code.equals("M", ignoreCase = true) }).isNotNull()
        assertThat(shouldBeExtended.values.find { it.code.equals("UNK", ignoreCase = true) }).isNotNull()
    }

    companion object {
        // below are a set of extension functions that assertK can use to run assertions on our code.
        // these come in two flavors:
        // - given, which just checks a value and returns Unit, meaning you cannot chain assertions
        // - transform, which not only checks your assertion, but returns a value wrapped in Assertion<> so you
        //   you can chain assertions.
        private fun Assert<List<*>?>.hasSize(expected: Int) = given { actual ->
            if (actual?.size == expected) return
            expected("size:${show(expected)} but was ${show(actual?.size?.toString() ?: "null")}")
        }

        private fun Assert<Schema>.hasElement(expected: String): Assert<Element?> = transform(
            appendName("elementName")
        ) { actual ->
            if (actual.findElement(expected) != null) {
                actual.findElement(expected)
            } else {
                expected("element named ${show(expected)} to exist")
            }
        }

        private fun Assert<Element?>.hasDefaultEqualTo(expected: String) = given { actual ->
            if (actual?.default == expected) return@given
            expected("default:${show(expected)} but was ${show(actual?.default ?: "null")}")
        }

        private fun Assert<Element?>.csvFieldsHasSize(expected: Int) = given { actual ->
            if (actual?.csvFields?.size == expected) return@given
            expected("csvFields size:${show(expected)} but was ${show(actual?.csvFields?.size ?: "null")}")
        }
    }

    @Test
    fun `check for database lookup table updates test`() {
        val mockDbTableAccess = mockk<DatabaseLookupTableAccess>()
        val now = Instant.now()

        // Initialization
        every { mockDbTableAccess.fetchTableList(any()) } returns emptyList()
        val metadata = Metadata(UnitTestUtils.simpleSchema, tableDbAccess = mockDbTableAccess)

        metadata.tablelastCheckedAt = now.plusSeconds(3600)
        metadata.checkForDatabaseLookupTableUpdates()
        assertThat(metadata.tablelastCheckedAt).isEqualTo(now.plusSeconds(3600))

        metadata.tablelastCheckedAt = now.minusSeconds(3600)
        every { mockDbTableAccess.fetchTableList() } returns emptyList()
        metadata.checkForDatabaseLookupTableUpdates()
        assertThat(metadata.tablelastCheckedAt).isNotEqualTo(now.minusSeconds(3600))
    }

    @Test
    fun `load database lookup table updates test`() {
        val mockDbTableAccess = mockk<DatabaseLookupTableAccess>()
        val table1 = LookupTableVersion()
        table1.tableName = "table1"
        table1.tableVersion = 1
        table1.isActive = true
        val table2 = LookupTableVersion()
        table2.tableName = "table2"
        table2.tableVersion = 3
        table2.isActive = true
        val tableData = listOf(LookupTableRow())
        tableData[0].data = JSONB.jsonb("""{"colA": "valueA", "colb": "valueB"}""")

        // Initialization
        every { mockDbTableAccess.fetchTableList(any()) } returns emptyList()
        val metadata = Metadata(UnitTestUtils.simpleSchema, tableDbAccess = mockDbTableAccess)

        // Database exception
        every { mockDbTableAccess.fetchTableList() } throws DataAccessException("error")
        metadata.loadDatabaseLookupTables() // No error or other calls done

        // Any inactive table in the list from the API returns an exception
        val inactiveTable = LookupTableVersion()
        inactiveTable.tableName = "some table"
        inactiveTable.isActive = false
        every { mockDbTableAccess.fetchTableList() } returns listOf(inactiveTable)
        assertFailsWith<IllegalStateException>(
            block = {
                metadata.loadDatabaseLookupTables()
            }
        )

        // No tables, nothing to do
        metadata.lookupTableStore = emptyMap()
        every { mockDbTableAccess.fetchTableList() } returns emptyList()
        metadata.loadDatabaseLookupTables()

        // Test good tables, no conflicting file tables
        metadata.lookupTableStore = emptyMap()
        every { mockDbTableAccess.fetchTableList() } returns listOf(table1, table2)
        every { mockDbTableAccess.fetchTable(any(), any()) } returns tableData
        metadata.loadDatabaseLookupTables()
        assertThat(metadata.lookupTableStore.size).isEqualTo(2)
        assertThat(metadata.lookupTableStore.containsKey(table1.tableName))
        assertThat(metadata.lookupTableStore.containsKey(table2.tableName))
        assertThat(metadata.lookupTableStore[table1.tableName]!!.isSourceDatabase).isTrue()
        assertThat(metadata.lookupTableStore[table2.tableName]!!.isSourceDatabase).isTrue()
        assertThat(metadata.lookupTableStore[table1.tableName]!!.rowCount).isEqualTo(1)

        // Test two good tables with one conflicting file table
        metadata.lookupTableStore = emptyMap()
        metadata.lookupTableStore = mapOf(table2.tableName to LookupTable(table = emptyList()))
        every { mockDbTableAccess.fetchTableList() } returns listOf(table1, table2)
        every { mockDbTableAccess.fetchTable(any(), any()) } returns tableData
        metadata.loadDatabaseLookupTables()
        assertThat(metadata.lookupTableStore.size).isEqualTo(2)
        assertThat(metadata.lookupTableStore.containsKey(table1.tableName))
        assertThat(metadata.lookupTableStore.containsKey(table2.tableName))
        assertThat(metadata.lookupTableStore[table1.tableName]!!.isSourceDatabase).isTrue()
        assertThat(!metadata.lookupTableStore[table2.tableName]!!.isSourceDatabase).isTrue()

        // Add a new table - Note this uses the results from the test above.
        val table3 = LookupTableVersion()
        table3.tableName = "table3"
        table3.tableVersion = 1
        table3.isActive = true
        every { mockDbTableAccess.fetchTableList() } returns listOf(table1, table2, table3)
        every { mockDbTableAccess.fetchTable(any(), any()) } returns tableData
        metadata.loadDatabaseLookupTables()
        assertThat(metadata.lookupTableStore.size).isEqualTo(3)
        assertThat(metadata.lookupTableStore.containsKey(table1.tableName))
        assertThat(metadata.lookupTableStore.containsKey(table2.tableName))
        assertThat(metadata.lookupTableStore.containsKey(table3.tableName))
        assertThat(metadata.lookupTableStore[table3.tableName]!!.isSourceDatabase).isTrue()

        // Now a table was deleted or deactivated - Note this uses the results from the test above.
        every { mockDbTableAccess.fetchTableList() } returns listOf(table1, table2)
        metadata.loadDatabaseLookupTables()
        assertThat(metadata.lookupTableStore.size).isEqualTo(3)
        assertThat(metadata.lookupTableStore.containsKey(table3.tableName))
        assertThat(metadata.lookupTableStore[table3.tableName]!!.rowCount).isEqualTo(0)
    }

    @Test
    fun `test schema validation`() {
        var schema = Schema("name", Topic.TEST, listOf(Element("a", type = Element.Type.TEXT)))
        assertThat { Metadata(schema).validateSchemas() }.isSuccess()

        schema = Schema("name", Topic.TEST, listOf(Element("a")))
        assertThat { Metadata(schema).validateSchemas() }.isFailure()

        schema = Schema(
            "name", Topic.TEST,
            listOf(
                Element("a", type = Element.Type.TEXT),
                Element("name")
            )
        )
        assertThat { Metadata(schema).validateSchemas() }.isFailure()
    }
}