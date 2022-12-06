package gov.cdc.prime.router.cli

import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TopicSender
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import java.io.File
import java.util.Locale

/**
 * A collection of file utilities for running tests
 */
object FileUtilities {
    fun createFakeCovidFile(
        metadata: Metadata,
        settings: SettingsProvider,
        sender: TopicSender,
        count: Int,
        targetStates: String? = null,
        targetCounties: String? = null,
        directory: String = ".",
        format: Report.Format = Report.Format.CSV,
        locale: Locale? = null
    ): File {
        val report = createFakeCovidReport(
            metadata,
            sender,
            count,
            targetStates,
            targetCounties,
            locale
        )
        return writeReportToFile(report, format, metadata, directory, null, settings)
    }

    fun createFakeCovidReport(
        metadata: Metadata,
        sender: TopicSender,
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
        settings: SettingsProvider,
        outputDir: String?,
        outputFileName: String?,
        echo: (message: Any?) -> Unit
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
                val outputFile = writeReportToFile(report, format, metadata, outputDir, outputFileName, settings)
                echo(outputFile.absolutePath)
            }
    }

    fun writeReportToFile(
        report: Report,
        format: Report.Format,
        metadata: Metadata,
        outputDir: String?,
        outputFileName: String?,
        settings: SettingsProvider
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
        val hl7Serializer = Hl7Serializer(metadata, settings)
        outputFile.outputStream().use {
            when (format) {
                Report.Format.INTERNAL -> csvSerializer.writeInternal(report, it)
                Report.Format.CSV, Report.Format.CSV_SINGLE -> csvSerializer.write(report, it)
                Report.Format.HL7 -> hl7Serializer.write(report, it)
                Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, it)
                else -> throw UnsupportedOperationException("Unsupported ${report.bodyFormat}")
            }
        }
        return outputFile
    }

    fun replaceText(
        path: String?,
        findText: String,
        replaceText: String,
    ): File {
        val file = File(path)

        // since the files are small, trying to read the whole file
        val content = file.readText(Charsets.UTF_8)
        val folderDir = File("./build/tmp")
        val fileW = File(folderDir, "otc-temp.csv")
        fileW.parentFile.mkdirs()
        fileW.writeBytes(content.replace(findText, replaceText).toByteArray())

        return fileW
    }

    /**
     * Does the file name of [file] match the internal file name pattern
     */
    fun isInternalFile(file: File): Boolean {
        return file.extension.equals("INTERNAL", ignoreCase = true) ||
            file.nameWithoutExtension.endsWith("INTERNAL", ignoreCase = true)
    }
}