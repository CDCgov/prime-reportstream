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
    private val converter: Hl7Converter
    private val csvConverter: CsvConverter

    init {
        val metadata = Metadata("./metadata")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        csvConverter = CsvConverter(metadata)
        converter = Hl7Converter(metadata)
        testReport = csvConverter.read("primedatainput/pdi-covid-19", inputStream, TestSource).report
    }

    @Test
    fun `Test write batch`() {
        val outputStream = ByteArrayOutputStream()
        converter.write(testReport, outputStream)
        val output = outputStream.toString(StandardCharsets.UTF_8)
        assertNotNull(output)
    }

    @Test
    fun `test write a message`() {
        val output = converter.createMessage(testReport, 0)
        assertNotNull(output)
    }
}