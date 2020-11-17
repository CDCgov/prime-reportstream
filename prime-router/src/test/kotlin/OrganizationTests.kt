package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.*

class OrganizationTests {
    private val servicesYaml = """
            ---
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                services: 
                - name: elr
                  topic: test
                  schema: one
                  jurisdictionalFilter: {a: 1}
                  transforms: {deidentify: false}
                  address: phd1
                  format: CSV
        """.trimIndent()

    private val clientsAndServicesYaml = """
            ---
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                services: 
                  - name: elr
                    topic: test
                    schema: one
                    jurisdictionalFilter: {a: 1}
                    transforms: {deidentify: false}
                    address: phd1
                    format: CSV
                clients:
                  - name: sender
                    topic: topic
                    schema: one
                    formats: [CSV]
        """.trimIndent()

    @Test
    fun `test loading a service`() {
        val input = ByteArrayInputStream(servicesYaml.toByteArray())
        Metadata.loadOrganizationList(input)

        val result = Metadata.findService("phd1.elr")

        assertEquals(1, result?.jurisdictionalFilter?.size)
    }

    @Test
    fun `test loading a client and service`() {
        val input = ByteArrayInputStream(clientsAndServicesYaml.toByteArray())
        Metadata.loadOrganizationList(input)

        val result = Metadata.findClient("phd1.sender")

        assertEquals("sender", result?.name)
    }

    @Test
    fun `test loading a single organization`() {
        var orgs = listOf(
            Organization(
                name = "single",
                description = "blah blah",
                clients = listOf(),
                services = listOf(
                    OrganizationService("elr", "topic", "schema")
                )
            )
        )
        Metadata.loadOrganizations(orgs)

        val result = Metadata.findService("single.elr") ?: fail("Expected to find service")

        assertEquals("elr", result.name)
    }

    @Test
    fun `test filterAndMapByService`() {
        val input = ByteArrayInputStream(servicesYaml.toByteArray())
        Metadata.loadOrganizationList(input)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), TestSource)

        val result = OrganizationService.filterAndMapByService(table1, Metadata.organizationServices)

        assertEquals(1, result.size)
        val (mappedTable, forReceiver) = result[0]
        assertEquals(table1.schema, mappedTable.schema)
        assertEquals(1, mappedTable.rowCount)
        assertEquals(Metadata.organizationServices[0], forReceiver)
    }
}