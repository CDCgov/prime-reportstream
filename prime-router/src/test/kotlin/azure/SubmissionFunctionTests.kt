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
import gov.cdc.prime.router.ActionResponse
import gov.cdc.prime.router.ReportId
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

    class TestSubmissionAccess(val dataset: List<SubmissionHistory>, val mapper: ObjectMapper) : SubmissionAccess {

        override fun <T> fetchActions(
            sendingOrg: String,
            order: SubmissionAccess.SortOrder,
            resultsAfterDate: OffsetDateTime?,
            limit: Int,
            klass: Class<T>
        ): List<T> {
            @Suppress("UNCHECKED_CAST")
            return dataset as List<T>
        }
    }

    val testData = listOf(
        SubmissionHistory(
            actionId = 8,
            createdAt = OffsetDateTime.parse("2021-11-30T16:36:54.919104Z"),
            sendingOrg = "simple_report",
            httpStatus = 201,
            externalName = "testname.csv",
            actionResponse = ActionResponse(
                id = "a2cf1c46-7689-4819-98de-520b5007e45f",
                topic = "covid-19",
                reportItemCount = 3,
                warningCount = 3,
                errorCount = 0
            )
        ),
        SubmissionHistory(
            actionId = 7,
            createdAt = OffsetDateTime.parse("2021-11-30T16:36:48.307109Z"),
            sendingOrg = "simple_report",
            httpStatus = 400,
            actionResponse = ActionResponse(
                id = null,
                topic = null,
                reportItemCount = null,
                warningCount = 1,
                errorCount = 1
            )
        )
    )

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
                    HttpStatus.OK,
                    listOf(
                        ExpectedSubmissionHistory(
                            taskId = 8,
                            createdAt = OffsetDateTime.parse("2021-11-30T16:36:54.919104Z"),
                            sendingOrg = "simple_report",
                            httpStatus = 201,
                            externalName = "testname.csv",
                            id = ReportId.fromString("a2cf1c46-7689-4819-98de-520b5007e45f"),
                            topic = "covid-19",
                            reportItemCount = 3,
                            warningCount = 3,
                            errorCount = 0
                        ),
                        ExpectedSubmissionHistory(
                            taskId = 7,
                            createdAt = OffsetDateTime.parse("2021-11-30T16:36:48.307109Z"),
                            sendingOrg = "simple_report",
                            httpStatus = 400,
                            id = null,
                            topic = null,
                            reportItemCount = null,
                            warningCount = 1,
                            errorCount = 1
                        )
                    )
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
            if (response.getStatus() == HttpStatus.OK) {
                val submissions: List<ExpectedSubmissionHistory> = mapper.readValue(response.body.toString())
                if (it.expectedResponse.body != null) {
                    assertThat(submissions.size).isEqualTo(it.expectedResponse.body.size)
                    assertThat(submissions).isEqualTo(it.expectedResponse.body)
                }
            }
        }
    }
}