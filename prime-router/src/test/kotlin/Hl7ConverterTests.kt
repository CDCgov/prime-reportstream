package gov.cdc.prime.router

import ca.uhn.hl7v2.DefaultHapiContext
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Hl7ConverterTests {
    private val testReport: Report
    private val context = DefaultHapiContext()

    init {
        Metadata.loadSchemaCatalog("./src/test/unit_test_files")
        Metadata.loadValueSetCatalog("./src/test/unit_test_files")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        val schema = Metadata.findSchema("pdi-covid-19") ?: error("Cannot find pdi-covid-19")
        testReport = CsvConverter.read(schema, inputStream, TestSource)
    }

    @Test
    fun `Test write batch`() {
        val outputStream = ByteArrayOutputStream()
        Hl7Converter.write(testReport, outputStream)
        val output = outputStream.toString(StandardCharsets.UTF_8)
        assertNotNull(output)
    }

    @Test
    fun `test write a message`() {
        val output = Hl7Converter.createMessage(testReport, 0)
        assertNotNull(output)
    }
}