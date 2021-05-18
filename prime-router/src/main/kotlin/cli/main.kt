@file:Suppress("unused", "unused")

package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import gov.cdc.prime.router.CsvComparer
import gov.cdc.prime.router.DocumentationFactory
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Translator
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
    help = "list known schemas, senders, and receivers"
) {
    fun listOrganizations(settings: SettingsProvider) {
        println("Current Clients (Senders to the Hub)")
        var formatTemplate = "%-18s\t%-10s\t%s"
        println(formatTemplate.format("Organization Name", "Client Name", "Schema Sent to Hub"))
        settings.senders.forEach {
            println(formatTemplate.format(it.organizationName, it.name, it.schemaName))
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
        settings.receivers.forEach {
            println(
                formatTemplate.format(
                    it.organizationName,
                    it.name,
                    it.schemaName,
                    it.jurisdictionalFilter.joinToString()
                )
            )
        }
    }

    override fun run() {
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        println()
        listSchemas(metadata)
        println()
        listOrganizations(settings)
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
    private val outputHl7Elements by option(
        "--mapped-hl7-elements"
    ).flag(default = false)
    private val generateHtml by option(
        "--generate-html", help = "generate the HTML version of the documentation. Default is not to generate HTML."
    ).flag("--no-generate-html", default = false)
    private val generateMarkup by option(
        "--generate-markup", help = "generate the markup version of the documentation.  Default is to generate markup."
    ).flag("--no-generate-markup", default = true)
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
        if (!generateMarkup && !generateHtml) {
            println("Nothing generated.  You need to specify at least one type of output.")
            return
        }
        else if (inputSchema.isNullOrBlank()) {
            println("Generating documentation for all schemas")

            // Clear the existing schema (we want to remove deleted schemas)
            if (Files.exists(Paths.get(outputDir))) {
                File(outputDir).deleteRecursively()
            }

            metadata.schemas.forEach {
                DocumentationFactory.writeDocumentationForSchema(
                    it,
                    outputDir,
                    outputFileName,
                    includeTimestamps,
                    generateMarkup,
                    generateHtml
                )
                println(it.name)
            }
        } else {
            val schemaName = inputSchema?.lowercase() ?: ""
            var schema = metadata.findSchema(schemaName)
            if (schema == null) {
                echo("$schemaName not found. Did you mean one of these?")
                listSchemas(metadata)
                return
            }
            // start generating documentation
            echo("Generating documentation for $schemaName")
            if (outputHl7Elements) {
                schema = buildMappedHl7Schema(schema)
            }
            DocumentationFactory.writeDocumentationForSchema(schema, outputDir, outputFileName, includeTimestamps,
                generateMarkup, generateHtml)
        }
    }

    override fun run() {
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        generateSchemaDocumentation(metadata)
    }

    /**
     * Build a schema that contains all hl7 elements that would result from translation of the input schema
     * This includes not only the elements from the input schema, but also mapped and default elements. Filter
     * these elements to only those that have HL7 fields. In other words, this schema represents the
     * output data dictionary for the input schema. The schema is sorted by HL7 segment.
     */
    private fun buildMappedHl7Schema(fromSchema: Schema): Schema {
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        val fileSettings = FileSettings(FileSettings.defaultSettingsDirectory)
        val translator = Translator(metadata, fileSettings)
        val mappedDefaults = mapOf(
            "receiving_application" to "AZ",
            "receiving_facility" to "AZDOH",
            "message_profile_id" to "AZELR"
        )
        val toSchema = metadata.findSchema("covid-19") ?: error("covid-19 schema not defined")
        val mapping = translator.buildMapping(toSchema, fromSchema, mappedDefaults)
        val set = mutableSetOf<String>()
        set.addAll(mapping.useDirectly.keys)
        set.addAll(mapping.useDefault.keys)
        set.addAll(mapping.useValueSet.keys)
        set.addAll(mapping.useMapper.keys)
        val reg = Regex("([A-Z]*)-?(\\d+)?-?(\\d+)?")
        val hl7Elements = set.toList().mapNotNull {
            val element = toSchema.findElement(it) ?: error("invalid element: $it")
            if (element.hl7Field != null || element.hl7AOEQuestion != null) element else null
        }.sortedBy {
            if (it.hl7Field != null) {
                val matches = reg.find(it.hl7Field) ?: return@sortedBy ""
                val groupValues = matches.groupValues
                val segment = groupValues[1]
                val field = if (groupValues[2].isNotBlank()) 'A'.plus(groupValues[2].toInt()) else ""
                val subField = if (groupValues[3].isNotBlank()) 'A'.plus(groupValues[3].toInt()) else ""
                "$segment$field$subField${it.name}"
            } else {
                ""
            }
        }
        return Schema(
            fromSchema.baseName,
            "covid-19",
            hl7Elements,
            description = "HL7 data elements resulting from ${fromSchema.baseName}"
        )
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
    .completionOption()
    .subcommands(
        ProcessData(),
        ListSchemas(),
        GenerateDocs(),
        CredentialsCli(),
        CompareCsvFiles(),
        TestReportStream(),
        LoginCommand(),
        LogoutCommand(),
        OrganizationSettings(),
        SenderSettings(),
        ReceiverSettings(),
        MultipleSettings(),
    )
    .main(args)