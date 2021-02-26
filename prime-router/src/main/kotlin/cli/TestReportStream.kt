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
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.OrganizationClient
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.ReportFunction
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.impl.DSL
import java.io.File
import java.io.IOException
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import kotlin.system.exitProcess

class TestReportStream : CliktCommand(
    name = "test",
    help = """Run tests of the Router functions

Database connection info is supplied by environment variables, for localhost, and Test, resp.:

export POSTGRES_USER=prime
export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
export POSTGRES_PASSWORD=<secret>

export POSTGRES_USER=prime@pdhtest-pgsql
export POSTGRES_URL=jdbc:postgresql://pdhtest-pgsql.postgres.database.azure.com:5432/prime_data_hub
export POSTGRES_PASSWORD=<SECRET>

Examples:
  ./prime test    This runs all the tests locally.
  
./prime test --run ping,end2end --env test --key xxxxxxx  This runs the ping and end2end tests in azure Test

""",
) {
    lateinit var metadata: Metadata
    lateinit var db: DatabaseAccess

    val receivingStates = "PM"
    val receivingOrgName = "prime"

    enum class AwesomeTest(val description: String) {
        ping("Is the reports endpoint alive and listening?"),
        end2end("Create Fake data, submit, wait, confirm sent via database lineage data"),
        strac("Submit data in the strac schema format, wait, confirm via database queries"),
        // 10,000 lines fake data generation took about 90 seconds on my laptop.  6Meg.
        huge("Submit $REPORT_MAX_ITEMS line csv file, wait, confirm via db.  Slow."),
        toobig("Submit ${REPORT_MAX_ITEMS + 1} lines, which should be an error.  Slower ;)"),
        toomanycols("Submit a file with more than $REPORT_MAX_ITEM_COLUMNS columns, which should error"),
        merge("Submit multiple files, wait, confirm via db that merge occurred"),
    }

    enum class TestingEnvironment(val endPoint: String) {
        // track headers and parameters separate from the base endpoint, since they vary
        TEST("https://pdhtest-functionapp.azurewebsites.net/api/reports"),
        LOCAL("http://localhost:7071/api/reports"),
        STAGING("https://pdhstaging-functionapp.azurewebsites.net/api/reports")
    }

    private val dir by option(
        "--dir",
        help = "specify a working directory for generated files"
    ).default("./target/csv_test_files")

    private val key by option(
        "--key",
        metavar = "<secret>",
        help = "Specify reports function access key"
    )

    private val run by option(
        "--run",
        metavar = "test1,test2",
        help = """Specify set of tests to run.   Default is to run all if not specified.
            Allowed tests are: ${AwesomeTest.values().joinToString(",")}
       """
    )

    private val list by option(
        "--list",
        help = "List available tests, then quit."
    ).flag(default = false)

    private val env by option(
        "--env",
        help = "Specify 'local, 'test', or 'staging'.  'local' will connect to ${TestingEnvironment.LOCAL.endPoint}," +
            " and 'test' will connect to ${TestingEnvironment.TEST.endPoint}"
    ).choice("test", "local", "staging").default("local").validate {
        when (it) {
            "test" -> require(!key.isNullOrBlank()) { "Must specify --key <secret> to submit reports to --env test" }
            "staging" -> require(!key.isNullOrBlank()) { "Must specify --key <secret> to submit reports to --env test" }
        }
    }

    override fun run() {
        if (list) doList()
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
            AwesomeTest.values().toList()
        }
        doTests(tests)
    }

    private fun doList() {
        echo("Available options to --run <test1,test2> are:")
        val formatTemplate = "%-20s\t%s"
        AwesomeTest.values().forEach {
            echo(formatTemplate.format(it.name, it.description))
        }
        exitProcess(0)
    }

    private fun doTests(tests: List<AwesomeTest>) {
        metadata = Metadata(Metadata.defaultMetadataDirectory)
        val receivingOrg = metadata.findOrganization(receivingOrgName)
            ?: error("Unable to find org '$receivingOrgName' in metadata")
        val environment = TestingEnvironment.valueOf(env.toUpperCase())

        tests.forEach { test ->
            when (test) {
                AwesomeTest.ping -> doCheckConnections(environment)
                AwesomeTest.end2end -> doEndToEndTest(receivingOrg, environment)
                AwesomeTest.strac -> doStracTest(receivingOrg, environment)
                AwesomeTest.huge -> doHugeTest(receivingOrg, environment)
                AwesomeTest.toobig -> doTooManyItemsTest(receivingOrg, environment)
                AwesomeTest.toomanycols -> doTooManyColumnsTest(environment)
                AwesomeTest.merge -> doMergeTest(receivingOrg, environment)
                else -> bad("Test $test not implemented")
            }
        }
    }

    private fun doMergeTest(receivingOrg: Organization, environment: TestReportStream.TestingEnvironment) {
        val sendingOrg = metadata.findOrganization("simple_report")
            ?: error("Unable to find org 'simple_report' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        val fakeItemCount = 20 // hack:  you need use a multiple of # of targetCounties
        echo("Merge test of: ${environment.endPoint}")
        val targetCounties = receivingOrg.services.map { it.name }.joinToString(",")
        echo("Testing $targetCounties")
        val file = createFakeFile(
            metadata,
            sendingOrgClient,
            fakeItemCount,
            receivingStates,
            targetCounties,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub
        val reportIds = (1..5).map {
            val (responseCode, json) = postReportFile(environment, file, sendingOrg.name, sendingOrgClient.name, key)
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
        examineMergeResults(reportIds[0], receivingOrg.services, fakeItemCount, 5)
    }

    private fun doStracTest(
        receivingOrg: Organization,
        environment: TestingEnvironment
    ) {
        val sendingOrg = metadata.findOrganization("strac")
            ?: error("Unable to find org 'strac' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        val fakeItemCount = 20 // hack:  you need use a multiple of # of targetCounties
        echo("Test sending Strac data to: ${environment.endPoint}")
        val targetCounties = receivingOrg.services.map { it.name }.joinToString(",")
        echo("Testing $targetCounties")
        val file = createFakeFile(
            metadata,
            sendingOrgClient,
            fakeItemCount,
            receivingStates,
            targetCounties,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = postReportFile(environment, file, sendingOrg.name, sendingOrgClient.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("**Strac Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(15, environment)
        examineLineageResults(reportId, receivingOrg.services, fakeItemCount)
    }

    private fun doHugeTest(
        receivingOrg: Organization,
        environment: TestingEnvironment
    ) {
        val sendingOrg = metadata.findOrganization("simple_report")
            ?: error("Unable to find org 'simple_report' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        val fakeItemCount = 10000
        echo("Attempting to send $fakeItemCount items to ${environment.endPoint}. This is slow.")
        // Send to just one type
        val receivingOrgSvc = receivingOrg.services.find { it.name == "CSV" }
            ?: error("Unable to find CSV as a sender in ${receivingOrg.name}")
        echo("Testing ${receivingOrgSvc.name}")
        val file = createFakeFile(
            metadata,
            sendingOrgClient,
            fakeItemCount,
            receivingStates,
            receivingOrgSvc.name,
            dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = postReportFile(environment, file, sendingOrg.name, sendingOrgClient.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***EndToEnd Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(20, environment)
        examineLineageResults(reportId, listOf(receivingOrgSvc), fakeItemCount)
    }

    private fun doTooManyItemsTest(
        receivingOrg: Organization,
        environment: TestingEnvironment
    ) {
        val sendingOrg = metadata.findOrganization("simple_report")
            ?: error("Unable to find org 'simple_report' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        val fakeItemCount = REPORT_MAX_ITEMS + 1
        echo("Attempting to send $fakeItemCount items to ${environment.endPoint}. This is slow.")
        // Send to just one type
        val receivingOrgSvc = receivingOrg.services.find { it.name == "CSV" }
            ?: error("Unable to find CSV as a sender in ${receivingOrg.name}")
        echo("Testing ${receivingOrgSvc.name}")
        val file = createFakeFile(
            metadata,
            sendingOrgClient,
            fakeItemCount,
            receivingStates,
            receivingOrgSvc.name,
            dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = postReportFile(environment, file, sendingOrg.name, sendingOrgClient.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        val tree = jacksonObjectMapper().readTree(json)
        val firstError = ((tree["errors"] as ArrayNode)[0]) as ObjectNode
        if (firstError["details"].textValue().contains("rows")) {
            good("Too Big Test passed.")
        } else {
            bad("***Too Big Test Test FAILED***: Did not find the error")
        }
    }

    private fun doTooManyColumnsTest(
        environment: TestingEnvironment
    ) {
        echo("Testing a file with too many columns.")
        val file = File("./src/test/csv_test_files/input/too-many-columns.csv")
        if (!file.exists()) {
            error("Unable to find file ${file.absolutePath} to do toomanycols test")
        }
        val sendingOrg = metadata.findOrganization("simple_report")
            ?: error("Unable to find org 'simple_report' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        // Now send it to the Hub.
        val (responseCode, json) = postReportFile(environment, file, sendingOrg.name, sendingOrgClient.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        val tree = jacksonObjectMapper().readTree(json)
        val firstError = ((tree["errors"] as ArrayNode)[0]) as ObjectNode
        if (firstError["details"].textValue().contains("columns")) {
            good("Test passed: Too many columns test.")
        } else {
            bad("***Too Many Columns Test FAILED***:  did not find the error.")
        }
    }

    private fun doCheckConnections(
        environment: TestingEnvironment
    ) {
        echo("CheckConnections of ${environment.endPoint}")
        val (responseCode, json) = postReportBytes(
            environment,
            "x".toByteArray(),
            "simple_report",
            "default",
            key,
            ReportFunction.Options.CheckConnections
        )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_OK) {
            echo("Test FAILED:  response code $responseCode")
            exitProcess(-1) // other tests won't work.
        }
        val tree = jacksonObjectMapper().readTree(json)
        if (tree["errorCount"].intValue() != 0 || tree["warningCount"].intValue() != 0) {
            bad("***CheckConnections Test FAILED***:  Response was $json")
        } else {
            good("Test passed: CheckConnections")
        }
    }

    private fun doEndToEndTest(
        receivingOrg: Organization,
        environment: TestingEnvironment
    ) {
        val sendingOrg = metadata.findOrganization("simple_report")
            ?: error("Unable to find org 'simple_report' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        val fakeItemCount = 20 // hack:  you need use a multiple of # of targetCounties
        echo("EndToEndTest of: ${environment.endPoint}")
        val targetCounties = receivingOrg.services.map { it.name }.joinToString(",")
        echo("Testing $targetCounties")
        val file = createFakeFile(
            metadata,
            sendingOrgClient,
            fakeItemCount,
            receivingStates,
            targetCounties,
            dir,
        )
        echo("Created datafile $file")
        // Now send it to the Hub.
        val (responseCode, json) = postReportFile(environment, file, sendingOrg.name, sendingOrgClient.name, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***EndToEnd Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit(15, environment)
        examineLineageResults(reportId, receivingOrg.services, fakeItemCount)
    }

    fun examineLineageResults(
        reportId: ReportId,
        receivers: List<OrganizationService>,
        totalRecords: Int,
    ) {
        db = DatabaseAccess(dataSource = DatabaseAccess.dataSource)
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
        receivers: List<OrganizationService>,
        itemsPerReport: Int,
        reportCount: Int
    ) {
        db = DatabaseAccess(dataSource = DatabaseAccess.dataSource)
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

        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_BLACK = "\u001B[30m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_BLUE = "\u001B[34m"

        fun good(msg: String) {
            echo(ANSI_GREEN + msg + ANSI_RESET)
        }

        fun bad(msg: String) {
            echo(ANSI_RED + msg + ANSI_RESET)
        }

        fun createFakeFile(
            metadata: Metadata,
            sendingOrgClient: OrganizationClient,
            count: Int,
            targetStates: String? = null,
            targetCounties: String? = null,
            directory: String = ".",
            format: Report.Format = Report.Format.CSV,
        ): File {
            val report = createFakeReport(
                metadata,
                sendingOrgClient,
                count,
                targetStates,
                targetCounties,
            )
            return ProcessData.writeReportToFile(report, format, metadata, directory, null)
        }

        fun createFakeReport(
            metadata: Metadata,
            sendingOrgClient: OrganizationClient,
            count: Int,
            targetStates: String? = null,
            targetCounties: String? = null,
        ): Report {
            return FakeReport(metadata).build(
                metadata.findSchema(sendingOrgClient.schema)
                    ?: error("Unable to find schema ${sendingOrgClient.schema}"),
                count,
                FileSource("fake"),
                targetStates,
                targetCounties,
            )
        }

        /**
         * A hack attempt to wait enough time, but not too long, for Hub to finish.
         * This assumes the batch/send executes on the minute boundary.
         */
        fun waitABit(plusSecs: Int, env: TestReportStream.TestingEnvironment) {
            // seconds elapsed so far in this minute
            val secsElapsed = OffsetDateTime.now().second % 60
            // Wait until the top of the next minute, and pluSecs more, for 'batch', and 'send' to finish.
            var waitSecs = 60 - secsElapsed + plusSecs
            if (secsElapsed > (60 - plusSecs) || env != TestingEnvironment.LOCAL) {
                // Uh oh, we are close to the top of the minute *now*, so 'receive' might not finish in time.
                // Or, we are in Test or Staging, which don't execute on the top of the minute.
                waitSecs += 60
            }
            echo("Waiting $waitSecs seconds for the Hub to fully receive, batch, and send the data")
            for (i in 1..waitSecs) {
                sleep(1000)
                print(".")
            }
        }

        /**
         * A generic function to POST a Prime Data Hub report File to a particular Prime Data Hub Environment,
         * as if from sendingOrgName.sendingOrgClientName.
         * Returns Pair(Http response code, json response text)
         */
        fun postReportFile(
            environment: TestingEnvironment,
            file: File,
            sendingOrgName: String,
            sendingOrgClientName: String? = null,
            key: String? = null,
            option: ReportFunction.Options ? = null
        ): Pair<Int, String> {
            if (!file.exists()) error("Unable to find file ${file.absolutePath}")
            return postReportBytes(environment, file.readBytes(), sendingOrgName, sendingOrgClientName, key, option)
        }

        /**
         * A generic function to POST data to a particular Prime Data Hub Environment,
         * as if from sendingOrgName.sendingOrgClientName.
         * Returns Pair(Http response code, json response text)
         */
        private fun postReportBytes(
            environment: TestingEnvironment,
            bytes: ByteArray,
            sendingOrgName: String,
            sendingOrgClientName: String?,
            key: String?,
            option: ReportFunction.Options?
        ): Pair<Int, String> {
            val headers = mutableListOf<Pair<String, String>>()
            headers.add("Content-Type" to "text/csv")
            val clientStr = sendingOrgName + if (sendingOrgClientName != null) ".$sendingOrgClientName" else ""
            headers.add("client" to clientStr)
            if (key == null && environment == TestingEnvironment.TEST) error("key is required for Test environment")
            if (key != null)
                headers.add("x-functions-key" to key)
            val url = environment.endPoint + if (option != null) "?option=$option" else ""
            return postHttp(url, bytes, headers)
        }

        /**
         * A generic function that posts data to a URL <address>.
         * Returns the HTTP response code and the text of the response
         */
        fun postHttp(urlStr: String, bytes: ByteArray, headers: List<Pair<String, String>>? = null): Pair<Int, String> {
            val urlObj = URL(urlStr)
            with(urlObj.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                headers?.forEach {
                    addRequestProperty(it.first, it.second)
                }
                outputStream.use {
                    it.write(bytes)
                }
                val response = try {
                    inputStream.bufferedReader().readText()
                } catch (e: IOException) {
                    return responseCode to responseMessage
                }
                return responseCode to response
            }
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

/**
 * This was my attempt to jooqify the query.
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