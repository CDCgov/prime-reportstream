package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.output.TermUi
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.cli.CoolTest
import gov.cdc.prime.router.cli.CoolTestOptions
import gov.cdc.prime.router.cli.TestStatus
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Generates a fake HL7 report and sends it to ReportStream, waits some time then checks the lineage to make
 * sure the data was sent to the configured senders.
 */
class DataCompareTest : CoolTest() {
    override val name = "compare"
    override val description = "Send data to API based on config file, check lineage and compare data against expected"
    override val status = TestStatus.DRAFT

    private val testDataDir = "/clitests/hl7ingest"
    private val testConfigFile = "hl7ingest-test-config.csv"

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        var passed = true
        val configs = readTestConfig("$testDataDir/$testConfigFile")
        if (configs.isNotEmpty()) {
            configs.forEach { config ->
                val input = config.key
                val outputList = config.value
                val sender = settings.findSender(input.sender)
                    ?: error("Unable to find sender ${input.sender}")
                val receivers = outputList.map { testOutput -> // TODO do we need receivers
                    // We expect only one receiver back
                    val rec = settings.receivers.filter { receiver ->
                        receiver.organizationName == testOutput.orgName && receiver.name == testOutput.receiverName
                    }
                    if (rec.isNotEmpty()) {
                        if (rec[0].format == Report.Format.HL7_BATCH || rec[0].format == Report.Format.CSV) {
                            testOutput.receiver = rec[0]
                            rec[0]
                        } else {
                            error("This test only supports receivers with CSV and HL7_BATCH formats.")
                        }
                    } else {
                        error("Receiver not found")
                    }
                }

                var reportId: ReportId? = null

                val inputFilePath = this.javaClass.getResource("$testDataDir/${config.key.inputFile}")?.path
                if (!inputFilePath.isNullOrBlank()) {
                    // Now send it to ReportStream.
                    val (responseCode, json) =
                        HttpUtilities.postReportFile(environment, File(inputFilePath), sender, options.key)
                    if (responseCode != HttpURLConnection.HTTP_CREATED) {
                        bad("***$name Test FAILED***:  response code $responseCode")
                        passed = false
                    } else {
                        good("Posting of report succeeded with response code $responseCode")
                    }

                    // Check the response from the endpoint
                    TermUi.echo(json)
                    try {
                        val tree = jacksonObjectMapper().readTree(json)
                        reportId = ReportId.fromString(tree["id"].textValue())
                        TermUi.echo("Id of submitted report: $reportId")
                        val topic = tree["topic"]
                        val errorCount = tree["errorCount"]
                        val destCount = tree["destinationCount"]

                        if (topic != null && !topic.isNull && topic.textValue().equals("covid-19", true)) {
                            good("'topic' is in response and correctly set to 'covid-19'")
                        } else {
                            bad("***$name Test FAILED***: 'topic' is missing from response json")
                            passed = false
                        }

                        if (errorCount != null && !errorCount.isNull && errorCount.intValue() == 0) {
                            good("No errors detected.")
                        } else {
                            bad("***$name Test FAILED***: There were errors reported.")
                            passed = false
                        }

                        if (destCount != null && !destCount.isNull && destCount.intValue() >= receivers.size) {
                            good("Data going to be sent to at least ${receivers.size} destinations.")
                        } else {
                            bad(
                                "***$name Test FAILED***: Incorrect number of destinations.  " +
                                    "Should be ${receivers.size}"
                            )
                            passed = false
                        }
                    } catch (e: NullPointerException) {
                        passed = bad("***$name Test FAILED***: Unable to properly parse response json")
                    }
                } else {
                    error("***$name Test FAILED***: Input file $inputFilePath not found.")
                }

                if (reportId != null) {
                    // Look at the lineage results
                    waitABit(25, environment)
                    var totalItemCount = 0
                    outputList.forEach { totalItemCount += it.expectedCount }
                    passed = passed and examineLineageResults(reportId, receivers, totalItemCount)

                    outputList.forEach { output ->
                        passed = passed and compareSentReports(
                            reportId,
                            output,
                            options.sftpDir
                        )
                    }
                }
            }
        }
        return passed
    }

    data class TestInput(val inputFile: String, val sender: String)
    data class TestOutput(
        val outputFile: String,
        val orgName: String,
        val receiverName: String,
        val expectedCount: Int,
        var receiver: Receiver? = null
    )
    enum class ConfigColumns(val colName: String) {
        INPUT_FILE("Input File"),
        OUTPUT_FILE("Output File"),
        SENDER("Sender"),
        ORG_NAME("Organization name"),
        RECEIVER_NAME("Receiver name"),
        EXPECTED_COUNT("Expected count")
    }

    private fun readTestConfig(configPathname: String): Map<TestInput, List<TestOutput>> {
        val config = mutableMapOf<TestInput, MutableList<TestOutput>>()
        val resourcePath = this.javaClass.getResource(configPathname)?.path
        if (!resourcePath.isNullOrBlank()) {
            csvReader().readAllWithHeader(File(resourcePath)).forEach {
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.SENDER.colName].isNullOrBlank() &&
                    !it[ConfigColumns.ORG_NAME.colName].isNullOrBlank() &&
                    !it[ConfigColumns.RECEIVER_NAME.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_COUNT.colName].isNullOrBlank()
                ) {
                    val input = TestInput(
                        it[ConfigColumns.INPUT_FILE.colName]!!.trim(),
                        it[ConfigColumns.SENDER.colName]!!.trim()
                    )
                    val output = TestOutput(
                        it[ConfigColumns.OUTPUT_FILE.colName]?.trim() ?: "",
                        it[ConfigColumns.ORG_NAME.colName]!!.trim(),
                        it[ConfigColumns.RECEIVER_NAME.colName]!!.trim(),
                        Integer.parseInt(it[ConfigColumns.EXPECTED_COUNT.colName]!!)
                    )
                    if (config.containsKey(input)) {
                        config[input]!!.add(output)
                    } else {
                        config[input] = mutableListOf(output)
                    }
                } else {
                    error("***$name Test FAILED***: One or more columns in config file are empty.")
                }
            }
        }
        return config
    }

    fun compareSentReports(
        reportId: ReportId,
        output: TestOutput,
        sftpDir: String
    ): Boolean {
        var passed = true
        db = WorkflowEngine().db
        db.transact { txn ->
            val expectedOutputPath = this.javaClass.getResource("$testDataDir/${output.outputFile}")?.path
            val outputFilename = sftpFilenameQuery(txn, reportId, output.receiver!!.name)
            val schema = metadata.findSchema(output.receiver!!.schemaName)
            if (outputFilename != null && !expectedOutputPath.isNullOrBlank() && schema != null) {
                TermUi.echo("Comparing expected data from $expectedOutputPath")
                TermUi.echo("with actual data from $sftpDir/$outputFilename")
                TermUi.echo("using schema ${schema.name}...")
                val result = CompareData().compare(
                    File(expectedOutputPath), File(sftpDir, outputFilename),
                    output.receiver!!.format, schema
                )
                if (result.isEqual) {
                    good("Data compares SUCCESSFULLY")
                } else {
                    bad("Comparison was NOT successful")
                }
                if (result.errors.size > 0) bad(result.errors.joinToString("\n", "ERROR: "))
                if (result.warnings.size > 0) TermUi.echo(result.warnings.joinToString("\n", "WARNING: "))
                passed = passed and result.isEqual
            }
        }
        return passed
    }
}

class CompareData {
    data class Result(
        var isEqual: Boolean = false,
        val errors: ArrayList<String> = ArrayList<String>(),
        val warnings: ArrayList<String> = ArrayList<String>()
    ) {
        val hasErrors: Boolean
            get() = errors.isNotEmpty()
    }

    private val result = Result()

    fun compare(
        expected: File,
        actual: File,
        format: Report.Format,
        schema: Schema
    ): Result {
        fun checkFile(file: File) {
            if (!file.canRead()) {
                result.errors.add("Unable to read ${file.absolutePath}")
            }
        }

        checkFile(expected)
        checkFile(actual)
        if (!result.hasErrors) {
            result.isEqual = compare(
                expected.inputStream(), actual.inputStream(),
                format, schema
            )
        }
        return result
    }

    fun compare(
        expected: InputStream,
        actual: InputStream,
        format: Report.Format,
        schema: Schema
    ): Boolean {
        var passed = false
        if (format == Report.Format.HL7 || format == Report.Format.HL7_BATCH) {
            // passed = CompareHl7(expected, actual, schema)
        } else {
            passed = compareCSV(expected, actual, schema)
        }
        return passed
    }

    /**
     * Compare the data in the [actual] report to the data in the [expected] report.  This
     * comparison uses the column names in the expected data to match it to the proper actual data,
     * hence the order of columns in the expected data is not important.
     * Errors are generated when:
     *  1. The number of reports is different
     *  2. A column in the expected values does not exist in the actual values
     *  3. A expected value does not match the actual value
     *
     * Warnings are generated when:
     *  1. An actual value exists, but no expected value.
     *  2. There are more columns in the actual data than the expected data
     *
     */
    private fun compareCSV(expected: InputStream, actual: InputStream, schema: Schema): Boolean {

        val expectedRows = csvReader().readAll(expected)
        val actualRows = csvReader().readAll(actual)
        val schemaMsgIdIndex = schema.findElementColumn("message_id")
        val schemaPatLastNameIndex = schema.findElementColumn("patient_last_name")
        val schemaPatStateIndex = schema.findElementColumn("patient_state")

        // Sanity check.  The schema need either the message ID, or patient last name and state.
        if (schemaMsgIdIndex == null && (schemaPatLastNameIndex == null || schemaPatStateIndex == null)) {
            error("Schema ${schema.name} needs to have message ID or (patient last name and state) for the test.")
        }

        // Check that we have the same number of records
        if (expectedRows.size == actualRows.size) {
            // Loop through all the actual rows ignoring the header row
            for (i in 1 until actualRows.size) {
                val actualRow = actualRows[i]
                val actualMsgId = if (schemaMsgIdIndex != null) actualRow[schemaMsgIdIndex].trim() else null
                val actualLastName = if (schemaPatLastNameIndex != null)
                    actualRow[schemaPatLastNameIndex].trim() else null
                val actualPatState = if (schemaPatStateIndex != null)
                    actualRow[schemaPatStateIndex].trim() else null

                // Find the expected row that matches the actual record
                val expectedRowRaw = expectedRows.filter {
                    schemaMsgIdIndex != null && it[schemaMsgIdIndex] == actualMsgId ||
                        (
                            schemaPatLastNameIndex != null && schemaPatStateIndex != null &&
                                it[schemaPatLastNameIndex] == actualLastName &&
                                it[schemaPatStateIndex] == actualPatState
                            )
                }
                if (expectedRowRaw.size == 1) {
                    if (!compareCsvRow(actualRow, expectedRowRaw[0], schema, i)) {
                        result.errors.add(" Row not equal")
                    }
                } else {
                    result.errors.add(" Cannot find expected")
                }
            }
        } else {
            result.errors.add(
                "Number of records does not match.  Expected ${expectedRows.size - 1} " +
                    "but got ${actualRows.size - 1}"
            )
        }

        return !result.hasErrors
    }

    fun compareCsvRow(actualRow: List<String>, expectedRow: List<String>, schema: Schema, actualRowNum: Int): Boolean {
        var isEqual = true
        if (actualRow.size < expectedRow.size) {
            result.errors.add(
                "Too few cols"
            )
        } else {
            if (actualRow.size > expectedRow.size) {
                result.warnings.add(
                    "Actual has more cols"
                )
            }

            // Loop through all the expected columns ignoring the header row
            for (j in 1 until expectedRow.size) {
                val colName = schema.elements[j].name
                // We want to error on differences when the expected data is not empty.
                if (expectedRow[j].isNotBlank() &&
                    actualRow[j].trim() != expectedRow[j].trim()
                ) {
                    result.errors.add(
                        "Data value does not match in report $actualRowNum column #${j + 1}, " +
                            "'$colName'. Expected: '${expectedRow[j].trim()}', " +
                            "Actual: '${actualRow[j].trim()}'"
                    )
                    isEqual = false
                } else if (expectedRow[j].trim().isEmpty() &&
                    actualRow[j].trim().isNotEmpty()
                ) {
                    result.warnings.add(
                        "Actual data has value in report $actualRowNum for column " +
                            "'$colName' - column #${j + 1}, but no expected value.  " +
                            "Actual: '${actualRow[j].trim()}'"
                    )
                }
            }
        }
        return isEqual
    }
}