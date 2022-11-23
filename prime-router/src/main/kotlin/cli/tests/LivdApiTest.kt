package gov.cdc.prime.router.cli.tests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.common.Environment

data class LivdApiTestCase(
    val name: String,
    val path: String,
    val parameters: List<Pair<String, Any?>>? = null,
    val expectedHttpStatus: HttpStatus = HttpStatus.OK,
    val jsonResponseChecker: (String) -> Boolean = fun(_: String) = true
)

class LivdApiTest : CoolTest() {
    override val name = "livdApi"
    override val description = "Test the LIVD API"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test: $description")
        return runLivdApiTestCases(
            listOf(
                LivdApiTestCase(
                    "check LIVD API is available",
                    "${environment.url}/api/metadata/livd"
                )
            )
        )
    }

    fun runLivdApiTestCases(testCases: List<LivdApiTestCase>): Boolean {
        return testCases.map {
            ugly("Starting test: ${it.name}")
            val (queryPass, json) = livdApiQuery(it)
            val sanityCheck = when {
                !queryPass -> false
                json.isNullOrBlank() -> true
                else -> it.jsonResponseChecker(json)
            }

            if (!sanityCheck && !json.isNullOrBlank()) bad("This json failed:\n$json")
            if (sanityCheck) good("Test passed: ${it.name}")
            sanityCheck
        }.reduce { acc, onePassed -> acc and onePassed }
    }

    private fun livdApiQuery(testCase: LivdApiTestCase): Pair<Boolean, String?> {
        val (_, response, result) = Fuel.get(testCase.path, testCase.parameters)
            .timeoutRead(45000)
            .responseString()

        return if (response.statusCode != testCase.expectedHttpStatus.value()) {
            bad(
                "***$name Test '${testCase.name}' FAILED:" +
                    " Expected HttpStatus ${testCase.expectedHttpStatus}. Got ${response.statusCode}"
            )
            Pair(false, null)
        } else if (testCase.expectedHttpStatus != HttpStatus.OK) {
            Pair(true, null)
        } else if (result !is Result.Success) {
            bad("***$name Test '${testCase.name}' FAILED: Result is $result")
            Pair(true, null)
        } else {
            val json: String = result.value
            if (json.isEmpty()) {
                bad("***$name Test '${testCase.name}' FAILED: empty body")
                Pair(false, null)
            } else {
                Pair(true, json)
            }
        }
    }
}