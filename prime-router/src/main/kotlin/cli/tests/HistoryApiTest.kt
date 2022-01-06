package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.cli.OktaCommand
import gov.cdc.prime.router.common.Environment
import java.net.HttpURLConnection
import java.time.OffsetDateTime

data class ExpectedSubmissionHistory(
    val taskId: Int,
    val createdAt: OffsetDateTime,
    val sendingOrg: String,
    val httpStatus: Int,
    val id: ReportId?,
    val topic: String?,
    val reportItemCount: Int?,
    val warningCount: Int?,
    val errorCount: Int?,
    val externalName: String? = ""
)

data class SubmissionAPITestCase(
    val name: String,
    val path: String,
    val headers: Map<String, String>,
    val parameters: List<Pair<String, Any?>>?,
    val bearer: String,
    val expectedHttpStatus: HttpStatus,
    val expectedReports: Set<ReportId>,
)

class HistoryApiTest : CoolTest() {
    override val name = "history"
    override val description = "Test the History/Lineage API"
    override val status = TestStatus.SMOKE

    // todo this code was copied from oktaAccessToken in SettingCommands.kt
    // This is now copied in 3 places:  here, SettingCommands, LookupTableCommands, in 3 different ways.
    // Best style is the lazy init in SettingCommands.  To factor up we need to pass Environment to
    // CoolTest constructor.  This is a lotta work.
    /**
     * The access token left by a previous login command as specified by the command line parameters
     */
    fun getAccessToken(environment: Environment): String {
        return if (environment.oktaApp == null) {
            "dummy"
        } else {
            OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: abort(
                    "Cannot run test $name.  Invalid access token. " +
                        "Run ./prime login to fetch/refresh your access token for the $environment environment."
                )
        }
    }

    /**
     * Create some fake history, so we have something to query for.
     * @return null on failure.  Otherwise returns the list of ReportIds created.
     */
    private fun submitTestData(environment: Environment, options: CoolTestOptions): Set<ReportId>? {
        val receivers = listOf<Receiver>(csvReceiver)
        val counties = receivers.map { it.name }.joinToString(",")
        val fakeItemCount = receivers.size * options.items
        ugly("Starting $name test: Submitting ${options.submits} reports, each going to to $counties")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            historyTestSender,
            fakeItemCount,
            receivingStates,
            counties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream over and over
        val reportIds = (1..options.submits).map {
            val (responseCode, json) =
                HttpUtilities.postReportFile(
                    environment,
                    file,
                    historyTestSender,
                    options.asyncProcessMode,
                    options.key,
                    payloadName = "$name ${status.description}",
                )
            echo("Response to POST: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                bad("***$name Test FAILED***:  response code $responseCode")
                return null // failure
            }
            val reportId = getReportIdFromResponse(json)
                ?: let {
                    bad("***$name Test FAILED***: A report ID came back as null")
                    return null
                }
            echo("Id of submitted report: $reportId")
            reportId
        }.toSet()
        return reportIds
    }

    /**
     * Submit a query to the History API, get a response, and do very basic sanity checks on it.
     * @return Pair(pass/fail status, json body string)
     * The json body may be null even if the test passed, in cases of an expected error code.
     */
    private fun historyApiQuery(testCase: SubmissionAPITestCase): Pair<Boolean, String?> {
        val (_, response, result) = Fuel.get(testCase.path, testCase.parameters)
            .authentication()
            .bearer(testCase.bearer)
            .responseString()
        if (response.statusCode != testCase.expectedHttpStatus.value()) {
            bad(
                "***$name Test '${testCase.name}' FAILED:" +
                    " Expected HttpStatus ${testCase.expectedHttpStatus}. Got ${response.statusCode}"
            )
            return Pair(false, null)
        }
        if (testCase.expectedHttpStatus != HttpStatus.OK) {
            good("$name test '${testCase.name}' passed:  Got expected http status ${testCase.expectedHttpStatus}")
            return Pair(true, null)
        }
        if (result !is Result.Success) {
            bad("***$name Test '${testCase.name}' FAILED:  Result is $result")
            return Pair(false, null)
        }
        val json: String = result.value
        if (json.isNullOrEmpty()) {
            bad("***$name Test '${testCase.name}' FAILED: empty body")
            return Pair(false, null)
        }
        return Pair(true, json)
    }

    /**
     * Compare the list of reportIds we got from the original submissions vs response from history API
     * @return whether the test passed
     */
    private fun checkHistorySummary(
        testCase: SubmissionAPITestCase,
        jsonHistory: String
    ): Boolean {
        val jsonMapper = jacksonObjectMapper()
        jsonMapper.registerModule(JavaTimeModule())
        val submissionsHistories = jsonMapper.readValue(jsonHistory, Array<ExpectedSubmissionHistory>::class.java)
        if (submissionsHistories.size != testCase.expectedReports.size) {
            return bad(
                "*** $name: TEST '${testCase.name}' FAILED: " +
                    "${testCase.expectedReports.size} reports submitted" +
                    " but got ${submissionsHistories.size} submission histories."
            )
        }

        val missingReportIds = testCase.expectedReports.minus(submissionsHistories.mapNotNull { it.id }.toSet())
        if (missingReportIds.isNotEmpty()) {
            return bad(
                "*** $name: TEST '${testCase.name}' FAILED: " +
                    "These ReportIds are missing from the history: ${missingReportIds.joinToString(",")}"
            )
        }
        good("$name test '${testCase.name}' passed.")
        return true
    }

    /**
     * Main function to run the History Api Tests
     */
    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test: get submission history ")
        val bearer = getAccessToken(environment)

        val reportIds = submitTestData(environment, options)
            ?: return bad("*** $name TEST FAILED:  Unable to submit test data")

        val testCases = mutableListOf(
            SubmissionAPITestCase(
                "simple history API happy path test",
                "${environment.url}/api/history/$historyTestOrgName/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.OK,
                expectedReports = reportIds,
            ),
            SubmissionAPITestCase(
                "no such organization",
                "${environment.url}/api/history/gobblegobble/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.OK,
                expectedReports = emptySet(),
            ),
        )
        if (environment != Environment.LOCAL) {
            testCases.add(
                SubmissionAPITestCase(
                    "bad bearer token - TESTED ON STAGING, NOT TESTED ON LOCAL",
                    "${environment.url}/api/history/$historyTestOrgName/submissions",
                    emptyMap(),
                    listOf("pagesize" to options.submits),
                    bearer + "x",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    expectedReports = emptySet(),
                ),
            )
        }
        var allPassed = testCases.map {
            val (queryPass, json) = historyApiQuery(it)

            val sanityCheck = when {
                !queryPass -> false // api query failed.  Stop here with a fail.
                json.isNullOrBlank() -> true // api query worked, but no json created.  Stop here with a pass.
                else -> checkHistorySummary(it, json)
            }

            // Only the test failed, print out the json.
            if (!sanityCheck && !json.isNullOrBlank())
                bad("This json failed:\n$json")
            sanityCheck
        }.reduce { acc, onePassed -> acc and onePassed }
        return allPassed
    }
}