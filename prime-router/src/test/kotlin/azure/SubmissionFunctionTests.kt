package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.SubmissionHistory
import gov.cdc.prime.router.cli.tests.ExpectedSubmissionHistory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.OffsetDateTime

data class ExpectedAPIResponse(
    val status: HttpStatus,
    val body: List<ExpectedSubmissionHistory>? = null
)

data class SubmissionUnitTestCase(
    val headers: Map<String, String>,
    val parameters: Map<String, String>,
    val expectedResponse: ExpectedAPIResponse,
    val name: String?
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubmissionFunctionTests {
    // Ignoring unknown properties because we don't require them. -DK
    val mapper = jacksonMapperBuilder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    class TestSubmissionAccess(val dataset: String, val mapper: ObjectMapper) : SubmissionAccess {

        override fun <T> fetchActions(
            sendingOrg: String,
            order: SubmissionAccess.SortOrder,
            resultsAfterDate: OffsetDateTime?,
            limit: Int,
            klass: Class<T>
        ): List<T> {
            var list: List<SubmissionHistory> = mapper.readValue(dataset)
            list = list.filter {
                it.sendingOrg == sendingOrg
            }
            return list as List<T>
        }
    }

    val testData = """[
    {
        "actionId": 8,
        "createdAt": "2021-11-30T16:36:54.919104Z",
        "sendingOrg": "simple_report",
        "httpStatus": 201,
        "actionResponse": {
            "id": "a2cf1c46-7689-4819-98de-520b5007e45f",
            "topic": "covid-19",
            "reportItemCount": 3,
            "warningCount": 3,
            "errorCount": 0
        }
    },
    {
        "actionId": 7,
        "createdAt": "2021-11-30T16:36:48.307109Z",
        "sendingOrg": "simple_report",
        "httpStatus": 400,
        "actionResponse": {
            "id": null,
            "topic": null,
            "reportItemCount": null,
            "warningCount": 1,
            "errorCount": 1
        }
    },
    {
        "actionId": 4,
        "createdAt": "2021-11-30T15:30:28.134875Z",
        "sendingOrg": "simple_report",
        "httpStatus": 201,
        "actionResponse": {
            "id": "046a5c0d-7719-48c8-80bd-832f5f68a1bd",
            "topic": "covid-19",
            "reportItemCount": 1,
            "warningCount": 1,
            "errorCount": 0
        }
    },
    {
        "actionId": 1,
        "createdAt": "2021-11-30T15:26:06.016247Z",
        "sendingOrg": "simple_report",
        "httpStatus": 201,
        "actionResponse": {
            "id": "508cdeb1-ac3f-4453-aa7c-45e4a825c7c0",
            "topic": "covid-19",
            "reportItemCount": 1,
            "warningCount": 1,
            "errorCount": 0
        }
    },
    {
        "actionId": 710,
        "createdAt": "2021-11-30T16:36:48.307109Z",
        "sendingOrg": "bobs_org",
        "httpStatus": 400,
        "actionResponse": {
            "id": null,
            "topic": null,
            "reportItemCount": null,
            "warningCount": 1,
            "errorCount": 1
        }
    }
]"""

    @Test
    fun `test list submissions`() {
        val testCases = listOf(
            SubmissionUnitTestCase(
                emptyMap(),
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.UNAUTHORIZED
                ),
                "unauthorized"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "simple success"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf("cursor" to "nonsense"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad date"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf("pagesize" to "-1"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pagesize"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf("pagesize" to "fdas"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pagesize, garbage"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf(
                    "pagesize" to "10",
                    "cursor" to "2021-11-30T16:36:48.307109Z",
                    "sort" to "ASC"
                ),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "good all params"
            )

        )

        testCases.forEach {
            var httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.httpHeaders += it.headers
            httpRequestMessage.parameters += it.parameters
            // Invoke
            var response = SubmissionFunction(
                SubmissionsFacade(
                    TestSubmissionAccess(testData, mapper)
                )
            ).submissions(httpRequestMessage)
            // Verify
            assertThat(response.getStatus()).isEqualTo(it.expectedResponse.status)
        }
    }
}