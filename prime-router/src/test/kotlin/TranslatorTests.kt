package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class TranslatorTests {
    private val servicesYaml = """
        ---
          # Arizona PHD
          - name: phd1
            description: Arizona PHD
            services: 
            - name: elr
              topic: test
              schema: one
              jurisdictionalFilter: [ "matches(a, 1)"]
              transforms: {deidentify: false}
              address: phd1
              format: CSV
    """.trimIndent()

    @Test
    fun `test buildMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata().loadSchemas(one, two)
        val translator = Translator(metadata)

        val oneToTwo = translator.buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap())
        assertEquals(one, oneToTwo.fromSchema)
        assertEquals(two, oneToTwo.toSchema)
        assertEquals(1, oneToTwo.useDirectly.size)
        assertEquals("a", oneToTwo.useDirectly["a"])
        assertEquals(true, oneToTwo.useDefault.contains("b"))
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
        val translator = Translator(metadata)

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
            elements = listOf(Element("a"), Element("c", required = true))
        )
        val metadata = Metadata().loadSchemas(one, three)
        val translator = Translator(metadata)

        val oneToThree = translator.buildMapping(fromSchema = one, toSchema = three, defaultValues = emptyMap())
        assertEquals(1, oneToThree.useDirectly.size)
        assertEquals("a", oneToThree.useDirectly["a"])
        assertEquals(0, oneToThree.useDefault.size)
        assertEquals(1, oneToThree.missing.size)
    }

    @Test
    fun `test filterAndMapByService`() {
        val metadata = Metadata()
        metadata.loadOrganizations(ByteArrayInputStream(servicesYaml.toByteArray()))
        val translator = Translator(metadata)

        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), TestSource)

        val result = translator.filterAndTranslateByService(table1)

        assertEquals(1, result.size)
        val (mappedTable, forReceiver) = result[0]
        assertEquals(table1.schema, mappedTable.schema)
        assertEquals(1, mappedTable.itemCount)
        assertEquals(metadata.organizationServices[0], forReceiver)
    }
}