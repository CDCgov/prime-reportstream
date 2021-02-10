@file:Suppress("unused", "unused")

package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import gov.cdc.prime.router.CsvComparer
import gov.cdc.prime.router.DocumentationFactory
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class InputSource {
    data class FileSource(val fileName: String) : InputSource()
    data class FakeSource(val count: Int) : InputSource()
    data class DirSource(val dirName: String) : InputSource()
    data class ListOfFilesSource(val commaSeparatedList: String) : InputSource() // supports merge.
}

class RouterCli : CliktCommand(
    name = "prime",
    help = "Tools and commands that support the PRIME Data Hub.",
    printHelpOnEmptyArgs = true,
) {
    override fun run() = Unit
}

class ProcessData : CliktCommand(
    name = "data",
    help = """
    process data
     
    Use this command to process data in a variety of ways, similar to how the
    REST service might handle data sent to it.
    
    You must always specify input data (via --input, --input-dir, --merge, or
    --input-fake to generate random data) and --input-schema.
    
    The data can then be transformed to a new schema (using --route, --route-to,
    or --output-schema) and written to a given location. Output will be written
    in CSV format unless otherwise specified.
    
    If no output location is set, files are written to the current working
    directory.
    
    To generate test data for later use, use --input-fake with no
    transformations. For example, to generate 5 rows of test data using the
    Florida COVID-19 schema:
    > prime data --input-fake 5 --input-schema fl/fl-covid-19 --output florida-data.csv
    """
) {
    // Input
    private val inputSource: InputSource? by mutuallyExclusiveOptions(
        option(
            "--merge",
            metavar = "<paths>",
            help = "list of comma-separated CSV files to merge as input"
        ).convert { InputSource.ListOfFilesSource(it) },
        option(
            "--input",
            metavar = "<path>",
            help = "path to CSV file to read"
        ).convert { InputSource.FileSource(it) },
        option(
            "--input-fake",
            metavar = "<int>",
            help = "generate N rows of random input according to --input-schema. " +
                "The value is the number of rows to generate."
        ).int().convert { InputSource.FakeSource(it) },
        option(
            "--input-dir",
            metavar = "<dir>",
            help = "path to directory of files"
        ).convert { InputSource.DirSource(it) },
    ).single()
    private val inputSchema by option(
        "--input-schema",
        metavar = "<schema_name>",
        help = "interpret input according to this schema"
    )

    // Actions
    private val validate by option(
        "--validate",
        help = "Validate stream"
    ).flag(default = true)
    private val send by option(
        "--send",
        help = "send output to receivers"
    ).flag(default = false)

    // Output schema
    private val route by option(
        "--route",
        help = "transform output to the schemas for each receiver the input would be routed to"
    ).flag(default = false)
    private val routeTo by option(
        "--route-to",
        metavar = "<receiver>",
        help = "transform output to the schema for the given receiver"
    )
    private val outputSchema by option(
        "--output-schema",
        metavar = "<name>",
        help = "transform output to the given schema"
    )

    // Output format
    private val forcedFormat by option(
        "--output-format",
        help = "serialize as the specified format. Use the destination format if not specified."
    )

    // Output location
    private val outputFileName by option(
        "--output",
        metavar = "<path>",
        help = "write output to this file. Do not use with --route, which generates multiple outputs."
    )
    private val outputDir by option(
        "--output-dir",
        metavar = "<path>",
        help = "write output files to this directory instead of the working directory. Ignored if --output is set."
    )

    // Fake data configuration
    private val targetStates: String? by
    option(
        "--target-states",
        metavar = "<abbrev>",
        help = "when using --input-fake, fill geographic fields using these states, separated by commas. " +
            "States should be two letters, e.g. 'FL'"
    )

    private val targetCounties: String? by
    option(
        "--target-counties",
        metavar = "<name>",
        help = "when using --input-fake, fill county-related fields with these county names, separated by commas."
    )

    private fun mergeReports(
        metadata: Metadata,
        listOfFiles: String
    ): Report {
        if (listOfFiles.isEmpty()) error("No files to merge.")
        val files = listOfFiles.split(",", " ").filter { it.isNotBlank() }
        if (files.isEmpty()) error("No files to merge found in comma separated list.  Need at least one file.")
        val reports = files.map { readReportFromFile(metadata, it) }
        echo("Merging ${reports.size} reports.")
        return Report.merge(reports)
    }

    private fun readReportFromFile(
        metadata: Metadata,
        fileName: String
    ): Report {
        val schemaName = inputSchema?.toLowerCase() ?: ""
        val schema = metadata.findSchema(schemaName) ?: error("Schema $schemaName is not found")
        val file = File(fileName)
        if (!file.exists()) error("$fileName does not exist")
        echo("Opened: ${file.absolutePath}")
        val csvSerializer = CsvSerializer(metadata)
        val result = csvSerializer.readExternal(schema.name, file.inputStream(), FileSource(file.nameWithoutExtension))
        if (result.report == null) {
            error(result.errorsToString())
        }
        if (result.errors.isNotEmpty()) {
            echo(result.errorsToString())
        }
        if (result.warnings.isNotEmpty()) {
            echo(result.warningsToString())
        }
        return result.report
    }

    private fun writeReportsToFile(
        reports: List<Pair<Report, Report.Format>>,
        writeBlock: (report: Report, format: Report.Format, outputStream: OutputStream) -> Unit
    ) {
        if (outputDir == null && outputFileName == null) return

        if (reports.isNotEmpty()) {
            echo("Creating these files:")
        }
        reports
            .flatMap { (report, format) ->
                // Some report formats only support one result per file
                if (format.isSingleItemFormat()) {
                    val splitReports = report.split()
                    splitReports.map { Pair(it, format) }
                } else {
                    listOf(Pair(report, format))
                }
            }.forEach { (report, format) ->
                val outputFile = if (outputFileName != null) {
                    File(outputFileName!!)
                } else {
                    val fileName = Report.formFilename(
                        report.id,
                        report.schema.baseName,
                        format,
                        report.createdDateTime
                    )
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

    // NOTE: This exists to support not-yet-implemented functionality.
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

    private fun getOutputFormat(default: Report.Format): Report.Format {
        return if (forcedFormat != null) Report.Format.valueOf(forcedFormat!!) else default
    }

    override fun run() {
        // Load the schema and receivers
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata)
        val redoxSerializer = RedoxSerializer(metadata)
        echo("Loaded schema and receivers")
        // Gather input source
        val inputReport: Report = when (inputSource) {
            is InputSource.ListOfFilesSource ->
                mergeReports(metadata, (inputSource as InputSource.ListOfFilesSource).commaSeparatedList)
            is InputSource.FileSource ->
                readReportFromFile(metadata, (inputSource as InputSource.FileSource).fileName)
            is InputSource.DirSource ->
                TODO("Dir source is not implemented")
            is InputSource.FakeSource -> {
                val schema = metadata.findSchema(inputSchema ?: "")
                    ?: error("$inputSchema is an invalid schema name")
                FakeReport(metadata).build(
                    schema,
                    (inputSource as InputSource.FakeSource).count,
                    FileSource("fake"),
                    targetStates,
                    targetCounties
                )
            }
            else -> {
                error("input source must be specified")
            }
        }

        if (!validate) TODO("validation cannot currently be disabled")
        if (send) TODO("--send is not implemented")

        // Transform reports
        val translator = Translator(metadata)
        val outputReports: List<Pair<Report, Report.Format>> = when {
            route ->
                translator
                    .filterAndTranslateByService(inputReport)
                    .map { it.first to getOutputFormat(it.second.format) }
            routeTo != null -> {
                val pair = translator.translate(input = inputReport, toService = routeTo!!)
                if (pair != null)
                    listOf(pair.first to getOutputFormat(pair.second.format))
                else
                    emptyList()
            }
            outputSchema != null -> {
                val toSchema = metadata.findSchema(outputSchema!!) ?: error("outputSchema is invalid")
                val mapping = translator.buildMapping(toSchema, inputReport.schema, defaultValues = emptyMap())
                if (mapping.missing.isNotEmpty()) {
                    error(
                        "Error: When translating to $'${toSchema.name} " +
                            "missing fields for ${mapping.missing.joinToString(", ")}"
                    )
                }
                val toReport = inputReport.applyMapping(mapping)
                listOf(Pair(toReport, getOutputFormat(Report.Format.CSV)))
            }
            else -> listOf(Pair(inputReport, getOutputFormat(Report.Format.CSV)))
        }

        // Output reports
        writeReportsToFile(outputReports) { report, format, stream ->
            when (format) {
                Report.Format.INTERNAL -> csvSerializer.writeInternal(report, stream)
                Report.Format.CSV -> csvSerializer.write(report, stream)
                Report.Format.HL7 -> hl7Serializer.write(report, stream)
                Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, stream)
                Report.Format.REDOX -> redoxSerializer.write(report, stream)
            }
        }
    }
}

fun listSchemas(metadata: Metadata) {
    println("Current Hub Schema Library")
    val formatTemplate = "%-25s\t%-10s\t%s"
    println(formatTemplate.format("Schema Name", "Topic", "Description"))
    metadata.schemas.forEach {
        println(formatTemplate.format(it.name, it.topic, it.description))
    }
}

class ListSchemas : CliktCommand(
    name = "list",
    help = "list known schemas, clients, and services"
) {
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
        println(
            formatTemplate.format(
                "Organization Name",
                "Service Name",
                "Schema Sent by Hub",
                "Filters Applied"
            )
        )
        metadata.organizationServices.forEach {
            println(
                formatTemplate.format(
                    it.organization.name, it.name, it.schema,
                    it.jurisdictionalFilter.joinToString()
                )
            )
        }
    }

    override fun run() {
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        println()
        listSchemas(metadata)
        println()
        listOrganizations(metadata)
        println()
    }
}

class GenerateDocs : CliktCommand(
    help = """
    generate documentation for schemas

    By default, this will generate documentation for all known schemas. To
    generate docs for a particular schema, use the --input-schema option.
    """
) {
    private val inputSchema by option(
        "--input-schema",
        metavar = "<schema_name>",
        help = "schema to document"
    )
    private val includeTimestamps by
    option("--include-timestamps", help = "include creation time in file names")
        .flag(default = false)
    private val outputFileName by option(
        "--output",
        metavar = "<path>",
        help = "write documentation to this file (should not include extension)"
    )
    private val defaultOutputDir = "docs/schema_documentation"
    private val outputDir by option(
        "--output-dir",
        metavar = "<path>",
        help = "interpret `--output` relative to this directory (default: \"$defaultOutputDir\")"
    )
        .default(defaultOutputDir)

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
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        generateSchemaDocumentation(metadata)
    }
}

class CompareCsvFiles : CliktCommand(
    name = "compare",
    help = """
    compares two CSV files so you can view the differences within them
    
    In order to compare two CSV files, you need to pass in a record ID value, which is the
    column header for a value that is unique per row. i.e. "Patient ID"
        
    Example:
    ./prime compare --record-id "MRN" --csv-file FILE_PATH --csv-file FILE_PATH
    """
) {
    // lets us compare CSV files generated by the application
    private val csvCompareFile by option(
        "--csv-file",
        help = "specify paths to CSV files to compare contents"
    ).multiple()
    private val csvRecordId by option(
        "--record-id",
        help = "the column header that identifies the id value for each row in a CSV file"
    ).default("Patient_Id")

    private fun compareCsvDocuments() {
        val fileOne = csvCompareFile[0]
        val fileTwo = csvCompareFile[1]
        val csvComparer = CsvComparer(fileOne, fileTwo, csvRecordId)
        csvComparer.compareFiles()
    }

    override fun run() {
        compareCsvDocuments()
    }
}

fun main(args: Array<String>) = RouterCli()
    .subcommands(ProcessData(), ListSchemas(), GenerateDocs(), CompareCsvFiles())
    .main(args)