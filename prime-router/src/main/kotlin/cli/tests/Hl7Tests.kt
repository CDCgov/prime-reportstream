// ktlint-disable filename
package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.output.TermUi
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.cli.CoolTest
import gov.cdc.prime.router.cli.CoolTestOptions
import gov.cdc.prime.router.cli.TestStatus
import java.io.File
import java.net.HttpURLConnection

const val SFTP_DIR = "build/sftp"

/**
 * Generates a fake HL7 report and sends it to ReportStream, waits some time then checks the lineage to make
 * sure the data was sent to the configured senders.
 */
class Hl7Ingest : CoolTest() {
    override val name = "hl7ingest"
    override val description = "Create HL7 Fake data, submit, wait, confirm sent via database lineage data"
    override val status = TestStatus.SMOKE

    private val testDataDir = "/clitests/hl7ingest"
    private val testConfigFile = "hl7ingest-test-config.csv"

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        val config = readTestConfig("$testDataDir/$testConfigFile")
        if (config.isNotEmpty()) {
            config.forEach { testInput ->
                var passed = true
                val sender = settings.findSender(testInput.key.sender)
                    ?: error("Unable to find sender ${testInput.key.sender}")
                var totalItemCount = 0
                testInput.value.forEach { totalItemCount += it.expectedCount }
                val receivers = testInput.value.map { testOutput ->
                    val recParts = testOutput.receiver.split(".")
                    if(recParts.size == 2) {
                        // We expect only one receiver back
                        settings.receivers.filter { receiver ->
                            receiver.organizationName == recParts[0] && receiver.name == recParts[1]
                        }[0]
                    } else {
                        error("Config file has invalid receiver name ${testOutput.receiver}.  Must be <org>.<name>.")
                    }
                }

                val inputFilePath = this.javaClass.getResource("$testDataDir/${testInput.key.inputFile}")?.path
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

                    //Check the response from the endpoint
                    TermUi.echo(json)
                    var reportId: ReportId? = null
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

                        if (destCount != null && !destCount.isNull && destCount.intValue() == receivers.size) {
                            good("Data going to be sent to ${receivers.size} destinations.")
                        } else {
                            bad("***$name Test FAILED***: Incorrect number of destinations.  " +
                                "Should be ${receivers.size}")
                            passed = false
                        }
                    } catch (e: NullPointerException) {
                        passed = bad("***$name Test FAILED***: Unable to properly parse response json")
                    }

                    if (reportId != null) {
                        // Look at the lineage results
                        waitABit(25, environment)
                        passed = passed and examineLineageResults(reportId, receivers, totalItemCount)
                    }

                } else {
                    error("***$name Test FAILED***: Input file $inputFilePath not found.")
                }

                testInput.value.forEach { testOutput ->
                }
            }
        }
        return true

//        compareSentReports(
//            UUID.fromString("6e3b51a8-b47b-47bb-88a5-531cc0ab2b71"), receivers, File(".\\build\\csv_test_files\\hl7-ingest-covid-19-54dd901a-3146-48b0-8e05-3e6f7698e97e-20210623160002.hl7"),
//            Report.Format.HL7_BATCH, sender.schemaName
//        )


//
//            // Now check the lineage data
//

//            // CompareData().compare(inputFile,)

//        return passed
    }

    data class TestInput(val inputFile: String, val sender: String)
    data class TestOutput(val outputFile: String, val receiver: String, val expectedCount: Int)
    enum class ConfigColumns(val colName: String) {
        INPUT_FILE("Input File"),
        OUTPUT_FILE("Output File"),
        SENDER("Sender"),
        RECEIVER("Receiver"),
        EXPECTED_COUNT("Expected count")
    }

    private fun readTestConfig(configPathname: String): Map<TestInput, List<TestOutput>> {
        val config = mutableMapOf<TestInput, MutableList<TestOutput>>()
        val resourcePath = this.javaClass.getResource(configPathname)?.path
        if (!resourcePath.isNullOrBlank()) {
            csvReader().readAllWithHeader(File(resourcePath)).forEach {
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.SENDER.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.RECEIVER.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_COUNT.colName].isNullOrBlank()
                ) {
                    val input = TestInput(it[ConfigColumns.INPUT_FILE.colName]!!, it[ConfigColumns.SENDER.colName]!!)
                    val output = TestOutput(
                        it[ConfigColumns.OUTPUT_FILE.colName]!!,
                        it[ConfigColumns.RECEIVER.colName]!!,
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
        receivers: List<Receiver>,
        inputFile: File,
        inputFormat: Report.Format,
        inputSchema: String
    ) {
        db = WorkflowEngine().db
        db.transact { txn ->
            receivers.forEach { receiver ->
                val outputReport = sftpFileQuery(txn, reportId, receiver.name)
                if (outputReport != null) {
                    CompareData().compare(
                        inputFile, inputFormat, inputSchema, File(SFTP_DIR, outputReport.externalName),
                        outputReport.bodyFormat, outputReport.schemaName
                    )
                }
                println()
            }
        }
    }
}