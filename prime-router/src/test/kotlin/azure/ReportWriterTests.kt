package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isFailure
import assertk.doesNotThrowAnyException
import gov.cdc.prime.router.*
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException

class ReportWriterTests {
    private val csvSerializer: CsvSerializer = BaseEngine.csvSerializerSingleton
    private val hl7Serializer: Hl7Serializer = BaseEngine.hl7SerializerSingleton
    val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
    val metadata = Metadata(schema = one)
    private val comparison = ByteArrayOutputStream()

    @Test
    fun `test getBodyBytes INTERNAL format`() {
        val report = Report(
            one,
            listOf(listOf("1", "2")),
            TestSource,
            bodyFormat = Report.Format.INTERNAL,
            metadata = metadata
        )
        csvSerializer.writeInternal(report, comparison)
        val bodyBytes = ReportWriter.getBodyBytes(report)
        assertThat { bodyBytes }.equals(comparison.toByteArray())
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
        hl7Serializer.write(report, comparison)
        val bodyBytes = ReportWriter.getBodyBytes(report)
        assertThat { bodyBytes }.equals(comparison.toByteArray())
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
        hl7Serializer.writeBatch(report, comparison)
        val bodyBytes = ReportWriter.getBodyBytes(report)
        assertThat { bodyBytes }.equals(comparison.toByteArray())
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
        csvSerializer.write(report, comparison)
        val bodyBytes = ReportWriter.getBodyBytes(report)
        assertThat { bodyBytes }.equals(comparison.toByteArray())
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
        csvSerializer.write(report, comparison)
        val bodyBytes = ReportWriter.getBodyBytes(report)
        assertThat { bodyBytes }.equals(comparison.toByteArray())
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

    @Test
    fun `test getBodyBytes happy path`() {}
}