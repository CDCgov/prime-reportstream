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
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.ReportFunction
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.jooq.impl.DSL
import java.io.File
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

class EndToEndTest : CliktCommand(
    name = "test",
    help = """
    Run tests of the Router functions, either locally or in Azure, by creating test data, submitting it,
    waiting for processing to complete, then examining the database audit trail to confirm success.
    
    Database connection info is supplied by environement variables as follows:
            export POSTGRES_USER=prime
            export POSTGRES_PASSWORD=<secret>
            export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub     <--- should work locally    

    If running against azure, you must also specify
    auth information to connect to the report submission endpoint.  Currently this is a --key xxxxx secret.
        
    Examples:
    ./prime test --env local --dbpass xxxxx
    ./prime test --env test --dbuser prime --dbpass xxxxx --key xxxxxxx
    """,
) {
    lateinit var metadata: Metadata
    lateinit var db: DatabaseAccess

    val sendingOrgName = "simple_report"
    val sendingOrgClientName = "default"
    val targetStates = "PM"
    val receivngOrgName = "prime"
    val fakeItemCount = 20

    enum class TestingEnvironment(val endPoint: String) {
        // track headers and parameters separate from the base endpoint, since they vary
        TEST("https://pdhtest-functionapp.azurewebsites.net/api/reports"),
        LOCAL("http://localhost:7071/api/reports")
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

    private val env by option(
        "--env",
        help = "Specify 'local' or 'test'.  'local' will connect to ${TestingEnvironment.LOCAL.endPoint}," +
            " and 'test' will connect to ${TestingEnvironment.TEST.endPoint}"
    ).choice("test", "local").default("local").validate {
        when (it) {
            "test" -> require(!key.isNullOrBlank()) { "Must specify --key <secret> to submit reports to --env test" }
        }
    }

    fun waitABit() {
        val seconds: Long = 70
        echo("Waiting $seconds seconds for the Hub to fully receive, batch, and send the data")
        for (i in 1..seconds / 2) {
            sleep(2000)
            print(".")
        }
    }

    private fun endToEndTest() {
        metadata = Metadata(Metadata.defaultMetadataDirectory)
        val sendingOrg = metadata.findOrganization(sendingOrgName)
            ?: error("Unable to find org '$sendingOrgName' in metadata")
        val sendingOrgClient = sendingOrg.clients.find { it.name == sendingOrgClientName }
            ?: error("Unable to find sender '$sendingOrgClientName' for organization $sendingOrgName")
        val receivingOrg = metadata.findOrganization(receivngOrgName)
            ?: error("Unable to find org '$receivngOrgName' in metadata")
        val targetCounties = receivingOrg.services.map { it.name }.joinToString(",")
        echo("Testing $targetCounties")
        val report = FakeReport(metadata).build(
            metadata.findSchema(sendingOrgClient.schema)
                ?: error("Unable to find schema ${sendingOrgClient.schema}"),
            fakeItemCount,
            FileSource("fake"),
            targetStates,
            targetCounties,
        )
        val file = ProcessData.writeReportToFile(report, Report.Format.CSV, metadata, dir, null)
        echo("Created datafile $file")
        // Now send it to the Hub.
        val environment = TestingEnvironment.valueOf(env.toUpperCase())
        echo("Sending to: ${environment.endPoint}")
        val (responseCode, json) = postReportFile(environment, file, sendingOrgName, sendingOrgClientName, key)
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            echo("Test FAILED:  response code $responseCode")
            exitProcess(-1)
        }
        val tree = jacksonObjectMapper().readTree(json)
        val reportId = ReportId.fromString(tree["id"].textValue())
        echo("Id of submitted report: $reportId")
        waitABit()
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
                            "***TEST FAILED*** for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records but got $count"
                        )
                    } else {
                        echo(
                            "Test passed: for ${receiver.fullName} action $action: " +
                                " Expecting $expected item lineage records and got $count"
                        )
                    }
                }
            }
        }
    }

    override fun run() {
        endToEndTest()
    }

    companion object {

        /**
         * A generic function to POST a Prime Data Hub report File to a particular Prime Data Hub Environment,
         * as if from sendingOrgName.sendingOrgClientName.
         * Returns Pair(Http response code, json response text)
         */
        fun postReportFile(
            environment: EndToEndTest.TestingEnvironment,
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
            environment: EndToEndTest.TestingEnvironment,
            bytes: ByteArray,
            sendingOrgName: String,
            sendingOrgClientName: String?,
            key: String?,
            option: ReportFunction.Options?
        ): Pair<Int, String> {
            var headers = mutableListOf<Pair<String, String>>()
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
                val response = inputStream.bufferedReader().readText()
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
            val sql = "select count(*)" +
                " from item_lineage as IL" +
                " join report_file as RF on IL.child_report_id = RF.report_id" +
                " join action as A on A.action_id = RF.action_id" +
                " where RF.receiving_org_svc = ? " +
                " and A.action_name = ? " +
                " and IL.item_lineage_id in" +
                " (select item_descendants(?))"
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