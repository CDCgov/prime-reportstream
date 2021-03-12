package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportFunction
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.impl.DSL
import java.io.File
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.time.OffsetDateTime
import kotlin.system.exitProcess

/**
 * Global variable to tell all our code if we're running a commandline test or not.
 * Used to turn off flyway database migrations when true.
 */
var isCommandLineTest: Boolean = false

class TestReportStream : CliktCommand(
    name = "test",
    help = """Run tests of the Router functions

Database connection info is supplied by environment variables.
Examples for localhost, and Azure Staging, respectively:
```
export POSTGRES_USER=prime
export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
export POSTGRES_PASSWORD=<secret>

export POSTGRES_USER=prime@pdhstaging-pgsql
export POSTGRES_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub
export POSTGRES_PASSWORD=<SECRET>
```
Examples:
```
 ./prime test --list        List detailed information about available tests
 ./prime test               Runs the set of tests labelled as '${TestStatus.GOODSTUFF.description}'
 ./prime test --run ping,end2end --env staging --key xxxxxxx       Runs the ping and end2end tests in azure Staging
```

""",
) {
    lateinit var db: DatabaseAccess

    enum class TestStatus(val description: String) {
        DRAFT("Experimental"), // Tests that are experimental
        FAILS("Always fails"), // For tests that just always fail, and we haven't fixed the issue yet.
        SLOW("Good test, but slow"), // For tests that are crazy slow
        GOODSTUFF("Part of Smoke test"), // Only Smoke the Good Stuff.
    }

    enum class AwesomeTest(
        val description: String,
        val status: TestStatus,
    ) {
        ping("CheckConnections: Is the reports endpoint alive and listening?", TestStatus.GOODSTUFF),
        end2end("Create Fake data, submit, wait, confirm sent via database lineage data", TestStatus.GOODSTUFF),
        merge("Submit multiple files, wait, confirm via db that merge occurred", TestStatus.GOODSTUFF),
        hl7null("The NULL transport does db work, but no transport.  Uses HL7 format", TestStatus.GOODSTUFF),
        toomanycols(
            "Submit a file with more than $REPORT_MAX_ITEM_COLUMNS columns, which should error",
            TestStatus.GOODSTUFF
        ),
        badcsv("Submit badly formatted csv files - should get errors", TestStatus.GOODSTUFF),
        stracbasic("Basic strac test to REDOX only.", TestStatus.GOODSTUFF),
        huge("Submit $REPORT_MAX_ITEMS line csv file, wait, confirm via db.  Slow.", TestStatus.SLOW),
        toobig("Submit ${REPORT_MAX_ITEMS + 1} lines, which should be an error.  Slower ;)", TestStatus.SLOW),
        dbconnections("Test weird issue wherein many 'sends' cause db connection failures", TestStatus.FAILS),
        strac("Submit data in the strac schema format, wait, confirm via database queries", TestStatus.FAILS),
        badsftp("Test ReportStream's response to sftp connection failures", TestStatus.DRAFT),
    }

    private val list by option(
        "--list",
        help = "List available tests, then quit."
    ).flag(default = false)

    private val run by option(
        "--run",
        metavar = "test1,test2",
        help = """Specify set of tests to run.   Default is to run all if not specified.
            Allowed tests are: ${AwesomeTest.values().joinToString(",")}
       """
    )

    private val env by option(
        "--env",
        help = "Specify local, test, staging, or prod.  'local' will connect to ${ReportStreamEnv.LOCAL.endPoint}," +
            " and 'test' will connect to ${ReportStreamEnv.TEST.endPoint}"
    ).choice("test", "local", "staging", "prod").default("local").validate {
        envSanityCheck()
        when (it) {
            "test" -> require(!key.isNullOrBlank()) { "Must specify --key <secret> to submit reports to --env test" }
            "staging" ->
                require(!key.isNullOrBlank()) { "Must specify --key <secret> to submit reports to --env staging" }
            "prod" -> error("Sorry, prod is not implemented yet")
        }
    }

    private val key by option(
        "--key",
        metavar = "<secret>",
        help = "Specify reports function access key"
    )

    private val dir by option(
        "--dir",
        help = "specify a working directory for generated files.  Default is $defaultWorkingDir"
    ).default(defaultWorkingDir)

    // Avoid accidentally connecting to the wrong database.
    private fun envSanityCheck() {
        val dbEnv = System.getenv("POSTGRES_URL") ?: error("Missing database env var. For help:  ./prime --help")
        val problem: Boolean = when (env) {
            "staging" -> !dbEnv.contains("pdhstaging")
            "test" -> !dbEnv.contains("pdhtest")
            "local" -> !dbEnv.contains("localhost")
            "prod" -> !dbEnv.contains("pdhprod")
            else -> true
        }
        if (problem) {
            echo("Error: --env is $env but database is set to $dbEnv")
            exitProcess(-1)
        }
    }

    override fun run() {
        DatabaseAccess.isFlywayMigrationOK = false
        if (list) {
            echo("Available options to --run <test1,test2> are:")
            doList(AwesomeTest.values().toList())
            exitProcess(0)
        }
        val environment = ReportStreamEnv.valueOf(env.toUpperCase())
        val tests = if (run != null) {
            run.toString().split(",").mapNotNull {
                try {
                    AwesomeTest.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    bad("Skipping unknown test: $it")
                    null
                }
            }
        } else {
            // No --run arg:  run the smoke tests.
            AwesomeTest.values().filter { it.status == TestStatus.GOODSTUFF }.toList()
        }
        ugly("Running the following tests, POSTing to ${environment.endPoint}:")
        doList(tests)
        doTests(tests, environment)
    }

    private fun doList(tests: List<AwesomeTest>) {
        val formatTemplate = "%-20s%-20s\t%s"
        tests.forEach {
            echo(formatTemplate.format(it.name, it.status.description, it.description))
        }
    }

    private fun doTests(tests: List<AwesomeTest>, environment: ReportStreamEnv) {
        tests.forEach { test ->
            when (test) {
                AwesomeTest.ping -> doPingTest(environment)
                AwesomeTest.end2end -> doEnd2EndTest(environment)
                AwesomeTest.merge -> doMergeTest(environment)
                AwesomeTest.hl7null -> doHL7NullTest(environment)
                AwesomeTest.toomanycols -> doTooManyColsTest(environment)
                AwesomeTest.badcsv -> doBadCsvTest(environment)
                AwesomeTest.stracbasic -> doStracBasicTest(environment)
                AwesomeTest.huge -> doHugeTest(environment)
                AwesomeTest.toobig -> doTooBigTest(environment)
                AwesomeTest.dbconnections -> doDbConnectionsTest(environment)
                AwesomeTest.strac -> doStracTest(environment)
                AwesomeTest.badsftp -> doBadSftpTest(environment)
                else -> bad("Test $test not implemented")
            }
        }
    }

    // ping
    private fun doPingTest(
        environment: ReportStreamEnv
    ) {
        ugly("Starting ping Test: run CheckConnections of ${environment.endPoint}")
        val (responseCode, json) = HttpUtilities.postReportBytes(
            environment,
            "x".toByteArray(),
            orgName,
            simpleReportSenderName,
            key,
            ReportFunction.Options.CheckConnections
        )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_OK) {
            echo("Ping/CheckConnections Test FAILED:  response code $responseCode")
            exitProcess(-1) // other tests won't work.
        }
        try {
            val tree = jacksonObjectMapper().readTree(json)
            if (tree["errorCount"].intValue() != 0 || tree["warningCount"].intValue() != 0) {
                bad("***Ping/CheckConnections Test FAILED***")
            } else {
                good("Test passed: Ping/CheckConnections")
            }
        } catch (e: NullPointerException) {
            bad("***Ping/CheckConnections FAILED***: Unable to properly parse response json")
        }
    }

    // end2end
    private fun doEnd2EndTest(
        environment: ReportStreamEnv
    ) {
        ugly("Starting ${AwesomeTest.end2end} Test: send ${simpleRepSender.fullName} data to $allGoodCounties")
        val fakeItemCount = allGoodReceivers.size * 5 // 5 to each receiver
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***end2end Test FAILED***:  response code $responseCode")
            return
        }
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            waitABit(25, environment)
            examineLineageResults(reportId, allGoodReceivers, fakeItemCount)
        } catch (e: NullPointerException) {
            bad("***end2end Test FAILED***: Unable to properly parse response json")
        }
    }

    private fun doMergeTest(environment: ReportStreamEnv) {
        val fakeItemCount = allGoodReceivers.size * 5 // 5 to each receiver
        val numResends = 5
        ugly("Starting merge test:  Merge $numResends reports, each of which sends to $allGoodCounties")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub over and over
        val reportIds = (1..numResends).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
            echo("Response to POST: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                bad("***Merge Test FAILED***:  response code $responseCode")
                return
            }
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            reportId
        }
        waitABit(30, environment)
        examineMergeResults(reportIds[0], allGoodReceivers, fakeItemCount, numResends)
    }

    /**
     * Test weirdness in Staging wherein we have strange HL7 'send' numbers
     *
     * This test, when it fails, exposes a database connection exception in Staging.
     *
     */
    private fun doDbConnectionsTest(environment: ReportStreamEnv) {
        val fakeItemCount = 40
        ugly("Starting dbconnections Test: test of many threads attempting to sftp $fakeItemCount HL7s.")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            "HL7",
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.   Make numResends > 1 to create merges.
        val numResends = 1
        val reportIds = (1..numResends).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
            echo("Response to POST: $responseCode")
            echo(json)
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                bad("***dbconnections Test FAILED***:  response code $responseCode")
                return
            }
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            reportId
        }
        waitABit(30, environment)
        examineMergeResults(reportIds[0], listOf(hl7Receiver), fakeItemCount, numResends)
    }

    /**
     * Test using the NULL transport.
     */
    private fun doHL7NullTest(environment: ReportStreamEnv) {
        val fakeItemCount = 40
        ugly("Starting hl7null Test: test of many threads all doing database interactions, but no sends. ")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            "HL7_NULL",
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.   Make numResends > 1 to create merges.
        val numResends = 1
        val reportIds = (1..numResends).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
            echo("Response to POST: $responseCode")
            echo(json)
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                bad("***hl7null Test FAILED***:  response code $responseCode")
                return
            }
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            reportId
        }
        waitABit(30, environment)
        examineMergeResults(reportIds[0], listOf(hl7NullReceiver), fakeItemCount, numResends)
    }

    /**
     * Test using the a receiver with a broken sftp site.  Note:  there are two failure modes here:
     * - if the sftp auth stuff is set up, then RS get repeated IOException, and multiple retries.
     * - if its NOT set up, then RS fails without retries.
     *
     * Either way, the lineage results for the 'send' step should be zero.
     */
    private fun doBadSftpTest(environment: ReportStreamEnv) {
        val fakeItemCount = 40
        ugly("Starting badsftp Test: test that our code handles sftp connectivity problems")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            sftpFailReceiver.name,
            dir,
        )
        echo("Created datafile $file")
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***badsftp Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        examineLineageResults(reportId, listOf(sftpFailReceiver), fakeItemCount)
    }

    private fun doHugeTest(environment: ReportStreamEnv) {
        val fakeItemCount = 10000
        ugly("Starting huge Test: Attempting to send a report with $fakeItemCount items. This is terrapin slow.")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            csvReceiver.name,
            dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***Huge Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        examineLineageResults(reportId, listOf(csvReceiver), fakeItemCount)
    }

    private fun doTooBigTest(environment: ReportStreamEnv) {
        val fakeItemCount = REPORT_MAX_ITEMS + 1
        ugly("Starting toobig test: Attempting to send a report with $fakeItemCount items. This is slllooooowww.")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            csvReceiver.name,
            dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = ((tree["errors"] as ArrayNode)[0]) as ObjectNode
            if (firstError["details"].textValue().contains("rows")) {
                good("toobig Test passed.")
            } else {
                bad("***toobig Test Test FAILED***: Did not find the error")
            }
        } catch (e: Exception) {
            bad("***toobig Test FAILED***: Unable to parse json response")
        }
    }

    private fun doTooManyColsTest(
        environment: ReportStreamEnv
    ) {
        ugly("Starting toomanycols Test: submitting a file with too many columns.")
        val file = File("./src/test/csv_test_files/input/too-many-columns.csv")
        if (!file.exists()) {
            error("Unable to find file ${file.absolutePath} to do toomanycols test")
        }
        val (responseCode, json) = HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = ((tree["errors"] as ArrayNode)[0]) as ObjectNode
            if (firstError["details"].textValue().contains("columns")) {
                good("toomanycols Test passed.")
            } else {
                bad("***toomanycols Test FAILED***:  did not find the error.")
            }
        } catch (e: Exception) {
            bad("***toomanycols Test FAILED***: Unable to parse json response")
        }
    }

    private fun doBadCsvTest(environment: ReportStreamEnv) {
        val filenames = listOf("not-a-csv-file.csv", /* "column-headers-only.csv", */ "completely-empty-file.csv")
        filenames.forEachIndexed { i, filename ->
            ugly("Starting badcsv file Test $i: submitting $filename")
            val file = File("./src/test/csv_test_files/input/$filename")
            if (!file.exists()) {
                error("Unable to find file ${file.absolutePath} to do badcsv test")
            }
            val (responseCode, json) = HttpUtilities.postReportFile(
                environment,
                file,
                org.name,
                simpleRepSender.name,
                key
            )
            echo("Response to POST: $responseCode")
            echo(json)
            if (responseCode >= 400) {
                good("badcsv Test $i of $filename passed: Failure HttpStatus code was returned.")
            } else {
                bad("***badcsv Test $i of $filename FAILED: Expecting a failure HttpStatus. ***")
            }
            try {
                val tree = jacksonObjectMapper().readTree(json)
                if (tree["id"] == null || tree["id"].isNull) {
                    good("badcsv Test $i of $filename passed: No UUID was returned.")
                } else {
                    bad("***badcsv Test $i of $filename FAILED: RS returned a valid UUID for a bad CSV. ***")
                }
                if (tree["errorCount"].intValue() > 0) {
                    good("badcsv Test $i of $filename passed: At least one error was returned.")
                } else {
                    bad("***badcsv Test $i of $filename FAILED: No error***")
                }
            } catch (e: Exception) {
                bad("***badcsv Test $i of $filename FAILED***: Unexpected json returned")
            }
        }
    }

    // Strac format (only) to Redox format (only)
    private fun doStracBasicTest(
        environment: ReportStreamEnv
    ) {
        val fakeItemCount = 100
        ugly("Starting stracbasic Test: sending Strac data to the ${redoxReceiver.name} receiver only.")
        val file = FileUtilities.createFakeFile(
            metadata,
            stracSender,
            fakeItemCount,
            receivingStates,
            redoxReceiver.name,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = HttpUtilities.postReportFile(environment, file, org.name, stracSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***StracBasic Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(25, environment)
        examineLineageResults(reportId, listOf(redoxReceiver), fakeItemCount)
    }

    // strac format to all output formats
    private fun doStracTest(
        environment: ReportStreamEnv
    ) {
        ugly("Starting bigly strac Test: sending Strac data to all of these receivers: $allGoodCounties!")
        val fakeItemCount = allGoodReceivers.size * 5 // 5 to each receiver
        val file = FileUtilities.createFakeFile(
            metadata,
            stracSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = HttpUtilities.postReportFile(environment, file, org.name, stracSender.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("**Strac Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(25, environment)
        examineLineageResults(reportId, allGoodReceivers, fakeItemCount)
    }

    fun examineLineageResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        totalRecords: Int,
    ) {
        db = WorkflowEngine().db
        db.transact { txn ->
            val expected = totalRecords / receivers.size
            val actionsList = listOf(TaskAction.receive, TaskAction.batch, TaskAction.send)
            receivers.forEach { receiver ->
                actionsList.forEach { action ->
                    val count = itemLineageCountQuery(txn, reportId, receiver.name, action)
                    if (count == null || expected != count) {
                        bad(
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records but got $count"
                        )
                    } else {
                        good(
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records and got $count"
                        )
                    }
                }
            }
        }
    }

    private fun examineMergeResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        itemsPerReport: Int,
        reportCount: Int
    ) {
        db = WorkflowEngine().db
        db.transact { txn ->
            val expected = (itemsPerReport * reportCount) / receivers.size
            val actionsList = listOf(TaskAction.batch, TaskAction.send)
            receivers.forEach { receiver ->
                actionsList.forEach { action ->
                    val count = reportLineageCountQuery(txn, reportId, receiver.name, action)
                    if (count == null || expected != count) {
                        bad(
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting sum(itemCount)=$expected but got $count"
                        )
                    } else {
                        good(
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting sum(itemCount)=$expected and got $count"
                        )
                    }
                }
            }
        }
    }

    companion object {
        val defaultWorkingDir = "./target/csv_test_files"
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)

        // Here is test setup of organization, senders, and receivers.   All static.
        val orgName = "ignore"
        val org = settings.findOrganization(orgName)
            ?: error("Unable to find org $orgName in metadata")
        val receivingStates = "IG"

        val simpleReportSenderName = "ignore-simple-report"
        val simpleRepSender = settings.findSender("$orgName.$simpleReportSenderName")
            ?: error("Unable to find sender $simpleReportSenderName for organization ${org.name}")

        val stracSenderName = "ignore-strac"
        val stracSender = settings.findSender("$orgName.$stracSenderName")
            ?: error("Unable to find sender $stracSenderName for organization ${org.name}")

        val allGoodReceivers = settings.receivers.filter {
            it.organizationName == orgName && !it.name.contains("FAIL")
        }
        val csvReceiver = allGoodReceivers.filter { it.name == "CSV" }[0]
        val hl7Receiver = allGoodReceivers.filter { it.name == "HL7" }[0]
        val hl7BatchReceiver = allGoodReceivers.filter { it.name == "HL7_BATCH" }[0]
        val redoxReceiver = allGoodReceivers.filter { it.name == "REDOX" }[0]
        val hl7NullReceiver = allGoodReceivers.filter { it.name == "HL7_NULL" }[0]
        val allGoodCounties = allGoodReceivers.map { it.name }.joinToString(",")
        val sftpFailReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "SFTP_FAIL"
        }[0]

        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_BLACK = "\u001B[30m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_BLUE = "\u001B[34m"
        const val ANSI_CYAN = "\u001B[36m"
        fun good(msg: String) {
            echo(ANSI_GREEN + msg + ANSI_RESET)
        }
        fun bad(msg: String) {
            echo(ANSI_RED + msg + ANSI_RESET)
        }
        fun ugly(msg: String) {
            echo(ANSI_CYAN + msg + ANSI_RESET)
        }

        /**
         * A hack attempt to wait enough time, but not too long, for Hub to finish.
         * This assumes the batch/send executes on the minute boundary.
         */
        fun waitABit(plusSecs: Int, env: ReportStreamEnv) {
            // seconds elapsed so far in this minute
            val secsElapsed = OffsetDateTime.now().second % 60
            // Wait until the top of the next minute, and pluSecs more, for 'batch', and 'send' to finish.
            var waitSecs = 60 - secsElapsed + plusSecs
            if (secsElapsed > (60 - plusSecs) || env != ReportStreamEnv.LOCAL) {
                // Uh oh, we are close to the top of the minute *now*, so 'receive' might not finish in time.
                // Or, we are in Test or Staging, which don't execute on the top of the minute.
                waitSecs += 60
            }
            echo("Waiting $waitSecs seconds for the Hub to fully receive, batch, and send the data")
            for (i in 1..waitSecs) {
                sleep(1000)
                print(".")
            }
            println()
        }

        fun itemLineageCountQuery(
            txn: DataAccessTransaction,
            reportId: ReportId,
            receivingOrgSvc: String,
            action: TaskAction,
        ): Int? {
            val ctx = DSL.using(txn)
            val sql = """select count(*)
              from item_lineage as IL
              join report_file as RF on IL.child_report_id = RF.report_id
              join action as A on A.action_id = RF.action_id
              where RF.receiving_org_svc = ? 
              and A.action_name = ? 
              and IL.item_lineage_id in 
              (select item_descendants(?)) """
            return ctx.fetchOne(sql, receivingOrgSvc, action, reportId)?.into(Int::class.java)
        }

        fun reportLineageCountQuery(
            txn: DataAccessTransaction,
            reportId: ReportId,
            receivingOrgSvc: String,
            action: TaskAction,
        ): Int? {
            val ctx = DSL.using(txn)
            val sql = """select sum(item_count)
              from report_file as RF
              join action as A on A.action_id = RF.action_id
              where RF.receiving_org_svc = ? 
              and A.action_name = ? 
              and RF.report_id in
              (select report_descendants(?)) """
            return ctx.fetchOne(sql, receivingOrgSvc, action, reportId)?.into(Int::class.java)
        }
    }
}

/**
 * This was my last attempt to jooqify the above itemLineage query.
 *  Remaining issue is the call to (select from item_descendants(report_id) part.
 *  I'm sure there's a correct jooq way to call a stored fn that returns a set, but couldn't find it.
 *  Could also treat just that part as "plain SQL", but I didn't see the difference
 *  between doing that, and treating the entire thing as plain sql, as I did above.
val nested = ctx.select().from(itemDescendants(reportId)).asTable("nested")
val count = ctx.select(count())
 .from(ITEM_LINEAGE)
 .join(REPORT_FILE).on(REPORT_FILE.REPORT_ID.eq(ITEM_LINEAGE.CHILD_REPORT_ID))
 .join(ACTION).on(ACTION.ACTION_ID.eq(REPORT_FILE.ACTION_ID))
 .join(nested).on(ITEM_LINEAGE.ITEM_LINEAGE_ID.eq(nested))
 .where(REPORT_FILE.RECEIVING_ORG_SVC.eq(receivingOrgSvc)
 .and(ACTION.ACTION_NAME.eq(action)))
 .groupBy(REPORT_FILE.REPORT_ID, REPORT_FILE.RECEIVING_ORG,
 REPORT_FILE.RECEIVING_ORG_SVC, ACTION.ACTION_NAME).fetchOne().into(Int)
*/