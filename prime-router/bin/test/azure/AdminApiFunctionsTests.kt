package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.tables.pojos.ListSendFailures
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.OktaAuthentication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminApiFunctionsTests {
    /**
     * The mock request.
     */
    private val mockRequest = mockk<HttpRequestMessage<String?>>()

    /**
     * Mapper to convert objects to JSON.
     */
    private val mapper = JacksonMapperUtilities.defaultMapper

    /**
     * Okta authenticator
     */
    private val mockOktaAuthenticator = mockk<OktaAuthentication>()

    @BeforeAll
    fun initDependencies() {
        every { mockRequest.headers } returns mapOf(HttpHeaders.AUTHORIZATION.lowercase() to "Bearer dummy")
        every { mockRequest.uri } returns URI.create("http://localhost:7071/api/adm/getsendfailures?days_to_show=13")
        val mockAuthenticatedClaims = mockk<AuthenticatedClaims>()
        every { mockAuthenticatedClaims.userName } returns "dummy"
        every { mockOktaAuthenticator.checkAccess(any(), any(), any(), any(), captureLambda()) } answers {
            lambda<(AuthenticatedClaims) -> HttpResponseMessage>().captured.invoke(mockAuthenticatedClaims)
        }
    }

    /**
     * Create a new response builder.  Useful to reset the verification count.
     */
    private fun createResponseBuilder(): HttpResponseMessage.Builder {
        val mockResponseBuilder = mockk<HttpResponseMessage.Builder>()
        every { mockResponseBuilder.body(any()) } returns mockResponseBuilder
        every { mockResponseBuilder.header(any(), any()) } returns mockResponseBuilder
        every { mockResponseBuilder.build() } returns mockk()
        return mockResponseBuilder
    }

    @Test
    fun `getsendfailures test`() {
        val mockdata1 = ListSendFailures()
        mockdata1.actionId = 1
        mockdata1.reportId = UUID.randomUUID()
        mockdata1.receiver = "ignore.ignore"
        mockdata1.fileName = "filename.txt"
        mockdata1.failedAt = OffsetDateTime.now()
        mockdata1.actionParams = "actionParams=1"
        mockdata1.actionResult = "actionResult failure message"
        mockdata1.bodyUrl = "http://azurite:10000/devstoreaccount1/reports/ready%foo.hl7"
        mockdata1.reportFileReceiver = "2Fignore.HL7_BATCH_OFFSET_TIME_UTC"
        mockdata1.fileName = "foo.hl7"

        // Only shows active
        val db = mockk<DatabaseAccess>()
        every { db.fetchSendFailures() } returns listOf(mockdata1)
        every { mockRequest.httpMethod } returns HttpMethod.GET
        every { mockRequest.queryParameters } returns emptyMap()
        val mockResponseBuilder = createResponseBuilder()
        every { mockRequest.createResponseBuilder(HttpStatus.OK) } returns mockResponseBuilder
        val function = AdminApiFunctions(db, mockOktaAuthenticator)
        function.getSendFailures(mockRequest)
        verify(exactly = 1) {
            mockResponseBuilder.body(
                withArg {
                    // Check that we have JSON data in the response body
                    assertTrue(it is String)
                    val result = mapper.readValue<List<ListSendFailures>>(it)
                    assertTrue(result.size == 1)
                    assertEquals(result[0].actionId, mockdata1.actionId)
                    assertEquals(result[0].reportId, mockdata1.reportId)
                    assertEquals(result[0].fileName, mockdata1.fileName)
                    assertEquals(result[0].bodyUrl, mockdata1.bodyUrl)
                }
            )
        }
    }

    @Test
    fun `Verify ReceiverConnectionCheckResultJoined synced with ReceiverConnectionCheckResult`() {
        val fieldsDB =
            Tables.RECEIVER_CONNECTION_CHECK_RESULTS::class.declaredMemberProperties.map { it.name }
        val fieldsDbJsonb = JacksonMapperUtilities.defaultMapper.writeValueAsString(fieldsDB)
        // if this test fails then RECEIVER_CONNECTION_CHECK_RESULTS may have changed and
        // ReceiverConnectionCheckResultJoined could need to be updated to stay in sync.
        // To fix this test:
        //   1) update ReceiverConnectionCheckResultJoined
        //   2) update compare string here with the latest (set breakpoint in this test)
        val verifyData = "[ \"RECEIVER_CONNECTION_CHECK_RESULT_ID\", \"ORGANIZATION_ID\", " +
            "\"RECEIVER_ID\", \"CONNECTION_CHECK_RESULT\", \"CONNECTION_CHECK_SUCCESSFUL\", " +
            "\"CONNECTION_CHECK_STARTED_AT\", \"CONNECTION_CHECK_COMPLETED_AT\", " +
            "\"_receiverConnectionCheckResultsOrganizationIdFkey\", " +
            "\"_receiverConnectionCheckResultsReceiverIdFkey\" ]"
        assertEquals(fieldsDbJsonb, verifyData)
    }
}