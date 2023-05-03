package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.LivdData
import gov.cdc.prime.router.common.Environment

/**
 * Wraps the test case for the LIVD API
 */
data class LivdApiTestCase(
    val name: String,
    val path: String,
    val parameters: List<Pair<String, Any?>>? = null,
    val expectedHttpStatus: HttpStatus = HttpStatus.OK,
    val jsonResponseChecker: (String, CoolTest, LivdApiTestCase) -> Boolean =
        fun(_: String, _: CoolTest, _: LivdApiTestCase) = true
)

/**
 * Our LIVD API test
 */
class LivdApiTest : CoolTest() {
    override val name = "livdApi"
    override val description = "Test the LIVD API"
    override val status = TestStatus.SMOKE

    /**
     * Runs our tests
     */
    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting $name Test: $description")
        return runLivdApiTestCases(
            listOf(
                LivdApiTestCase(
                    "check LIVD API is available",
                    "${environment.url}$apiEndpointPath"
                ),
                LivdApiTestCase(
                    "check LIVD API returns data",
                    "${environment.url}$apiEndpointPath",
                    jsonResponseChecker = fun(
                        json: String,
                        testBeingRun: CoolTest,
                        testCase: LivdApiTestCase
                    ): Boolean {
                        val livdValues = jsonMapper.readValue(json, Array<LivdData>::class.java)
                        if (livdValues.isEmpty()) {
                            return testBeingRun.bad(
                                "***${testBeingRun.name}: TEST '${testCase.name}' FAILED: " +
                                    "Call to API succeeded but results array is empty"
                            )
                        }
                        return true
                    }
                ),
                LivdApiTestCase(
                    "check filtering data",
                    "${environment.url}$apiEndpointPath",
                    listOf(
                        Pair(
                            "manufacturer",
                            "Zymo Research Corporation"
                        )
                    ),
                    jsonResponseChecker = fun(
                        json: String,
                        testBeingRun: CoolTest,
                        testCase: LivdApiTestCase
                    ): Boolean {
                        val livdValues = jsonMapper.readValue(json, Array<LivdData>::class.java)
                        if (
                            livdValues.all { livdTest ->
                                livdTest.manufacturer.equals("Zymo Research Corporation", true)
                            }
                        ) {
                            return true
                        }
                        return testBeingRun.bad(
                            "***${testBeingRun.name}: TEST '${testCase.name}' FAILED: " +
                                "Filtering via the API did not succeed."
                        )
                    }
                )
            )
        )
    }

    /**
     * Run each individual test case
     */
    private fun runLivdApiTestCases(testCases: List<LivdApiTestCase>): Boolean {
        return testCases.map {
            ugly("Starting test: ${it.name}")
            val (queryPass, json) = livdApiQuery(it)
            val sanityCheck = when {
                !queryPass -> false
                json.isNullOrBlank() -> true
                else -> it.jsonResponseChecker(json, this, it)
            }

            if (!sanityCheck && !json.isNullOrBlank()) bad("This json failed:\n$json")
            if (sanityCheck) good("Test passed: ${it.name}")
            sanityCheck
        }.reduce { acc, onePassed -> acc and onePassed }
    }

    /**
     * Runs the query against the LIVD API for the given path and parameters
     */
    private fun livdApiQuery(testCase: LivdApiTestCase): Pair<Boolean, String?> {
        val (_, response, result) = Fuel.get(testCase.path, testCase.parameters)
            .timeoutRead(75000)
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

    companion object {
        const val apiEndpointPath = "/api/metadata/livd"

        /** A static instance of the mapper to work with in our checks */
        val jsonMapper: ObjectMapper = jacksonObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        )
    }
}