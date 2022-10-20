package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class MessagesFunctionTests {

    private fun buildMessagesFunction(
        mockDbAccess: DatabaseAccess? = null
    ): MessagesFunction {
        val dbAccess = mockDbAccess ?: mockk()
        return MessagesFunction(dbAccess)
    }

    private fun buildCovidResultMetadata(reportId: UUID? = null, messageId: String? = null): CovidResultMetadata {
        return CovidResultMetadata(
            0,
            reportId,
            1,
            messageId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now(),
            null,
            "simple_report.default",
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    @Test
    fun `test processSearchRequest function`() {
        val reportId: UUID = UUID.randomUUID()
        val messageId: String = UUID.randomUUID().toString()
        val mockDbAccess = mockk<DatabaseAccess>()
        val messagesFunction = spyk(buildMessagesFunction(mockDbAccess))

        // Happy path
        val mockRequestWithMessageId = MockHttpRequestMessage()
        mockRequestWithMessageId.parameters["messageId"] = messageId
        every { mockDbAccess.fetchCovidResultMetadatasByMessageId(any(), any()) } returns listOf(
            buildCovidResultMetadata(reportId, messageId)
        )
        val response = messagesFunction.processSearchRequest(mockRequestWithMessageId)

        val jsonResponse = JSONArray(response.body.toString())
        val messageObject = jsonResponse.first() as JSONObject
        val messageObjectId = messageObject.get("messageId")
        assert(messageObjectId.equals(messageId))
        assert(response.status.equals(HttpStatus.OK))

        // Missing message id param
        val mockRequestMissingMessageIdParam = MockHttpRequestMessage()
        val missingMessageIdParamsResponse = messagesFunction.processSearchRequest(mockRequestMissingMessageIdParam)
        assert(missingMessageIdParamsResponse.status.equals(HttpStatus.BAD_REQUEST))

        // empty message id
        val mockRequestEmptyMessageId = MockHttpRequestMessage()
        mockRequestEmptyMessageId.parameters += mapOf(
            "messageId" to ""
        )
        val emptyMessageIdResponse = messagesFunction.processSearchRequest(mockRequestEmptyMessageId)
        assert(emptyMessageIdResponse.status.equals(HttpStatus.BAD_REQUEST))

        // whitespace message id
        val mockRequestWhitespaceMessageId = MockHttpRequestMessage()
        mockRequestWhitespaceMessageId.parameters += mapOf(
            "messageId" to " "
        )
        val whitespaceMessageIdResponse = messagesFunction.processSearchRequest(mockRequestWhitespaceMessageId)
        assert(whitespaceMessageIdResponse.status.equals(HttpStatus.BAD_REQUEST))

        // Exception in the database
        val mockRequestMissingMessageIdValue = MockHttpRequestMessage()
        mockRequestMissingMessageIdValue.parameters["messageId"] = "message-id"
        every {
            mockDbAccess.fetchCovidResultMetadatasByMessageId(
                any(),
                any()
            )
        }.throws(Exception("missing a message id"))
        val missingMessageIdValueResponse = messagesFunction.processSearchRequest(mockRequestMissingMessageIdValue)
        assert(missingMessageIdValueResponse.status.equals(HttpStatus.BAD_REQUEST))
    }

    @Test
    fun `test run function`() {
        val messagesFunction = spyk(MessagesFunction())

        // Happy path
        val req = MockHttpRequestMessage()
        val resp = HttpUtilities.okResponse(req, "fakeOkay")

        every { messagesFunction.processSearchRequest(any()) } returns resp

        val res = messagesFunction.run(req)
        assert(res.status.equals(HttpStatus.OK))

        // unauthorized - no claims
        val unAuthReq = MockHttpRequestMessage()
        unAuthReq.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes = messagesFunction.run(unAuthReq)
        assert(unAuthRes.status.equals(HttpStatus.UNAUTHORIZED))

        // unauthorized - not an admin
        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val unAuthReq2 = MockHttpRequestMessage()
        unAuthReq2.httpHeaders += mapOf(
            "authorization" to "x.y.z"
        )

        val unAuthRes2 = messagesFunction.run(unAuthReq)
        assert(unAuthRes2.status.equals(HttpStatus.UNAUTHORIZED))
    }

    @Test
    fun `test run function internal server error`() {
        // throw exception
        val internalServerErrorReq = MockHttpRequestMessage()
        val messagesFunction = spyk(buildMessagesFunction())

        every { messagesFunction.processSearchRequest(any()) }.throws(Exception("something went wrong somewhere"))

        var internalErrorRes = messagesFunction.run(internalServerErrorReq)

        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))

        every { messagesFunction.processSearchRequest(any()) }.throws(Exception())

        internalErrorRes = messagesFunction.run(internalServerErrorReq)
        assert(internalErrorRes.status.equals(HttpStatus.INTERNAL_SERVER_ERROR))
    }
}