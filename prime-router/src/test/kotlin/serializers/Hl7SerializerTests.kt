package gov.cdc.prime.router.serializers

import ca.uhn.hl7v2.DefaultHapiContext
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.TestSource
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Hl7SerializerTests {
    private val testReport: Report
    private val context = DefaultHapiContext()
    private val serializer: Hl7Serializer
    private val csvSerializer: CsvSerializer

    init {
        val metadata = Metadata("./metadata")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        csvSerializer = CsvSerializer(metadata)
        serializer = Hl7Serializer(metadata)
        testReport = csvSerializer.readExternal("primedatainput/pdi-covid-19", inputStream, TestSource).report ?: fail()
    }

    @Test
    fun `Test write batch`() {
        val outputStream = ByteArrayOutputStream()
        serializer.writeBatch(testReport, outputStream)
        val output = outputStream.toString(StandardCharsets.UTF_8)
        assertNotNull(output)
    }

    @Test
    fun `test write a message`() {
        val output = serializer.createMessage(testReport, 0)
        assertNotNull(output)
    }
}