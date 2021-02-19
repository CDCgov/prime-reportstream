@file:Suppress("unused", "unused")

package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import gov.cdc.prime.router.CsvComparer
import gov.cdc.prime.router.DocumentationFactory
import gov.cdc.prime.router.Metadata

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
    .subcommands(ProcessData(), ListSchemas(), GenerateDocs(), CompareCsvFiles(), EndToEndTest())
    .main(args)