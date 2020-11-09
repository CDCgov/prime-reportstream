package gov.cdc.prime.router

import java.io.ByteArrayInputStream
import kotlin.test.*

class ReceiverTests {
    private val receiversYaml = """
            ---
            receivers:
              # Arizona PHD
              - name: phd1
                description: Arizona PHD
                topic: test
                schema: one
                patterns: {a: 1}
                transforms: {deidentify: false}
                address: phd1
                format: CSV
        """.trimIndent()

    @Test
    fun `test loading a receiver`() {
        val input = ByteArrayInputStream(receiversYaml.toByteArray())
        Metadata.loadReceiversList(input)
        assertEquals(2, Metadata.findReceiver("phd1", "test")?.patterns?.size)
    }

    @Test
    fun `test loading a single receiver`() {
        Metadata.loadReceivers(listOf(Receiver("name", "topic", "schema")))
        assertNotNull(Metadata.findReceiver("name", "topic"))
    }

    @Test
    fun `test filterAndMapByReceiver`() {
        val input = ByteArrayInputStream(receiversYaml.toByteArray())
        Metadata.loadReceiversList(input)
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val table1 = MappableTable("example", one, listOf(listOf("1", "2"), listOf("3", "4")))
        val result = Receiver.filterAndMapByReceiver(table1, Metadata.receivers)
        assertEquals(1, result.size)
        val (mappedTable, forReceiver) = result[0]
        assertEquals(table1.schema, mappedTable.schema)
        assertEquals(1, mappedTable.rowCount)
        assertEquals(Metadata.receivers[0], forReceiver)
    }
}