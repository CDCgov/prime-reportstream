package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.MockSettings
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.cli.tests.ExpectedSubmissionList
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.SubmissionHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TestDefaultJwt
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.test.Test

data class ExpectedAPIResponse(
    val status: HttpStatus,
    val body: List<ExpectedSubmissionList>? = null
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
    val overallStatus: String,
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
            sortDir: SubmissionAccess.SortDir,
            sortColumn: SubmissionAccess.SortColumn,
            cursor: OffsetDateTime?,
            since: OffsetDateTime?,
            until: OffsetDateTime?,
            pageSize: Int,
            showFailed: Boolean,
            klass: Class<T>
        ): List<T> {
            @Suppress("UNCHECKED_CAST")
            return dataset as List<T>
        }

        override fun <T> fetchAction(
            sendingOrg: String,
            submissionId: Long,
            klass: Class<T>
        ): T? {
            @Suppress("UNCHECKED_CAST")
            return dataset.first() as T
        }

        override fun <T> fetchRelatedActions(
            submissionId: Long,
            klass: Class<T>
        ): List<T> {
            @Suppress("UNCHECKED_CAST")
            return dataset as List<T>
        }
    }

    private val testData = listOf(
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

    private val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    private val accessSpy = spyk(DatabaseAccess(connection))

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy).build()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test list submissions`() {
        val testCases = listOf(
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer 111.222.333", "authentication-type" to "okta"),
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.UNAUTHORIZED
                ),
                "unauthorized"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"), // no 'okta' auth-type, so this uses server2server auth
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.OK,
                    listOf(
                        ExpectedSubmissionList(
                            submissionId = 8,
                            timestamp = OffsetDateTime.parse("2021-11-30T16:36:54.919Z"),
                            sender = "simple_report",
                            httpStatus = 201,
                            externalName = "testname.csv",
                            id = ReportId.fromString("a2cf1c46-7689-4819-98de-520b5007e45f"),
                            topic = "covid-19",
                            reportItemCount = 3
                        ),
                        ExpectedSubmissionList(
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
                mapOf("authorization" to "Bearer fdafads", "authentication-type" to "okta"),
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
                    "sortDir" to "ASC"
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
                    "since" to "2021-11-30T16:36:54.307109Z",
                    "until" to "2021-11-30T16:36:53.919104Z",
                    "sortCol" to "CREATED_AT",
                    "sortDir" to "ASC"
                ),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "good all params"
            )
        )

        val metadata = Metadata(schema = Schema(name = "one", topic = "test"))
        val settings = MockSettings()
        val sender = CovidSender(
            name = "default",
            organizationName = "simple_report",
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender.fullName] = sender
        val engine = makeEngine(metadata, settings)

        testCases.forEach {
            logger.info("Executing list submissions unit test ${it.name}")
            val httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.httpHeaders += it.headers
            httpRequestMessage.parameters += it.parameters
            // Invoke
            val response = SubmissionFunction(
                SubmissionsFacade(TestSubmissionAccess(testData, mapper)),
                workflowEngine = engine,
            ).getOrgSubmissionsList(
                httpRequestMessage,
                "simple_report",
            )
            // Verify
            assertThat(response.status).isEqualTo(it.expectedResponse.status)
            if (response.status == HttpStatus.OK) {
                val submissions: List<ExpectedSubmissionList> = mapper.readValue(response.body.toString())
                if (it.expectedResponse.body != null) {
                    assertThat(submissions.size).isEqualTo(it.expectedResponse.body.size)
                    assertThat(submissions).isEqualTo(it.expectedResponse.body)
                }
            }
        }
    }

    fun `test list params`() {
    }

    private fun setupSubmissionFunctionForTesting(
        oktaClaimsOrganizationName: String,
        facade: SubmissionsFacade,
    ): SubmissionFunction {
        val claimsMap = buildClaimsMap(oktaClaimsOrganizationName)
        val metadata = Metadata(schema = Schema(name = "one", topic = "test"))
        val settings = MockSettings()
        val sender = CovidSender(
            name = "default",
            organizationName = organizationName,
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender.fullName] = sender
        val sender2 = CovidSender(
            name = "default",
            organizationName = otherOrganizationName,
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender2.fullName] = sender2
        val engine = makeEngine(metadata, settings)
        mockkObject(OktaAuthentication.Companion)
        every { OktaAuthentication.Companion.decodeJwt(any()) } returns
            TestDefaultJwt(
                "a.b.c",
                Instant.now(),
                Instant.now().plusSeconds(60),
                claimsMap
            )
        return SubmissionFunction(
            submissionsFacade = facade,
            workflowEngine = engine
        )
    }

    private fun setupHttpRequestMessageForTesting(): MockHttpRequestMessage {
        val httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.httpHeaders += mapOf(
            "authorization" to "Bearer 111.222.333",
            "authentication-type" to "okta"
        )
        return httpRequestMessage
    }

    @Test
    fun `test access user can view their org's submission history`() {
        val facade = SubmissionsFacade(TestSubmissionAccess(testData, mapper))
        val submissionFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, facade)
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, organizationName)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot view another org's submission history`() {
        val facade = SubmissionsFacade(TestSubmissionAccess(testData, mapper))
        val submissionFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, facade)
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, otherOrganizationName)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can view all org's submission history`() {
        val facade = SubmissionsFacade(TestSubmissionAccess(testData, mapper))
        val submissionFunction = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, facade)
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, organizationName)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, otherOrganizationName)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test get report detail history`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"

        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, mockSubmissionFacade)

        mockkObject(AuthenticationStrategy.Companion)
        every { AuthenticationStrategy.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        // Invalid id:  not a UUID nor a Long
        var response = function.getReportDetailedHistory(mockRequest, "bad")
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Database error
        every { mockSubmissionFacade.fetchActionForReportId(any()) }.throws(DataAccessException("dummy"))
        response = function.getReportDetailedHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)

        // Good UUID, but Not found
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns null
        response = function.getReportDetailedHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good return
        val returnBody = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(), 201,
            null
        )
        // Happy path with a good UUID
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns action
        every { mockSubmissionFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkSenderAccessAuthorization(any(), any()) } returns true
        response = function.getReportDetailedHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        var responseBody: DetailSubmissionHistoryResponse = mapper.readValue(response.body.toString())
        assertThat(responseBody.submissionId).isEqualTo(returnBody.actionId)
        assertThat(responseBody.overallStatus).isEqualTo(returnBody.overallStatus.toString())

        // Good uuid, but not a 'receive' step report.
        action.actionName = TaskAction.process
        response = function.getReportDetailedHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not found
        val goodActionId = "550"
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchAction(any()) } returns null
        response = function.getReportDetailedHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not authorized
        every { mockSubmissionFacade.fetchAction(any()) } returns action
        every { mockSubmissionFacade.checkSenderAccessAuthorization(any(), any()) } returns false // not authorized
        response = function.getReportDetailedHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)

        // Happy path with a good actionId
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns null // not used for an actionId
        every { mockSubmissionFacade.fetchAction(any()) } returns action
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkSenderAccessAuthorization(any(), any()) } returns true
        response = function.getReportDetailedHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        responseBody = mapper.readValue(response.body.toString())
        assertThat(responseBody.submissionId).isEqualTo(returnBody.actionId)
        assertThat(responseBody.sender).isEqualTo(returnBody.sender)

        // bad actionId, Not found
        val badActionId = "24601"
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchAction(any()) } returns null
        response = function.getReportDetailedHistory(mockRequest, badActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // empty actionId, Not found
        val emptyActionId = ""
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchAction(any()) } returns null
        response = function.getReportDetailedHistory(mockRequest, emptyActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
    }
}