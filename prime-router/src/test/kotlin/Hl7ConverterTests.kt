package gov.cdc.prime.router

import ca.uhn.hl7v2.DefaultHapiContext
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Hl7ConverterTests {
    private val testTable: MappableTable
    private val context = DefaultHapiContext()
    private val modelClassFactory = context.modelClassFactory
    private val pipeParser = context.pipeParser

    init {
        Metadata.loadSchemaCatalog("./src/test/unit_test_files")
        Metadata.loadValueSetCatalog("./src/test/unit_test_files")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        val schema = Metadata.findSchema("pdi-covid-19") ?: error("Cannot find pdi-covid-19")
        testTable = CsvConverter.read("test", schema, inputStream)
    }

    @Test
    fun `Test write batch`() {
        val outputStream = ByteArrayOutputStream()
        Hl7Converter.write(testTable, outputStream)
        val output = outputStream.toString(StandardCharsets.UTF_8)
        assertNotNull(output)
    }

    @Test
    fun `test write a message`() {
        val output = Hl7Converter.createMessage(testTable, 0)
        assertNotNull(output)
    }
}