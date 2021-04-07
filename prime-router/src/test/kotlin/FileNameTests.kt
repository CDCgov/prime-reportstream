package gov.cdc.prime.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals

class FileNameTests {
    private val literal = """
        ---
        elements:
          - literal(cdcprime)
    """.trimIndent()
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    @Test
    fun `test literal name element`() {
        val literalElement = Literal()
        val expected = "cdcprime"
        val actual = literalElement.getElementValue(listOf(expected))
        assertEquals(expected, actual)
    }

    @Test
    fun `test reading literal name element from yaml`() {
        val inputStream = literal.byteInputStream()
        val fileName = mapper.readValue<FileName>(inputStream)
        val actual = fileName.getFileName()
        assertEquals("cdcprime", actual)
    }
}