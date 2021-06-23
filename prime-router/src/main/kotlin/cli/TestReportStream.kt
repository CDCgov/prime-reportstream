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
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.base.CharMatcher
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
import gov.cdc.prime.router.azure.db.Tables.ACTION
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.cli.tests.Hl7Ingest
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
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

enum class TestStatus(val description: String) {
    DRAFT("Experimental"), // Tests that are experimental
    FAILS("Always fails"), // For tests that just always fail, and we haven't fixed the issue yet.
    LOAD("Load Test"),
    SMOKE("Part of Smoke test"), // Only Smoke the Good Stuff.
}

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

    private val sftpDir by option(
        "--sftpdir",
        help = "specify the folder where files were uploaded to the SFTP server.  Default is $SFTP_DIR"
    ).default(SFTP_DIR)

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

        // Create directory if it does not exist
        val dirPath = Paths.get(dir)
        if (Files.notExists(dirPath)) Files.createDirectory(dirPath)

        if (list) {
            echo("Available options to --run <test1,test2> are:")
            printTestList(coolTestList)
            exitProcess(0)
        }
        val environment = ReportStreamEnv.valueOf(env.uppercase())

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
            CoolTest.ugly("Running the following tests, POSTing to ${environment.endPoint}:")
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

    private fun runTests(tests: List<CoolTest>, environment: ReportStreamEnv) {
        val failures = mutableListOf<CoolTest>()
        val options = CoolTestOptions(items, submits, key, dir, sftpDir = sftpDir, env = env)
        tests.forEach { test ->
            if (!test.run(environment, options))
                failures.add(test)
        }
        if (failures.isNotEmpty()) {
            CoolTest.bad("*** Tests FAILED:  ${failures.map { it.name }.joinToString(",")} ***")
        } else {
            CoolTest.good("All tests passed")
        }
    }

    companion object {
        val coolTestList = listOf<CoolTest>(
            Ping(),
            End2End(),
            Merge(),
            Garbage(),
            QualityFilter(),
            Hl7Null(),
            TooManyCols(),
            BadCsv(),
            Strac(),
            Huge(),
            TooBig(),
            DbConnections(),
            BadSftp(),
            StracPack(),
            HammerTime(),
            Waters(),
            RepeatWaters(),
            InternationalContent(),
            Hl7Ingest()
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
    val env: String
)

abstract class CoolTest {
    abstract val name: String
    abstract val description: String
    abstract val status: TestStatus

    abstract fun run(
        environment: ReportStreamEnv,
        options: CoolTestOptions
    ): Boolean

    lateinit var db: DatabaseAccess

    fun examineLineageResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        totalItems: Int,
    ): Boolean {
        var passed = true
        db = WorkflowEngine().db
        db.transact { txn ->
            val expected = totalItems / receivers.size
            receivers.forEach { receiver ->
                val actionsList = mutableListOf(TaskAction.receive)
                // Bug:  this is looking at local cli data, but might be querying staging or prod.
                if (receiver.timing != null) actionsList.add(TaskAction.batch)
                if (receiver.transport != null) actionsList.add(TaskAction.send)
                actionsList.forEach { action ->
                    val count = itemLineageCountQuery(txn, reportId, receiver.name, action)
                    if (count == null || expected != count) {
                        bad(
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records but got $count"
                        )
                        passed = false
                    } else {
                        good(
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records and got $count"
                        )
                    }
                }
            }
        }
        return passed
    }

    fun examineMergeResults(
        reportId: ReportId,
        receivers: List<Receiver>,
        itemsPerReport: Int,
        reportCount: Int
    ): Boolean {
        var passed = true
        db = WorkflowEngine().db
        db.transact { txn ->
            val expected = (itemsPerReport * reportCount) / receivers.size
            receivers.forEach { receiver ->
                val actionsList = mutableListOf<TaskAction>()
                if (receiver.timing != null) actionsList.add(TaskAction.batch)
                if (receiver.transport != null) actionsList.add(TaskAction.send)
                actionsList.forEach { action ->
                    val count = reportLineageCountQuery(txn, reportId, receiver.name, action)
                    if (count == null || expected != count) {
                        bad(
                            "*** TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting sum(itemCount)=$expected but got $count"
                        )
                        passed = false
                    } else {
                        good(
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting sum(itemCount)=$expected and got $count"
                        )
                    }
                }
            }
        }
        return passed
    }

    companion object {
        val metadata = Metadata(Metadata.defaultMetadataDirectory)
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

        val allGoodReceivers = settings.receivers.filter {
            it.organizationName == orgName &&
                !it.name.contains("FAIL") &&
                !it.name.contains("BLOBSTORE") &&
                !it.name.contains("QUALITY") &&
                !it.name.contains("AS2")
        }
        val allGoodCounties = allGoodReceivers.map { it.name }.joinToString(",")

        val csvReceiver = allGoodReceivers.filter { it.name == "CSV" }[0]
        val hl7Receiver = allGoodReceivers.filter { it.name == "HL7" }[0]
        val hl7BatchReceiver = allGoodReceivers.filter { it.name == "HL7_BATCH" }[0]
        val redoxReceiver = allGoodReceivers.filter { it.name == "REDOX" }[0]
        val hl7NullReceiver = allGoodReceivers.filter { it.name == "HL7_NULL" }[0]
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
        fun good(msg: String): Boolean {
            echo(ANSI_GREEN + msg + ANSI_RESET)
            return true
        }
        fun bad(msg: String): Boolean {
            echo(ANSI_RED + msg + ANSI_RESET)
            return false
        }
        fun ugly(msg: String) {
            echo(ANSI_CYAN + msg + ANSI_RESET)
        }

        /**
         * A hack attempt to wait enough time, but not too long, for ReportStream to finish.
         * This assumes the batch/send executes on the minute boundary.
         */
        fun waitABit(plusSecs: Int, env: ReportStreamEnv, silent: Boolean = false) {
            // seconds elapsed so far in this minute
            val secsElapsed = OffsetDateTime.now().second % 60
            // Wait until the top of the next minute, and pluSecs more, for 'batch', and 'send' to finish.
            var waitSecs = 60 - secsElapsed + plusSecs
            if (secsElapsed > (60 - plusSecs) || env != ReportStreamEnv.LOCAL) {
                // Uh oh, we are close to the top of the minute *now*, so 'receive' might not finish in time.
                // Or, we are in Test or Staging, which don't execute on the top of the minute.
                waitSecs += 90
            }
            echo("Waiting $waitSecs seconds for ReportStream to fully receive, batch, and send the data")
            for (i in 1..waitSecs) {
                sleep(1000)
                if (!silent) print(".")
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting ping Test: run CheckConnections of ${environment.endPoint}")
        val (responseCode, json) = HttpUtilities.postReportBytes(
            environment,
            "x".toByteArray(),
            orgName,
            simpleRepSender,
            options.key,
            ReportFunction.Options.CheckConnections
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        var passed = true
        ugly("Starting $name Test: send ${simpleRepSender.fullName} data to $allGoodCounties")
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***end2end Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of report succeeded with response code $responseCode")
        }
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            val topic = tree["topic"]
            if (topic != null && !topic.isNull && topic.textValue().equals("covid-19", true)) {
                good("'topic' is in response and correctly set to 'covid-19'")
            } else {
                bad("***end2end Test FAILED***: 'topic' is missing from response json")
                passed = false
            }
            waitABit(25, environment)
            return passed and examineLineageResults(reportId, allGoodReceivers, fakeItemCount)
        } catch (e: NullPointerException) {
            return bad("***end2end Test FAILED***: Unable to properly parse response json")
        }
    }
}

class Merge : CoolTest() {
    override val name = "merge"
    override val description = "Submit multiple files, wait, confirm via db that merge occurred"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        // Remove HL7 - it does not merge   TODO write a notMerging test for HL7, but its similar to end2end
        val mergingReceivers = listOf<Receiver>(csvReceiver, hl7BatchReceiver, redoxReceiver)
        val mergingCounties = mergingReceivers.map { it.name }.joinToString(",")
        val fakeItemCount = mergingReceivers.size * options.items
        ugly("Starting merge test:  Merge ${options.submits} reports, each of which sends to $allGoodCounties")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            mergingCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream over and over
        val reportIds = (1..options.submits).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
            echo("Response to POST: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                return bad("***Merge Test FAILED***:  response code $responseCode")
            }
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            reportId
        }
        waitABit(40, environment)
        return examineMergeResults(reportIds[0], mergingReceivers, fakeItemCount, options.submits)
    }
}

/**
 * Test using the NULL transport.
 */
class Hl7Null : CoolTest() {
    override val name = "hl7null"
    override val description = "The NULL transport does db work, but no transport.  Uses HL7 format"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        val fakeItemCount = 100
        ugly("Starting hl7null Test: test of many threads all doing database interactions, but no sends. ")
        val file = FileUtilities.createFakeFile(
            metadata,
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
                HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
            echo("Response to POST: $responseCode")
            echo(json)
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                return bad("***hl7null Test FAILED***:  response code $responseCode")
            }
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            reportId
        }
        waitABit(30, environment)
        return examineLineageResults(reportIds[0], listOf(hl7NullReceiver), fakeItemCount)
    }
}

class TooManyCols : CoolTest() {
    override val name = "toomanycols"
    override val description = "Submit a file with more than $REPORT_MAX_ITEM_COLUMNS columns, which should error"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting toomanycols Test: submitting a file with too many columns.")
        val file = File("./src/test/csv_test_files/input/too-many-columns.csv")
        if (!file.exists()) {
            error("Unable to find file ${file.absolutePath} to do toomanycols test")
        }
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = ((tree["errors"] as ArrayNode)[0]) as ObjectNode
            if (firstError["details"].textValue().contains("columns")) {
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
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
                org.name,
                simpleRepSender,
                options.key
            )
            echo("Response to POST: $responseCode")
            if (responseCode >= 400) {
                good("badcsv Test $i of $filename passed: Failure HttpStatus code was returned.")
            } else {
                bad("***badcsv Test $i of $filename FAILED: Expecting a failure HttpStatus. ***")
                passed = false
            }
            try {
                val tree = jacksonObjectMapper().readTree(json)
                if (tree["id"] == null || tree["id"].isNull) {
                    good("badcsv Test $i of $filename passed: No UUID was returned.")
                } else {
                    bad("***badcsv Test $i of $filename FAILED: RS returned a valid UUID for a bad CSV. ***")
                    passed = false
                }
                if (tree["errorCount"].intValue() > 0) {
                    good("badcsv Test $i of $filename passed: At least one error was returned.")
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting bigly strac Test: sending Strac data to all of these receivers: $allGoodCounties!")
        var passed = true
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            stracSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, stracSender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("**Strac Test FAILED***:  response code $responseCode")
        }
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            echo("Id of submitted report: $reportId")
            val expectedWarningCount = 0
            val warningCount = tree["warningCount"].intValue()
            if (warningCount == expectedWarningCount) {
                good("First part of strac Test passed: $warningCount warnings were returned.")
            } else {
                // Current expectation is that all non-REDOX counties fail.   If those issues get fixed,
                // then we'll need to fix this test as well.
                bad("***strac Test FAILED: Expecting $expectedWarningCount warnings but got $warningCount***")
                passed = false
            }
            // OK, fine, the others failed.   All our hope now rests on you, REDOX - don't let us down!
            waitABit(25, environment)
            return passed and examineLineageResults(reportId, listOf(redoxReceiver), options.items)
        } catch (e: Exception) {
            return bad("***strac Test FAILED***: Unexpected json returned")
        }
    }
}

class StracPack : CoolTest() {
    override val name = "stracpack" // no its not 'strackpack'
    override val description = "Does '--submits X' simultaneous strac " +
        "submissions, each with '--items Y' items. Redox only"
    override val status = TestStatus.LOAD

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly(
            "Starting stracpack Test: simultaneously submitting ${options.submits} batches " +
                "of Strac ${options.items} items per batch to the ${redoxReceiver.name} receiver only."
        )
        val file = FileUtilities.createFakeFile(
            metadata,
            stracSender,
            options.items,
            receivingStates,
            redoxReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream over and over
        var reportIds = mutableListOf<ReportId>()
        var passed = true
        // submit in thread grouping somewhat smaller than our database pool size.
        for (i in 1..options.submits) {
            thread {
                val (responseCode, json) =
                    HttpUtilities.postReportFile(environment, file, org.name, stracSender, options.key)
                echo("$i: Response to POST: $responseCode")
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    echo(json)
                    passed = bad("$i: ***StracPack Test FAILED***:  response code $responseCode")
                } else {
                    val tree = jacksonObjectMapper().readTree(json)
                    val reportId = ReportId.fromString(tree["id"].textValue())
                    echo("$i: Id of submitted report: $reportId")
                    synchronized(reportIds) {
                        reportIds.add(reportId)
                    }
                }
            }
        }
        // Since we have to wait for the sends anyway, I didn't bother with a join here.
        waitABit(5 * options.submits, environment) // SWAG: wait extra seconds extra per file submitted
        reportIds.forEach {
            passed = passed and
                examineLineageResults(it, listOf(redoxReceiver), options.items)
        }
        return passed
    }
}

class Waters : CoolTest() {
    override val name = "waters"
    override val description = "Submit data in waters schema, send to BLOBSTORE only"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting Waters: sending ${options.items} Waters items to ${blobstoreReceiver.name} receiver")
        val file = FileUtilities.createFakeFile(
            metadata,
            watersSender,
            options.items,
            receivingStates,
            blobstoreReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, watersSender, options.key)
        echo("Response to POST: $responseCode")
        if (!options.muted) echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("***Waters Test FAILED***:  response code $responseCode")
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(60, environment, options.muted)
        if (file.exists()) file.delete() // because of RepeatWaters
        return examineLineageResults(reportId, listOf(blobstoreReceiver), options.items)
    }
}

class RepeatWaters : CoolTest() {
    override val name = "repeatwaters"
    override val description = "Submit waters over and over, sending to BLOBSTORE"
    override val status = TestStatus.LOAD

    @ExperimentalTime
    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test: sending Waters data ${options.submits} times.")
        var allPassed = true
        var totalItems = 0
        val sleepBetweenSubmitMillis = 360
        val variationMillis = 360
        val pace = (3600000 / sleepBetweenSubmitMillis) * options.items
        echo("Submitting at an expected pace of $pace items per hour")
        options.muted = true
        val elapsed: Duration = measureTime {
            val threads = mutableListOf<Thread>()
            for (i in 1..options.submits) {
                val thread = thread {
                    val success = Waters().run(environment, options)
                    synchronized(allPassed) {
                        if (success) totalItems += options.items
                        allPassed = allPassed && success
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
        echo("$name Test took ${elapsed.inWholeSeconds} seconds. Expected pace/hr: $pace.")
        if (elapsed.inWholeSeconds > 600) {
            // pace calculation is inaccurate for short times, due to the hack long wait at the end.
            val actualPace = (totalItems / elapsed.inWholeSeconds) * 3600
            echo(" Actual pace: $actualPace")
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
    override val description = "Does '--submits X' ${receiverToTest.name} " +
        "submissions in parallel, each with '--items Y' items."
    override val status = TestStatus.LOAD

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting Hammertime Test: $description")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            options.items,
            receivingStates,
            receiverToTest.name,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream over and over
        var reportIds = mutableListOf<ReportId>()
        var passed = true
        // submit in thread grouping somewhat smaller than our database pool size.
        for (i in 1..options.submits) {
            thread {
                val (responseCode, json) =
                    HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
                echo("Response to POST: $responseCode")
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    echo(json)
                    passed = bad("***Hammertime Test FAILED***:  response code $responseCode")
                } else {
                    val tree = jacksonObjectMapper().readTree(json)
                    val reportId = ReportId.fromString(tree["id"].textValue())
                    echo("Id of submitted report: $reportId")
                    synchronized(reportIds) {
                        reportIds.add(reportId)
                    }
                }
            }
        }
        // Since we have to wait for the sends anyway, I didn't bother with a join here.
        waitABit(5 * options.submits, environment) // SWAG: wait 5 seconds extra per file submitted
        reportIds.forEach {
            passed = passed and
                examineLineageResults(it, listOf(receiverToTest), options.items)
        }
        return passed
    }
}

class Garbage : CoolTest() {
    override val name = "garbage"
    override val description = "Garbage in - Nice error message out"
    override val status = TestStatus.FAILS // new quality checks now prevent any data from flowing to other checks

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test: send ${emptySender.fullName} data to $allGoodCounties")
        var passed = true
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            emptySender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, emptySender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
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
    fun checkJsonItemCountForReceiver(receiver: Receiver, expectedCount: Int, json: String): Boolean {
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test")
        // ALLOW ALL
        ugly("\nTest the allowAll QualityFilter")
        val fakeItemCount = 5
        val file = FileUtilities.createFakeFile(
            metadata,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityAllReceiver.name, // Has the 'allowAll' quality filter
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, emptySender, options.key)
        echo("Response to POST: $responseCode")
        var passed = checkJsonItemCountForReceiver(qualityAllReceiver, fakeItemCount, json)

        // QUALITY_PASS
        ugly("\nTest a QualityFilter that allows some data through")
        val file2 = FileUtilities.createFakeFile(
            metadata,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityGoodReceiver.name + ",removed", // 3 kept, 2 removed
            options.dir,
        )
        echo("Created datafile $file2")
        // Now send it to ReportStream.
        val (responseCode2, json2) =
            HttpUtilities.postReportFile(environment, file2, org.name, emptySender, options.key)
        echo("Response to POST: $responseCode2")
        passed = passed and checkJsonItemCountForReceiver(qualityGoodReceiver, 3, json2)

        // FAIL
        ugly("\nTest a QualityFilter that allows NO data through.")
        val file3 = FileUtilities.createFakeFile(
            metadata,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityFailReceiver.name,
            options.dir,
        )
        echo("Created datafile $file3")
        // Now send it to ReportStream.
        val (responseCode3, json3) =
            HttpUtilities.postReportFile(environment, file3, org.name, emptySender, options.key)
        echo("Response to POST: $responseCode3")
        passed = passed and checkJsonItemCountForReceiver(qualityFailReceiver, 0, json3)

        // QUALITY_REVERSED
        ugly("\nTest the REVERSE of the QualityFilter that allows some data through")
        val file4 = FileUtilities.createFakeFile(
            metadata,
            emptySender,
            fakeItemCount,
            receivingStates,
            qualityReversedReceiver.name + ",kept", // 3 removed, 2 kept
            options.dir,
        )
        echo("Created datafile $file4")
        // Now send it to ReportStream.
        val (responseCode4, json4) =
            HttpUtilities.postReportFile(environment, file4, org.name, emptySender, options.key)
        echo("Response to POST: $responseCode4")
        passed = passed and checkJsonItemCountForReceiver(qualityReversedReceiver, 2, json4)

        return passed
    }
}

class Huge : CoolTest() {
    override val name = "huge"
    override val description = "Submit $REPORT_MAX_ITEMS line csv file, wait, confirm via db.  Slow."
    override val status = TestStatus.LOAD

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        val fakeItemCount = REPORT_MAX_ITEMS
        ugly("Starting huge Test: Attempting to send a report with $fakeItemCount items. This is terrapin slow.")
        val file = FileUtilities.createFakeFile(
            metadata,
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
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***Huge Test FAILED***:  response code $responseCode")
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        return examineLineageResults(reportId, listOf(csvReceiver), fakeItemCount)
    }
}

// Bigger than Huge
class TooBig : CoolTest() {
    override val name = "toobig"
    override val description = "Submit ${REPORT_MAX_ITEMS + 1} lines, which should be an error.  Slower ;)"
    override val status = TestStatus.LOAD

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        val fakeItemCount = REPORT_MAX_ITEMS + 1
        ugly("Starting toobig test: Attempting to send a report with $fakeItemCount items. This is slllooooowww.")
        val file = FileUtilities.createFakeFile(
            metadata,
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
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = ((tree["errors"] as ArrayNode)[0]) as ObjectNode
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting dbconnections Test: test of many threads attempting to sftp ${options.items} HL7s.")
        val file = FileUtilities.createFakeFile(
            metadata,
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
                HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
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
                examineLineageResults(it, listOf(hl7Receiver), options.items)
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting badsftp Test: test that our code handles sftp connectivity problems")
        val file = FileUtilities.createFakeFile(
            metadata,
            simpleRepSender,
            options.items,
            receivingStates,
            sftpFailReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***badsftp Test FAILED***:  response code $responseCode")
            return false
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        echo("For this test, failure during send, is a 'pass'.   Need to fix this.")
        return examineLineageResults(reportId, listOf(sftpFailReceiver), options.items)
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

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
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
            HttpUtilities.postReportFile(environment, file, org.name, simpleRepSender, options.key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("***intcontent Test FAILED***:  response code $responseCode")
        }
        try {
            // Read the response
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())

            echo("Id of submitted report: $reportId")
            waitABit(25, environment)
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