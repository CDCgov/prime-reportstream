package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MetadataTests {
    @Test
    fun `test loading schema catalog`() {
        Metadata.loadSchemaCatalog("./src/test/unit_test_files")
        assertNotNull(Metadata.findSchema("lab_test_results_schema"))
        val elements = Metadata.findSchema("lab_test_results_schema")?.elements
        assertEquals("lab", elements?.get(0)?.name)
        assertEquals("extra", elements?.get(6)?.name)
        assertEquals(Element.Type.POSTAL_CODE, elements?.get(8)?.type)
    }

    @Test
    fun `test loading two schemas`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        Metadata.loadSchemas(listOf(one, two))
        assertNotNull(Metadata.findSchema("one"))
    }

    @Test
    fun `load valueSets`() {
        val one = ValueSet("one", ValueSet.SetSystem.HL7)
        val two = ValueSet("two", ValueSet.SetSystem.LOCAL)
        Metadata.loadValueSets(listOf(one, two))
        assertNotNull(Metadata.findValueSet("one"))
    }

    @Test
    fun `load value set directory`() {
        Metadata.loadValueSetCatalog("./src/test/unit_test_files")
        assertNotNull(Metadata.findValueSet("hl70136"))
    }

    @Test
    fun `test find schemas`() {
        val one = Schema(name = "One", topic = "test", elements = listOf(Element("a")))
        val two = Schema(name = "Two", topic = "test", elements = listOf(Element("a"), Element("b")))
        Metadata.loadSchemas(listOf(one, two))
        assertNotNull(Metadata.findSchema("one"))
    }

    @Test
    fun `test find client`() {
        Metadata.loadOrganizationList("./src/test/unit_test_files/organizations.yml")
        val client = Metadata.findClient("simple_report")
        assertNotNull(client)
    }
}