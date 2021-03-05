package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class TranslatorTests {
    private val receiversYaml = """
        ---
          # Arizona PHD
          - name: phd1
            description: Arizona PHD
            jurisdiction: STATE
            stateCode: AZ
            receivers: 
            - name: elr
              organizationName: phd1
              topic: test
              jurisdictionalFilter: [ "matches(a, 1)"]
              translation: 
                type: CUSTOM
                schemaName: one
                format: CSV
    """.trimIndent()

    @Test
    fun `test buildMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata().loadSchemas(one, two)
        val translator = Translator(metadata, FileSettings())

        val oneToTwo = translator.buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap())
        assertEquals(one, oneToTwo.fromSchema)
        assertEquals(two, oneToTwo.toSchema)
        assertEquals(1, oneToTwo.useDirectly.size)
        assertEquals("a", oneToTwo.useDirectly["a"])
        assertEquals(false, oneToTwo.useDefault.contains("b"))
        assertEquals(0, oneToTwo.missing.size)

        val twoToOne = translator.buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap())
        assertEquals(1, twoToOne.useDirectly.size)
        assertEquals("a", twoToOne.useDirectly["a"])
        assertEquals(0, twoToOne.useDefault.size)
        assertEquals(0, twoToOne.missing.size)
    }

    @Test
    fun `test buildMapping with default`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b", default = "x")))
        val metadata = Metadata().loadSchemas(one, two)
        val translator = Translator(metadata, FileSettings())

        val oneToTwo = translator.buildMapping(fromSchema = one, toSchema = two, defaultValues = mapOf("b" to "foo"))
        assertEquals(true, oneToTwo.useDefault.contains("b"))
        assertEquals("foo", oneToTwo.useDefault["b"])
    }

    @Test
    fun `test buildMapping with missing`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val three = Schema(
            name = "three",
            topic = "test",
            elements = listOf(Element("a"), Element("c", cardinality = Element.Cardinality.ONE))
        )
        val metadata = Metadata().loadSchemas(one, three)
        val translator = Translator(metadata, FileSettings())

        val oneToThree = translator.buildMapping(fromSchema = one, toSchema = three, defaultValues = emptyMap())
        assertEquals(1, oneToThree.useDirectly.size)
        assertEquals("a", oneToThree.useDirectly["a"])
        assertEquals(0, oneToThree.useDefault.size)
        assertEquals(1, oneToThree.missing.size)
    }

    @Test
    fun `test filterAndMapByReceiver`() {
        val metadata = Metadata()
        val settings = FileSettings()
        settings.loadOrganizations(ByteArrayInputStream(receiversYaml.toByteArray()))
        val translator = Translator(metadata, settings)

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), TestSource)

        val result = translator.filterAndTranslateByReceiver(table1)

        assertEquals(1, result.size)
        val (mappedTable, forReceiver) = result[0]
        assertEquals(table1.schema, mappedTable.schema)
        assertEquals(1, mappedTable.itemCount)
        assertEquals(settings.receivers.toTypedArray()[0], forReceiver)
    }
}