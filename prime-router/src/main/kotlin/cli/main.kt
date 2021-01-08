@file:Suppress("unused", "unused")

package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import gov.cdc.prime.router.DocumentationFactory
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
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
    private val inputSchema by option("--input_schema", help = "<schema_name>")
    private val validate by option("--validate", help = "Validate stream").flag(default = true)
    private val route by option("--route", help = "route to receivers lists").flag(default = false)
    private val list by option("--list", help = "list all schemas.  Ignores other parameters").flag(default = false)
    private val send by option("--send", help = "send to a receiver if specified").flag(default = false)
    private val routeTo by option("--route_to", help = "route a receiver")

    private val outputFileName by option("--output", help = "<file> not compatible with route or partition")
    private val outputDir by option("--output_dir", help = "<directory>")
    private val outputSchema by option("--output_schema", help = "<schema_name> or use input schema if not specified")
    private val outputHl7 by option("--output_hl7", help = "True for HL7 output").flag(default = false)

    private val generateDocumentation by
    option("--generate-docs", help = "generate documentation from the provided schema.  If no --input_schema is specified, generate for all schemas")
        .flag(default = false)
    private val includeTimestamps by
    option("--include-timestamps", help = "includes creation time stamps when generating documentation")
        .flag(default = false)
    private val targetState: String? by
    option(
        "--target-state",
        help = "specifies a state to generate test data for. " +
            "This is only used when generating test data, and has no meaning in other contexts"
    )

    private fun readReportFromFile(
        metadata: Metadata,
        fileName: String,
        readBlock: (name: String, schema: Schema, stream: InputStream) -> Report
    ): Report {
        val schemaName = inputSchema?.toLowerCase() ?: ""
        val schema = metadata.findSchema(schemaName) ?: error("Schema $schemaName is not found")
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

        if (reports.isNotEmpty()) {
            echo("Creating these files:")
        }

        reports.forEach { (report, format) ->
            val outputFile = if (outputFileName != null) {
                File(outputFileName!!)
            } else {
                val fileName = Report.formFileName(report.id, report.schema.baseName, format, report.createdDateTime)
                File(outputDir ?: ".", fileName)
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

    fun listSchemas(metadata: Metadata) {
        println("Current Hub Schema Library")
        var formatTemplate = "%-25s\t%-10s\t%s"
        println(formatTemplate.format("Schema Name", "Topic", "Description"))
        metadata.schemas.forEach {
            println(formatTemplate.format(it.name, it.topic, it.description))
        }
    }

    fun listOrganizations(metadata: Metadata) {
        println("Current Clients (Senders to the Hub)")
        var formatTemplate = "%-18s\t%-10s\t%s"
        println(formatTemplate.format("Organization Name", "Client Name", "Schema Sent to Hub"))
        metadata.organizationClients.forEach {
            println(formatTemplate.format(it.organization.name, it.name, it.schema))
        }
        println()
        println("Current Services (Receivers from the Hub)")
        formatTemplate = "%-18s\t%-10s\t%-25s\t%s"
        println(formatTemplate.format("Organization Name", "Service Name", "Schema Sent by Hub", "Filters Applied"))
        metadata.organizationServices.forEach {
            println(
                formatTemplate.format(
                    it.organization.name, it.name, it.schema,
                    it.jurisdictionalFilter.joinToString { it -> it }
                )
            )
        }
    }

    fun generateSchemaDocumentation(metadata: Metadata) {
        if (inputSchema.isNullOrBlank()) {
            println("Generating documentation for all schemas")
            metadata.schemas.forEach {
                DocumentationFactory.writeDocumentationForSchema(
                    it,
                    outputDir,
                    outputFileName,
                    includeTimestamps
                )
                println(it.name)
            }
        } else {
            val schemaName = inputSchema?.toLowerCase() ?: ""
            val schema = metadata.findSchema(schemaName)
            if (schema == null) {
                echo("$schemaName not found. Did you mean one of these?")
                listSchemas(metadata)
                return
            }
            // start generating documentation
            echo("Generating documentation for $schemaName")
            DocumentationFactory.writeDocumentationForSchema(schema, outputDir, outputFileName, includeTimestamps)
        }
    }

    override fun run() {
        // Load the schema and receivers
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata)
        val redoxSerializer = RedoxSerializer(metadata)
        echo("Loaded schema and receivers")

        if (list) {
            println()
            listSchemas(metadata)
            println()
            listOrganizations(metadata)
            println()
            return
        }
        if (generateDocumentation) {
            generateSchemaDocumentation(metadata)
            return
        }

        // Gather input source
        val inputReport: Report = when (inputSource) {
            is InputSource.FileSource -> {
                readReportFromFile(metadata, (inputSource as InputSource.FileSource).fileName) { name, schema, stream ->
                    val result = csvSerializer.read(schema.name, stream, FileSource(name))
                    if (result.report == null) {
                        error(result.errorsToString())
                    }
                    if (result.errors.isNotEmpty()) {
                        echo(result.errorsToString())
                    }
                    if (result.warnings.isNotEmpty()) {
                        echo(result.warningsToString())
                    }
                    result.report
                }
            }
            is InputSource.DirSource -> TODO("Dir source is not implemented")
            is InputSource.FakeSource -> {
                val schema = metadata.findSchema(inputSchema ?: "") ?: error("$inputSchema is an invalid schema name")
                FakeReport(metadata).build(
                    schema,
                    (inputSource as InputSource.FakeSource).count,
                    FileSource("fake"),
                    targetState
                )
            }
            else -> {
                error("input source must be specified")
            }
        }

        // Transform reports
        val translator = Translator(metadata)
        val outputFormat = if (outputHl7) OrganizationService.Format.HL7 else OrganizationService.Format.CSV
        val outputReports: List<Pair<Report, OrganizationService.Format>> = when {
            route ->
                translator
                    .filterAndTranslateByService(inputReport)
                    .map { it.first to it.second.format }
            routeTo != null -> {
                val pair = translator.translate(input = inputReport, toService = routeTo!!)
                if (pair != null) listOf(Pair(pair.first, pair.second.format)) else emptyList()
            }
            outputSchema != null -> {
                val toSchema = metadata.findSchema(outputSchema!!) ?: error("outputSchema is invalid")
                val mapping = translator.buildMapping(toSchema, inputReport.schema, defaultValues = emptyMap())
                if (mapping.missing.isNotEmpty()) {
                    error("Error: When translating to $'${toSchema.name} missing fields for ${mapping.missing.joinToString(", ")}")
                }
                val toReport = inputReport.applyMapping(mapping)
                listOf(Pair(toReport, outputFormat))
            }
            else -> listOf(Pair(inputReport, outputFormat))
        }

        // Output reports
        writeReportsToFile(outputReports) { report, format, stream ->
            when (format) {
                OrganizationService.Format.CSV -> csvSerializer.write(report, stream)
                OrganizationService.Format.HL7 -> hl7Serializer.write(report, stream)
                OrganizationService.Format.REDOX -> redoxSerializer.write(report, stream)
            }
        }
    }
}

fun main(args: Array<String>) = RouterCli().main(args)