package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.base.CharMatcher
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.REPORT_FILE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.common.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import java.io.File
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

enum class TestStatus(val description: String) {
    DRAFT("Experimental Test"), // Tests that are experimental
    FAILS("(Always fails)"), // For tests that just always fail, and we haven't fixed the issue yet.
    LOAD("Load Test"),
    SMOKE("Smoke Test"), // Only Smoke the Good Stuff.
}

class TestReportStream : CliktCommand(
    name = "test",
    help = """Run tests of the Router functions

Database connection info is supplied by environment variables.
Examples for local host, and Azure Staging, respectively:
```
# "local"
export POSTGRES_USER=prime
export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
export POSTGRES_PASSWORD=<secret>

# "docker"
export POSTGRES_USER=prime
export POSTGRES_URL=jdbc:postgresql://postgresql:5432/prime_data_hub
export POSTGRES_PASSWORD=<secret>

# staging
export POSTGRES_USER=prime@pdhstaging-pgsql
export POSTGRES_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub
export POSTGRES_PASSWORD=<SECRET>
```
Examples:
```
 ./prime test --list        List detailed information about available tests
 ./prime test               Runs the set of tests labelled as '${TestStatus.SMOKE.description}'
 ./prime test --run ping,end2end --env staging --key xxxxxxx       Runs the ping and end2end tests in azure Staging
```

""",
) {
    val defaultWorkingDir = "./build/csv_test_files"

    /**
     * The local folder used by the dev Docker instance to save files uploaded to the SFTP server
     */
    val SFTP_DIR = "build/sftp"

    private val list by option(
        "--list",
        help = "List available tests, then quit."
    ).flag(default = false)

    private val sender by option(
        "--sender",
        help = "Indicates the sender to use for the 'santaclaus' test."
    )

    private val targetStates: String? by
    option(
        "--target-states",
        metavar = "<abbrev>",
        help = "For the 'santaclaus' test, create data only for these states. " +
            "States should be two letters, comma-separated, e.g. 'FL,PA'. " +
            "  Default is all states and territories."
    )

    private val run by option(
        "--run",
        metavar = "test1,test2",
        help = """Specify names of tests to run.   Default is to run all smoke tests if not specified.
            Use --list to see a list of all the tests.
       """
    )
    private val itemsDefault: Int = 5
    private val items by option(
        "--items",
        metavar = "<int>",
        help = "For tests involving fake data, 'items' rows **per receiver getting data**. " +
            "Default is $itemsDefault"
    ).int().default(itemsDefault)

    private val submitsDefault: Int = 5
    private val submits by option(
        "--submits",
        metavar = "<int>",
        help = "For tests involving multiple submits, do this many submissions. " +
            "Default is $submitsDefault"
    ).int().default(itemsDefault)

    private val env by option(
        "--env",
        help = "Specify the environment to connect to"
    ).choice("test", "local", "staging").default("local").validate {
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

    private val sftpDir by option(
        "--sftpdir",
        help = "specify the folder where files were uploaded to the SFTP server.  Default is $SFTP_DIR"
    ).default(SFTP_DIR)

    private val runSequential by option(
        "--sequential",
        help = "Run tests one at a time."
    ).flag(default = false)

    private val asyncProcessMode by option(
        "--async",
        help = "Use async processing when sending data"
    ).flag(default = false)

    // Avoid accidentally connecting to the wrong database.
    private fun envSanityCheck() {
        val dbEnv = System.getenv("POSTGRES_URL") ?: error("Missing database env var. For help:  ./prime --help")
        val problem: Boolean = when (env) {
            "staging" -> !dbEnv.contains("pdhstaging")
            "test" -> !dbEnv.contains("pdhtest")
            "local" -> !(dbEnv.contains("postgresql:5432") || dbEnv.contains("localhost"))
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

        // Create directory if it does not exist
        val dirPath = Paths.get(dir)
        if (Files.notExists(dirPath)) Files.createDirectory(dirPath)

        if (list) {
            echo("Available options to --run <test1,test2> are:")
            printTestList(coolTestList)
            exitProcess(0)
        }
        val environment = Environment.get(env)

        val tests = if (run != null) {
            run.toString().split(",").mapNotNull { test ->
                coolTestList.find {
                    it.name.equals(test, ignoreCase = true)
                } ?: run {
                    echo("$test: not found")
                    null
                }
            }
        } else {
            // No --run arg:  run the smoke tests.
            coolTestList.filter { it.status == TestStatus.SMOKE }
        }
        if (tests.isNotEmpty()) {
            TermUi.echo(
                CoolTest.uglyMsgFormat("Running the following tests, POSTing to ${environment.url}:")
            )
            printTestList(tests)
            runTests(tests, environment)
        } else {
            echo("No tests to run.")
        }
    }

    private fun printTestList(tests: List<CoolTest>) {
        val formatTemplate = "%-20s%-20s\t%s"
        tests.forEach {
            echo(formatTemplate.format(it.name, it.status.description, it.description))
        }
    }

    private fun runTests(tests: List<CoolTest>, environment: Environment) {
        val failures = mutableListOf<CoolTest>()
        val options = CoolTestOptions(
            items, submits, key, dir, sftpDir = sftpDir, env = env, sender = sender, targetStates = targetStates,
            runSequential = runSequential, asyncProcessMode = asyncProcessMode
        )

        /**
         * Run a [test].
         */
        suspend fun runTest(test: CoolTest) {
            echo("Running test ${test.name}...")
            test.outputToConsole = options.runSequential
            test.echo("********************************")
            test.echo("Output for test ${test.name}...")
            val passed = try {
                test.run(environment, options)
            } catch (e: java.lang.Exception) {
                test.echo(
                    "Exception: ${e.javaClass.name}, ${e.message}: " +
                        "${e.stackTrace.joinToString(System.lineSeparator())}"
                )
                false
            }
            test.outputAllMsgs()
            test.echo("********************************")
            if (!passed)
                failures.add(test)
        }

        runBlocking {
            tests.forEach { test ->
                if (runSequential) {
                    runTest(test)
                } else {
                    launch { runTest(test) }
                }
            }
        }

        if (failures.isNotEmpty()) {
            TermUi.echo(
                CoolTest
                    .badMsgFormat("*** Tests FAILED:  ${failures.map { it.name }.joinToString(",")} ***")
            )
            exitProcess(-1)
        } else {
            TermUi.echo(
                CoolTest.goodMsgFormat("All tests passed")
            )
        }
    }

    companion object {
        val coolTestList = listOf(
            Ping(),
            SftpcheckTest(),
            End2End(),
            Merge(),
            WatersAuthTests(),
            QualityFilter(),
            Hl7Null(),
            TooManyCols(),
            BadCsv(),
            Strac(),
            Waters(),
            Hl7Ingest(),
            OtcProctored(),
            BadHl7(),
            Jti(),
            Huge(),
            TooBig(),
            Parallel(),
            Simulator(),
            HammerTime(),
            StracPack(),
            RepeatWaters(),
            InternationalContent(),
            DataCompareTest(),
            SantaClaus(),
            DbConnections(),
            BadSftp(),
            Garbage(),
            SettingsTest(),
            TestSubmissionsAPI(),
        )
    }
}

data class CoolTestOptions(
    val items: Int = 5,
    val submits: Int = 5,
    val key: String? = null,
    val dir: String,
    var muted: Boolean = false, // if true, print out less stuff,
    val sftpDir: String,
    val env: String,
    val sender: String? = null, // who is santa sending from?
    val targetStates: String? = null, // who is santa sending to?
    val runSequential: Boolean = false,
    val asyncProcessMode: Boolean = false // if true, pass 'processing=async' on all tests
)

abstract class CoolTest {
    abstract val name: String
    abstract val description: String
    abstract val status: TestStatus
    var outputToConsole = false

    /**
     * Stores a list of output messages instead of printing the messages to the console.
     */
    private val outputMsgs = mutableListOf<String>()

    abstract suspend fun run(
        environment: Environment,
        options: CoolTestOptions
    ): Boolean

    lateinit var db: DatabaseAccess

    /**
     * Store a message [msg] string.
     */
    private fun storeMsg(msg: String) {
        if (outputToConsole)
            TermUi.echo(msg)
        else
            outputMsgs.add(msg)
    }

    /**
     * Output all messages to the console.
     */
    fun outputAllMsgs() {
        outputMsgs.forEach { TermUi.echo(it) }
    }

    /**
     * Store a good [msg] message.
     * @return true
     */
    fun good(msg: String): Boolean {
        storeMsg(goodMsgFormat(msg))
        return true
    }

    /**
     * Store a bad [msg] message.
     * @return false
     */
    fun bad(msg: String): Boolean {
        storeMsg(badMsgFormat(msg))
        return false
    }

    /**
     * Store an ugly [msg] message.
     */
    fun ugly(msg: String) {
        storeMsg(uglyMsgFormat(msg))
    }

    /**
     * Store a [msg] message with no formatting.
     */
    fun echo(msg: String = "") {
        storeMsg(msg)
    }

    suspend fun waitABit(plusSecs: Int, env: Environment) {
        // seconds elapsed so far in this minute
        val secsElapsed = OffsetDateTime.now().second % 60
        // Wait until the top of the next minute, and pluSecs more, for 'batch', and 'send' to finish.
        var waitSecs = 60 - secsElapsed + plusSecs
        if (env != Environment.LOCAL) {
            // We are in Test or Staging, which don't execute on the top of the minute. Hack:
            waitSecs += 130
        } else if (secsElapsed > (60 - plusSecs)) {
            // Uh oh, we are close to the top of the minute *now*, so 'receive' might not finish in time.
            waitSecs += 60
        }
        echo("Waiting $waitSecs seconds for ReportStream to fully receive, batch, and send the data")
        // Print out some nice dots to show we are waiting only when the output goes directly to the console.
        if (outputToConsole) {
            for (i in 1..waitSecs) {
                delay(1000)
                print(".")
            }
            echo()
        } else {
            delay(waitSecs.toLong() * 1000)
        }
    }

    /**
     * Polls for the json result of the process action for [reportId]
     */
    suspend fun pollForProcessResult(
        reportId: ReportId,
        maxPollSecs: Int = 180,
        pollSleepSecs: Int = 20,
    ): Map<ReportId, String?> {
        var timeElapsedSecs = 0
        var queryResult = emptyMap<ReportId, String?>()
        echo("Polling for ReportStream process results, looking for $reportId.  (Max poll time $maxPollSecs seconds)")
        val actualTimeElapsedMillis = measureTimeMillis {
            while (timeElapsedSecs <= maxPollSecs) {
                if (outputToConsole) {
                    for (i in 1..pollSleepSecs) {
                        delay(1000)
                        // Print out some contemplative dots to show we are waiting.
                        print(".")
                    }
                    echo()
                } else {
                    delay(pollSleepSecs.toLong() * 1000)
                }
                timeElapsedSecs += pollSleepSecs
                queryResult = queryForProcessResults(reportId)
                if (queryResult != null)
                    break
            }
        }
        echo("Polling for PROCESS records finished in ${actualTimeElapsedMillis / 1000 } seconds")

        return queryResult
    }

    /**
     * Looks for at least one row in the covidResultMetadata table for [reportId]
     */
    fun queryForCovidResults(
        reportId: ReportId
    ): Boolean {
        var passed = false
        db = WorkflowEngine().db
        db.transact { txn ->
            val ctx = DSL.using(txn)
            val sql = """select cr.covid_results_metadata_id
                from covid_result_metadata as cr
                where cr.report_id = ?"""
            val ret = ctx.fetch(sql, reportId)?.into(Int::class.java)
            passed = ret != null && ret.size > 0
        }
        if (passed)
            good("Covid result metadata found.")
        return passed
    }

    /**
     * Gets the single child of the passed in [reportId]. Throws an error if there is more than one
     */
    fun getSingleChildReportId(
        reportId: ReportId,
    ): ReportId {
        var childReportId: ReportId? = null
        db = WorkflowEngine().db
        db.transact { txn ->
            val ctx = DSL.using(txn)
            // get internally generated reportId
            childReportId = ctx.selectFrom(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.eq(reportId))
                .fetchOne(REPORT_LINEAGE.CHILD_REPORT_ID)
        }
        return childReportId!!
    }

    /**
     * Returns all children produced by the process step for the parent [reportId], along with json response for each
     */
    private fun queryForProcessResults(
        reportId: ReportId,
    ): Map<ReportId, String?> {
        var queryResult = emptyMap<ReportId, String?>()
        db = WorkflowEngine().db
        db.transact { txn ->
            queryResult = processActionResultQuery(txn, reportId)
        }
        return queryResult
    }

    /**
     * Examine the [jsonResponse] from the process action, makes sure there is at least one destination reported
     * and report any errors
     * @param jsonResponse The json that was generated by the process function
     * @return true if there are no errors in the response, false otherwise
     */
    fun examineProcessResponse(jsonResponse: String?): Boolean {

        var passed = true
        try {
            // if there is no process response, this test fails
            if (jsonResponse == null)
                return bad("Test Failed: No process response")

            val tree = jacksonObjectMapper().readTree(jsonResponse)
            val reportId = getReportIdFromResponse(jsonResponse)
            echo("Id of submitted report: $reportId")
            val topic = tree["topic"]
            val errorCount = tree["errorCount"]
            val destCount = tree["destinationCount"]
            val destinations = tree["destinations"]

            if (topic != null && !topic.isNull && topic.textValue().equals("covid-19", true)) {
                good("'topic' is in response and correctly set to 'covid-19'")
            } else {
                passed = bad("***$name Test FAILED***: 'topic' is missing from response json")
            }

            if (errorCount != null && !errorCount.isNull && errorCount.intValue() == 0) {
                good("No errors detected.")
            } else {
                passed = bad("***$name Test FAILED***: There were errors reported.")
            }

            if (destCount != null && !destCount.isNull && destCount.intValue() > 0) {
                good("Data going to be sent to one or more destinations.")
            } else {
                passed = bad("***$name Test FAILED***: There are no destinations set for sending the data.")
            }

            if (reportId == null) {
                passed = bad("***$name Test FAILED***: Report ID was empty.")
            }
        } catch (e: NullPointerException) {
            passed = bad("***$name Test FAILED***: Unable to properly parse response json")
        }
        return passed
    }

    suspend fun pollForLineageResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        totalItems: Int,
        filterOrgName: Boolean = false,
        silent: Boolean = false,
        maxPollSecs: Int = 180,
        pollSleepSecs: Int = 20, // I had this as every 5 secs, but was getting failures.  The queries run unfastly.
        asyncProcessMode: Boolean = false
    ): Boolean {
        var timeElapsedSecs = 0
        var queryResults = listOf<Pair<Boolean, String>>()
        echo("Polling for ReportStream results.  (Max poll time $maxPollSecs seconds)")
        val actualTimeElapsedMillis = measureTimeMillis {
            while (timeElapsedSecs <= maxPollSecs) {
                if (outputToConsole) {
                    for (i in 1..pollSleepSecs) {
                        delay(1000)
                        // Print out some contemplative dots to show we are waiting.
                        print(".")
                    }
                    echo()
                } else {
                    delay(pollSleepSecs.toLong() * 1000)
                }
                timeElapsedSecs += pollSleepSecs
                queryResults = queryForLineageResults(reportId, receivers, totalItems, filterOrgName, asyncProcessMode)
                if (!queryResults.map { it.first }.contains(false)) break // everything passed!
            }
        }
        echo("Test $name finished in ${actualTimeElapsedMillis / 1000 } seconds")
        if (!silent) {
            queryResults.forEach {
                if (it.first)
                    good(it.second)
                else
                    bad(it.second)
            }
        }
        return ! queryResults.map { it.first }.contains(false) // no falses == it passed!
    }

    private fun queryForLineageResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        totalItems: Int,
        filterOrgName: Boolean = false,
        asyncProcessMode: Boolean = false
    ): List<Pair<Boolean, String>> {
        var queryResults = mutableListOf<Pair<Boolean, String>>()
        db = WorkflowEngine().db
        db.transact { txn ->
            receivers.forEach { receiver ->
                val actionsList = mutableListOf(TaskAction.receive)
                // Bug:  this is looking at local cli data, but might be querying staging or prod.
                // The hope is that the 'ignore' org is same in local, staging, prod.
                if (asyncProcessMode) actionsList.add(TaskAction.process)
                if (receiver.timing != null) actionsList.add(TaskAction.batch)
                if (receiver.transport != null) actionsList.add(TaskAction.send)
                actionsList.forEach { action ->
                    val count = itemLineageCountQuery(
                        txn = txn,
                        reportId = reportId,
                        // if we are processing asynchronously the receive step doesn't have any receivers yet
                        receivingOrgSvc = if (action == TaskAction.receive && asyncProcessMode) null else receiver.name,
                        receivingOrg = if (filterOrgName) receiver.organizationName else null,
                        action = action
                    )
                    val expected = if (action == TaskAction.receive && asyncProcessMode) {
                        totalItems
                    } else totalItems / receivers.size
                    if (count == null || expected != count) {
                        queryResults += Pair(
                            false,
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records but got $count"
                        )
                    } else {
                        queryResults += Pair(
                            true,
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records and got $count"
                        )
                    }
                }
            }
        }
        return queryResults
    }

    /**
     * Get the report ID from the [jsonResponse].
     * @return the report ID or null
     */
    fun getReportIdFromResponse(jsonResponse: String): ReportId? {
        var reportId: ReportId? = null
        val tree = jacksonObjectMapper().readTree(jsonResponse)
        if (!tree.isNull && !tree["id"].isNull) {
            reportId = ReportId.fromString(tree["id"].textValue())
        }
        return reportId
    }

    /**
     * Examine the [jsonResponse] from the API, makes sure there is at least one destination reported
     * and report any errors
     * @param jsonResponse The json that was returned from the API
     * @param shouldHaveDestination When posting async, the destination will not yet be calculated
     * @return true if there are no errors in the response, false otherwise
     */
    fun examinePostResponse(jsonResponse: String, shouldHaveDestination: Boolean): Boolean {

        var passed = true
        try {
            val tree = jacksonObjectMapper().readTree(jsonResponse)
            val reportId = getReportIdFromResponse(jsonResponse)
            echo("Id of submitted report: $reportId")
            val topic = tree["topic"]
            val errorCount = tree["errorCount"]
            val destCount = tree["destinationCount"]

            if (topic != null && !topic.isNull && topic.textValue().equals("covid-19", true)) {
                good("'topic' is in response and correctly set to 'covid-19'")
            } else {
                passed = bad("***$name Test FAILED***: 'topic' is missing from response json")
            }

            if (errorCount != null && !errorCount.isNull && errorCount.intValue() == 0) {
                good("No errors detected.")
            } else {
                passed = bad("***$name Test FAILED***: There were errors reported.")
            }

            if (shouldHaveDestination)
                if (destCount != null && !destCount.isNull && destCount.intValue() > 0) {
                    good("Data going to be sent to one or more destinations.")
                } else {
                    passed = bad("***$name Test FAILED***: There are no destinations set for sending the data.")
                }

            if (reportId == null) {
                passed = bad("***$name Test FAILED***: Report ID was empty.")
            }
        } catch (e: NullPointerException) {
            passed = bad("***$name Test FAILED***: Unable to properly parse response json")
        }
        return passed
    }

    companion object {
        val metadata by lazy { Metadata.getInstance() }
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)

        // Here is test setup of organization, senders, and receivers.   All static.
        const val orgName = "ignore"
        val org = settings.findOrganization(orgName)
            ?: error("Unable to find org $orgName in metadata")
        const val receivingStates = "IG"

        const val simpleReportSenderName = "ignore-simple-report"
        val simpleRepSender = settings.findSender("$orgName.$simpleReportSenderName")
            ?: error("Unable to find sender $simpleReportSenderName for organization ${org.name}")

        const val stracSenderName = "ignore-strac"
        val stracSender = settings.findSender("$orgName.$stracSenderName")
            ?: error("Unable to find sender $stracSenderName for organization ${org.name}")

        const val watersSenderName = "ignore-waters"
        val watersSender = settings.findSender("$orgName.$watersSenderName")
            ?: error("Unable to find sender $watersSenderName for organization ${org.name}")

        const val emptySenderName = "ignore-empty"
        val emptySender = settings.findSender("$orgName.$emptySenderName")
            ?: error("Unable to find sender $emptySenderName for organization ${org.name}")

        const val hl7SenderName = "ignore-hl7"
        val hl7Sender = settings.findSender("$orgName.$hl7SenderName")
            ?: error("Unable to find sender $hl7SenderName for organization ${org.name}")

        val csvReceiver = settings.receivers.filter { it.organizationName == orgName && it.name == "CSV" }[0]
        val hl7Receiver = settings.receivers.filter { it.organizationName == orgName && it.name == "HL7" }[0]
        val hl7BatchReceiver = settings.receivers.filter { it.organizationName == orgName && it.name == "HL7_BATCH" }[0]
        val redoxReceiver = settings.receivers.filter { it.organizationName == orgName && it.name == "REDOX" }[0]
        val hl7NullReceiver = settings.receivers.filter { it.organizationName == orgName && it.name == "HL7_NULL" }[0]

        lateinit var allGoodReceivers: MutableList<Receiver>
        lateinit var allGoodCounties: String

        fun initListOfGoodReceiversAndCounties(env: Environment) {
            allGoodReceivers = mutableListOf(csvReceiver, hl7Receiver, hl7BatchReceiver, hl7NullReceiver)
            if (env == Environment.LOCAL) {
                allGoodReceivers.add(redoxReceiver)
            }

            allGoodCounties = allGoodReceivers.map { it.name }.joinToString(",")
        }

        val blobstoreReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "BLOBSTORE"
        }[0]
        val sftpFailReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "SFTP_FAIL"
        }[0]
        val qualityGoodReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "QUALITY_PASS"
        }[0]
        val qualityAllReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "QUALITY_ALL"
        }[0]
        val qualityFailReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "QUALITY_FAIL"
        }[0]
        val qualityReversedReceiver = settings.receivers.filter {
            it.organizationName == orgName && it.name == "QUALITY_REVERSED"
        }[0]

        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_BLACK = "\u001B[30m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_BLUE = "\u001B[34m"
        const val ANSI_CYAN = "\u001B[36m"

        /**
         * Format a [msg] string as a good message.
         * @return a formatted message string
         */
        fun goodMsgFormat(msg: String): String {
            return ANSI_GREEN + msg + ANSI_RESET
        }

        /**
         * Format a [msg] string as a bad message.
         * @return a formatted message string
         */
        fun badMsgFormat(msg: String): String {
            return ANSI_RED + msg + ANSI_RESET
        }

        /**
         * Format a [msg] string as an ugly message.
         * @return a formatted message string
         */
        fun uglyMsgFormat(msg: String): String {
            return ANSI_CYAN + msg + ANSI_RESET
        }

        /**
         * Queries the database and pulls back the action_response json for the requested [reportId]
         * @return String representing the jsonb value of action_result for the process action for this report
         */
        fun processActionResultQuery(
            txn: DataAccessTransaction,
            reportId: ReportId
        ): Map<ReportId, String?> {
            val ctx = DSL.using(txn)

            // get the reports generated by the 'process' step
            val processingReportIds = ctx.selectFrom(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.eq(reportId))
                .fetch(REPORT_LINEAGE.CHILD_REPORT_ID)

            // get the action_response from the action table for the process task
            val actionResponses = mutableMapOf<ReportId, String?>()
            for (processingReportId in processingReportIds) {
                val ret = ctx.select(ACTION.ACTION_RESPONSE)
                    .from(ACTION)
                    .join(REPORT_FILE)
                    .on(ACTION.ACTION_ID.eq(REPORT_FILE.ACTION_ID))
                    .and(REPORT_FILE.REPORT_ID.eq(processingReportId))
                    .and(ACTION.ACTION_NAME.eq(TaskAction.process))
                    .fetchOne(ACTION.ACTION_RESPONSE)
                actionResponses[processingReportId] = ret?.toString()
            }

            return actionResponses
        }

        fun itemLineageCountQuery(
            txn: DataAccessTransaction,
            reportId: ReportId,
            receivingOrgSvc: String? = null,
            receivingOrg: String? = null,
            action: TaskAction,
        ): Int? {
            val ctx = DSL.using(txn)
            val sql = """select count(*)
              from item_lineage as IL
              join report_file as RF on IL.child_report_id = RF.report_id
              join action as A on A.action_id = RF.action_id
              where
              ${if (receivingOrgSvc != null) "RF.receiving_org_svc = ? and" else ""}
              ${if (receivingOrg != null) "RF.receiving_org = ? and" else ""}
              A.action_name = ?
              and IL.item_lineage_id in
              (select item_descendants(?)) """

            return if (receivingOrg != null && receivingOrgSvc != null) {
                ctx.fetchOne(sql, receivingOrgSvc, receivingOrg, action, reportId)?.into(Int::class.java)
            } else if (receivingOrgSvc != null) {
                ctx.fetchOne(sql, receivingOrgSvc, action, reportId)?.into(Int::class.java)
            } else {
                ctx.fetchOne(sql, action, reportId)?.into(Int::class.java)
            }
        }

        /**
         * Fetch the one uploaded file to SFTP for a given [reportId] and [receivingOrgSvc].
         * @return the filename of the uploaded file
         */
        fun sftpFilenameQuery(
            txn: DataAccessTransaction,
            reportId: ReportId,
            receivingOrgSvc: String
        ): String? {
            val ctx = DSL.using(txn)
            val sql = """select RF.external_name
                from report_file as RF
                join action as A ON A.action_id = RF.action_id
                where RF.report_id in (select find_sent_reports(?)) AND RF.receiving_org_svc = ?
                order by A.action_id """
            return ctx.fetchOne(sql, reportId, receivingOrgSvc)?.into(String::class.java)
        }

        // Find the most recent action taken in the system
        fun actionQuery(
            txn: DataAccessTransaction,
        ): Action? {
            val ctx = DSL.using(txn)
            return ctx.selectFrom(ACTION)
                .where(ACTION.ACTION_ID.eq(ctx.select(max(ACTION.ACTION_ID)).from(ACTION)))
                .fetchOne()?.into(Action::class.java)
        }
    }
}

class Ping : CoolTest() {
    override val name = "ping"
    override val description = "CheckConnections: Is the reports endpoint alive and listening?"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting ping Test: run CheckConnections of ${environment.url}")
        val (responseCode, json) = HttpUtilities.postReportBytes(
            environment,
            "x".toByteArray(),
            simpleRepSender,
            options.key,
            Options.CheckConnections,
            payloadName = "$name ${status.description}",
        )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_OK) {
            bad("Ping/CheckConnections Test FAILED:  response code $responseCode")
            exitProcess(-1) // other tests won't work.
        }
        try {
            val tree = jacksonObjectMapper().readTree(json)
            if (tree["errorCount"].intValue() != 0 || tree["warningCount"].intValue() != 0) {
                return bad("***Ping/CheckConnections Test FAILED***")
            } else {
                return good("Test passed: Ping/CheckConnections")
            }
        } catch (e: NullPointerException) {
            return bad("***Ping/CheckConnections FAILED***: Unable to properly parse response json")
        }
    }
}

class End2End : CoolTest() {
    override val name = "end2end"
    override val description = "Create Fake data, submit, wait, confirm sent via database lineage data"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties(environment)
        ugly("Starting $name Test: send ${simpleRepSender.fullName} data to $allGoodCounties")

        // run both sync and async end2end test
        return forceSync(environment, options) && forceAsync(environment, options)
    }

    /**
     * Forces synchronous end2end test
     */
    private suspend fun forceSync(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Running end2end synchronously -- with no query param")
        var passed = true
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )

        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            // force sync processing
            HttpUtilities.postReportFile(
                environment, file, simpleRepSender, false, options.key,
                payloadName = "$name ${status.description} with async = false",
            )
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***end2end Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of sync report succeeded with response code $responseCode")
        }
        echo(json)
        passed = passed and examinePostResponse(json, true)
        val reportId = getReportIdFromResponse(json)
        if (reportId != null) {
            // check for covid result metadata - the examinePostResponse function above has already
            //  verified that the topic is covid-19. This will need to be updated once we are supporting
            //  non-covid record types
            passed = passed and queryForCovidResults(reportId)
            if (!passed)
                bad("***sync end2end FAILED***: Covid metadata record not found")

            // check that lineages were generated properly
            passed = passed and pollForLineageResults(
                reportId,
                allGoodReceivers,
                fakeItemCount,
                asyncProcessMode = false
            )
        }

        return passed
    }

    /**
     * Forces asynchronous end2end test
     */
    private suspend fun forceAsync(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Running end2end asynchronously -- with query param")
        var passed = true
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )

        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            // force async processing
            HttpUtilities.postReportFile(
                environment, file, simpleRepSender, true, options.key,
                payloadName = "$name ${status.description} with async = true",
            )
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***end2end Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of async report succeeded with response code $responseCode")
        }
        echo(json)
        passed = passed and examinePostResponse(json, false)
        if (!passed)
            bad("***async end2end FAILED***: Error in post response")
        // gets back 'received' reportId
        val reportId = getReportIdFromResponse(json)

        if (reportId != null) {
            // gets back the id of the internal report
            val internalReportId = getSingleChildReportId(reportId)

            val processResults = pollForProcessResult(internalReportId)
            // verify each result is valid
            for (result in processResults.values)
                passed = passed && examineProcessResponse(result)
            if (!passed)
                bad("***async end2end FAILED***: Process result invalid")

            // check for covid result metadata - the examinePostResponse function above has already
            //  verified that the topic is covid-19. This will need to be updated once we are supporting
            //  non-covid record types
            passed = passed and queryForCovidResults(reportId)
            if (!passed)
                bad("***async end2end FAILED***: Covid metadata record not found")

            // check that lineages were generated properly
            passed = passed and pollForLineageResults(
                reportId, allGoodReceivers,
                fakeItemCount,
                asyncProcessMode = true
            )
        }

        return passed
    }
}

class Merge : CoolTest() {
    override val name = "merge"
    override val description = "Submit multiple files, wait, confirm via db that merge occurred"
    override val status = TestStatus.SMOKE

    /**
     * Examine the results stored in the database and compare the number of reports to the expected number.
     * @param reportIds the list of report IDs that were sent to the API
     * @param receivers the list of receivers the reports were sent to
     * @param itemsPerReport the number of reports in each file sent
     * @param reportCount the total number of reports sent
     * @return true if the number of reports matches the expected, false otherwise
     */
    fun queryForMergeResults(
        reportIds: List<ReportId>,
        receivers: List<Receiver>,
        itemsPerReport: Int,
    ): List<Pair<Boolean, String>> {
        var queryResults = mutableListOf<Pair<Boolean, String>>()
        db = WorkflowEngine().db
        db.transact { txn ->
            val expected = (itemsPerReport * reportIds.size) / receivers.size
            receivers.forEach { receiver ->
                val actionsList = mutableListOf<TaskAction>()
                if (receiver.timing != null) actionsList.add(TaskAction.batch)
                if (receiver.transport != null) actionsList.add(TaskAction.send)
                actionsList.forEach { action ->
                    var count = 0
                    reportIds.forEach { reportId ->
                        count += itemLineageCountQuery(txn, reportId, receiver.name, action = action) ?: 0
                    }
                    if (expected != count) {
                        queryResults += Pair(
                            false,
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting sum(itemCount)=$expected but got $count"
                        )
                    } else {
                        queryResults += Pair(
                            true,
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting sum(itemCount)=$expected and got $count"
                        )
                    }
                }
            }
        }
        return queryResults
    }

    // todo  arglebargle - this is mostly repeat of pollForLineageResults, but ever so slightly different.
    suspend fun pollForMergeResults(
        reportIds: List<ReportId>,
        receivers: List<Receiver>,
        itemsPerReport: Int,
        silent: Boolean = false,
        maxPollSecs: Int = 180,
        pollSleepSecs: Int = 20,
    ): Boolean {
        var timeElapsedSecs = 0
        var queryResults = listOf<Pair<Boolean, String>>()
        echo("Polling for ReportStream results.  (Max poll time $maxPollSecs seconds)")
        // Print out some nice dots to show we are waiting only when the output goes directly to the console.
        while (timeElapsedSecs <= maxPollSecs) {
            if (outputToConsole) {
                for (i in 1..pollSleepSecs) {
                    delay(1000)
                    print(".")
                }
                echo()
            } else {
                delay(pollSleepSecs.toLong() * 1000)
            }
            timeElapsedSecs += pollSleepSecs
            queryResults = queryForMergeResults(reportIds, receivers, itemsPerReport)
            if (! queryResults.map { it.first }.contains(false)) break // everything passed!
        }
        if (!silent) {
            queryResults.forEach {
                if (it.first)
                    good(it.second)
                else
                    bad(it.second)
            }
        }
        return ! queryResults.map { it.first }.contains(false) // no falses == it passed!
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed: Boolean
        val mergingReceivers = listOf<Receiver>(csvReceiver, hl7BatchReceiver)
        val mergingCounties = mergingReceivers.map { it.name }.joinToString(",")
        val fakeItemCount = mergingReceivers.size * options.items
        ugly("Starting merge test:  Merge ${options.submits} reports, each of which sends to $mergingCounties")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            mergingCounties,
            options.dir,
        )
        echo("Created datafile $file")
        val actualTimeElapsedMillis = measureTimeMillis {
            // Now send it to ReportStream over and over
            val reportIds = (1..options.submits).map {
                val (responseCode, json) =
                    HttpUtilities.postReportFile(
                        environment,
                        file,
                        simpleRepSender,
                        options.asyncProcessMode,
                        options.key,
                        payloadName = "$name ${status.description}",
                    )
                echo("Response to POST: $responseCode")
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    return bad("***Merge Test FAILED***:  response code $responseCode")
                }
                val reportId = getReportIdFromResponse(json)
                    ?: return bad("***$name Test FAILED***: A report ID came back as null")
                echo("Id of submitted report: $reportId")
                reportId
            }
            passed = pollForMergeResults(reportIds, mergingReceivers, fakeItemCount)
        }
        echo("$name test took ${actualTimeElapsedMillis / 1000} seconds.")
        return passed
    }
}

/**
 * Test using the NULL transport.
 */
class Hl7Null : CoolTest() {
    override val name = "hl7null"
    override val description = "The NULL transport does db work, but no transport.  Uses HL7 format"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val fakeItemCount = 100
        ugly("Starting hl7null Test: test of many threads all doing database interactions, but no sends. ")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            "HL7_NULL",
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.   Make numResends > 1 to create merges.
        val numResends = 1
        val reportIds = (1..numResends).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(
                    environment, file, simpleRepSender, options.asyncProcessMode, options.key,
                    payloadName = "$name ${status.description}",
                )
            echo("Response to POST: $responseCode")
            echo(json)
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                return bad("***hl7null Test FAILED***:  response code $responseCode")
            }
            val reportId = getReportIdFromResponse(json)
                ?: return bad("***$name Test FAILED***: A report ID came back as null")
            echo("Id of submitted report: $reportId")
            reportId
        }
        return pollForLineageResults(
            reportId = reportIds[0],
            receivers = listOf(hl7NullReceiver),
            totalItems = fakeItemCount,
            asyncProcessMode = options.asyncProcessMode
        )
    }
}

class TooManyCols : CoolTest() {
    override val name = "toomanycols"
    override val description = "Submit a file with more than $REPORT_MAX_ITEM_COLUMNS columns, which should error"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting toomanycols Test: submitting a file with too many columns.")
        val file = File("./src/test/csv_test_files/input/too-many-columns.csv")
        if (!file.exists()) {
            error("Unable to find file ${file.absolutePath} to do toomanycols test")
        }
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment, file, simpleRepSender, options.asyncProcessMode, options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = (tree["errors"][0]) as ObjectNode
            if (firstError["message"].textValue().contains("columns")) {
                return good("toomanycols Test passed.")
            } else {
                return bad("***toomanycols Test FAILED***:  did not find the error.")
            }
        } catch (e: Exception) {
            return bad("***toomanycols Test FAILED***: Unable to parse json response")
        }
    }
}

class BadCsv : CoolTest() {
    override val name = "badcsv"
    override val description = "Submit badly formatted csv files - should get errors"
    override val status = TestStatus.SMOKE
    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val filenames = listOf("not-a-csv-file.csv", /* "column-headers-only.csv", */ "completely-empty-file.csv")
        var passed = true
        filenames.forEachIndexed { i, filename ->
            ugly("Starting badcsv file Test $i: submitting $filename")
            val file = File("./src/test/csv_test_files/input/$filename")
            if (!file.exists()) {
                error("Unable to find file ${file.absolutePath} to do badcsv test")
            }
            val (responseCode, json) = HttpUtilities.postReportFile(
                environment,
                file,
                simpleRepSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
            echo("Response to POST: $responseCode")
            if (responseCode >= 400) {
                good("Test of Bad CSV file $filename passed: Failure HttpStatus code was returned.")
            } else {
                bad("***badcsv Test $i of $filename FAILED: Expecting a failure HttpStatus. ***")
                passed = false
            }
            try {
                val tree = jacksonObjectMapper().readTree(json)
                if (tree["id"] == null || tree["id"].isNull) {
                    good("Test of Bad CSV file $filename passed: No UUID was returned.")
                } else {
                    bad("***badcsv Test $i of $filename FAILED: RS returned a valid UUID for a bad CSV. ***")
                    passed = false
                }
                if (tree["errorCount"].intValue() > 0) {
                    good("Test of Bad CSV file $filename passed: At least one error was returned.")
                } else {
                    bad("***badcsv Test $i of $filename FAILED: No error***")
                    passed = false
                }
            } catch (e: Exception) {
                passed = bad("***badcsv Test $i of $filename FAILED***: Unexpected json returned")
            }
        }
        return passed
    }
}

class Strac : CoolTest() {
    override val name = "strac"
    override val description = "Submit data in strac schema, send to all formats and variety of schemas"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties(environment)
        ugly("Starting bigly strac Test: sending Strac data to all of these receivers: $allGoodCounties!")
        var passed = true
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            stracSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                stracSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("**Strac Test FAILED***:  response code $responseCode")
        }
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = getReportIdFromResponse(json)
                ?: return bad("***$name Test FAILED***: A report ID came back as null")
            echo("Id of submitted report: $reportId")
            val expectedWarningCount = 0
            val warningCount = tree["warningCount"].intValue()
            if (warningCount == expectedWarningCount) {
                good("First part of strac Test passed: $warningCount warnings were returned.")
            } else {
                bad("***strac Test FAILED: Expecting $expectedWarningCount warnings but got $warningCount***")
                passed = false
            }
            return passed and pollForLineageResults(
                reportId = reportId,
                receivers = allGoodReceivers,
                totalItems = fakeItemCount,
                asyncProcessMode = options.asyncProcessMode
            )
        } catch (e: Exception) {
            return bad("***strac Test FAILED***: Unexpected json returned")
        }
    }
}

class StracPack : CoolTest() {
    override val name = "stracpack" // no its not 'strackpack'
    override val description = "Submits via '--submits X' threads sending Strac data to Redox" +
        ", Each submit has '--items Y' items." +
        "  Same as hammertime, no delays between thread starts, so all threads start at once."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly(
            "Starting stracpack Test: Starting ${options.submits} simultaneous threads, each submitting" +
                " ${options.items} items of Strac data to the ${redoxReceiver.name} receiver only."
        )
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            stracSender,
            options.items,
            receivingStates,
            redoxReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream over and over
        val reportIds = mutableListOf<ReportId>()
        var passed = true
        // submit in thread grouping somewhat smaller than our database pool size.
        for (i in 1..options.submits) {
            thread {
                val (responseCode, json) =
                    HttpUtilities.postReportFile(
                        environment,
                        file,
                        stracSender,
                        options.asyncProcessMode,
                        options.key,
                        payloadName = "$name ${status.description}",
                    )
                echo("$i: Response to POST: $responseCode")
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    echo(json)
                    passed = bad("$i: ***StracPack Test FAILED***:  response code $responseCode")
                } else {
                    val reportId = getReportIdFromResponse(json)
                    echo("$i: Id of submitted report: $reportId")
                    synchronized(reportIds) {
                        reportId?.let { reportIds.add(reportId) }
                    }
                }
            }
        }
        // Since we have to wait for the sends anyway, I didn't bother with a join here.
        waitABit(5 * options.submits, environment) // SWAG: wait extra seconds extra per file submitted
        reportIds.forEach {
            passed = passed and
                pollForLineageResults(
                    reportId = it,
                    receivers = listOf(redoxReceiver),
                    totalItems = options.items,
                    asyncProcessMode = options.asyncProcessMode
                )
        }
        return passed
    }
}

class Parallel : CoolTest() {
    val n = 50
    override val name = "parallel"
    override val description = "Each thread Submits $n submissions as fast as it can, " +
        "first with 1 thread, then 2 threads etc up to 10 threads." +
        " Each submit has --items Y items.  Does NOT check batch/send steps."
    override val status = TestStatus.LOAD

    fun runTheParallelTest(
        file: File,
        numThreads: Int,
        numRounds: Int,
        environment: Environment,
        options: CoolTestOptions
    ): Boolean {
        var passed = true
        var totalMillisAllSubmissions: Long = 0
        val elapsedMillisTotal = measureTimeMillis {
            val threads = mutableListOf<Thread>()
            echo("Parallel Test: Starting $numThreads threads, each submitting $numRounds times")
            echo("Options: $options")
            for (threadNum in 1..numThreads) {
                val th = thread {
                    for (i in 1..numRounds) {
                        val elapsedMillisOneSubmission = measureTimeMillis {
                            val (responseCode, json) =
                                HttpUtilities.postReportFile(
                                    environment,
                                    file,
                                    stracSender,
                                    options.asyncProcessMode,
                                    options.key,
                                    Options.SkipSend,
                                    payloadName = "$name ${status.description}",
                                )
                            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                                echo(json)
                                passed = bad("$threadNum: ***Parallel Test FAILED***:  response code $responseCode")
                            } else {
                                val reportId = getReportIdFromResponse(json)
                                if (reportId == null) {
                                    passed = bad("$threadNum: ***Parallel Test FAILED***:  No reportId.")
                                }
                            }
                        }
                        totalMillisAllSubmissions += elapsedMillisOneSubmission // hrm.  Not threadsafe.
                        print(".")
                    }
                }
                threads.add(th)
            }
            threads.forEach { it.join() }
        }
        echo("")
        val totalSubmissionsCount = numThreads * numRounds
        val avgSecsPerSubmissionString = String.format(
            "%.2f",
            (totalMillisAllSubmissions / 1000.0) / totalSubmissionsCount
        )
        val rateString = String.format(
            "%.2f",
            totalSubmissionsCount.toDouble() / (elapsedMillisTotal / 1000.0)
        )
        if (passed) {
            good(
                "$numThreads X $numRounds  = $totalSubmissionsCount total submissions" +
                    " in ${elapsedMillisTotal / 1000} seconds:\n" +
                    "$numThreads threads: $rateString items/second, $avgSecsPerSubmissionString seconds/item"
            )
        } else {
            bad(
                "$numThreads X $numRounds  = $totalSubmissionsCount total submissions" +
                    " in ${elapsedMillisTotal / 1000} seconds"
            )
        }
        return passed
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly(
            "Starting parallel Test: Increasing numbers of threads submitting in parallel," +
                " as fast as they can for $n rounds.  hl7null ${options.items} items per submission."
        )
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            stracSender,
            options.items,
            receivingStates,
            hl7NullReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        echo("Priming the pump by submitting twice:")
        val (r1, _) =
            HttpUtilities.postReportFile(
                environment,
                file,
                stracSender,
                options.asyncProcessMode,
                options.key,
                Options.SkipSend,
                payloadName = "$name ${status.description}",
            )
        echo("First response to POST: $r1")
        val (r2, _) =
            HttpUtilities.postReportFile(
                environment,
                file,
                stracSender,
                options.asyncProcessMode,
                options.key,
                Options.SkipSend,
                payloadName = "$name ${status.description}",
            )
        echo("Second response to POST: $r2.  Ready for the real test:")
        var passed = runTheParallelTest(file, 1, n, environment, options)
        passed = passed and runTheParallelTest(file, 2, n, environment, options)
        passed = passed and runTheParallelTest(file, 3, n, environment, options)
        passed = passed and runTheParallelTest(file, 4, n, environment, options)
        passed = passed and runTheParallelTest(file, 5, n, environment, options)
        passed = passed and runTheParallelTest(file, 6, n, environment, options)
        passed = passed and runTheParallelTest(file, 7, n, environment, options)
        passed = passed and runTheParallelTest(file, 8, n, environment, options)
        passed = passed and runTheParallelTest(file, 9, n, environment, options)
        passed = passed and runTheParallelTest(file, 10, n, environment, options)
        return passed
    }
}

class Waters : CoolTest() {
    override val name = "waters"
    override val description = "Submit data in waters schema, send to BLOBSTORE only"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting Waters: sending ${options.items} Waters items to ${blobstoreReceiver.name} receiver")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            watersSender,
            options.items,
            receivingStates,
            blobstoreReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                watersSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        if (!options.muted) echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("***Waters Test FAILED***:  response code $responseCode")
        }
        val reportId = getReportIdFromResponse(json)
            ?: return bad("***$name Test FAILED***: A report ID came back as null")
        echo("Id of submitted report: $reportId")
        return pollForLineageResults(
            reportId = reportId,
            receivers = listOf(blobstoreReceiver),
            totalItems = options.items,
            asyncProcessMode = options.asyncProcessMode
        )
    }
}

class RepeatWaters : CoolTest() {
    override val name = "repeatwaters"
    override val description = "Submits via '--submits X' threads, sending Waters data to BLOBSTORE" +
        "  Each submit has '--items Y' items.  Brief sleep between each thread creation." +
        "  Can vary the sleeptime to determine what is a sustainable pace of submissions."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val sleepBetweenSubmitMillis = 1000
        val variationMillis = 360
        ugly(
            "Starting repeatwaters Test: Starting ${options.submits} submissions, each submitting" +
                " ${options.items} items of Waters data to the ${blobstoreReceiver.name} receiver only. " +
                " Delay between each submission of $sleepBetweenSubmitMillis millis."
        )
        var allPassed = true
        var totalItems = 0
        val pace = (3600000 / sleepBetweenSubmitMillis) * options.items
        echo("Submitting at an expected pace of $pace items per hour")
        options.muted = true
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            watersSender,
            options.items,
            receivingStates,
            blobstoreReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        val elapsedMillis = measureTimeMillis {
            val threads = mutableListOf<Thread>()
            for (i in 1..options.submits) {
                val thread = thread {
                    var success: Boolean
                    runBlocking {
                        val (responseCode, json) =
                            HttpUtilities.postReportFile(
                                environment,
                                file,
                                watersSender,
                                options.asyncProcessMode,
                                options.key,
                                payloadName = "$name ${status.description}",
                            )
                        echo("Response to POST: $responseCode")
                        if (!options.muted) echo(json)
                        if (responseCode != HttpURLConnection.HTTP_CREATED) {
                            success = bad("***One Waters Test FAILED***:  response code $responseCode")
                        } else {
                            val reportId = getReportIdFromResponse(json)
                            if (reportId == null) {
                                success = bad("***$name Test FAILED***: A report ID came back as null")
                            } else {
                                echo("Id of submitted report: $reportId")
                                waitABit(30, environment)
                                success = pollForLineageResults(
                                    reportId = reportId,
                                    receivers = listOf(blobstoreReceiver),
                                    totalItems = options.items,
                                    asyncProcessMode = options.asyncProcessMode
                                )
                            }
                        }
                        synchronized(allPassed) {
                            if (success) totalItems += options.items
                            allPassed = allPassed && success
                        }
                    }
                }
                threads.add(thread)
                if (i < options.submits) {
                    val random = Random.nextInt(-variationMillis, variationMillis)
                    val sleepMillis = kotlin.math.max(sleepBetweenSubmitMillis + random, 0)
                    echo("$i: Sleeping for $sleepMillis milliseconds before next submit")
                    sleep(sleepMillis.toLong())
                }
            }
            echo("Submits done.  Now waiting for checking results to complete")
            threads.forEach { it.join() }
        }
        echo("$name Test took ${elapsedMillis / 1000} seconds. Expected pace/hr: $pace.")
        if ((elapsedMillis / 1000) > 600) {
            // pace calculation is inaccurate for short times, due to the kluge long wait at the end.
            val actualPace = (totalItems / (elapsedMillis / 1000)) * 3600 // advanced math
            echo(" Actual pace/hr: $actualPace")
        }
        return allPassed
    }
}

/**
 * This test can be used to hammer the reports endpoint, as it does all its submits in parallel.
 */
class HammerTime : CoolTest() {
    val receiverToTest = hl7Receiver
    override val name = "hammertime"
    override val description = "Submits via '--submits X' threads sending SimpleRep data to an sftp site" +
        ", Each submit has '--items Y' items." +
        "  Unlike repeatwaters, no delays between thread starts, so all threads start at once."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly(
            "Starting hammertime test: Starting ${options.submits} simultaneous threads, each submitting" +
                " ${options.items} items of SimpleRep data to ${receiverToTest.name}"
        )
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            options.items,
            receivingStates,
            receiverToTest.name,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream over and over
        val reportIds = mutableListOf<ReportId>()
        var passed = true
        for (i in 1..options.submits) {
            thread {
                val (responseCode, json) =
                    HttpUtilities.postReportFile(
                        environment,
                        file,
                        simpleRepSender,
                        options.asyncProcessMode,
                        options.key,
                        payloadName = "$name ${status.description}",
                    )
                echo("Response to POST: $responseCode")
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    echo(json)
                    passed = bad("***Hammertime Test FAILED***:  response code $responseCode")
                } else {
                    val reportId = getReportIdFromResponse(json)
                    echo("Id of submitted report: $reportId")
                    synchronized(reportIds) {
                        reportId?.let { reportIds.add(reportId) }
                    }
                }
            }
        }
        // Since we have to wait for the sends anyway, I didn't bother with a join here.
        waitABit(5 * options.submits, environment) // SWAG: wait 5 seconds extra per file submitted
        reportIds.forEach {
            passed = passed and
                pollForLineageResults(
                    reportId = it,
                    receivers = listOf(receiverToTest),
                    totalItems = options.items,
                    asyncProcessMode = options.asyncProcessMode
                )
        }
        return passed
    }
}

class Garbage : CoolTest() {
    override val name = "garbage"
    override val description = "Garbage in - Nice error message out"
    override val status = TestStatus.FAILS // new quality checks now prevent any data from flowing to other checks

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties(environment)
        ugly("Starting $name Test: send ${emptySender.fullName} data to $allGoodCounties")
        var passed = true
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            emptySender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                emptySender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = getReportIdFromResponse(json)
            echo("Id of submitted report: $reportId")
            val warningCount = tree["warningCount"].intValue()
            if (warningCount == allGoodReceivers.size) {
                good("garbage Test passed: $warningCount warnings were returned.")
            } else {
                passed =
                    bad("***garbage Test FAILED: Expecting ${allGoodReceivers.size} warnings but got $warningCount***")
            }
            val destinationCount = tree["destinationCount"].intValue()
            if (destinationCount == 0) {
                good("garbage Test passed: Items went to $destinationCount destinations.")
            } else {
                passed = bad("***garbage Test FAILED: Expecting 0 destinationCount but got $destinationCount ***")
            }
        } catch (e: Exception) {
            passed = bad("***garbage Test FAILED***: Unexpected json returned")
        }
        return passed
    }
}

class QualityFilter : CoolTest() {
    override val name = "qualityfilter"
    override val description = "Test the QualityFilter feature"
    override val status = TestStatus.SMOKE

    /**
     * In the returned json, check the itemCount associated with receiver.name in the list of destinations.
     */
    private fun checkJsonItemCountForReceiver(receiver: Receiver, expectedCount: Int, json: String): Boolean {
        try {
            echo(json)
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            val destinations = tree["destinations"] as ArrayNode
            for (i in 0 until destinations.size()) {
                val dest = destinations[i] as ObjectNode
                if (dest["service"].textValue() == receiver.name) {
                    return if (dest["itemCount"].intValue() == expectedCount) {
                        good("Test Passed: For ${receiver.name} expected $expectedCount and found $expectedCount")
                    } else {
                        bad(
                            "***Test FAILED***; For ${receiver.name} expected " +
                                "$expectedCount but got ${dest["itemCount"].intValue()}"
                        )
                    }
                }
            }
            if (expectedCount == 0)
                return good("Test Passed: No data went to ${receiver.name} dest")
            else
                return bad("***Test FAILED***: No data went to ${receiver.name} dest")
        } catch (e: Exception) {
            return bad("***$name Test FAILED***: Unexpected json returned for ${receiver.name}")
        }
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test")
        // ALLOW ALL
        ugly("\nTest the allowAll QualityFilter")
        val fakeItemCount = 5
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityAllReceiver.name, // Has the 'allowAll' quality filter
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                emptySender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}"
            )
        echo("Response to POST: $responseCode")

        var passed = true
        // if running in async mode, the initial response will not have destinations - the 'process' action result will
        if (options.asyncProcessMode) {
            val reportId = getReportIdFromResponse(json)
            if (reportId != null) {
                // gets back the id of the internal report
                val internalReportId = getSingleChildReportId(reportId)

                val processResults = pollForProcessResult(internalReportId)
                // verify each result is valid
                for (result in processResults.values)
                    passed = passed &&
                        examineProcessResponse(result) &&
                        checkJsonItemCountForReceiver(qualityAllReceiver, fakeItemCount, result!!)
            }
        } else
            passed = passed && checkJsonItemCountForReceiver(qualityAllReceiver, fakeItemCount, json)

        // QUALITY_PASS
        ugly("\nTest a QualityFilter that allows some data through")
        val file2 = FileUtilities.createFakeFile(
            metadata,
            settings,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityGoodReceiver.name + ",removed", // 3 kept, 2 removed
            options.dir,
        )
        echo("Created datafile $file2")
        // Now send it to ReportStream.
        val (responseCode2, json2) =
            HttpUtilities.postReportFile(
                environment,
                file2,
                emptySender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}"
            )
        echo("Response to POST: $responseCode2")
        // if running in async mode, the initial response will not have destinations - the 'process' action result will
        if (options.asyncProcessMode) {
            val reportId = getReportIdFromResponse(json2)
            if (reportId != null) {
                // gets back the id of the internal report
                val internalReportId2 = getSingleChildReportId(reportId)

                val processResults2 = pollForProcessResult(internalReportId2)
                // verify each result is valid
                for (result in processResults2.values)
                    passed = passed &&
                        examineProcessResponse(result) &&
                        checkJsonItemCountForReceiver(qualityGoodReceiver, fakeItemCount, result!!)
            }
        } else
            passed = passed && checkJsonItemCountForReceiver(qualityGoodReceiver, 3, json2)

        // FAIL
        ugly("\nTest a QualityFilter that allows NO data through.")
        val file3 = FileUtilities.createFakeFile(
            metadata,
            settings,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityFailReceiver.name,
            options.dir,
        )
        echo("Created datafile $file3")
        // Now send it to ReportStream.
        val (responseCode3, json3) =
            HttpUtilities.postReportFile(
                environment,
                file3,
                emptySender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode3")
        // if running in async mode, the initial response will not have destinations - the 'process' action result will
        if (options.asyncProcessMode) {
            val reportId = getReportIdFromResponse(json3)
            if (reportId != null) {
                // gets back the id of the internal report
                val internalReportId3 = getSingleChildReportId(reportId)

                val processResults3 = pollForProcessResult(internalReportId3)
                // verify each result is valid
                for (result in processResults3.values)
                    passed = passed &&
                        examineProcessResponse(result) &&
                        checkJsonItemCountForReceiver(qualityFailReceiver, fakeItemCount, result!!)
            }
        } else
            passed = passed && checkJsonItemCountForReceiver(qualityFailReceiver, 0, json3)

        // QUALITY_REVERSED
        ugly("\nTest the REVERSE of the QualityFilter that allows some data through")
        val file4 = FileUtilities.createFakeFile(
            metadata,
            settings,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityReversedReceiver.name + ",kept", // 3 removed, 2 kept
            options.dir,
        )
        echo("Created datafile $file4")
        // Now send it to ReportStream.
        val (responseCode4, json4) =
            HttpUtilities.postReportFile(
                environment,
                file4,
                emptySender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode4")
        // if running in async mode, the initial response will not have destinations - the 'process' action result will
        if (options.asyncProcessMode) {
            val reportId = getReportIdFromResponse(json4)
            if (reportId != null) {
                // gets back the id of the internal report
                val internalReportId4 = getSingleChildReportId(reportId)

                val processResults4 = pollForProcessResult(internalReportId4)
                // verify each result is valid
                for (result in processResults4.values)
                    passed = passed &&
                        examineProcessResponse(result) &&
                        checkJsonItemCountForReceiver(qualityReversedReceiver, fakeItemCount, result!!)
            }
        } else
            passed = passed && checkJsonItemCountForReceiver(qualityReversedReceiver, 2, json4)

        return passed
    }
}

class Huge : CoolTest() {
    override val name = "huge"
    override val description = "Submit $REPORT_MAX_ITEMS line csv file, wait, confirm via db.  Slow."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val fakeItemCount = REPORT_MAX_ITEMS
        ugly("Starting huge Test: Attempting to send a report with $fakeItemCount items. This is terrapin slow.")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            csvReceiver.name,
            options.dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                simpleRepSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***Huge Test FAILED***:  response code $responseCode")
        }
        val reportId = getReportIdFromResponse(json)
            ?: return bad("***$name Test FAILED***: A report ID came back as null")
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        return pollForLineageResults(
            reportId = reportId,
            receivers = listOf(csvReceiver),
            totalItems = fakeItemCount,
            asyncProcessMode = options.asyncProcessMode
        )
    }
}

// Bigger than Huge
class TooBig : CoolTest() {
    override val name = "toobig"
    override val description = "Submit ${REPORT_MAX_ITEMS + 1} lines, which should be an error.  Slower ;)"
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val fakeItemCount = REPORT_MAX_ITEMS + 1
        ugly("Starting toobig test: Attempting to send a report with $fakeItemCount items. This is slllooooowww.")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            csvReceiver.name,
            options.dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                simpleRepSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = (tree["errors"][0]) as ObjectNode
            if (firstError["details"].textValue().contains("rows")) {
                return good("toobig Test passed.")
            } else {
                return bad("***toobig Test Test FAILED***: Did not find the error")
            }
        } catch (e: Exception) {
            return bad("***toobig Test FAILED***: Unable to parse json response")
        }
    }
}

/**
 * Test weirdness in Staging wherein we have strange HL7 'send' numbers
 *
 * This test, when it fails, exposes a database connection exception in Staging.
 * I think this is actually passing now, but the query isn't quite right.
 */
class DbConnections : CoolTest() {
    override val name = "dbconnections"
    override val description = "Test issue wherein many 'sends' caused db connection failures"
    override val status = TestStatus.DRAFT

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting dbconnections Test: test of many threads attempting to sftp ${options.items} HL7s.")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            options.items,
            receivingStates,
            "HL7",
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.   Make numResends > 1 to create merges.
        val reportIds = (1..options.submits).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(
                    environment,
                    file,
                    simpleRepSender,
                    options.asyncProcessMode,
                    options.key,
                    payloadName = "$name ${status.description}",
                )
            echo("Response to POST: $responseCode")
            echo(json)
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                bad("***dbconnections Test FAILED***:  response code $responseCode")
                return false
            }
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            reportId
        }
        waitABit(30, environment)
        var passed = true
        reportIds.forEach {
            passed = passed and
                pollForLineageResults(
                    reportId = it,
                    receivers = listOf(hl7Receiver),
                    totalItems = options.items,
                    asyncProcessMode = options.asyncProcessMode
                )
        }
        return passed
    }
}

/**
 * Test using the a receiver with a broken sftp site.  Note:  there are two failure modes here:
 * - if the sftp auth stuff is set up, then RS get repeated IOException, and multiple retries.
 * - if its NOT set up, then RS fails without retries.
 *
 * Either way, the lineage results for the 'send' step should be zero.
 */
class BadSftp : CoolTest() {
    override val name = "badsftp"
    override val description = "Test ReportStream's response to sftp connection failures. Tests RETRY too!"
    override val status = TestStatus.DRAFT

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting badsftp Test: test that our code handles sftp connectivity problems")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            options.items,
            receivingStates,
            sftpFailReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment, file, simpleRepSender, options.asyncProcessMode, options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***badsftp Test FAILED***:  response code $responseCode")
            return false
        }
        val reportId = getReportIdFromResponse(json)
            ?: return bad("***$name Test FAILED***: A report ID came back as null")
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        echo("For this test, failure during send, is a 'pass'.   Need to fix this.")
        return pollForLineageResults(
            reportId = reportId,
            receivers = listOf(sftpFailReceiver),
            totalItems = options.items,
            asyncProcessMode = options.asyncProcessMode
        )
    }
}

/**
 * Generate a report with international characters to verify we can handle them.  This test will send the
 * report file and inspect the uploaded file to the SFTP server to make sure the international characters are
 * present.  This test can only be run locally because of the testing of the uploaded files.
 */
class InternationalContent : CoolTest() {
    override val name = "intcontent"
    override val description = "Create Fake data that includes international characters, " +
        "submit, wait, confirm sent via database lineage data"
    override val status = TestStatus.DRAFT // Because this can only be run local to get access to the SFTP folder

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        if (options.env != "local") {
            return bad(
                "***intcontent Test FAILED***: This test can only be run locally " +
                    "as it needs access to the SFTP folder."
            )
        }

        // Make sure we have access to the SFTP folder
        if (!Files.isDirectory(Paths.get(options.sftpDir))) {
            return bad("***intcontent Test FAILED***: The folder ${options.sftpDir} cannot be found.")
        }
        val receiverName = hl7Receiver.name
        ugly("Starting $name Test: send ${simpleRepSender.fullName} data to $receiverName")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            1,
            receivingStates,
            receiverName,
            options.dir,
            // Use the Chinese locale since the fake data is mainly Chinese characters
            // https://github.com/DiUS/java-faker/blob/master/src/main/resources/zh-CN.yml
            locale = Locale("zh_CN")
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment, file, simpleRepSender, options.asyncProcessMode, options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("***intcontent Test FAILED***:  response code $responseCode")
        }
        try {
            // Read the response
            val reportId = getReportIdFromResponse(json)
                ?: return bad("***$name Test FAILED***: A report ID came back as null")

            echo("Id of submitted report: $reportId")
            waitABit(60, environment)
            // Go to the database and get the SFTP filename that was sent
            db = WorkflowEngine().db
            var asciiOnly = false
            db.transact { txn ->
                val filename = sftpFilenameQuery(txn, reportId, receiverName)
                // If we get a file, test the contents to see if it is all ASCII only.
                if (filename != null) {
                    val contents = File(options.sftpDir, filename).inputStream().readBytes().toString(Charsets.UTF_8)
                    asciiOnly = CharMatcher.ascii().matchesAllOf(contents)
                }
            }
            if (asciiOnly) {
                return bad("***intcontent Test FAILED***: File contents are only ASCII characters")
            } else {
                return good("Test passed: for intcontent")
            }
        } catch (e: NullPointerException) {
            return bad("***intcontent Test FAILED***: Unable to properly parse response json")
        } catch (e: DataAccessException) {
            echo(e)
            return bad("***intcontent Test FAILED***: There was an error fetching data from the database.")
        }
    }
}

/**
 * Creates fake data as if from a sender and tries to send it to every state and territory
 */
class SantaClaus : CoolTest() {

    override val name = "santaclaus"
    override val description = "Creates fake data as if from a sender and tries to send it to every state and territory"
    override val status = TestStatus.DRAFT

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed = true
        if (options.env !in listOf("local", "staging")) {
            return createBad("This test can only be run locally or on staging")
        }
        var sendersToTestWith = settings.senders.filter {
            it.organizationName in listOf("simple_report", "waters", "strac", "safehealth")
        }
        options.sender?.let {
            // validates sender existence
            val sender = settings.findSender(it) ?: return createBad("The sender indicated doesn't exists '$it'")
            // replace the list of senders to test
            // with the indicated by parameter
            sendersToTestWith = listOf(sender)
        }

        val states = if (options.targetStates.isNullOrEmpty()) {
            metadata.findLookupTable("fips-county")?.getDistinctValuesInColumn("State")
                ?.toList() ?: error("Santa is unable to find any states in the fips-county table")
        } else {
            options.targetStates.split(",")
        }
        ugly("Santa is sending data to these nice states: $states")

        sendersToTestWith.forEach { sender ->
            ugly("Starting $name Test: send with ${sender.fullName}")
            val file = FileUtilities.createFakeFile(
                metadata = metadata,
                settings = settings,
                sender = sender,
                count = states.size,
                format = if (sender.format == Sender.Format.CSV) Report.Format.CSV else Report.Format.HL7_BATCH,
                directory = System.getProperty("java.io.tmpdir"),
                targetStates = states.joinToString(","),
                targetCounties = null
            )
            echo("Created datafile $file")
            // Now send it to ReportStream.
            val (responseCode, json) =
                HttpUtilities.postReportFile(
                    environment, file, sender, options.asyncProcessMode, options.key,
                    payloadName = "$name ${status.description}",
                )
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                return bad("***$name Test FAILED***:  response code $responseCode")
            } else {
                good("Posting of report succeeded with response code $responseCode")
            }
            echo(json)
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = getReportIdFromResponse(json)
                ?: return bad("***$name Test FAILED***: A report ID came back as null")
            val destinations = tree["destinations"]
            if (destinations != null && destinations.size() > 0) {
                val receivers = mutableListOf<Receiver>()
                destinations.forEach { destination ->
                    if (destination != null && destination.has("service")) {
                        val receiverName = destination["service"].textValue()
                        val organizationId = destination["organization_id"].textValue()
                        receivers.addAll(
                            settings.receivers.filter {
                                it.organizationName == organizationId && it.name == receiverName
                            }
                        )
                    }
                }
                if (!receivers.isNullOrEmpty()) {
                    passed = passed and pollForLineageResults(
                        reportId = reportId,
                        receivers = receivers,
                        totalItems = receivers.size,
                        filterOrgName = true,
                        silent = false,
                        asyncProcessMode = options.asyncProcessMode
                    )
                }
            }
        }
        return passed
    }

    /**
     * Executes the 'block' function parameter and
     * evaluates its boolean result. If it's false it
     * retries the execution in 1 second, at most the
     * quantity of retries indicated by parameter.
     *
     * @param retries Max times to repeat the execution of @param[block] while it returns false
     * @param block Function to evaluate its result
     * @param callback Returns the state of the last execution and the retry count
     *
     * @return true if the @param[block] returns true, false otherwise or if the retry count reaches to its limit
     */
    private fun waitWithConditionalRetry(
        retries: Int,
        block: () -> Boolean,
        callback: (succeed: Boolean, retryCount: Int) -> Unit
    ): Boolean {

        var retriesCopy = retries

        while (retriesCopy > 0) {
            val blockSuccess = block()
            callback(blockSuccess, abs(retriesCopy - retries))
            if (blockSuccess) {
                return true
            }
            retriesCopy--
            sleep(1000)
        }

        return false
    }

    private fun createBad(message: String): Boolean {
        return bad("***$name Test FAILED***: $message")
    }
}

class OtcProctored : CoolTest() {
    override val name = "otcproctored"
    override val description = "Verify that otc/proctored flags are working as expected on api response"
    override val status = TestStatus.SMOKE
    val failures = mutableListOf<String>()

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val otcPairs = listOf(
            Pair("BinaxNOW COVID-19 Antigen Self Test_Abbott Diagnostics Scarborough, Inc.", "OTC_PROCTORED_YYY"),
            Pair("QuickVue At-Home COVID-19 Test_Quidel Corporation", "OTC_PROCTORED_NYY"),
            Pair("00810055970001", "OTC_PROCTORED_NUNKUNK"),
        )
        for (pair in otcPairs) {
            ugly(
                "Starting Otc Test: submitting a file containing a device_id:" +
                    " ${pair.first} should match receiver ${pair.second}."
            )
            val reFile = FileUtilities.replaceText(
                "./src/test/csv_test_files/input/otc-template.csv",
                "replaceMe",
                "${pair.first}"
            )

            if (!reFile.exists()) {
                error("Unable to find file ${reFile.absolutePath} to do otc test")
            }
            val (responseCode, json) = HttpUtilities.postReportFile(
                environment,
                reFile,
                watersSender,
                options.asyncProcessMode,
                options.key
            )

            echo("Response to POST: $responseCode")
            if (examinePostResponse(json, !options.asyncProcessMode)) {
                // if testing async, verify the process result was generated
                if (options.asyncProcessMode) {
                    val reportId = getReportIdFromResponse(json)
                    if (reportId != null) {
                        // gets back the id of the internal report
                        val internalReportId = getSingleChildReportId(reportId)

                        val processResults = pollForProcessResult(internalReportId)
                        // verify each result is valid
                        for (result in processResults.values)
                            if (!examineProcessResponse(result))
                                bad("***async end2end FAILED***: Process result invalid")
                    }
                }
                good("Test PASSED: ${pair.first}")
            } else {
                bad("Test FAILED: ${pair.first}")
                failures.add("${pair.first}")
            }
        }

        if (failures.size == 0) {
            return true
        } else {
            return bad("Tests FAILED: $failures")
        }
    }
}