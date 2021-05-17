package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import gov.cdc.prime.router.DefaultValues
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
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
        help = "write output files to this directory instead of the working directory. Ignored if --output is set."
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

    private fun handleReadResult(result: ReadResult): Report {
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

    private fun readReportFromFile(
        metadata: Metadata,
        fileName: String
    ): Report {
        val schemaName = inputSchema?.toLowerCase() ?: ""
        val schema = metadata.findSchema(schemaName) ?: error("Schema $schemaName is not found")
        val file = File(fileName)
        if (!file.exists()) error("$fileName does not exist")
        echo("Opened: ${file.absolutePath}")
        return when (file.extension.toLowerCase()) {
            "hl7" -> {
                val hl7Serializer = Hl7Serializer(metadata)
                val result =  hl7Serializer.readExternal(
                    schemaName,
                    file.inputStream(),
                    FileSource(file.nameWithoutExtension)
                )
                handleReadResult(result)
            }
            else -> {
                val csvSerializer = CsvSerializer(metadata)
                return if (file.extension.toUpperCase() == "INTERNAL") {
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
                            FileSource(file.nameWithoutExtension)
                        )
                    handleReadResult(result)
                }
            }
        }
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

                    val fileName = Report.formFilename(
                        report.id,
                        report.schema.baseName,
                        format,
                        report.createdDateTime,
                        getNameFormat(Report.NameFormat.STANDARD),
                        receivingOrganization ?: report.destination?.translation?.receivingOrganization
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

    private fun getNameFormat(default: Report.NameFormat): Report.NameFormat {
        return if (nameFormat != null) Report.NameFormat.valueOf(nameFormat!!) else default
    }

    private fun getDefaultValues(): DefaultValues {
        val values = mutableMapOf<String, String>()
        receivingApplication?.let { values["receiving_application"] = it }
        receivingFacility?.let { values["receiving_facility"] = it }
        return values
    }

    override fun run() {
        // Load the schema and receivers
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        val fileSettings = FileSettings(FileSettings.defaultSettingsDirectory)
        val csvSerializer = CsvSerializer(metadata)
        val hl7Serializer = Hl7Serializer(metadata)
        val redoxSerializer = RedoxSerializer(metadata)
        echo("Loaded schema and receivers")
        // Gather input source
        var inputReport: Report = when (inputSource) {
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
        val warnings = mutableListOf<ResultDetail>()
        val outputReports: List<Pair<Report, Report.Format>> = when {
            route ->
                translator
                    .filterAndTranslateByReceiver(inputReport, getDefaultValues(), emptyList(), warnings)
                    .map { it.first to getOutputFormat(it.second.format) }
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
                echo("${it.scope} ${it.id}: ${it.details}")
            }
            echo()
        }

        // Output reports
        writeReportsToFile(outputReports) { report, format, stream ->
            val hl7Configuration = Hl7Configuration(
                nameFormat = getNameFormat(Report.NameFormat.STANDARD),
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
            when (format) {
                Report.Format.INTERNAL -> csvSerializer.writeInternal(report, stream)
                Report.Format.CSV -> csvSerializer.write(report, stream)
                Report.Format.HL7 -> hl7Serializer.write(report, stream, hl7Configuration)
                Report.Format.HL7_BATCH -> hl7Serializer.writeBatch(report, stream)
                Report.Format.REDOX -> redoxSerializer.write(report, stream)
            }
        }
    }
}