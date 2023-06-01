package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isNullOrEmpty
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.SenderItems
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.messages.ReportFileMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class SenderFilesFunctionTests {
    private val mapper = JacksonMapperUtilities.defaultMapper

    private fun buildSenderFilesFunction(
        mockDbAccess: DatabaseAccess? = null,
        mockBlobAccess: BlobAccess? = null
    ): SenderFilesFunction {
        val dbAccess = mockDbAccess ?: mockk()
        val blobAccess = mockBlobAccess ?: mockk()
        return SenderFilesFunction(dbAccess = dbAccess, blobAccess = blobAccess)
    }

    private fun buildRequest(params: Map<String, String>): HttpRequestMessage<String?> {
        val mockRequest = mockk<HttpRequestMessage<String?>>()
        every { mockRequest.headers } returns mapOf(HttpHeaders.AUTHORIZATION.lowercase() to "Bearer dummy")
        every { mockRequest.uri } returns URI.create("http://localhost:7071/api/sender-files")
        every { mockRequest.queryParameters } returns params
        return mockRequest
    }

    private fun buildReportFile(reportId: UUID): ReportFile {
        return ReportFile(
            reportId,
            1,
            TaskAction.send,
            OffsetDateTime.now().minusWeeks(1),
            "test",
            "test-sender",
            "test",
            "test-receiver",
            "",
            "",
            "covid-19",
            Topic.COVID_19,
            "https://localhost/blob",
            "",
            "CSV",
            byteArrayOf(),
            1,
            null,
            OffsetDateTime.now().minusWeeks(1),
            "",
            null,
        )
    }

    private fun buildCovidResultMetadata(reportId: UUID? = null, messageID: String? = null): CovidResultMetadata {
        return CovidResultMetadata(
            0,
            reportId,
            1,
            messageID,
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
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    @Test
    fun `test checkParameters`() {
        // Happy path
        val reportId: ReportId = UUID.randomUUID()
        val mockRequestWithReportId = buildRequest(mapOf("report-id" to reportId.toString()))
        val senderFileFunctions = buildSenderFilesFunction()
        val functionParams = senderFileFunctions.checkParameters(mockRequestWithReportId)
        assertThat(functionParams.reportFileName).isNull()
        assertThat(functionParams.reportId).isEqualTo(reportId)
        assertThat(functionParams.onlyDestinationReportItems).isEqualTo(false)
        assertThat(functionParams.limit).isGreaterThan(0)
        assertThat(functionParams.offset).isEqualTo(0)
        verify(atLeast = 1) { mockRequestWithReportId.queryParameters }

        // Bad value
        val mockRequestWithBadReportId = buildRequest(mapOf("report-id" to "1234"))
        assertThat {
            senderFileFunctions.checkParameters(mockRequestWithBadReportId)
        }.isFailure()
        verify(atLeast = 1) { mockRequestWithBadReportId.queryParameters }

        // Missing value
        val mockRequestMissing = buildRequest(emptyMap())
        assertThat {
            senderFileFunctions.checkParameters(mockRequestMissing)
        }.isFailure()
        verify(atLeast = 1) { mockRequestMissing.queryParameters }

        // MessageID
        val mockRequestWithMessageId = buildRequest(mapOf("message-id" to "1234"))

        val mockDbAccess = mockk<DatabaseAccess>()
        val mockBlobAccess = mockk<BlobAccess>()
        val senderFileFunction = buildSenderFilesFunction(mockDbAccess, mockBlobAccess)
        every { mockDbAccess.fetchSingleMetadata(any(), any()) } returns buildCovidResultMetadata(reportId)
        val functionParamsWithMessageID = senderFileFunction.checkParameters(mockRequestWithMessageId)
        assertThat(functionParamsWithMessageID.reportFileName).isNull()
        assertThat(functionParamsWithMessageID.reportId).isEqualTo(reportId)
        assertThat(functionParamsWithMessageID.messageId).isEqualTo("1234")
        assertThat(functionParamsWithMessageID.onlyDestinationReportItems).isEqualTo(true)
        assertThat(functionParamsWithMessageID.limit).isGreaterThan(0)
        assertThat(functionParamsWithMessageID.offset).isEqualTo(0)
        verify(atLeast = 1) { mockRequestWithReportId.queryParameters }

        // MessageID with no report id
        val mockRequestWithMessageIdNoReportID = buildRequest(mapOf("message-id" to "1234"))
        val senderFileFunctionNoReport = buildSenderFilesFunction(mockDbAccess, mockBlobAccess)
        every { mockDbAccess.fetchSingleMetadata(any(), any()) } returns buildCovidResultMetadata(null)
        assertThat {
            senderFileFunctionNoReport.checkParameters(mockRequestWithMessageIdNoReportID)
        }.isFailure()
        verify(atLeast = 1) { mockRequestWithBadReportId.queryParameters }
    }

    @Test
    fun `test processRequest`() {
        mockkObject(BlobAccess.Companion)
        // Happy path
        val receiverReportId: ReportId = UUID.randomUUID()
        val senderReportId: ReportId = UUID.randomUUID()
        val body = """
            A,B,C
            0,1,2
        """.trimIndent()
        val functionParams = SenderFilesFunction.FunctionParameters(receiverReportId, null, null, false, 0, 1)
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockBlobAccess = mockk<BlobAccess>()
        every { mockDbAccess.fetchSenderItems(any(), any(), any()) } returns listOf(
            SenderItems(senderReportId, 0, receiverReportId, 0)
        )
        every { BlobAccess.Companion.downloadBlob(any()) } returns body.toByteArray()
        every { mockDbAccess.fetchReportFile(any(), any(), any()) } returns buildReportFile(senderReportId)
        val senderFileFunctions = buildSenderFilesFunction(mockDbAccess, mockBlobAccess)
        val result = senderFileFunctions.processRequest(functionParams)
        val reportFileMessages = mapper.readValue(result.payload, Array<ReportFileMessage>::class.java)
        assertThat(reportFileMessages[0].reportId).isEqualTo(senderReportId.toString())
        assertThat(reportFileMessages[0].contentType).isEqualTo("text/csv")
        assertThat(reportFileMessages[0].content.trim()).isEqualTo(body)
        assertThat(reportFileMessages[0].request?.reportId).isEqualTo(receiverReportId.toString())
    }

    @Test
    fun `test processRequest with No Blob`() {
        mockkObject(BlobAccess.Companion)
        // Test that the case where the blob is deleted is handled as expected
        val receiverReportId: ReportId = UUID.randomUUID()
        val senderReportId: ReportId = UUID.randomUUID()
        val functionParams = SenderFilesFunction.FunctionParameters(receiverReportId, null, null, false, 0, 1)
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockBlobAccess = mockk<BlobAccess>()
        every { mockDbAccess.fetchSenderItems(any(), any(), any()) } returns listOf(
            SenderItems(senderReportId, 0, receiverReportId, 0)
        )
        every { mockDbAccess.fetchReportFile(any(), any(), any()) } returns buildReportFile(senderReportId)
        every { BlobAccess.Companion.downloadBlob(any()) } throws IOException("File not found")
        val senderFileFunctions = buildSenderFilesFunction(mockDbAccess, mockBlobAccess)
        assertThat { senderFileFunctions.processRequest(functionParams) }
            .isFailure()
            .isInstanceOf(FileNotFoundException::class.java)
    }

    @Test
    fun `test processRequestWithMessageID`() {
        mockkObject(BlobAccess.Companion)
        // Happy path
        val senderReportId: ReportId = UUID.randomUUID()
        val messageId: String = UUID.randomUUID().toString()
        val body = """
            A,B,C
            0,1,2
        """.trimIndent()
        val functionParams = SenderFilesFunction.FunctionParameters(senderReportId, null, messageId, false, 0, 1)
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockBlobAccess = mockk<BlobAccess>()
        every { mockDbAccess.fetchSenderItems(any(), any(), any()) } returns listOf(
            SenderItems(senderReportId, 0, null, 0)
        )
        every { BlobAccess.Companion.downloadBlob(any()) } returns body.toByteArray()
        every { mockDbAccess.fetchReportFile(any(), any(), any()) } returns buildReportFile(senderReportId)
        val senderFileFunctions = buildSenderFilesFunction(mockDbAccess, mockBlobAccess)
        val result = senderFileFunctions.processRequest(functionParams)
        val reportFileMessages = mapper.readValue(result.payload, Array<ReportFileMessage>::class.java)
        assertThat(reportFileMessages[0].reportId).isEqualTo(senderReportId.toString())
        assertThat(reportFileMessages[0].contentType).isEqualTo("text/csv")
        assertThat(reportFileMessages[0].content.trim()).isEqualTo(body)
        assertThat(reportFileMessages[0].request?.reportId).isNullOrEmpty()
    }

    @Test
    fun `test the case with no ancestors`() {
        mockkObject(BlobAccess.Companion)
        val receiverReportId: ReportId = UUID.randomUUID()
        val senderReportId: ReportId = UUID.randomUUID()
        val functionParams = SenderFilesFunction.FunctionParameters(receiverReportId, null, null, false, 0, 1)
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockBlobAccess = mockk<BlobAccess>()
        every { mockDbAccess.fetchSenderItems(any(), any(), any()) } returns emptyList()
        every { mockDbAccess.fetchReportFile(any(), any(), any()) } returns buildReportFile(senderReportId)
        every { BlobAccess.Companion.downloadBlob(any()) } throws IOException("File not found")
        val senderFileFunctions = buildSenderFilesFunction(mockDbAccess, mockBlobAccess)
        assertThat { senderFileFunctions.processRequest(functionParams) }
            .isFailure()
            .isInstanceOf(FileNotFoundException::class.java)
    }
}