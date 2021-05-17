package gov.cdc.prime.router

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameAs
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.isSameAs
import assertk.assertions.prop
import assertk.assertions.support.appendName
import assertk.assertions.support.expected
import assertk.assertions.support.show
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetadataTests {
    @Test
    fun `test loading metadata catalog`() {
        val metadata = Metadata("./metadata")
        assertThat(metadata).isNotNull()
    }

    @Test
    fun `test loading two schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a"), name = "one", topic = "test"),
            Schema(Element("a"), Element("b"), name = "two", topic = "test")
        )
        assertThat(metadata.findSchema("one")).isNotNull()
    }

    @Test
    fun `test loading basedOn schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), name = "one", topic = "test"),
            Schema(Element("a"), Element("b"), name = "two", topic = "test", basedOn = "one")
        )
        val two = metadata.findSchema("two")
        assertThat(two)
            .isNotNull()
            .hasElement("a")
            .hasDefaultEqualTo("foo")
    }

    @Test
    fun `test loading extends schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = "test"),
            Schema(Element("a"), name = "two", topic = "test", extends = "one")
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
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = "test"),
            Schema(Element("a"), Element("c"), name = "two", topic = "test", basedOn = "one"),
            Schema(Element("a"), Element("d"), name = "three", topic = "test", extends = "two")
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
        val metadata = Metadata().loadValueSets(
            ValueSet("one", ValueSet.SetSystem.HL7),
            ValueSet("two", ValueSet.SetSystem.LOCAL)
        )
        assertThat(metadata.findValueSet("one")).isNotNull()
    }

    @Test
    fun `load value set directory`() {
        val metadata = Metadata().loadValueSetCatalog("./metadata/valuesets")
        assertThat(metadata.findValueSet("hl70136")).isNotNull()
    }

    @Test
    fun `test find schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(name = "One", topic = "test", elements = listOf(Element("a"))),
            Schema(name = "Two", topic = "test", elements = listOf(Element("a"), Element("b")))
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
        val baseSchema = Schema(name = "base_schema", topic = "test", elements = listOf(elementA))
        val childSchema = Schema(
            name = "child_schema",
            extends = "base_schema",
            topic = "test",
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
            topic = "test",
            elements = listOf(
                Element("a", csvFields = listOf(Element.CsvField("yes/no", format = null)))
            )
        )
        val twinSchema = Schema(
            name = "twin_schema",
            basedOn = "base_schema",
            topic = "test",
            elements = listOf(
                Element("a", csvFields = listOf(Element.CsvField("yes/no", format = null)))
            )
        )

        // act
        val metadata = Metadata()
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
        assertNotNull(sibling)
        val siblingElement = sibling.findElement(elementName)
        assertThat(siblingElement)
            .isNotNull()
            .csvFieldsHasSize(1)
        assertNull(siblingElement!!.csvFields?.first()?.format)
        // twin uses basedOn instead of extends
        val twin = metadata.findSchema("twin_schema")
        assertNotNull(twin)
        assertThat(twin.findElement(elementName)).all {
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

        assertNotNull(shouldBeSame.values.find { it.code.equals("UNK", ignoreCase = true) })
        assertNotNull(shouldBeDifferent.values.find { it.code.equals("U", ignoreCase = true) })
        assertNotNull(shouldBeDifferent.values.find { it.replaces.equals("UNK", ignoreCase = true) })
        assertNull(shouldBeDifferent.values.find { it.code.equals("UNK", ignoreCase = true) })

        assertNotNull(shouldBeExtended.values.find { it.code.equals("M", ignoreCase = true) })
        assertNotNull(shouldBeExtended.values.find { it.code.equals("UNK", ignoreCase = true) })
    }

    companion object {
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
}