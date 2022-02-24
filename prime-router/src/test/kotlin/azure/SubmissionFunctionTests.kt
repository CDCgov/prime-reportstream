package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ActionResponse
import gov.cdc.prime.router.DetailedActionResponse
import gov.cdc.prime.router.DetailedSubmissionHistory
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SubmissionHistory
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.cli.tests.ExpectedSubmissionHistory
import gov.cdc.prime.router.common.JacksonMapperUtilities
import io.mockk.every
import io.mockk.mockk
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.TestInstance
import java.time.OffsetDateTime
import kotlin.test.Test

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

/**
 * Detail Submission History Response from the API.
 */
data class DetailSubmissionHistoryResponse(
    val submissionId: Long,
    val id: String?,
    val timestamp: OffsetDateTime,
    val sender: String?,
    val httpStatus: Int?,
    val externalName: String? = "",
    val actionResponse: DetailedActionResponse? = null
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubmissionFunctionTests {
    private val mapper = JacksonMapperUtilities.datesAsTextMapper

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

        override fun <T, P, U> fetchAction(
            sendingOrg: String,
            submissionId: Long,
            klass: Class<T>,
            reportsKlass: Class<P>,
            logsKlass: Class<U>,
        ): T? {
            @Suppress("UNCHECKED_CAST")
            return dataset.first() as T
        }

        override fun <T, P, U> fetchRelatedActions(
            submissionId: Long,
            klass: Class<T>,
            reportsKlass: Class<P>,
            logsKlass: Class<U>,
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
            val httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.httpHeaders += it.headers
            httpRequestMessage.parameters += it.parameters
            // Invoke
            val response = SubmissionFunction(
                SubmissionsFacade(
                    TestSubmissionAccess(testData, mapper)
                )
            ).getOrgSubmissions(
                httpRequestMessage,
                "simple_report",
            )
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

    @Test
    fun `test get report history`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"

        val function = SubmissionFunction(mockSubmissionFacade)

        // Invalid UUID
        var response = function.getReportHistory(mockRequest, "org", "badUUID")
        assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
        response = function.getReportHistory(mockRequest, "org", "550")
        assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)

        // Database error
        every { mockSubmissionFacade.findReport(any(), any()) }.throws(DataAccessException("dummy"))
        response = function.getReportHistory(mockRequest, "org", goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)

        // Not found
        every { mockSubmissionFacade.findReport(any(), any()) } returns null
        response = function.getReportHistory(mockRequest, "org", goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good return
        val returnBody = DetailedSubmissionHistory(
            100, TaskAction.receive, OffsetDateTime.now(), "org",
            null, null, null, null, null
        )
        every { mockSubmissionFacade.findReport(any(), any()) } returns returnBody
        response = function.getReportHistory(mockRequest, "org", goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        val responseBody: DetailSubmissionHistoryResponse = mapper.readValue(response.body.toString())
        assertThat(responseBody.submissionId).isEqualTo(returnBody.actionId)
        assertThat(responseBody.sender).isEqualTo(returnBody.sendingOrg)
    }
}