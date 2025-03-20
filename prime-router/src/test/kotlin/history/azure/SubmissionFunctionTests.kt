package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.TranslatorConfiguration
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.MockSettings
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.ReportLineage
import gov.cdc.prime.router.cli.tests.ExpectedSubmissionList
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.credentials.RestCredential
import gov.cdc.prime.router.credentials.UserJksCredential
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.history.Destination
import gov.cdc.prime.router.history.DetailedReport
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.SubmissionHistory
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TestDefaultJwt
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import gov.cdc.prime.router.transport.RESTTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.spyk
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.exception.DataAccessException
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test

@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubmissionFunctionTests : Logging {
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    private val organizationName = "test-lab"
    private val organizationClient = "default"
    private val oktaClaimsOrganizationName = "DHSender_$organizationName"
    private val otherOrganizationName = "test-lab-2"

    private fun buildClaimsMap(organizationName: String): Map<String, Any> = mapOf(
            "sub" to "test",
            "organization" to listOf(organizationName)
        )

    data class ExpectedAPIResponse(val status: HttpStatus, val body: List<ExpectedSubmissionList>? = null)

    data class SubmissionUnitTestCase(
        val headers: Map<String, String>,
        val parameters: Map<String, String>,
        val expectedResponse: ExpectedAPIResponse,
        val name: String?,
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

    private val testData = listOf(
        SubmissionHistory(
            actionId = 8,
            createdAt = OffsetDateTime.parse("2021-11-30T16:36:54.919104Z"),
            externalName = "test-name.csv",
            reportId = "a2cf1c46-7689-4819-98de-520b5007e45f",
            schemaTopic = Topic.COVID_19,
            itemCount = 3,
            sendingOrg = organizationName,
            sendingOrgClient = organizationClient,
            httpStatus = 201,
            bodyUrl = "http://anyblob.com",
            schemaName = "None",
            bodyFormat = "",
        ),
        SubmissionHistory(
            actionId = 7,
            createdAt = OffsetDateTime.parse("2021-11-30T16:36:48.307109Z"),
            externalName = "test-name.csv",
            reportId = null,
            schemaTopic = null,
            itemCount = null,
            sendingOrg = organizationName,
            sendingOrgClient = organizationClient,
            httpStatus = 400,
            bodyUrl = "",
            schemaName = "",
            bodyFormat = "",
        )
    )

    private val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    private val accessSpy = spyk(DatabaseAccess(connection))

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider, useMockDatabase: Boolean): WorkflowEngine {
        val dbAccess = if (useMockDatabase) accessSpy else ReportStreamTestDatabaseContainer.testDatabaseAccess
        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(dbAccess).build()
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
                mapOf("authorization" to "Bearer fads"), // no 'okta' auth-type, so this uses server2server auth
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.OK,
                    listOf(
                        ExpectedSubmissionList(
                            submissionId = 8,
                            timestamp = OffsetDateTime.parse("2021-11-30T16:36:54.919Z"),
                            sender = ClientSource(organizationName, organizationClient).name,
                            httpStatus = 201,
                            externalName = "test-name.csv",
                            id = ReportId.fromString("a2cf1c46-7689-4819-98de-520b5007e45f"),
                            topic = "covid-19",
                            reportItemCount = 3,
                            fileName = "http://anyblob.com",
                            fileType = ""
                        ),
                        ExpectedSubmissionList(
                            submissionId = 7,
                            timestamp = OffsetDateTime.parse("2021-11-30T16:36:48.307Z"),
                            sender = ClientSource(organizationName, organizationClient).name,
                            httpStatus = 400,
                            externalName = "test-name.csv",
                            id = null,
                            topic = null,
                            reportItemCount = null,
                            fileName = "",
                            fileType = ""
                        )
                    )
                ),
                "simple success"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf("cursor" to "nonsense"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad date"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf("pageSize" to "-1"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pageSize"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf("pageSize" to "fads"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pageSize, garbage"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf(
                    "pageSize" to "10",
                    "cursor" to "2021-11-30T16:36:48.307Z",
                    "sortDir" to "ASC"
                ),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "good minimum params"
            ),
            SubmissionUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf(
                    "pageSize" to "10",
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

        val settings = MockSettings()
        val sender = CovidSender(
            name = "default",
            organizationName = organizationName,
            format = MimeFormat.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender.fullName] = sender

        testCases.forEach {
            logger.info("Executing list submissions unit test ${it.name}")
            val httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.httpHeaders += it.headers
            httpRequestMessage.parameters += it.parameters
            // Invoke
            val submissionsFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
            val response = submissionsFunction.getOrgSubmissionsList(
                httpRequestMessage,
                organizationName
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

    private fun mockFacade(): SubmissionsFacade {
        val mockDatabaseAccess = mockkClass(HistoryDatabaseAccess::class)

        every {
            mockDatabaseAccess.fetchActionsForSubmissions<SubmissionHistory>(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns testData
        every {
            mockDatabaseAccess.fetchAction<SubmissionHistory>(
                any(),
                any(),
                any(),
            )
        } returns testData.first()

        every {
            mockDatabaseAccess.fetchRelatedActions<SubmissionHistory>(
                any(),
                any(),
            )
        } returns testData

        return SubmissionsFacade(mockDatabaseAccess)
    }

    private fun setupSubmissionFunctionForTesting(
        oktaClaimsOrganizationName: String,
        facade: SubmissionsFacade,
        useMockDatabase: Boolean = true,
    ): SubmissionFunction {
        val claimsMap = buildClaimsMap(oktaClaimsOrganizationName)
        val metadata = Metadata(schema = Schema(name = "one", topic = Topic.TEST))
        val settings = MockSettings()
        val sender = CovidSender(
            name = "default",
            organizationName = organizationName,
            format = MimeFormat.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender.fullName] = sender
        val sender2 = CovidSender(
            name = "default",
            organizationName = otherOrganizationName,
            format = MimeFormat.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )

        val receiver = Receiver(
            "flexion.etor-service-receiver-orders",
            "flexion.etor-service-receiver-orders",
            Topic.ETOR_TI,
            CustomerStatus.ACTIVE,
            mockk<TranslatorConfiguration>(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            false,
            emptyList(),
            emptyList(),
            false,
            "test", null, "", mockk<RESTTransportType>(), "", emptyList()
        )

        settings.senderStore[sender2.fullName] = sender2
        settings.receiverStore["flexion.etor-service-receiver-orders"] = receiver
        val engine = makeEngine(metadata, settings, useMockDatabase)
        engine.settings.receivers.plus(receiver)
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
        )
        return httpRequestMessage
    }

    @Test
    fun `test access user can view their organization's submission history`() {
        val submissionFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, organizationName)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot view another organization's submission history`() {
        val submissionFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, otherOrganizationName)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test retrieval of a non-existent organization's submission history`() {
        val submissionFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, "foo")
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test retrieval of a an organization's submission history for a non-existent sending channel`() {
        val submissionFunction = setupSubmissionFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = submissionFunction.getOrgSubmissionsList(httpRequestMessage, "test-lab.foo")
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test access DHPrimeAdmins can view all organization's submission history`() {
        val submissionFunction = setupSubmissionFunctionForTesting("DHPrimeAdmins", mockFacade())
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

        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        val submissionTableService = mockk<SubmissionTableService>()
        every { submissionTableService.getSubmission(any(), any()) } returns null

        mockkObject(SubmissionTableService.Companion)
        every { SubmissionTableService.getInstance() } returns submissionTableService

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
            mutableListOf(), emptyList()
        )
        // Happy path with a good UUID
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns action
        every { mockSubmissionFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkAccessAuthorizationForAction(any(), any(), any()) } returns true
        every { mockSubmissionFacade.fetchReportForActionId(any(), any()) } returns null
        response = function.getReportDetailedHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        var responseBody: DetailSubmissionHistoryResponse = mapper.readValue(response.body.toString())
        assertThat(responseBody.submissionId).isEqualTo(returnBody.actionId)
        assertThat(responseBody.overallStatus).isEqualTo(returnBody.overallStatus.toString())

        // Good uuid, but with `process` action step report.
        action.actionName = TaskAction.process
        response = function.getReportDetailedHistory(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but with `send` action name
        val goodActionId = "550"
        action.actionName = TaskAction.send
        every { mockSubmissionFacade.fetchAction(any()) } returns null
        response = function.getReportDetailedHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not authorized
        action.actionName = TaskAction.receive
        every { mockSubmissionFacade.fetchAction(any()) } returns action
        every {
            mockSubmissionFacade.checkAccessAuthorizationForAction(any(), any(), any())
        } returns false // unauthorized
        response = function.getReportDetailedHistory(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)

        // Happy path with a good actionId
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns null // not used for an actionId
        every { mockSubmissionFacade.fetchAction(any()) } returns action
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkAccessAuthorizationForAction(any(), any(), any()) } returns true
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

    private fun setupEtorTestData(): UUID {
        var receiveReportId: UUID = UUID.randomUUID()
        ReportStreamTestDatabaseContainer.testDatabaseAccess.transact { txn ->
            val action = Action()
            action.actionName = TaskAction.receive
            action.sendingOrg = "test org"
            val receiveActionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            val receiveReport = ReportFile().setSchemaTopic(Topic.ETOR_TI).setReportId(UUID.randomUUID())
                .setActionId(receiveActionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                receiveReport, txn, action
            )

            receiveReportId = receiveReport.reportId

            action.actionName = TaskAction.convert

            val convertActionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            val convertReport = ReportFile().setSchemaTopic(Topic.ETOR_TI).setReportId(UUID.randomUUID())
                .setActionId(convertActionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
                .setReceivingOrg("flexion")
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                convertReport, txn, action
            )

            action.actionName = TaskAction.destination_filter

            val destinationFilterActionId = ReportStreamTestDatabaseContainer
                .testDatabaseAccess.insertAction(txn, action)
            val destinationFilterReport = ReportFile().setSchemaTopic(Topic.ETOR_TI).setReportId(UUID.randomUUID())
                .setActionId(destinationFilterActionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
                .setReceivingOrg("flexion")
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                destinationFilterReport, txn, action
            )

            action.actionName = TaskAction.receiver_filter

            val receiverFilterActionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            val receiverFilterReport = ReportFile().setSchemaTopic(Topic.ETOR_TI).setReportId(UUID.randomUUID())
                .setActionId(receiverFilterActionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
                .setReceivingOrg("flexion")
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                receiverFilterReport, txn, action
            )

            action.actionName = TaskAction.translate

            val translateActionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            val translateReport = ReportFile().setSchemaTopic(Topic.ETOR_TI).setReportId(UUID.randomUUID())
                .setActionId(translateActionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
                .setReceivingOrg("flexion")
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                translateReport, txn, action
            )

            action.actionName = TaskAction.batch

            val batchActionId = ReportStreamTestDatabaseContainer.testDatabaseAccess.insertAction(txn, action)
            val batchReport = ReportFile().setSchemaTopic(Topic.ETOR_TI).setReportId(UUID.randomUUID())
                .setActionId(batchActionId).setSchemaName("schema").setBodyFormat("hl7").setItemCount(1)
                .setReceivingOrg("flexion")
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportFile(
                batchReport, txn, action
            )

            val reportLineage = ReportLineage()
            reportLineage.actionId = convertActionId
            reportLineage.childReportId = convertReport.reportId
            reportLineage.parentReportId = receiveReport.reportId
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportLineage(reportLineage, txn)

            reportLineage.actionId = destinationFilterActionId
            reportLineage.childReportId = destinationFilterReport.reportId
            reportLineage.parentReportId = convertReport.reportId
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportLineage(reportLineage, txn)

            reportLineage.actionId = receiverFilterActionId
            reportLineage.childReportId = receiverFilterReport.reportId
            reportLineage.parentReportId = destinationFilterReport.reportId
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportLineage(reportLineage, txn)

            reportLineage.actionId = translateActionId
            reportLineage.childReportId = translateReport.reportId
            reportLineage.parentReportId = receiverFilterReport.reportId
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportLineage(reportLineage, txn)

            reportLineage.actionId = batchActionId
            reportLineage.childReportId = batchReport.reportId
            reportLineage.parentReportId = translateReport.reportId
            ReportStreamTestDatabaseContainer.testDatabaseAccess.insertReportLineage(reportLineage, txn)
        }
        return receiveReportId
    }

    @Test
    fun `test getEtorMetadata`() {
        val goodUuid = setupEtorTestData()

        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"

        val submissionFacade = SubmissionsFacade(
            DatabaseSubmissionsAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess),
            ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, submissionFacade, false)
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        mockkConstructor(RESTTransport::class)
        mockkConstructor(HttpClient::class)

        val restCreds = mockk<RestCredential>()
        val userCreds = mockk<UserJksCredential>()

        val creds = Pair(restCreds, userCreds)

        every { anyConstructed<RESTTransport>().getCredential(any(), any()) } returns creds

        every { anyConstructed<RESTTransport>().getHeaders(any(), any()) } returns mutableMapOf("a" to "b")

        coEvery {
            anyConstructed<RESTTransport>().getOAuthToken(
                any(),
                any(),
                any(),
                any(),
            )
        } returns "TEST"

        val mock = MockEngine {
            respond(
                "{}",
                HttpStatusCode.OK,
                headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val customContext = mockk<ExecutionContext>()
        every { customContext.logger } returns mockk<Logger>()

        val response = function.retrieveETORIntermediaryMetadata(
            mockRequest, goodUuid, customContext, mock, "etor_base_url"
        )

        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test getEtorMetadata returns 404 when ID is invalid`() {
        val badUuid = "762202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, mockSubmissionFacade)
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        mockkConstructor(RESTTransport::class)
        mockkConstructor(HttpClient::class)
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.receive

        mockkConstructor(ReportGraph::class)

        every { anyConstructed<ReportGraph>().getDescendantReports(any(), any(), any()) } returns emptyList()
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns action
        every { mockSubmissionFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any(), any()) } returns null
        every { mockSubmissionFacade.checkAccessAuthorizationForAction(any(), any(), any()) } returns true

        val restCreds = mockk<RestCredential>()
        val userCreds = mockk<UserJksCredential>()

        val creds = Pair(restCreds, userCreds)

        every { anyConstructed<RESTTransport>().getCredential(any(), any()) } returns creds

        every { anyConstructed<RESTTransport>().getHeaders(any(), any()) } returns mutableMapOf("a" to "b")

        coEvery {
            anyConstructed<RESTTransport>().getOAuthToken(
                any(),
                any(),
                any(),
                any(),
            )
        } returns "TEST"

        val customContext = mockk<ExecutionContext>()
        every { customContext.logger } returns mockk<Logger>()

        val response = function.retrieveETORIntermediaryMetadata(
            mockRequest, UUID.fromString(badUuid), customContext, null, "etor_base_url"
        )

        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body.toString()).isEqualTo("{\"error\": \"lookup Id not found\"}")
    }

    @Test
    fun `test getEtorMetadata returns 404 when TI returns 404`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, mockSubmissionFacade)
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        val detailedReport = DetailedReport(
            UUID.randomUUID(),
            "flexion", "flexion", "lab", "lab", Topic.ETOR_TI, "external",
            null, null, 1, 1, true, null,
            null,
            null
        )

        // Good return
        val returnBody = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(), 201,
            mutableListOf(detailedReport), emptyList()
        )

        returnBody.destinations = listOf(
            Destination(
                "flexion", "test", mutableListOf(), mutableListOf(),
                OffsetDateTime.now(), 1, 1, mutableListOf(detailedReport), mutableListOf()
            )
        ).toMutableList()

        mockkConstructor(RESTTransport::class)
        mockkConstructor(HttpClient::class)
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.receive

        mockkConstructor(ReportGraph::class)

        val firstReport = ReportFile()
        firstReport.reportId = UUID.randomUUID()
        firstReport.receivingOrg = "not-flexion"

        val secondReport = ReportFile()
        secondReport.reportId = UUID.randomUUID()
        secondReport.receivingOrg = "flexion"

        every {
            anyConstructed<ReportGraph>().getDescendantReports(any(), any(), any())
        } returns listOf(firstReport, secondReport)
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns action
        every { mockSubmissionFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkAccessAuthorizationForAction(any(), any(), any()) } returns true

        val restCreds = mockk<RestCredential>()
        val userCreds = mockk<UserJksCredential>()

        val creds = Pair(restCreds, userCreds)

        every { anyConstructed<RESTTransport>().getCredential(any(), any()) } returns creds

        every { anyConstructed<RESTTransport>().getHeaders(any(), any()) } returns mutableMapOf("a" to "b")

        coEvery {
            anyConstructed<RESTTransport>().getOAuthToken(
                any(),
                any(),
                any(),
                any(),
            )
        } returns "TEST"

        val mock = MockEngine {
            respond(
                "{}",
                HttpStatusCode.NotFound,
                headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val customContext = mockk<ExecutionContext>()
        every { customContext.logger } returns mockk<Logger>()

        val response = function.retrieveETORIntermediaryMetadata(
            mockRequest, UUID.fromString(goodUuid), customContext, mock, "etor_base_url"
        )

        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test getEtorMetadata returns 500 for non-404 errors`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, mockSubmissionFacade)
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        val detailedReport = DetailedReport(
            UUID.randomUUID(),
            "flexion", "flexion", "lab", "lab", Topic.ETOR_TI, "external",
            null, null, 1, 1, true, null,
            null,
            null
        )

        // Good return
        val returnBody = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(), 201,
            mutableListOf(detailedReport), emptyList()
        )

        returnBody.destinations = listOf(
            Destination(
                "flexion", "test", mutableListOf(), mutableListOf(),
                OffsetDateTime.now(), 1, 1, mutableListOf(detailedReport), mutableListOf()
            )
        ).toMutableList()

        mockkConstructor(RESTTransport::class)
        mockkConstructor(HttpClient::class)
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.receive

        mockkConstructor(ReportGraph::class)

        val firstReport = ReportFile()
        firstReport.reportId = UUID.randomUUID()
        firstReport.receivingOrg = "not-flexion"

        val secondReport = ReportFile()
        secondReport.reportId = UUID.randomUUID()
        secondReport.receivingOrg = "flexion"

        every {
            anyConstructed<ReportGraph>().getDescendantReports(any(), any(), any())
        } returns listOf(firstReport, secondReport)
        every { mockSubmissionFacade.fetchActionForReportId(any()) } returns action
        every { mockSubmissionFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockSubmissionFacade.findDetailedSubmissionHistory(any(), any(), any()) } returns returnBody
        every { mockSubmissionFacade.checkAccessAuthorizationForAction(any(), any(), any()) } returns true

        val restCreds = mockk<RestCredential>()
        val userCreds = mockk<UserJksCredential>()

        val creds = Pair(restCreds, userCreds)

        every { anyConstructed<RESTTransport>().getCredential(any(), any()) } returns creds

        every { anyConstructed<RESTTransport>().getHeaders(any(), any()) } returns mutableMapOf("a" to "b")

        coEvery {
            anyConstructed<RESTTransport>().getOAuthToken(
                any(),
                any(),
                any(),
                any(),
            )
        } returns "TEST"

        val mock = MockEngine {
            respond(
                "{}",
                HttpStatusCode.Forbidden,
                headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val customContext = mockk<ExecutionContext>()
        every { customContext.logger } returns mockk<Logger>()

        val response = function.retrieveETORIntermediaryMetadata(
            mockRequest, UUID.fromString(goodUuid), customContext, mock, "etor_base_url"
        )

        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `test getEtorMetadata returns 401 when not authorized`() {
        val badUuid = "762202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, mockSubmissionFacade)
        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            null

        val customContext = mockk<ExecutionContext>()
        every { customContext.logger } returns mockk<Logger>()

        val response = function.retrieveETORIntermediaryMetadata(
            mockRequest, UUID.fromString(badUuid), customContext, null, "etor_base_url"
        )

        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body.toString()).isEqualTo("{\"error\": \"Authentication Failed\"}")
    }

    @Test
    fun `test getEtorMetadata returns 500 when the ETOR TI base URL is not set`() {
        val mockRequest = MockHttpRequestMessage()
        val mockSubmissionFacade = mockk<SubmissionsFacade>()
        val function = setupSubmissionFunctionForTesting(oktaSystemAdminGroup, mockSubmissionFacade)

        val customContext = mockk<ExecutionContext>()
        every { customContext.logger } returns mockk<Logger>()

        val response = function.retrieveETORIntermediaryMetadata(
            mockRequest, UUID.randomUUID(), customContext, null, null
        )

        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body.toString()).contains("ETOR_TI_baseurl")
    }
}