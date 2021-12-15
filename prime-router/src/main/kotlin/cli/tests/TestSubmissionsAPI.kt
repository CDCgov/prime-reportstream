package gov.cdc.prime.router.cli.tests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.common.Environment

class TestSubmissionsAPI : CoolTest() {
    override val name = "end2end-submission"
    override val description = "Confirm records via submission API."
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties(environment)
        var passed = true

        /*
         * On local development we always use `simple_report` as the claiming organization.
         */
        ugly(
            "Starting $name Test: get submission history " +
                "from ${simpleRepSender.fullName} or Simple Report " +
                "on localhost."
        )

        passed = passed and isValidJsonResponse(
            "${environment.url}/api/history/submissions",
            listOf(
                "limit" to "10",
            ),
            simpleReportSenderName
        )

        return passed
    }

    private fun isValidJsonResponse(path: String, param: List<Pair<String, Any?>>?, bearer: String): Boolean {
        val (request, response, result) = Fuel.get(path, param)
            .authentication()
            .bearer(bearer)
            .responseJson()

        echo(request.toString())
        echo(response.toString())
        echo(result.toString())

        return when (result) {
            is Result.Failure -> throw result.getException()
            is Result.Success -> true
        }
    }
}