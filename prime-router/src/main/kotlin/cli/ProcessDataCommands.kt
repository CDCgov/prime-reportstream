package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.CustomConfiguration
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DefaultValues
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.TopicSender
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
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

sealed class InputClientInfo {
    data class InputSchema(val schemaName: String) : InputClientInfo()
    data class InputClient(val clientName: String) : InputClientInfo()
}

/**
 * Command to process data in a variety of ways. Pass in a [metadataInstance]
 * and/or [fileSettingsInstance] to reuse this class programatically and make it run faster.
 */
class ProcessData(
    private val metadataInstance: Metadata? = null,
    private val fileSettingsInstance: FileSettings? = null,
) : CliktCommand(
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
    """,
    printHelpOnEmptyArgs = true,
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
            help = "path to CSV file to read",
            completionCandidates = CompletionCandidates.Path
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
            help = "path to directory of files",
            completionCandidates = CompletionCandidates.Path
        ).convert { InputSource.DirSource(it) },
    ).single()

    private val inputClientInfo: InputClientInfo? by mutuallyExclusiveOptions(
        option(
            "--input-schema",
            metavar = "<schema_name>",
            help = "interpret input according to this schema"
        ).convert { InputClientInfo.InputSchema(it) },
        option(
            "--input-client",
            metavar = "<client_name>",
            help = "interpret input according to this client"
        ).convert { InputClientInfo.InputClient(it) }
    ).single()

    private val testFileSettingsInstance by option(
        "--test-dir-setting",
        metavar = "<path>",
        help = "Test organization setting dir"
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
    private val synthesize by option(
        "--synthesize",
        help = "converts live production data into synthesized data"
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
        help = "write output files to this directory instead of the working directory. Ignored if --output is set.",
        completionCandidates = CompletionCandidates.Path
    )
    private val nameFormat by option(
        "--name-format",
        metavar = "<file name format>",
        help = "Output using the APHL file format"
    )
    private val receivingOrganization by option(
        "--output-receiving-org",
        metavar = "<org name>",
        help = "Output using the APHL file format"
    )
    private val suppressQstForAoe by option(
        "--suppress-qst-for-aoe",
        help = "Turns off the QST marker on AOE questions when converting to HL7"
    ).flag(default = false)
    private val reportingFacilityName by option(
        "--reporting-facility-name",
        metavar = "<reporting facility name>",
        help = "The name of the reporting facility"
    )
    private val reportingFacilityId by option(
        "--reporting-facility-id",
        metavar = "<reporting facility ID>",
        help = "The ID of the reporting facility"
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
    private val receivingApplication by option(
        "--receiving-application",
        help = "the receiving application"
    )
    private val receivingFacility by option(
        "--receiving-facility",
        help = "the receiving facility"
    )
    private val includeNcesFacilities by option(
        "--include-nces-facilities",
        help = "matching zip codes to those in the NCES dataset."
    ).flag(default = false)

    /**
     * A list of generated output files.
     */
    val outputReportFiles = mutableListOf<String>()

    private fun mergeReports(
        metadata: Metadata,
        schema: Schema?,
        sender: Sender?,
        settings: SettingsProvider,
        listOfFiles: String,
    ): Report {
        if (listOfFiles.isEmpty()) error("No files to merge.")
        val files = listOfFiles.split(",", " ").filter { it.isNotBlank() }
        if (files.isEmpty()) error("No files to merge found in comma separated list.  Need at least one file.")
        val reports = files.map { readReportFromFile(metadata, schema, sender, settings, it) }
        echo("Merging ${reports.size} reports.")
        return Report.merge(reports)
    }

    private fun handleReadResult(result: ReadResult): Report {
        /**
         * Print the action [log].
         */
        fun printLog(log: ActionLog) {
            val itemIndexStr = if (log.index != null) "INDEX=${log.index}, " else ""
            echo("${log.type}: $itemIndexStr${log.detail.message}")
        }
        result.actionLogs.errors.forEach { printLog(it) }
        result.actionLogs.warnings.forEach { printLog(it) }
        return result.report
    }

    private fun readReportFromFile(
        metadata: Metadata,
        schema: Schema?,
        sender: Sender?,
        settings: SettingsProvider,
        fileName: String,
    ): Report {
        val schemaName = schema?.name as String
        val file = File(fileName)
        if (!file.exists()) error("$fileName does not exist")
        echo("Opened: ${file.absolutePath}")
        return when (file.extension.lowercase()) {
            "hl7" -> {
                val hl7Serializer = Hl7Serializer(metadata, settings)
                val result = hl7Serializer.readExternal(
                    schemaName,
                    file.inputStream(),
                    FileSource(file.nameWithoutExtension),
                    sender = sender
                )
                handleReadResult(result)
            }
            else -> {
                val csvSerializer = CsvSerializer(metadata)
                return if (FileUtilities.isInternalFile(file)) {
                    csvSerializer.readInternal(
                        schema.name,
                        file.inputStream(),
                        listOf(FileSource(file.nameWithoutExtension))
                    )
                } else {
                    val result =
                        csvSerializer.readExternal(
                            schema.name,
                            file.inputStream(),
                            FileSource(file.nameWithoutExtension),
                            sender = sender
                        )
                    handleReadResult(result)
                }
            }
        }
    }

    private fun writeReportsToFile(
        reports: List<Pair<Report, Report.Format>>,
        metadata: Metadata,
        writeBlock: (report: Report, format: Report.Format, outputStream: OutputStream) -> Unit,
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
                val outputFile = if (outputFileName != null) {
                    File(outputFileName!!)
                } else {
                    // generate a translation config if we don't have
                    val translationConfig = report.destination?.translation ?: CustomConfiguration(
                        report.schema.baseName,
                        format,
                        receivingOrganization = null,
                        nameFormat = nameFormat ?: "standard"
                    )
                    val fileName = Report.formFilename(
                        report.id,
                        report.schema.baseName,
                        format,
                        report.createdDateTime,
                        translationConfig,
                        metadata
                    )
                    File(outputDir ?: ".", fileName)
                }
                outputReportFiles.add(outputFile.absolutePath)
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

    private fun getDefaultValues(): DefaultValues {
        val values = mutableMapOf<String, String>()
        receivingApplication?.let { values["receiving_application"] = it }
        receivingFacility?.let { values["receiving_facility"] = it }
        return values
    }

    override fun run() {
        // Load the schema and receivers
        val metadata = metadataInstance ?: Metadata.getInstance()
        val fileSettings = testFileSettingsInstance?.let { FileSettings(it) }
            ?: (fileSettingsInstance ?: FileSettings(FileSettings.defaultSettingsDirectory))
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata, fileSettings)
        echo("Loaded schema and receivers")

        val (schema, sender) = when (inputClientInfo) {
            is InputClientInfo.InputClient -> {
                val clientName = (inputClientInfo as InputClientInfo.InputClient).clientName
                val sender = fileSettings.findSender(clientName) ?: error("Sender $clientName was not found")
                if (sender is TopicSender) {
                    Pair(
                        sender.let {
                            metadata.findSchema(it.schemaName) ?: error("Schema ${it.schemaName} was not found")
                        },
                        sender
                    )
                } else {
                    Pair(
                        null,
                        sender
                    )
                }
            }
            is InputClientInfo.InputSchema -> {
                val inputSchema = (inputClientInfo as InputClientInfo.InputSchema).schemaName
                val schName = inputSchema.lowercase()
                metadata.findSchema(schName) ?: error("Schema $inputSchema was not found")
                // Get a random sender name that uses the provided schema, or null if no sender is found.
                val sender = fileSettings.senders.filter {
                    it is TopicSender && it.schemaName == schName
                }.randomOrNull()
                Pair(metadata.findSchema(schName), sender)
            }
            else -> {
                error("input schema or client's name must be specified")
            }
        }

        // Gather input source
        var inputReport: Report = when (inputSource) {
            is InputSource.ListOfFilesSource ->
                mergeReports(
                    metadata, schema, sender, fileSettings,
                    (inputSource as InputSource.ListOfFilesSource).commaSeparatedList
                )
            is InputSource.FileSource ->
                readReportFromFile(
                    metadata, schema, sender, fileSettings,
                    (inputSource as InputSource.FileSource).fileName
                )
            is InputSource.DirSource ->
                TODO("Dir source is not implemented")
            is InputSource.FakeSource -> {
                FakeReport(metadata).build(
                    schema as Schema,
                    (inputSource as InputSource.FakeSource).count,
                    FileSource("fake"),
                    targetStates,
                    targetCounties,
                    includeNcesFacilities
                )
            }
            else -> {
                error("input source must be specified")
            }
        }

        // synthesize the data here
        // todo: put these strategies into metadata so we can load them from a file
        val synthesizeStrategies = mapOf(
            "patient_last_name" to Report.SynthesizeStrategy.FAKE,
            "patient_first_name" to Report.SynthesizeStrategy.FAKE,
            "patient_middle_name" to Report.SynthesizeStrategy.FAKE,
            "patient_middle_initial" to Report.SynthesizeStrategy.FAKE,
            "patient_gender" to Report.SynthesizeStrategy.SHUFFLE,
            "patient_race" to Report.SynthesizeStrategy.SHUFFLE,
            "patient_ethnicity" to Report.SynthesizeStrategy.SHUFFLE,
            "patient_dob" to Report.SynthesizeStrategy.SHUFFLE,
            "patient_phone_number" to Report.SynthesizeStrategy.FAKE,
            "patient_street" to Report.SynthesizeStrategy.FAKE,
            "patient_state" to Report.SynthesizeStrategy.FAKE,
            "patient_city" to Report.SynthesizeStrategy.FAKE,
            "patient_county" to Report.SynthesizeStrategy.FAKE,
            "patient_zip_code" to Report.SynthesizeStrategy.FAKE,
            "message_id" to Report.SynthesizeStrategy.FAKE,
            "patient_email" to Report.SynthesizeStrategy.FAKE,
        )

        if (!validate) TODO("validation cannot currently be disabled")
        if (send) TODO("--send is not implemented")
        if (synthesize) inputReport = inputReport.synthesizeData(
            synthesizeStrategies,
            targetStates,
            targetCounties,
            metadata
        )

        // Transform reports
        val translator = Translator(metadata, fileSettings)
        val warnings = mutableListOf<ActionLog>()
        val outputReports: List<Pair<Report, Report.Format>> = when {
            route -> {
                val (reports, byReceiverWarnings) = translator
                    .filterAndTranslateByReceiver(inputReport, getDefaultValues(), emptyList())
                warnings += byReceiverWarnings
                reports.filter { it.report.itemCount > 0 }
                    .map { it.report to getOutputFormat(it.receiver.format) }
            }
            routeTo != null -> {
                val pair = translator.translate(
                    input = inputReport,
                    toReceiver = routeTo!!,
                    defaultValues = getDefaultValues()
                )
                if (pair != null)
                    listOf(pair.first to getOutputFormat(pair.second.format))
                else
                    emptyList()
            }
            outputSchema != null -> {
                val toSchema = metadata.findSchema(outputSchema!!) ?: error("outputSchema is invalid")
                val mapping = translator.buildMapping(
                    toSchema = toSchema,
                    fromSchema = inputReport.schema,
                    defaultValues = getDefaultValues()
                )
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

        if (warnings.size > 0) {
            echo("Problems occurred during translation to output schema:")
            warnings.forEach {
                echo("${it.scope} ${it.trackingId}: ${it.detail.message}")
            }
            echo()
        }

        // Output reports
        writeReportsToFile(outputReports, metadata) { report, format, stream ->
            when (format) {
                Report.Format.INTERNAL -> csvSerializer.writeInternal(report, stream)
                Report.Format.CSV, Report.Format.CSV_SINGLE -> csvSerializer.write(report, stream)
                Report.Format.HL7 -> {
                    // create a default hl7 config
                    val hl7Configuration = Hl7Configuration(
                        nameFormat = nameFormat ?: "standard",
                        suppressQstForAoe = suppressQstForAoe,
                        receivingApplicationName = receivingApplication,
                        receivingFacilityName = receivingFacility,
                        receivingOrganization = receivingOrganization,
                        receivingApplicationOID = "",
                        receivingFacilityOID = "",
                        messageProfileId = "",
                        useBatchHeaders = format == Report.Format.HL7_BATCH,
                        reportingFacilityId = reportingFacilityId,
                        reportingFacilityName = reportingFacilityName,
                    )
                    // and create a report with a new destination
                    val reportWithTranslation = if (report.destination?.translation == null) {
                        val destination = Receiver(
                            "emptyReceiver",
                            "emptyOrganization",
                            Topic.COVID_19,
                            CustomerStatus.INACTIVE,
                            hl7Configuration
                        )
                        report.copy(destination, Report.Format.HL7_BATCH)
                    } else {
                        report
                    }
                    hl7Serializer.write(reportWithTranslation, stream)
                }
                Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, stream)
                else -> throw UnsupportedOperationException("Unsupported ${report.bodyFormat}")
            }
        }
    }
}