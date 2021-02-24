package gov.cdc.prime.router.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.default
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
        strac("Submit our prepared simplereport.csv, wait, confirm via database queries"),
        // 10,000 lines fake data generation took about 90 seconds on my laptop.  6Meg.
        huge("Submit $REPORT_MAX_ITEMS line csv file, wait, confirm via db"),
        toobig("Submit ${REPORT_MAX_ITEMS + 1} lines, which shoudl be an error"),
        merge("Submit multiple files, wait, confirm via db that merge occurred"),
    }

    enum class TestingEnvironment(val endPoint: String) {
        // track headers and parameters separate from the base endpoint, since they vary
        TEST("https://pdhtest-functionapp.azurewebsites.net/api/reports"),
        LOCAL("http://localhost:7071/api/reports"),
        STAGE("https://pdhstage-functionapp.azurewebsites.net/api/reports")
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
        help = "Specify set of tests to run.   Default is to run all if not specified." +
            " Allowed tests are:  ${AwesomeTest.values().joinToString(",")}"
    )

    private val env by option(
        "--env",
        help = "Specify 'local, 'test', or 'stage'.  'local' will connect to ${TestingEnvironment.LOCAL.endPoint}," +
            " and 'test' will connect to ${TestingEnvironment.TEST.endPoint}"
    ).choice("test", "local", "stage").default("local").validate {
        when (it) {
            "test" -> require(!key.isNullOrBlank()) { "Must specify --key <secret> to submit reports to --env test" }
        }
    }

    override fun run() {
        val tests = if (run != null) {
            run.toString().split(",").mapNotNull {
                try {
                    AwesomeTest.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    echo("Skipping unknown test: $it")
                    null
                }
            }
        } else {
            AwesomeTest.values().toList()
        }
        doTests(tests)
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
                else -> echo("Test $test not implemented")
            }
        }
    }

    private fun doStracTest(
        receivingOrg: Organization,
        environment: TestingEnvironment
    ) {
        val sendingOrg = metadata.findOrganization("strac")
            ?: error("Unable to find org 'strac' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == "default" }
            ?: error("Unable to find sender 'default' for organization ${sendingOrg.name}")
        val fakeItemCount = 20
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
            echo("**Strac Test FAILED***:  response code $responseCode")
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
            echo("***EndToEnd Test FAILED***:  response code $responseCode")
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
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            echo("***TooBigTest Test FAILED***:  response code $responseCode")
            return
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
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
            echo("***CheckConnections Test FAILED***:  Response was $json")
        } else {
            echo("CheckConnections Test passed.")
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
        val fakeItemCount = 20
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
            echo("***EndToEnd Test FAILED***:  response code $responseCode")
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
                    val count = lineageCountQuery(txn, reportId, receiver.name, action)
                    if (count == null || expected != count) {
                        echo(
                            "***EndToEnd TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records but got $count"
                        )
                    } else {
                        echo(
                            "EndToEnd Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records and got $count"
                        )
                    }
                }
            }
        }
    }

    companion object {

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
            if (secsElapsed > (60 - plusSecs) || env == TestingEnvironment.TEST) {
                // Uh oh, we are close to the top of the minute *now*, so 'receive' might not finish in time.
                // Or, we are in Test, which doesn't execute on the top of the hour.
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

        fun lineageCountQuery(
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