package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.LegacyPipelineSender
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.Tables.ACTION_LOG
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.enums.ActionLogType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.SystemExitCodes
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.azure.DatabaseSubmissionsAccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

enum class TestStatus(val description: String) {
    DRAFT("Experimental Test"), // Tests that are experimental
    FAILS("(Always fails)"), // For tests that just always fail, and we haven't fixed the issue yet.
    LOAD("Load Test"),
    SMOKE("Smoke Test"), // Only Smoke the Good Stuff.
}

/**
 * This class implements the `./prime test` commands, which runs one or more [CoolTest]
 * Each individual Test then implements [CoolTest].
 *
 */
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
            exitProcess(SystemExitCodes.FAILURE.exitCode)
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
            exitProcess(SystemExitCodes.SUCCESS.exitCode)
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
            echo(
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
                        e.stackTrace.joinToString(System.lineSeparator())
                )
                false
            }
            test.outputAllMsgs()
            test.echo("********************************")
            if (!passed) {
                failures.add(test)
            }
        }

        if (runSequential) {
            runBlocking {
                tests.forEach { test ->
                    launch { runTest(test) }
                }
            }
        } else {
            tests.forEach { test ->
                runBlocking {
                    runTest(test)
                }
            }
        }

        if (failures.isNotEmpty()) {
            echo(
                CoolTest
                    .badMsgFormat("*** Tests FAILED:  ${failures.joinToString(",") { it.name }} ***")
            )
            exitProcess(SystemExitCodes.FAILURE.exitCode)
        } else {
            echo(
                CoolTest.goodMsgFormat("All tests passed")
            )
        }
    }

    companion object {
        val coolTestList = listOf(
            Ping(),
            SftpcheckTest(),
            Merge(),
            Server2ServerAuthTests(),
            OktaAuthTests(),
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
            SenderSettings(),
            InternationalContent(),
            DataCompareTest(),
            SantaClaus(),
            DbConnections(),
            BadSftp(),
            Garbage(),
            SettingsTest(),
            HistoryApiTest(),
            Huge(),
            TooBig(),
            QuickLoad(),
            ProductionLoad(),
            DbConnectionsLoad(),
            LongLoad(),
            ABot(),
            LivdApiTest(),
            End2End(),
            End2EndUniversalPipeline(),
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
     * Clikt has hidden the TermUI namespace which we were depending on. This function property
     * allows us to wire it into TermUI if we want, but for now, the logic exactly mirrors what's in
     * the TermUI echo function.
     */
    var echoFn: (
        message: Any?,
        trailingNewline: Boolean,
        err: Boolean,
        lineSeparator: String
    ) -> Unit = fun(
        /** [message] is what you want to write to the command line. it will have `toString` called on it */
        message: Any?,
        /** Flag for appending a trailing newline to the what you're writing to the output stream */
        trailingNewline: Boolean,
        /** Flag for whether or not to write to stderr instead of stdout */
        err: Boolean,
        /** The line separator, typically \n, though could be \r\n if you're on Windows */
        lineSeparator: String
    ) {
        // munge the text
        val text = message?.toString()?.replace(Regex("\r?\n"), lineSeparator) ?: "null"
        // get the stream per error or out
        val stream = if (err) System.err else System.out
        // write it out
        stream.print(if (trailingNewline) text + lineSeparator else text)
    }

    /**
     * Stores a list of output messages instead of printing the messages to the console.
     */
    val outputMsgs = mutableListOf<String>()

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
            echoFn(msg, true, false, "\n")
        else
            outputMsgs.add(msg)
    }

    /**
     * Output all messages to the console.
     */
    fun outputAllMsgs() {
        outputMsgs.forEach { echoFn(it, true, false, "\n") }
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
     * Polls for the json result of the [taskAction] action for [reportId]
     */
    suspend fun pollForStepResult(
        reportId: ReportId,
        taskAction: TaskAction,
        maxPollSecs: Int = 840,
        pollSleepSecs: Int = 20,
    ): Map<UUID, DetailedSubmissionHistory?> {
        var timeElapsedSecs = 0
        var queryResult = emptyMap<UUID, DetailedSubmissionHistory?>()
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
                queryResult = queryForStepResults(reportId, taskAction)
                if (queryResult.isNotEmpty())
                    break
            }
        }
        echo("Polling for PROCESS records finished in ${actualTimeElapsedMillis / 1000} seconds")

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
                where cr.report_id = ?
            """
            val ret = ctx.fetch(sql, reportId).into(Int::class.java)
            @Suppress("SENSELESS_COMPARISON")
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
    ): ReportId? {
        var childReportId: ReportId? = null
        db = WorkflowEngine().db
        db.transact { txn ->
            val ctx = DSL.using(txn)
            // get internally generated reportId
            childReportId = ctx.selectFrom(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.eq(reportId))
                .fetchOne(REPORT_LINEAGE.CHILD_REPORT_ID)
        }
        return childReportId
    }

    /**
     * Gets all children of the passed in [reportId].
     */
    fun getAllChildrenReportId(
        reportId: ReportId,
    ): List<ReportId> {
        var childReportId: List<ReportId> = listOf()
        db = WorkflowEngine().db
        db.transact { txn ->
            val ctx = DSL.using(txn)
            // get internally generated reportId
            childReportId = ctx.selectFrom(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.eq(reportId))
                .fetch(REPORT_LINEAGE.CHILD_REPORT_ID)
        }
        return childReportId
    }

    /**
     * Returns all children produced by the process step for the parent [reportId], along with json response for each
     */
    private fun queryForStepResults(
        reportId: ReportId,
        taskAction: TaskAction
    ): Map<UUID, DetailedSubmissionHistory?> {
        var queryResult = emptyMap<UUID, DetailedSubmissionHistory?>()
        db = WorkflowEngine().db
        db.transact { txn ->
            queryResult = stepActionResultQuery(txn, reportId, taskAction)
        }
        return queryResult
    }

    /**
     * Examine the [history] from the process action, makes sure there is at least one destination reported
     * and report any errors
     * @return true if there are no errors in the response, false otherwise
     */
    fun examineProcessResponse(history: DetailedSubmissionHistory?): Boolean {

        var passed = true
        try {
            // if there is no process response, this test fails
            if (history == null)
                return bad("Test Failed: No process response")

            val reportId = history.reportId
            echo("Id of submitted report: $reportId")
            val topic = history.topic
            val errorCount = history.errorCount

            if (topic != null && topic.equals(Topic.COVID_19)) {
                good("'topic' is in response and correctly set to 'covid-19'")
            } else if (topic == null) {
                passed = bad("***$name Test FAILED***: 'topic' is missing from response json")
            } else {
                passed = bad("***$name Test FAILED***: unexpected 'topic' $topic in response json")
            }

            if (errorCount == 0) {
                good("No errors detected.")
            } else {
                passed = bad("***$name Test FAILED***: There were errors reported.")
            }

            if (reportId == null) {
                passed = bad("***$name Test FAILED***: Report ID was empty.")
            }
        } catch (e: NullPointerException) {
            passed = bad("***$name Test FAILED***: Unable to properly parse response json")
        }
        return passed
    }

    /**
     * Examine the [history] from the convert action, make sure it successfully converted and there is a fhir bundle
     * @return true if there are no errors in the response, false otherwise
     */
    fun examineStepResponse(history: DetailedSubmissionHistory?, step: String, senderTopic: Topic): Boolean {

        var passed = true
        try {
            // if there is no response, this test fails
            if (history == null)
                return bad("Test Failed: No $step response")

            val reportId = history.reportId
            echo("Id of submitted report: $reportId")
            val topic = history.topic
            val errorCount = history.errorCount

            if (topic != null && topic == senderTopic) {
                good("'topic' is in response and correctly set to $topic")
            } else if (topic == null) {
                passed = bad("***$name Test FAILED***: 'topic' is missing from response json")
            } else {
                passed =
                    bad("***$name Test FAILED***: expected 'topic' $senderTopic, but found $topic in response json")
            }

            if (errorCount == 0) {
                good("No errors detected.")
            } else {
                passed = bad("***$name Test FAILED***: There were errors reported.")
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
        maxPollSecs: Int = 840,
        pollSleepSecs: Int = 30, // I had this as every 5 secs, but was getting failures.  The queries run unfastly.
        asyncProcessMode: Boolean = false,
        isUniversalPipeline: Boolean = false
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
                queryResults = queryForLineageResults(
                    reportId,
                    receivers,
                    totalItems,
                    filterOrgName,
                    asyncProcessMode,
                    isUniversalPipeline
                )
                if (!queryResults.map { it.first }.contains(false))
                    break // everything passed!
            }
        }
        echo("Test $name finished in ${actualTimeElapsedMillis / 1000} seconds")
        if (!silent) {
            queryResults.forEach {
                if (it.first)
                    good(it.second)
                else
                    bad(it.second)
            }
        }
        return !queryResults.map { it.first }.contains(false) // no falses == it passed!
    }

    private fun queryForLineageResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        totalItems: Int,
        filterOrgName: Boolean = false,
        asyncProcessMode: Boolean = false,
        isUniversalPipeline: Boolean
    ): List<Pair<Boolean, String>> {
        val queryResults = mutableListOf<Pair<Boolean, String>>()
        db = WorkflowEngine().db
        db.transact { txn ->
            receivers.forEach { receiver ->
                val actionsList = mutableListOf(TaskAction.receive)
                // Bug:  this is looking at local cli data, but might be querying staging or prod.
                // The hope is that the 'ignore' org is same in local, staging, prod.
                if (asyncProcessMode && receiver.topic == Topic.COVID_19) actionsList.add(TaskAction.process)
                if (receiver.topic.isUniversalPipeline) {
                    actionsList.add(TaskAction.convert)
                    actionsList.add(TaskAction.route)
                    actionsList.add(TaskAction.translate)
                }
                if (receiver.timing != null) actionsList.add(TaskAction.batch)
                if (receiver.transport != null) actionsList.add(TaskAction.send)
                actionsList.forEach { action ->
                    val useRecevingServiceName = !(
                        (action == TaskAction.receive && asyncProcessMode) ||
                            action == TaskAction.convert ||
                            action == TaskAction.route
                        )
                    val count = itemLineageCountQuery(
                        txn = txn,
                        reportId = reportId,
                        // if we are processing asynchronously the receive step doesn't have any receivers yet
                        receivingOrgSvc = if (useRecevingServiceName) receiver.name else null,
                        receivingOrg = if (filterOrgName) receiver.organizationName else null,
                        action = action,
                        isUniversalPipeline = isUniversalPipeline
                    )
                    val expected = if (action == TaskAction.receive && asyncProcessMode && !isUniversalPipeline) {
                        totalItems
                    } else totalItems / receivers.size
                    queryResults += if (count == null || expected != count) {
                        Pair(
                            false,
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records but got $count"
                        )
                    } else {
                        Pair(
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
        if (!tree.isNull && !tree["reportId"].isNull) {
            reportId = ReportId.fromString(tree["reportId"].textValue())
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

            if (topic != null && !topic.isNull &&
                (
                    listOf(
                            Topic.COVID_19.jsonVal,
                            Topic.FULL_ELR.jsonVal,
                            Topic.ETOR_TI.jsonVal,
                            Topic.ELR_ELIMS.jsonVal
                        ).contains(topic.textValue())
                    )
            ) {
                good("'topic' is in response and correctly set")
            } else if (topic == null) {
                passed = bad("***$name Test FAILED***: 'topic' is missing from response json")
            } else {
                passed = bad("***$name Test FAILED***: unexpected 'topic' $topic in response json")
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
        const val org1Name = "ignore"
        val org1 = settings.findOrganization(org1Name)
            ?: error("Unable to find org $org1Name in metadata")
        const val org2Name = "waters"
        val org2 = settings.findOrganization(org2Name)
            ?: error("Unable to find org $org2Name in metadata")
        const val receivingStates = "IG"

        const val fullELRSenderName = "ignore-full-elr"
        val fullELRSender by lazy {
            settings.findSender("$org1Name.$fullELRSenderName") as? UniversalPipelineSender
                ?: error("Unable to find sender $fullELRSenderName for organization ${org1.name}")
        }

        const val etorTISenderName = "ignore-etor-ti"
        val etorTISender by lazy {
            settings.findSender("$org1Name.$etorTISenderName") as? UniversalPipelineSender
                ?: error("Unable to find sender $etorTISenderName for organization ${org1.name}")
        }

        const val elrElimsSenderName = "ignore-elr-elims"
        val elrElimsSender by lazy {
            settings.findSender("$org1Name.$elrElimsSenderName") as? UniversalPipelineSender
                ?: error("Unable to find sender $elrElimsSenderName for organization ${org1.name}")
        }

        const val simpleReportSenderName = "ignore-simple-report"
        val simpleRepSender by lazy {
            settings.findSender("$org1Name.$simpleReportSenderName") as? LegacyPipelineSender
                ?: error("Unable to find sender $simpleReportSenderName for organization ${org1.name}")
        }

        const val stracSenderName = "ignore-strac"
        val stracSender by lazy {
            settings.findSender("$org1Name.$stracSenderName") as? LegacyPipelineSender
                ?: error("Unable to find sender $stracSenderName for organization ${org1.name}")
        }

        const val watersSenderName = "ignore-waters"
        val watersSender by lazy {
            settings.findSender("$org1Name.$watersSenderName") as? LegacyPipelineSender
                ?: error("Unable to find sender $watersSenderName for organization ${org1.name}")
        }

        const val emptySenderName = "ignore-empty"
        val emptySender by lazy {
            settings.findSender("$org1Name.$emptySenderName") as? LegacyPipelineSender
                ?: error("Unable to find sender $emptySenderName for organization ${org1.name}")
        }

        const val hl7SenderName = "ignore-hl7"
        val hl7Sender by lazy {
            settings.findSender("$org1Name.$hl7SenderName") as? LegacyPipelineSender
                ?: error("Unable to find sender $hl7SenderName for organization ${org1.name}")
        }

        const val hl7MonkeypoxSenderName = "ignore-monkeypox"
        val hl7MonkeypoxSender by lazy {
            settings.findSender("$org1Name.$hl7MonkeypoxSenderName") as? LegacyPipelineSender
                ?: error("Unable to find sender $hl7MonkeypoxSenderName for organization ${org1.name}")
        }

        val universalPipelineReceiver1 = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "FULL_ELR"
        }[0]
        val universalPipelineReceiver2 = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "FULL_ELR_FHIR"
        }[0]
        val etorReceiver = settings.receivers.first { it.topic == Topic.ETOR_TI }
        val elimsReceiver = settings.receivers.first { it.topic == Topic.ELR_ELIMS }
        val csvReceiver = settings.receivers.filter { it.organizationName == org1Name && it.name == "CSV" }[0]
        val hl7Receiver = settings.receivers.filter { it.organizationName == org1Name && it.name == "HL7" }[0]
        val hl7BatchReceiver =
            settings.receivers.filter { it.organizationName == org1Name && it.name == "HL7_BATCH" }[0]
        val hl7NullReceiver = settings.receivers.filter { it.organizationName == org1Name && it.name == "HL7_NULL" }[0]
        val hl7PpkReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "HL7_BATCH_PPK"
        }[0]
        val hl7PemReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "HL7_BATCH_PEM"
        }[0]

        lateinit var allGoodReceivers: MutableList<Receiver>
        lateinit var allGoodCounties: String
        const val historyTestOrgName = "historytest"
        val historyTestSender = (
            settings.findSender("$historyTestOrgName.default")
                ?: error("Unable to find sender $historyTestOrgName.default")
            ) as CovidSender
        val defaultIgnoreSender = (
            settings.findSender("$org1Name.default")
                ?: error("Unable to find sender $org1Name.default")
            ) as CovidSender

        fun initListOfGoodReceiversAndCountiesForTopicPipeline() {
            allGoodReceivers = mutableListOf(
                csvReceiver, hl7Receiver,
                hl7BatchReceiver, hl7NullReceiver
            )
            allGoodCounties = allGoodReceivers.joinToString(",") { it.name }
        }

        fun initListOfGoodReceiversAndCountiesForUniversalPipeline() {
            allGoodReceivers = mutableListOf(universalPipelineReceiver1)
            allGoodCounties = allGoodReceivers.joinToString(",") { it.name }
        }

        val blobstoreReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "BLOBSTORE"
        }[0]
        val sftpFailReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "SFTP_FAIL"
        }[0]
        val qualityGoodReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "QUALITY_PASS"
        }[0]
        val qualityAllReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "QUALITY_ALL"
        }[0]
        val qualityFailReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "QUALITY_FAIL"
        }[0]
        val qualityReversedReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "QUALITY_REVERSED"
        }[0]
        val settingsTestReceiver = settings.receivers.filter {
            it.organizationName == org1Name && it.name == "SETTINGS_TEST"
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
         * @return String representing the jsonb value of action_result for the [taskAction] action for this report
         */
        fun stepActionResultQuery(
            txn: DataAccessTransaction,
            reportId: UUID,
            taskAction: TaskAction
        ): Map<UUID, DetailedSubmissionHistory?> {
            val ctx = DSL.using(txn)

            // get the child reports of the report passed in
            val childReportIds = ctx.selectFrom(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.eq(reportId))
                .fetch(REPORT_LINEAGE.CHILD_REPORT_ID)
            val actionResponses = mutableMapOf<UUID, DetailedSubmissionHistory?>()
            for (childReportId in childReportIds) {
                val report = ctx.selectFrom(Tables.REPORT_FILE)
                    .where(Tables.REPORT_FILE.REPORT_ID.eq(childReportId))
                    .fetchOne()
                if (report != null && report.actionId != null) {
                    val ret = ctx.select(
                        DatabaseSubmissionsAccess().detailedSelect()
                    )
                        .from(ACTION)
                        .where(
                            ACTION.ACTION_NAME.eq(taskAction)
                                .and(ACTION.ACTION_ID.eq(report.actionId))
                        )
                        .fetchOne()?.into(DetailedSubmissionHistory::class.java)
                    // Fill out the rest of the history data
                    if (ret != null) {
                        ret.reportId = childReportId.toString()
                        ret.reportItemCount = report.itemCount
                        ret.externalName = report.externalName
                        ret.topic = report.schemaTopic
                        if (!report.sendingOrg.isNullOrBlank() && !report.sendingOrgClient.isNullOrBlank())
                            ret.sender = ClientSource(report.sendingOrg, report.sendingOrgClient).name

                        // Get errors and warnings
                        ret.logs = ctx.selectFrom(ACTION_LOG).where(
                            ACTION_LOG.ACTION_ID.eq(report.actionId)
                                .and(ACTION_LOG.REPORT_ID.eq(childReportId))
                                .and(ACTION_LOG.TYPE.eq(ActionLogType.warning))
                        ).fetchInto(DetailedActionLog::class.java)
                    }
                    actionResponses[childReportId] = ret
                } else actionResponses[childReportId] = null
            }

            return actionResponses
        }

        /**
         * Returns the count of item lineages for the parent [reportId] passed in for the [action] specified.
         * If a [receivingOrgSvc] or [receivingOrg] are specified, only the lineages that match those values will
         * be counted. If this is looking for Universal Pipeline lineage count, use a different query due to how
         * parent/child relationships work in lineage
         * @return Count of matching records.
         */
        fun itemLineageCountQuery(
            txn: DataAccessTransaction,
            reportId: ReportId,
            receivingOrgSvc: String? = null,
            receivingOrg: String? = null,
            action: TaskAction,
            isUniversalPipeline: Boolean
        ): Int? {
            val ctx = DSL.using(txn)
            return if (isUniversalPipeline) {
                val sql = """
                select count(*)
                from (
                    select *
                    from (
                        select il.item_lineage_id, a.action_name, rf.receiving_org, rf.receiving_org_svc
                        from item_lineage il
                        inner join report_file rf on il.child_report_id = rf.report_id
                        inner join action a on a.action_id = rf.action_id
                        where item_lineage_id in (select * from item_descendants(?))
                        and a.action_name != 'receive'
                        union
                        select il.item_lineage_id, a.action_name, rf.receiving_org, rf.receiving_org_svc
                        from item_lineage il
                        inner join report_file rf on il.parent_report_id = rf.report_id
                        inner join action a on a.action_id = rf.action_id
                        where item_lineage_id in (select * from item_descendants(?))
                        and a.action_name = 'receive'
                    ) all_lineage
                  where
                  action_name = ? 
                  ${if (receivingOrgSvc != null) "and receiving_org_svc = ?" else ""}
                  ${if (receivingOrg != null) "and receiving_org = ?" else ""}
              ) results
                """
                if (receivingOrg != null && receivingOrgSvc != null) {
                    ctx.fetchOne(sql, reportId, reportId, action, receivingOrgSvc, receivingOrg)?.into(Int::class.java)
                } else if (receivingOrgSvc != null) {
                    ctx.fetchOne(sql, reportId, reportId, action, receivingOrgSvc)?.into(Int::class.java)
                } else {
                    ctx.fetchOne(sql, reportId, reportId, action)?.into(Int::class.java)
                }
            } else {
                val sql = """select count(*)
              from item_lineage as IL
              join report_file as RF on IL.child_report_id = RF.report_id
              join action as A on A.action_id = RF.action_id
              where
              ${if (receivingOrgSvc != null) "RF.receiving_org_svc = ? and" else ""}
              ${if (receivingOrg != null) "RF.receiving_org = ? and" else ""}
              A.action_name = ?
              and IL.item_lineage_id in
              (select item_descendants(?)) 
                """

                if (receivingOrg != null && receivingOrgSvc != null) {
                    ctx.fetchOne(sql, receivingOrgSvc, receivingOrg, action, reportId)?.into(Int::class.java)
                } else if (receivingOrgSvc != null) {
                    ctx.fetchOne(sql, receivingOrgSvc, action, reportId)?.into(Int::class.java)
                } else {
                    ctx.fetchOne(sql, action, reportId)?.into(Int::class.java)
                }
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
                order by A.action_id 
            """
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