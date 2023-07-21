package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import java.net.HttpURLConnection
import java.time.OffsetDateTime

data class ExpectedSubmissionList(
    val submissionId: Int,
    val timestamp: OffsetDateTime,
    val sender: String,
    val httpStatus: Int,
    val id: ReportId?,
    val topic: String?,
    val reportItemCount: Int?,
    val externalName: String? = ""
)

/**
 * This is just a partial mimic of DetailedSubmissionHistory, just key fields.
 */
data class ExpectedSubmissionDetails(
    val reportId: ReportId?,
    val submissionId: Int,
    val overallStatus: String?,
    val timestamp: OffsetDateTime,
    val sender: String,
    val reportItemCount: Int,
    val errorCount: Int,
    val warningCount: Int,
)

/**
 * [name] A nicehuman readable name of this test case.
 * [path] REST API path for this test.
 * [headers] http headers to send in this test.
 * [parameters] to tack onto the end of the query.
 * [bearer] token to pass as an Authorization: Bearer token for this test case.
 * [jsonResponseChecker] This allows HistoryApiTestCase to be used for any returned json object.   Implement a subclass
 * of HistoryJsonResponseChecker specific to the needs of your test.
 * [doMinimalChecking] A flag to tell the [jsonResponseChecker] to only do very basic checking (each implementation
 * can interpret this as needed.  For example, in auth tests, we don't care about the details of the response)
 */
data class HistoryApiTestCase(
    val name: String,
    val path: String,
    val headers: Map<String, String>,
    val parameters: List<Pair<String, Any?>>?,
    val bearer: String,
    val expectedHttpStatus: HttpStatus,
    val expectedReports: Set<ReportId>,
    val jsonResponseChecker: HistoryJsonResponseChecker,
    val doMinimalChecking: Boolean,
    val extraCheck: ((Array<ExpectedSubmissionList>) -> String?)? = null
)

class HistoryApiTest : CoolTest() {
    override val name = "history"
    override val description = "Test the History/Lineage API"
    override val status = TestStatus.SMOKE

    /**
     * Create some fake history, so we have something to query for.
     * @return null on failure. Otherwise returns the list of ReportIds created.
     */
    private fun submitTestData(environment: Environment, options: CoolTestOptions): Set<ReportId>? {
        val receivers = listOf(csvReceiver)
        val counties = receivers.map { it.name }.joinToString(",")
        val fakeItemCount = receivers.size * options.items
        ugly("Starting $name test: Submitting ${options.submits} reports, each going to to $counties")
        val file = FileUtilities.createFakeCovidFile(
            metadata,
            settings,
            historyTestSender.schemaName,
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
                bad("***$name Test FAILED***:  response code $responseCode.  Error: $json")
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
    private fun historyApiQuery(testCase: HistoryApiTestCase): Pair<Boolean, String?> {
        val (_, response, result) = Fuel.get(testCase.path, testCase.parameters)
            .authentication()
            .bearer(testCase.bearer)
            .header(testCase.headers)
            .timeoutRead(45000) // default timeout is 15s; raising higher due to slow Function startup issues
            .responseString()
        if (response.statusCode != testCase.expectedHttpStatus.value()) {
            bad(
                "***$name Test '${testCase.name}' FAILED:" +
                    " Expected HttpStatus ${testCase.expectedHttpStatus}. Got ${response.statusCode}"
            )
            return Pair(false, null)
        }
        if (testCase.expectedHttpStatus != HttpStatus.OK) {
            return Pair(true, null)
        }
        if (result !is Result.Success) {
            bad("***$name Test '${testCase.name}' FAILED:  Result is $result")
            return Pair(false, null)
        }
        val json: String = result.value
        if (json.isEmpty()) {
            bad("***$name Test '${testCase.name}' FAILED: empty body")
            return Pair(false, null)
        }
        return Pair(true, json)
    }

    /**
     * Main function to run the History Api Tests
     */
    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test: get submission history ")
        val bearer = OktaAuthTests.getOktaAccessToken(environment, name)

        val reportIds = submitTestData(environment, options)
            ?: return bad("*** $name TEST FAILED:  Unable to submit test data")

        val testCases = mutableListOf(
            HistoryApiTestCase(
                "simple history API happy path test",
                "${environment.url}/api/waters/org/$historyTestOrgName/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.OK,
                expectedReports = reportIds,
                SubmissionListChecker(this),
                doMinimalChecking = true,
            ),
            HistoryApiTestCase(
                "no such organization",
                "${environment.url}/api/waters/org/gobblegobble/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.NOT_FOUND,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true,
            ),
            HistoryApiTestCase(
                "no such sender",
                "${environment.url}/api/waters/org/$historyTestOrgName.gobblegobble/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.NOT_FOUND,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true,
            ),
            HistoryApiTestCase(
                "single sender",
                "${environment.url}/api/waters/org/$org1Name.$fullELRSenderName/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.OK,
                expectedReports = reportIds,
                SubmissionListChecker(this),
                doMinimalChecking = true,
                extraCheck = {
                    var retVal: String? = null
                    for (submission in it) {
                        if (submission.sender != "$org1Name.$fullELRSenderName")
                            retVal = "Mismatched sender"
                    }
                    retVal
                }
            ),
            HistoryApiTestCase(
                "all senders",
                "${environment.url}/api/waters/org/$org1Name/submissions",
                emptyMap(),
                listOf("pagesize" to options.submits),
                bearer,
                HttpStatus.OK,
                expectedReports = reportIds,
                SubmissionListChecker(this),
                doMinimalChecking = true,
                extraCheck = {
                    var retVal: String? = null
                    if (it.map { it.sender }.toSet().size == 1)
                        retVal = "Only one sender channel returned"
                    retVal
                }
            )
        )
        if (environment != Environment.LOCAL) {
            testCases.add(
                HistoryApiTestCase(
                    "bad bearer token - TESTED ON STAGING, NOT TESTED ON LOCAL",
                    "${environment.url}/api/waters/org/$historyTestOrgName/submissions",
                    emptyMap(),
                    listOf("pagesize" to options.submits),
                    bearer + "x",
                    HttpStatus.UNAUTHORIZED,
                    expectedReports = emptySet(),
                    SubmissionListChecker(this),
                    doMinimalChecking = true,
                ),
            )
        }
        return runHistoryTestCases(testCases)
    }

    /**
     * Submit a set of [testCases], parse the results, and confirm they worked.
     * @return true if all tests passed, otherwise false.
     */
    fun runHistoryTestCases(testCases: List<HistoryApiTestCase>): Boolean {
        val allPassed = testCases.map {
            ugly("Starting test: ${it.name}")
            val (queryPass, json) = historyApiQuery(it)
            val sanityCheck = when {
                !queryPass -> false // api query failed.  Stop here with a fail.
                json.isNullOrBlank() -> true // api query worked, but no json created.  Stop here with a pass.
                else -> it.jsonResponseChecker.checkJsonResponse(it, json)
            }

            // Only the test failed, print out the json.
            if (!sanityCheck && !json.isNullOrBlank()) bad("This json failed:\n$json")
            if (sanityCheck) good("Test Passed:  ${it.name}")
            sanityCheck
        }.reduce { acc, onePassed -> acc and onePassed }
        return allPassed
    }
}

/**
 * This enables the HistoryApiTestCase to be re-used for any kind of json response object.
 * Just create a child class of [HistoryJsonResponseChecker].
 * This takes a reference to the [testBeingRun] so that this class can access its methods, fields, and state.
 */
abstract class HistoryJsonResponseChecker(val testBeingRun: CoolTest) {
    abstract fun checkJsonResponse(testCase: HistoryApiTestCase, json: String): Boolean
}

/**
 * A json parser/checker used to check the response to a submission list query, which
 * returns an array of [ExpectedSubmissionList].
 * [testBeingRun] is the test calling this checker.
 */
class SubmissionListChecker(testBeingRun: CoolTest) : HistoryJsonResponseChecker(testBeingRun) {
    /**
     * Compare the list of reportIds we got from the original submissions vs response from history API
     * @return whether the test passed
     */
    override fun checkJsonResponse(
        testCase: HistoryApiTestCase,
        json: String
    ): Boolean {
        val jsonMapper = jacksonObjectMapper()
        jsonMapper.registerModule(JavaTimeModule())
        val submissionsHistories = jsonMapper.readValue(json, Array<ExpectedSubmissionList>::class.java)
            ?: return testBeingRun.bad("Bad submission list returned")
        // For some tests (e.g. Auth tests), we just care whether we got anything back; we don't care what's in it.
        if (!testCase.doMinimalChecking) {
            if (submissionsHistories.size != testCase.expectedReports.size) {
                return testBeingRun.bad(
                    "*** ${testBeingRun.name}: TEST '${testCase.name}' FAILED: " +
                        "${testCase.expectedReports.size} reports submitted" +
                        " but got ${submissionsHistories.size} submission histories."
                )
            }

            val missingReportIds = testCase.expectedReports.minus(submissionsHistories.mapNotNull { it.id }.toSet())
            if (missingReportIds.isNotEmpty()) {
                return testBeingRun.bad(
                    "*** ${testBeingRun.name}: TEST '${testCase.name}' FAILED: " +
                        "These ReportIds are missing from the history: ${missingReportIds.joinToString(",")}"
                )
            }
        }
        val checkResult = testCase.extraCheck?.invoke(submissionsHistories)
        if (checkResult != null) {
            return testBeingRun.bad(
                "*** ${testBeingRun.name}: TEST '${testCase.name}' FAILED: $checkResult"
            )
        }
        return true
    }
}

/**
 * A json parser/checker used to check the response to a submission details query, which
 * returns a single [DetailedSubmissionHistory] object.
 * [testBeingRun] is the test calling this checker.
 */
class ReportDetailsChecker(testBeingRun: CoolTest) : HistoryJsonResponseChecker(testBeingRun) {
    /**
     * Compare the list of reportIds we got from the original submissions vs response from history API
     * @return whether the test passed
     */
    override fun checkJsonResponse(
        testCase: HistoryApiTestCase,
        json: String
    ): Boolean {
        val jsonMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        jsonMapper.registerModule(JavaTimeModule())
        val submissionDetails = jsonMapper.readValue(json, ExpectedSubmissionDetails::class.java)
            ?: return testBeingRun.bad("Bad submission details obj returned: $json")
        // For some tests (e.g. Auth tests), we just care whether we got anything back; we don't care what's in it.
        if (!testCase.doMinimalChecking) {
            if (submissionDetails.reportId == null) {
                return testBeingRun.bad("Got a json response with a null reportId: $json")
            }
            if (testCase.expectedReports.size == 1) {
                if (testCase.expectedReports.first() != submissionDetails.reportId) {
                    testBeingRun.bad(
                        "Expecting reportId ${testCase.expectedReports.toList()[0]} but " +
                            " got reportId ${submissionDetails.reportId} in submission details response"
                    )
                    return false
                }
            } else {
                testBeingRun.bad(
                    "Test is not written correctly - please fix:" +
                        " For DetailedSubmissionHistory tests, Pass exactly one reportId in the expectedReports set."
                )
                return false
            }
        }
        return true
    }
}