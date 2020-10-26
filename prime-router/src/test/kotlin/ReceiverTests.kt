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
                topic: covid-19
                schema: covid-19
                patterns: {observation: "covid-19:*", state: AZ}
                transforms: {deidentify: false}
                address: phd1
                format: CSV
        """.trimIndent()

    @Test
    fun `test loading a receiver`() {
        val input = ByteArrayInputStream(receiversYaml.toByteArray())
        Receiver.loadReceiversList(input)
        assertEquals(1, Receiver.receivers.size)
        assertEquals(2, Receiver.get("phd1", "covid-19")?.patterns?.size)
    }

    @Test
    fun `test loading a single receiver`() {
        Receiver.loadReceivers(listOf(Receiver("name", "topic", "schema")))
        assertEquals(1, Receiver.receivers.size)
    }
}