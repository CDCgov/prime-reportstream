package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isFailure
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class ReportWriterTests {
    private val csvSerializer: CsvSerializer = BaseEngine.csvSerializerSingleton
    private val hl7Serializer: Hl7Serializer = BaseEngine.hl7SerializerSingleton
    val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
    val metadata = Metadata(schema = one)
    private val comparison = ByteArrayOutputStream()

    /**
     * Compare and assert the output of most test cases in this file
     * Stringify the input data, then split and assert a comparison on a specific segment
     *
     * @param bodyBytes Output of ReportWriter::getBodyBytes
     * @param comparison Output of a manually converted file
     * @param index Index for the segment to compare. Play with this value to avoid timestamp conflicts!
     */
    private fun assertByteArrayMatch(bodyBytes: ByteArray, comparison: ByteArrayOutputStream, index: Int = 0) {
        val bbString = String(bodyBytes).split('\r')
        val comp = String(comparison.toByteArray()).split('\r')
        assert(bbString[index] == comp[index])
    }

    @Test
    fun `test getBodyBytes INTERNAL format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.INTERNAL,
            metadata = metadata
        )
        val bodyBytes = ReportWriter.getBodyBytes(report)
        csvSerializer.writeInternal(report, comparison)
        assertByteArrayMatch(bodyBytes, comparison)
    }

    @Test
    fun `test getBodyBytes HL7 format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.HL7,
            metadata = metadata
        )
        val bodyBytes = ReportWriter.getBodyBytes(report)
        hl7Serializer.write(report, comparison)
        assertByteArrayMatch(bodyBytes, comparison, 1)
    }

    @Test
    fun `test getBodyBytes HL7_BATCH format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.HL7_BATCH,
            metadata = metadata
        )
        val bodyBytes = ReportWriter.getBodyBytes(report)
        hl7Serializer.writeBatch(report, comparison)
        assertByteArrayMatch(bodyBytes, comparison, 3)
    }

    @Test
    fun `test getBodyBytes CSV format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.CSV,
            metadata = metadata
        )
        val bodyBytes = ReportWriter.getBodyBytes(report)
        csvSerializer.write(report, comparison)
        assertByteArrayMatch(bodyBytes, comparison)
    }

    @Test
    fun `test getBodyBytes CSV_SINGLE format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.CSV_SINGLE,
            metadata = metadata
        )
        val bodyBytes = ReportWriter.getBodyBytes(report)
        csvSerializer.write(report, comparison)
        assertByteArrayMatch(bodyBytes, comparison)
    }

    @Test
    fun `test getBodyBytes unsupported format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.FHIR, // so this test freaks out if fhir gets support
            metadata = metadata
        )
        assertThat { ReportWriter.getBodyBytes(report) }.isFailure()
            .hasClass(UnsupportedOperationException::class)
    }
}