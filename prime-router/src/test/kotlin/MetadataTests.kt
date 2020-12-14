package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MetadataTests {
    @Test
    fun `test loading actual metadata catalog`() {
        val metadata = Metadata("./metadata")
        assertNotNull(metadata)
    }

    @Test
    fun `test loading two schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(name = "one", topic = "test", elements = listOf(Element("a"))),
            Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        )
        assertNotNull(metadata.findSchema("one"))
    }

    @Test
    fun `load valueSets`() {
        val metadata = Metadata().loadValueSets(
            ValueSet("one", ValueSet.SetSystem.HL7),
            ValueSet("two", ValueSet.SetSystem.LOCAL)
        )
        assertNotNull(metadata.findValueSet("one"))
    }

    @Test
    fun `load value set directory`() {
        val metadata = Metadata().loadValueSetCatalog("./metadata/valuesets")
        assertNotNull(metadata.findValueSet("hl70136"))
    }

    @Test
    fun `test find schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(name = "One", topic = "test", elements = listOf(Element("a"))),
            Schema(name = "Two", topic = "test", elements = listOf(Element("a"), Element("b")))
        )
        assertNotNull(metadata.findSchema("one"))
    }

    @Test
    fun `test find client`() {
        val metadata = Metadata()
        metadata.loadOrganizations("./metadata/organizations.yml")
        val client = metadata.findClient("simple_report")
        assertNotNull(client)
    }

    @Test
    fun `test buildMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata().loadSchemas(one, two)

        val oneToTwo = metadata.buildMapping(fromSchema = one, toSchema = two)
        assertEquals(one, oneToTwo.fromSchema)
        assertEquals(two, oneToTwo.toSchema)
        assertEquals(1, oneToTwo.useDirectly.size)
        assertEquals("a", oneToTwo.useDirectly["a"])
        assertEquals(true, oneToTwo.useDefault.contains("b"))
        assertEquals(0, oneToTwo.missing.size)

        val twoToOne = metadata.buildMapping(fromSchema = two, toSchema = one)
        assertEquals(1, twoToOne.useDirectly.size)
        assertEquals("a", twoToOne.useDirectly["a"])
        assertEquals(0, twoToOne.useDefault.size)
        assertEquals(0, twoToOne.missing.size)
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
        val oneToThree = metadata.buildMapping(fromSchema = one, toSchema = three)
        assertEquals(1, oneToThree.useDirectly.size)
        assertEquals("a", oneToThree.useDirectly["a"])
        assertEquals(0, oneToThree.useDefault.size)
        assertEquals(1, oneToThree.missing.size)
    }
}