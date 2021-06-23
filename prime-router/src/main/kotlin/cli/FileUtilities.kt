package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.output.TermUi.echo
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import java.io.File
import java.util.Locale

class FileUtilities {
    companion object {
        fun createFakeFile(
            metadata: Metadata,
            sender: Sender,
            count: Int,
            targetStates: String? = null,
            targetCounties: String? = null,
            directory: String = ".",
            format: Report.Format = Report.Format.CSV,
            locale: Locale? = null
        ): File {
            val report = createFakeReport(
                metadata,
                sender,
                count,
                targetStates,
                targetCounties,
                locale
            )
            return writeReportToFile(report, format, metadata, directory, null)
        }

        fun createFakeReport(
            metadata: Metadata,
            sender: Sender,
            count: Int,
            targetStates: String? = null,
            targetCounties: String? = null,
            locale: Locale? = null
        ): Report {
            return FakeReport(metadata, locale).build(
                metadata.findSchema(sender.schemaName)
                    ?: error("Unable to find schema ${sender.schemaName}"),
                count,
                FileSource("fake"),
                targetStates,
                targetCounties,
            )
        }

        fun writeReportsToFile(
            reports: List<Pair<Report, Report.Format>>,
            metadata: Metadata,
            outputDir: String?,
            outputFileName: String?,
        ) {
            if (outputDir == null && outputFileName == null) return

            if (reports.isNotEmpty()) {
                echo("Creating these files:")
            }
            reports
                .flatMap { (report, format) ->
                    // Some report formats only support one result per file
                    if (format.isSingleItemFormat) {
                        val splitReports = report.split()
                        splitReports.map { Pair(it, format) }
                    } else {
                        listOf(Pair(report, format))
                    }
                }.forEach { (report, format) ->
                    val outputFile = writeReportToFile(report, format, metadata, outputDir, outputFileName)
                    echo(outputFile.absolutePath)
                }
        }

        fun writeReportToFile(
            report: Report,
            format: Report.Format,
            metadata: Metadata,
            outputDir: String?,
            outputFileName: String?,
        ): File {
            val outputFile = if (outputFileName != null) {
                File(outputFileName)
            } else {
                val fileName = Report.formFilename(
                    report.id,
                    report.schema.baseName,
                    format,
                    report.createdDateTime,
                    nameFormat = "standard",
                    report.destination?.translation,
                    metadata
                )
                File(outputDir ?: ".", fileName)
            }
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            val csvSerializer = CsvSerializer(metadata)
            val hl7Serializer = Hl7Serializer(metadata)
            val redoxSerializer = RedoxSerializer(metadata)
            outputFile.outputStream().use {
                when (format) {
                    Report.Format.INTERNAL -> csvSerializer.writeInternal(report, it)
                    Report.Format.CSV -> csvSerializer.write(report, it)
                    Report.Format.HL7 -> hl7Serializer.write(report, it)
                    Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, it)
                    Report.Format.REDOX -> redoxSerializer.write(report, it)
                }
            }
            return outputFile
        }
    }
}