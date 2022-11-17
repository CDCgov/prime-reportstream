package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Report
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import java.io.ByteArrayOutputStream

object ReportWriter {
    private val csvSerializer: CsvSerializer by lazy { BaseEngine.csvSerializerSingleton }
    private val hl7Serializer: Hl7Serializer by lazy { BaseEngine.hl7SerializerSingleton }

    /**
     * Uses a serializer that matches the bodyFormat of the passed in [report] to generate a ByteArray to upload
     * to the blobstore. [sendingApplicationReport], [receivingApplicationReport], and [receivingFacilityReport] are
     * optional parameter that should be populated solely for empty HL7_BATCH files.
     */
    fun getBodyBytes(
        report: Report,
        sendingApplicationReport: String? = null,
        receivingApplicationReport: String? = null,
        receivingFacilityReport: String? = null
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        when (report.bodyFormat) {
            Report.Format.INTERNAL -> csvSerializer.writeInternal(report, outputStream)
            // HL7 needs some additional configuration we set on the translation in organization
            Report.Format.HL7 -> hl7Serializer.write(report, outputStream)
            Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(
                report,
                outputStream,
                sendingApplicationReport,
                receivingApplicationReport,
                receivingFacilityReport
            )
            Report.Format.CSV, Report.Format.CSV_SINGLE -> csvSerializer.write(report, outputStream)
            else -> throw UnsupportedOperationException("Unsupported ${report.bodyFormat}")
        }
        return outputStream.toByteArray()
    }
}