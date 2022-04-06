package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.DetailedSubmissionHistory
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SubmissionHistory
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.cli.tests.ExpectedSubmissionHistory
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TestDefaultJwt
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.TestInstance
import java.time.Instant
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
    val status: String,
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubmissionFunctionTests : Logging {
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    private val organizationName = "test-lab"
    private val oktaClaimsOrganizationName = "DHSender_$organizationName"
    private val otherOrganizationName = "test-lab-2"

    private fun buildClaimsMap(organizationName: String): Map<String, Any> {
        return mapOf(
            "sub" to "test",
            "organization" to listOf(organizationName)
        )
    }

    class TestSubmissionAccess(val dataset: List<SubmissionHistory>, val mapper: ObjectMapper) : SubmissionAccess {

        override fun <T> fetchActions(
            sendingOrg: String,
            order: SubmissionAccess.SortOrder,
            sortColumn: SubmissionAccess.SortColumn,
            cursor: OffsetDateTime?,
            toEnd: OffsetDateTime?,
            limit: Int,
            showFailed: Boolean,
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
            reportId = "a2cf1c46-7689-4819-98de-520b5007e45f",
            schemaTopic = "covid-19",
            itemCount = 3,
//                warningCount = 3,
//                errorCount = 0
        ),
        SubmissionHistory(
            actionId = 7,
            createdAt = OffsetDateTime.parse("2021-11-30T16:36:48.307109Z"),
            sendingOrg = "simple_report",
            httpStatus = 400,
            externalName = "testname.csv",
            reportId = null,
            schemaTopic = null,
            itemCount = null,
//                warningCount = 1,
//                errorCount = 1
        )
    )

    @Test
    fun `test list submissions`() {
        val testCases = listOf(
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer 111.222.333"),
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
                            submissionId = 8,
                            timestamp = OffsetDateTime.parse("2021-11-30T16:36:54.919Z"),
                            sender = "simple_report",
                            httpStatus = 201,
                            externalName = "testname.csv",
                            id = ReportId.fromString("a2cf1c46-7689-4819-98de-520b5007e45f"),
                            topic = "covid-19",
                            reportItemCount = 3
                        ),
                        ExpectedSubmissionHistory(
                            submissionId = 7,
                            timestamp = OffsetDateTime.parse("2021-11-30T16:36:48.307Z"),
                            sender = "simple_report",
                            httpStatus = 400,
                            externalName = "testname.csv",
                            id = null,
                            topic = null,
                            reportItemCount = null
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
                    "cursor" to "2021-11-30T16:36:48.307Z",
                    "sort" to "ASC"
                ),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "good minimum params"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf(
                    "pagesize" to "10",
                    "cursor" to "2021-11-30T16:36:54.307109Z",
                    "endCursor" to "2021-11-30T16:36:53.919104Z",
                    "sortCol" to "CREATED_AT",
                    "sort" to "ASC"
                ),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "good all params"
            )
        )

        testCases.forEach {
            logger.info("Executing list submissions unit test ${it.name}")
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
            assertThat(response.status).isEqualTo(it.expectedResponse.status)
            if (response.status == HttpStatus.OK) {
                val submissions: List<ExpectedSubmissionHistory> = mapper.readValue(response.body.toString())
                if (it.expectedResponse.body != null) {
                    assertThat(submissions.size).isEqualTo(it.expectedResponse.body.size)
                    assertThat(submissions).isEqualTo(it.expectedResponse.body)
                }
            }
        }
    }

    private fun setUpAccessTests(organizationName: String): Pair<SubmissionFunction, MockHttpRequestMessage> {
        val oktaAuth = spyk<OktaAuthentication>()
        val claimsMap = buildClaimsMap(organizationName)

        every { oktaAuth.decodeJwt(any()) } returns
            TestDefaultJwt(
                "a.b.c",
                Instant.now(),
                Instant.now().plusSeconds(60),
                claimsMap
            )

        val httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.httpHeaders += mapOf("authorization" to "Bearer 111.222.333")

        val submissionFunction = SubmissionFunction(
            SubmissionsFacade(
                TestSubmissionAccess(testData, mapper)
            ),
            oktaAuth
        )
        return Pair(submissionFunction, httpRequestMessage)
    }

    @Test
    fun `test access user can view their org's submission history`() {
        val (submissionFunction, httpRequestMessage) = setUpAccessTests(oktaClaimsOrganizationName)

        val response = submissionFunction.getOrgSubmissions(httpRequestMessage, organizationName)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot view another org's submission history`() {
        val (submissionFunction, httpRequestMessage) = setUpAccessTests(oktaClaimsOrganizationName)

        val response = submissionFunction.getOrgSubmissions(httpRequestMessage, otherOrganizationName)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access dhadmins can view all org's submission history`() {
        val (submissionFunction, httpRequestMessage) = setUpAccessTests(oktaSystemAdminGroup)

        val response = submissionFunction.getOrgSubmissions(httpRequestMessage, organizationName)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test get report history`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"

        val function = SubmissionFunction(mockSubmissionFacade)

        // Invalid id:  not a UUID nor a Long
        var response = function.getReportHistory(mockRequest, "bad")
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Database error
        every { mockSubmissionFacade.fetchActionForReportId(any()) }.throws(DataAccessException("dummy"))
        response = function.getReportHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)

        // Good UUID, but Not found
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns null
        response = function.getReportHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good return
        val returnBody = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(), 201,
            null
        )

        // Happy path with a good UUID
        val action = Action()
        action.actionId = 550
        action.sendingOrg = "foobar"
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns action
        every { mockSubmissionFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkSenderAccessAuthorization(any(), any()) } returns true
        response = function.getReportHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        var responseBody: DetailSubmissionHistoryResponse = mapper.readValue(response.body.toString())
        assertThat(responseBody.submissionId).isEqualTo(returnBody.actionId)
        assertThat(responseBody.status).isEqualTo(returnBody.status)

        // Good uuid, but not a 'receive' step report.
        action.actionName = TaskAction.process
        response = function.getReportHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not found
        val goodActionId = "550"
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchAction(any()) } returns null
        response = function.getReportHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not authorized
        every { mockSubmissionFacade.fetchAction(any()) } returns action
        every { mockSubmissionFacade.checkSenderAccessAuthorization(any(), any()) } returns false // not authorized
        response = function.getReportHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)

        // Happy path with a good actionId
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns null // not used for an actionId
        every { mockSubmissionFacade.fetchAction(any()) } returns action
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkSenderAccessAuthorization(any(), any()) } returns true
        response = function.getReportHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        responseBody = mapper.readValue(response.body.toString())
        assertThat(responseBody.submissionId).isEqualTo(returnBody.actionId)
        assertThat(responseBody.sender).isEqualTo(returnBody.sender)
    }
}