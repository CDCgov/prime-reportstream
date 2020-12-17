@file:Suppress("unused", "unused")

package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import gov.cdc.prime.router.CsvConverter
import gov.cdc.prime.router.DocumentationFactory
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Hl7Converter
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class InputSource {
    data class FileSource(val fileName: String) : InputSource()
    data class FakeSource(val count: Int) : InputSource()
    data class DirSource(val dirName: String) : InputSource()
}

class RouterCli : CliktCommand(
    name = "prime",
    help = "Send health messages to their destinations",
    printHelpOnEmptyArgs = true,
) {
    private val inputSource: InputSource? by mutuallyExclusiveOptions(
        option("--input", help = "<file1>").convert { InputSource.FileSource(it) },
        option("--input_fake", help = "fake the input").int().convert { InputSource.FakeSource(it) },
        option("--input_dir", help = "<dir>").convert { InputSource.DirSource(it) },
    ).single()
    private val inputSchema by option("--input_schema", help = "<schema_name>").required()

    private val validate by option("--validate", help = "Validate stream").flag(default = true)
    private val route by option("--route", help = "route to receivers lists").flag(default = false)
    private val send by option("--send", help = "send to a receiver if specified").flag(default = false)

    private val outputFileName by option("--output", help = "<file> not compatible with route or partition")
    private val outputDir by option("--output_dir", help = "<directory>")
    private val outputSchema by option("--output_schema", help = "<schema_name> or use input schema if not specified")
    private val outputHl7 by option("--output_hl7", help = "True for HL7 output").flag(default = false)

    private val generateDocumentation by
    option("--generate-docs", help = "generate documentation from the provided schema")
        .flag(default = false)

    private fun readReportFromFile(
        fileName: String,
        readBlock: (name: String, schema: Schema, stream: InputStream) -> Report
    ): Report {
        val schemaName = inputSchema.toLowerCase()
        val schema = Metadata.findSchema(schemaName) ?: error("Schema $schemaName is not found")
        val file = File(fileName)
        if (!file.exists()) error("$fileName does not exist")
        echo("Opened: ${file.absolutePath}")
        return readBlock(file.nameWithoutExtension, schema, file.inputStream())
    }

    private fun writeReportsToFile(
        reports: List<Pair<Report, OrganizationService.Format>>,
        writeBlock: (report: Report, format: OrganizationService.Format, outputStream: OutputStream) -> Unit
    ) {
        if (outputDir == null && outputFileName == null) return
        if (reports.size > 0) {
            echo("Creating these files:")
        }
        reports.forEach { (report, format) ->
            val outputFile = if (outputFileName != null) {
                File(outputFileName!!)
            } else {
                val fileName = Report.formFileName(report.id, report.schema.baseName, format, report.createdDateTime)
                File(outputDir ?: ".", "$fileName")
            }
            echo(outputFile.absolutePath)
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }
            outputFile.outputStream().use {
                writeBlock(report, format, it)
            }
        }
    }

    private fun postHttp(address: String, block: (stream: OutputStream) -> Unit) {
        echo("Sending to: $address")
        val urlObj = URL(address)
        val connection = urlObj.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        val outputStream = connection.outputStream
        outputStream.use {
            block(it)
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            echo("connection: ${connection.responseCode}")
        }
    }

    override fun run() {
        // Load the schema and receivers
        Metadata.loadAll()
        echo("Loaded schema and receivers")

        // if we are generating the documentation from the schema, we don't want
        // to generate the reports below
        if (generateDocumentation) {
            val schemaName = inputSchema.toLowerCase()
            val schema = Metadata.findSchema(schemaName)

            if (schema == null) {
                echo("$schemaName not found. Did you mean one of these?")
                Metadata.listAll()
                return
            }

            // start generating documentation
            echo("Generating documentation for $schemaName")
            DocumentationFactory.writeDocumentationForSchema(schema, outputDir, outputFileName)
        } else {

            // Gather input source
            val inputReport: Report = when (inputSource) {
                is InputSource.FileSource -> {
                    readReportFromFile((inputSource as InputSource.FileSource).fileName) { name, schema, stream ->
                        CsvConverter.read(schema, stream, FileSource(name))
                    }
                }
                is InputSource.DirSource -> TODO("Dir source is not implemented")
                is InputSource.FakeSource -> {
                    val schema = Metadata.findSchema(inputSchema) ?: error("$inputSchema is an invalid schema name")
                    FakeReport.build(
                        schema,
                        (inputSource as InputSource.FakeSource).count,
                        FileSource("fake")
                    )
                }
                else -> {
                    error("input source must be specified")
                }
            }

            // Transform reports
            val outputFormat = if (outputHl7) OrganizationService.Format.HL7 else OrganizationService.Format.CSV
            val outputReports: List<Pair<Report, OrganizationService.Format>> = when {
                route ->
                    OrganizationService
                        .filterAndMapByService(inputReport, Metadata.organizationServices)
                        .map { it.first to it.second.format }
                outputSchema != null -> {
                    val toSchema = Metadata.findSchema(outputSchema!!) ?: error("outputSchema is invalid")
                    val mapping = inputReport.schema.buildMapping(toSchema)
                    val toReport = inputReport.applyMapping(mapping)
                    listOf(Pair(toReport, outputFormat))
                }
                else -> listOf(Pair(inputReport, outputFormat))
            }

            // Output reports
            writeReportsToFile(outputReports) { report, format, stream ->
                when (format) {
                    OrganizationService.Format.CSV -> CsvConverter.write(report, stream)
                    OrganizationService.Format.HL7 -> Hl7Converter.write(report, stream)
                }
            }
        }
    }
}

fun main(args: Array<String>) = RouterCli().main(args)