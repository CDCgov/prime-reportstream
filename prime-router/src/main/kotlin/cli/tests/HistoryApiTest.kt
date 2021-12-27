package gov.cdc.prime.router.cli.tests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.CommandUtilities.Companion.abort
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.cli.OktaCommand
import gov.cdc.prime.router.common.Environment
import java.net.HttpURLConnection

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
    fun getAccessToken(env: Environment): String {
        return if (env.oktaApp == null) {
            "dummy"
        } else {
            OktaCommand.fetchAccessToken(env.oktaApp)
                ?: abort(
                    "Cannot run test $name.  Invalid access token. " +
                        "Run ./prime login to fetch/refresh your access token for the $env environment."
                )
        }
    }

    /**
     * Create some fake history, so we have something to query for.
     * @return null on failure.  Otherwise returns the list of ReportIds created.
     */
    fun submitTestData(environment: Environment, options: CoolTestOptions): List<ReportId>? {
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
        }
        return reportIds
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties(environment)
        var passed = true

        /*
         * On local development we always use `simple_report` as the claiming organization.
         */
        ugly("Starting $name Test: get submission history ")

        val reportIds = submitTestData(environment, options)
            ?: return bad("*** $name TEST FAILED:  Unable to submit test data")
        passed = isValidJsonResponse(
            // todo Is "ignore" right?   Make this a param?  Submit data first, then check it
            "${environment.url}/api/history/$historyTestOrgName/submissions",
            listOf(
                "limit" to "10",
            ),
            getAccessToken(environment)
        )

        return passed
    }

    private fun isValidJsonResponse(path: String, param: List<Pair<String, Any?>>?, bearer: String): Boolean {
        val (request, response, result) = Fuel.get(path, param)
            .authentication()
            .bearer(bearer)
            .responseJson()

        echo(response.toString())
        echo(result.toString())

        return if (result is Result.Success) {
            true
        } else {
            bad("***$name Test FAILED: $result")
            false
        }
    }
}