package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.MockSettings
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TestDefaultJwt
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.OffsetDateTime
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
        val sent: OffsetDateTime,
        val expires: OffsetDateTime,
        val receivingOrg: String,
        val receivingOrgSvc: String,
        val httpStatus: Int,
        val reportId: String,
        val topic: String,
        val reportItemCount: Int,
        val fileName: String,
    )

    class TestDeliveryAccess(
        private val dataset: List<DeliveryHistory>,
        val mapper: ObjectMapper
    ) : ReportFileAccess {
        override fun <T> fetchActions(
            organization: String,
            orgService: String?,
            sortDir: ReportFileAccess.SortDir,
            sortColumn: ReportFileAccess.SortColumn,
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
            organization: String,
            actionId: Long,
            klass: Class<T>
        ): T? {
            @Suppress("UNCHECKED_CAST")
            return dataset.first() as T
        }

        override fun <T> fetchRelatedActions(
            actionId: Long,
            klass: Class<T>
        ): List<T> {
            @Suppress("UNCHECKED_CAST")
            return dataset as List<T>
        }
    }

    val testData = listOf(
        DeliveryHistory(
            actionId = 922,
            createdAt = OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            receivingOrg = "ca-dph",
            receivingOrgSvc = "elr-secondary",
            httpStatus = 201,
            externalName = null,
            reportId = "b9f63105-bbed-4b41-b1ad-002a90f07e62",
            schemaTopic = "covid-19",
            itemCount = 14,
            bodyUrl = null,
            schemaName = "covid-19",
            bodyFormat = "HL7_BATCH",
        ),
        DeliveryHistory(
            actionId = 284,
            createdAt = OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
            receivingOrg = "ca-dph",
            receivingOrgSvc = "elr-secondary",
            httpStatus = 201,
            externalName = null,
            reportId = "c3c8e304-8eff-4882-9000-3645054a30b7",
            schemaTopic = "covid-19",
            itemCount = 1,
            bodyUrl = null,
            schemaName = "primedatainput/pdi-covid-19",
            bodyFormat = "CSV"
        )
    )

    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))

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
                mapOf("authorization" to "Bearer 111.222.333", "authentication-type" to "okta"),
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.UNAUTHORIZED
                ),
                "unauthorized"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"), // no 'okta' auth-type, so this uses server2server auth
                emptyMap(),
                ExpectedAPIResponse(
                    HttpStatus.OK,
                    listOf(
                        ExpectedDelivery(
                            deliveryId = 922,
                            sent = OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
                            expires = OffsetDateTime.parse("2022-05-19T18:04:26.534Z"),
                            receivingOrg = "ca-dph",
                            receivingOrgSvc = "elr-secondary",
                            httpStatus = 201,
                            reportId = "b9f63105-bbed-4b41-b1ad-002a90f07e62",
                            topic = "covid-19",
                            reportItemCount = 14,
                            fileName = "covid-19-b9f63105-bbed-4b41-b1ad-002a90f07e62-20220419180426.hl7",
                        ),
                        ExpectedDelivery(
                            deliveryId = 284,
                            sent = OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
                            expires = OffsetDateTime.parse("2022-05-12T17:06:10.534Z"),
                            receivingOrg = "ca-dph",
                            receivingOrgSvc = "elr-secondary",
                            httpStatus = 201,
                            reportId = "c3c8e304-8eff-4882-9000-3645054a30b7",
                            topic = "covid-19",
                            reportItemCount = 1,
                            fileName = "pdi-covid-19-c3c8e304-8eff-4882-9000-3645054a30b7-20220412170610.csv",
                        ),
                    )
                ),
                "simple success"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fdafads", "authentication-type" to "okta"),
                mapOf("cursor" to "nonsense"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad date"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf("pagesize" to "-1"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pagesize"
            ),
            DeliveryUnitTestCase(
                mapOf("authorization" to "Bearer fdafads"),
                mapOf("pagesize" to "fdas"),
                ExpectedAPIResponse(
                    HttpStatus.BAD_REQUEST
                ),
                "bad pagesize, garbage"
            ),
            DeliveryUnitTestCase(
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
            DeliveryUnitTestCase(
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
        val receiver = Receiver(
            "elr-secondary",
            organizationName,
            "elr",
            CustomerStatus.TESTING,
            "schema1",
        )
        settings.receiverStore[receiver.fullName] = receiver
        val engine = makeEngine(metadata, settings)

        testCases.forEach {
            logger.info("Executing list deliveries unit test ${it.name}")
            val httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.httpHeaders += it.headers
            httpRequestMessage.parameters += it.parameters
            // Invoke
            val response = DeliveryFunction(
                DeliveryFacade(TestDeliveryAccess(testData, mapper)),
                workflowEngine = engine,
            ).getDeliveries(
                httpRequestMessage,
                "$organizationName.elr-secondary",
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

    private fun setupDeliveryFunctionForTesting(
        oktaClaimsOrganizationName: String,
        facade: DeliveryFacade,
    ): DeliveryFunction {
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

        val receiver = Receiver(
            "elr-secondary",
            organizationName,
            "elr",
            CustomerStatus.TESTING,
            "schema1",
        )
        settings.receiverStore[receiver.fullName] = receiver

        val receiver2 = Receiver(
            "test-lab-2",
            otherOrganizationName,
            "elr",
            CustomerStatus.TESTING,
            "schema1",
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
            "authorization" to "Bearer 111.222.333",
            "authentication-type" to "okta"
        )
        return httpRequestMessage
    }

    @Test
    fun `test access user can view their org's delivery history`() {
        val facade = DeliveryFacade(TestDeliveryAccess(testData, mapper))
        val deliveryFunction = setupDeliveryFunctionForTesting(oktaClaimsOrganizationName, facade)
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = deliveryFunction.getDeliveries(httpRequestMessage, "$organizationName.elr-secondary")
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot view another org's delivery history`() {
        val facade = DeliveryFacade(TestDeliveryAccess(testData, mapper))
        val deliveryFunction = setupDeliveryFunctionForTesting(oktaClaimsOrganizationName, facade)
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = deliveryFunction.getDeliveries(httpRequestMessage, "test-lab-2.test-lab-2")
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can view all org's delivery history`() {
        val facade = DeliveryFacade(TestDeliveryAccess(testData, mapper))
        val deliveryFunction = setupDeliveryFunctionForTesting(oktaSystemAdminGroup, facade)
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = deliveryFunction.getDeliveries(httpRequestMessage, "$organizationName.elr-secondary")
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }
}