package gov.cdc.prime.router

import kotlin.test.*

class MetadataTests {
    @Test
    fun `test loading actual metadata catalog`() {
        val metadata = Metadata("./metadata")
        assertNotNull(metadata)
    }

    @Test
    fun `test loading two schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a"), name = "one", topic = "test"),
            Schema(Element("a"), Element("b"), name = "two", topic = "test")
        )
        assertNotNull(metadata.findSchema("one"))
    }

    @Test
    fun `test loading basedOn schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), name = "one", topic = "test",),
            Schema(Element("a"), Element("b"), name = "two", topic = "test", basedOn = "one")
        )
        val two = metadata.findSchema("two")
        assertEquals("foo", two?.findElement("a")?.default)
    }

    @Test
    fun `test loading extends schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = "test",),
            Schema(Element("a"), name = "two", topic = "test", extends = "one")
        )
        val two = metadata.findSchema("two")
        assertEquals("foo", two?.findElement("a")?.default)
        assertNotNull(two?.findElement("b"))
    }

    @Test
    fun `test loading multi-level schemas`() {
        val metadata = Metadata().loadSchemas(
            Schema(Element("a", default = "foo"), Element("b"), name = "one", topic = "test",),
            Schema(Element("a"), Element("c"), name = "two", topic = "test", basedOn = "one"),
            Schema(Element("a"), Element("d"), name = "three", topic = "test", extends = "two")
        )
        val three = metadata.findSchema("three")
        assertEquals("foo", three?.findElement("a")?.default)
        assertNull(three?.findElement("b"))
        assertNotNull(three?.findElement("c"))
        assertNotNull(three?.findElement("d"))
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
    fun `test duplicate service name`() {
        val metadata = Metadata()
        val org1 = Organization(
            "test", "test",
            services = listOf(
                OrganizationService("service1", "topic1", "schema1"),
                OrganizationService("service1", "topic1", "schema1")
            )
        )
        assertFails { metadata.loadOrganizationList(listOf(org1)) }
    }
}