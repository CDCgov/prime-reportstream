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
}