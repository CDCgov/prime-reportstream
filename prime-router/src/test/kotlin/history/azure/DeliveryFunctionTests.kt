package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.MockSettings
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryFacility
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TestDefaultJwt
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
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
import java.util.UUID
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeliveryFunctionTests : Logging {
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

    data class ExpectedAPIResponse(
        val status: HttpStatus,
        val body: List<ExpectedDelivery>? = null
    )

    data class DeliveryUnitTestCase(
        val headers: Map<String, String>,
        val parameters: Map<String, String>,
        val expectedResponse: ExpectedAPIResponse,
        val name: String?
    )

    /**
     * Delivery list response from the API
     */
    data class ExpectedDelivery(
        val deliveryId: Int,
        val batchReadyAt: OffsetDateTime,
        val expires: OffsetDateTime,
        val receiver: String, // fullname, eg, md-phd.elr
        val reportId: String,
        val topic: String,
        val reportItemCount: Int,
        val fileName: String
    )

    private val testData = listOf(
        DeliveryHistory(
            actionId = 922,
            createdAt = OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            externalName = null,
            reportId = "b9f63105-bbed-4b41-b1ad-002a90f07e62",
            schema_topic = "covid-19",
            itemCount = 14,
            receivingOrg = "ca-dph",
            receivingOrgSvc = "elr-secondary",
            bodyUrl = null,
            schemaName = "covid-19",
            bodyFormat = "HL7_BATCH"
        ),
        DeliveryHistory(
            actionId = 284,
            createdAt = OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
            externalName = null,
            reportId = "c3c8e304-8eff-4882-9000-3645054a30b7",
            schema_topic = "covid-19",
            itemCount = 1,
            receivingOrg = "ca-dph",
            receivingOrgSvc = null,
            bodyUrl = null,
            schemaName = "primedatainput/pdi-covid-19",
            bodyFormat = "CSV"
        )
    )

    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    private val accessSpy = spyk(DatabaseAccess(connection))

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine {
        // Report.formExternalFilename has some metadata shenanigans that need to be tested in a different place
        mockkObject(Metadata.Companion)
        every { Metadata.getInstance() } returns metadata

        return spyk(
            WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy).build()
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test list deliveries`() {
        val testCases = listOf(
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fads"), // no 'okta' auth-type, so this uses server2server auth
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.OK,
                    listOf(
                        ExpectedDelivery(
                            deliveryId = 922,
                            batchReadyAt = OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
                            expires = OffsetDateTime.parse("2022-05-19T18:04:26.534Z"),
                            receiver = "ca-dph.elr-secondary",
                            reportId = "b9f63105-bbed-4b41-b1ad-002a90f07e62",
                            topic = "covid-19",
                            reportItemCount = 14,
                            fileName = "covid-19-b9f63105-bbed-4b41-b1ad-002a90f07e62-20220419180426.hl7"
                        ),
                        ExpectedDelivery(
                            deliveryId = 284,
                            batchReadyAt = OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
                            expires = OffsetDateTime.parse("2022-05-12T17:06:10.534Z"),
                            receiver = "ca-dph",
                            reportId = "c3c8e304-8eff-4882-9000-3645054a30b7",
                            topic = "covid-19",
                            reportItemCount = 1,
                            fileName = "pdi-covid-19-c3c8e304-8eff-4882-9000-3645054a30b7-20220412170610.csv"
                        )
                    )
                ),
                "simple success"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf("cursor" to "nonsense"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad date"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf("pageSize" to "-1"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pageSize"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf("pageSize" to "not a num"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pageSize, garbage"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf(
                    "pageSize" to "10",
                    "cursor" to "2021-11-30T16:36:48.307Z",
                    "sort" to "ASC"
                ),
                ExpectedAPIResponse(
                    HttpStatus.OK
                ),
                "good minimum params"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fads"),
                mapOf(
                    "pageSize" to "10",
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

        val settings = MockSettings()
        val sender = CovidSender(
            name = "default",
            organizationName = "simple_report",
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender.fullName] = sender
        val receiver = Receiver(
            "elr-secondary",
            organizationName,
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            "schema1"
        )
        settings.receiverStore[receiver.fullName] = receiver

        testCases.forEach {
            logger.info("Executing list deliveries unit test ${it.name}")
            val httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.httpHeaders += it.headers
            httpRequestMessage.parameters += it.parameters
            // Invoke
            val deliveryFunction = setupDeliveryFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
            val response = deliveryFunction.getDeliveries(
                httpRequestMessage,
                "$organizationName.elr-secondary"
            )
            // Verify
            assertThat(response.status).isEqualTo(it.expectedResponse.status)
            if (response.status == HttpStatus.OK) {
                val deliveries: List<ExpectedDelivery> = mapper.readValue(response.body.toString())
                if (it.expectedResponse.body != null) {
                    assertThat(deliveries.size).isEqualTo(it.expectedResponse.body.size)
                    assertThat(deliveries).isEqualTo(it.expectedResponse.body)
                }
            }
        }
    }

    private fun mockFacade(): DeliveryFacade {
        val mockDatabaseAccess = mockkClass(DatabaseDeliveryAccess::class)

        every {
            mockDatabaseAccess.fetchActions<DeliveryHistory>(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns testData
        every {
            mockDatabaseAccess.fetchAction<DeliveryHistory>(
                any(),
                any(),
                any()
            )
        } returns testData.first()

        every {
            mockDatabaseAccess.fetchRelatedActions<DeliveryHistory>(
                any(),
                any()
            )
        } returns testData

        return DeliveryFacade(mockDatabaseAccess)
    }

    private fun setupDeliveryFunctionForTesting(
        oktaClaimsOrganizationName: String,
        facade: DeliveryFacade
    ): DeliveryFunction {
        val claimsMap = buildClaimsMap(oktaClaimsOrganizationName)
        val metadata = Metadata(schema = Schema(name = "one", topic = Topic.TEST))
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

        val receiver = Receiver(
            "elr-secondary",
            organizationName,
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            "schema1"
        )
        settings.receiverStore[receiver.fullName] = receiver

        val receiver2 = Receiver(
            "test-lab-2",
            otherOrganizationName,
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            "schema1"
        )
        settings.receiverStore[receiver2.fullName] = receiver2

        val engine = makeEngine(metadata, settings)
        mockkObject(OktaAuthentication.Companion)
        every { OktaAuthentication.Companion.decodeJwt(any()) } returns
            TestDefaultJwt(
                "a.b.c",
                Instant.now(),
                Instant.now().plusSeconds(60),
                claimsMap
            )

        return DeliveryFunction(
            deliveryFacade = facade,
            workflowEngine = engine
        )
    }

    private fun setupHttpRequestMessageForTesting(): MockHttpRequestMessage {
        val httpRequestMessage = MockHttpRequestMessage()
        httpRequestMessage.httpHeaders += mapOf(
            "authorization" to "Bearer 111.222.333"
        )
        return httpRequestMessage
    }

    @Test
    fun `test access user can view their organization's delivery history`() {
        val deliveryFunction = setupDeliveryFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = deliveryFunction.getDeliveries(httpRequestMessage, "$organizationName.elr-secondary")
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot view another organization's delivery history`() {
        val deliveryFunction = setupDeliveryFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = deliveryFunction.getDeliveries(httpRequestMessage, "$otherOrganizationName.test-lab-2")
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can view all organization's delivery history`() {
        val deliveryFunction = setupDeliveryFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = deliveryFunction.getDeliveries(httpRequestMessage, "$organizationName.elr-secondary")
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = deliveryFunction.getDeliveries(httpRequestMessage, "$otherOrganizationName.test-lab-2")
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test delivery details`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"

        val mockDeliveryFacade = mockk<DeliveryFacade>()
        val function = setupDeliveryFunctionForTesting(oktaSystemAdminGroup, mockDeliveryFacade)

        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        // Invalid id:  not a UUID nor a Long
        var response = function.getDeliveryDetails(mockRequest, "bad")
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Database error
        every { mockDeliveryFacade.fetchActionForReportId(any()) }.throws(DataAccessException("dummy"))
        response = function.getDeliveryDetails(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)

        // Good UUID, but Not found
        every { mockDeliveryFacade.fetchActionForReportId(any()) } returns null
        response = function.getDeliveryDetails(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good return
        val returnBody = DeliveryHistory(
            550,
            OffsetDateTime.now(),
            null,
            "c3c8e304-8eff-4882-9000-3645054a30b7",
            "covid-19",
            1,
            "ca-dph",
            "elr-secondary",
            null,
            "primedatainput/pdi-covid-19",
            "CSV"
        )
        // Happy path with a good UUID
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.batch
        every { mockDeliveryFacade.fetchActionForReportId(any()) } returns action
        every { mockDeliveryFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockDeliveryFacade.findDetailedDeliveryHistory(any()) } returns returnBody
        every { mockDeliveryFacade.checkAccessAuthorization(any(), any(), null, any()) } returns true
        response = function.getDeliveryDetails(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        var responseBody: ExpectedDelivery = mapper.readValue(response.body.toString())
        assertThat(responseBody.deliveryId.toLong()).isEqualTo(returnBody.actionId)
        assertThat(responseBody.receiver).isEqualTo("${returnBody.receivingOrg}.${returnBody.receivingOrgSvc}")

        // Good uuid, but with `process` action step report.
        action.actionName = TaskAction.process
        response = function.getDeliveryDetails(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but with `receive` action name
        val goodActionId = "550"
        action.actionName = TaskAction.receive
        every { mockDeliveryFacade.fetchAction(any()) } returns null
        response = function.getDeliveryDetails(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not authorized
        action.actionName = TaskAction.batch
        every { mockDeliveryFacade.fetchAction(any()) } returns action
        every { mockDeliveryFacade.checkAccessAuthorization(any(), any(), null, any()) } returns false // not authorized
        response = function.getDeliveryDetails(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)

        // Happy path with a good actionId
        every { mockDeliveryFacade.fetchActionForReportId(any()) } returns null // not used for an actionId
        every { mockDeliveryFacade.fetchAction(any()) } returns action
        every { mockDeliveryFacade.findDetailedDeliveryHistory(any()) } returns returnBody
        every { mockDeliveryFacade.checkAccessAuthorization(any(), any(), null, any()) } returns true
        response = function.getDeliveryDetails(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        responseBody = mapper.readValue(response.body.toString())
        assertThat(responseBody.deliveryId.toLong()).isEqualTo(returnBody.actionId)

        // bad actionId, Not found
        val badActionId = "24601"
        action.actionName = TaskAction.receive
        every { mockDeliveryFacade.fetchAction(any()) } returns null
        response = function.getDeliveryDetails(mockRequest, badActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // empty actionId, Not found
        val emptyActionId = ""
        action.actionName = TaskAction.receive
        every { mockDeliveryFacade.fetchAction(any()) } returns null
        response = function.getDeliveryDetails(mockRequest, emptyActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test list facilities`() {
        val goodUuid = "662202ba-e3e5-4810-8cb8-161b75c63bc1"
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"

        val mockDeliveryFacade = mockk<DeliveryFacade>()
        val function = setupDeliveryFunctionForTesting(oktaSystemAdminGroup, mockDeliveryFacade)

        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.authenticate(any()) } returns
            AuthenticatedClaims.generateTestClaims()

        // Good return

        val returnBody = listOf(
            DeliveryFacility(
                "Any lab USA",
                "Kurtistown",
                "HI",
                "43D1961163",
                0,
                1
            )
        )

        every {
            mockDeliveryFacade.findDeliveryFacilities(
                any(),
                any(),
                any()
            )
        } returns returnBody

        // Happy path with a good UUID
        val action = Action()
        action.actionId = 550
        action.sendingOrg = organizationName
        action.actionName = TaskAction.batch
        every { mockDeliveryFacade.fetchActionForReportId(any()) } returns action
        every { mockDeliveryFacade.fetchAction(any()) } returns null // not used for a UUID
        every { mockDeliveryFacade.checkAccessAuthorization(any(), any(), null, any()) } returns true

        mockRequest.parameters["sortCol"] = "facility"
        mockRequest.parameters["sortDir"] = "DESC"
        var response = function.getDeliveryFacilities(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        var responseBody: List<DeliveryFunction.Facility> = mapper.readValue(response.body.toString())
        assertThat(responseBody.first().facility).isEqualTo(returnBody.last().testingLabName)
        assertThat(responseBody.first().location).isEqualTo(returnBody.last().location)
        assertThat(responseBody.first().clia).isEqualTo(returnBody.last().testingLabClia)
        assertThat(responseBody.first().positive).isEqualTo(returnBody.last().positive)
        assertThat(responseBody.first().total).isEqualTo(returnBody.last().countRecords)

        // Happy path with a good actionId
        val reportFile = ReportFile()
        reportFile.actionId = action.actionId
        reportFile.reportId = UUID.fromString(goodUuid)

        every { mockDeliveryFacade.fetchReportForActionId(any()) } returns reportFile
        response = function.getDeliveryFacilities(mockRequest, "550")
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        responseBody = mapper.readValue(response.body.toString())
        assertThat(responseBody.first().facility).isEqualTo(returnBody.last().testingLabName)
        assertThat(responseBody.first().location).isEqualTo(returnBody.last().location)
        assertThat(responseBody.first().clia).isEqualTo(returnBody.last().testingLabClia)
        assertThat(responseBody.first().positive).isEqualTo(returnBody.last().positive)
        assertThat(responseBody.first().total).isEqualTo(returnBody.last().countRecords)

        mockRequest.parameters["sortDir"] = "ASC"
        response = function.getDeliveryFacilities(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        responseBody = mapper.readValue(response.body.toString())
        assertThat(responseBody.first().facility).isEqualTo(returnBody.first().testingLabName)
        assertThat(responseBody.first().location).isEqualTo(returnBody.first().location)
        assertThat(responseBody.first().clia).isEqualTo(returnBody.first().testingLabClia)
        assertThat(responseBody.first().positive).isEqualTo(returnBody.first().positive)
        assertThat(responseBody.first().total).isEqualTo(returnBody.first().countRecords)

        // Good uuid, but not a with `process` action step report.
        action.actionName = TaskAction.process
        response = function.getDeliveryFacilities(mockRequest, goodUuid)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but with `receive` action name
        val goodActionId = "550"
        action.actionName = TaskAction.receive
        every { mockDeliveryFacade.fetchAction(any()) } returns null
        response = function.getDeliveryFacilities(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // Good actionId, but Not authorized
        action.actionName = TaskAction.batch
        every { mockDeliveryFacade.fetchAction(any()) } returns action
        every { mockDeliveryFacade.checkAccessAuthorization(any(), any(), null, any()) } returns false // not authorized
        response = function.getDeliveryFacilities(mockRequest, goodActionId)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)

        // bad actionId, Not found
        val badActionId = "24601"
        action.actionName = TaskAction.receive
        every { mockDeliveryFacade.fetchAction(any()) } returns null
        response = function.getDeliveryFacilities(mockRequest, badActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)

        // empty actionId, Not found
        val emptyActionId = ""
        action.actionName = TaskAction.receive
        every { mockDeliveryFacade.fetchAction(any()) } returns null
        response = function.getDeliveryFacilities(mockRequest, emptyActionId)
        assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
    }
}