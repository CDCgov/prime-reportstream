package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.SenderItems
import gov.cdc.prime.router.common.CsvUtilities
import gov.cdc.prime.router.common.Hl7Utilities
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.messages.ReportFileMessage
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.PrincipalLevel
import org.apache.logging.log4j.kotlin.Logging
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

class SenderFilesFunction(
    private val oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN),
    private val dbAccess: DatabaseAccess = DatabaseAccess(),
    private val blobAccess: BlobAccess = BlobAccess()
) : Logging {
    /**
     * The sender file end-point retrieves the reports that contributed to the specified output report.
     */
    @FunctionName("getSenderFiles")
    fun getSenderFiles(
        @HttpTrigger(
            name = "getSenderFiles",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "sender-files"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            try {
                val parameters = checkParameters(request)
                val result = processRequest(parameters)
                logger.info(fromAuditMessage(it.userName, result.reportsIds))
                HttpUtilities.okResponse(request, result.payload)
            } catch (ex: IllegalArgumentException) {
                HttpUtilities.badRequestResponse(request, ex.message ?: "")
            } catch (ex: FileNotFoundException) {
                HttpUtilities.notFoundResponse(request, ex.message ?: "")
            } catch (ex: Exception) {
                logger.error("Internal error for ${it.userName} request: $ex")
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * To indicate a bad request error throw an [IllegalArgumentException] with [message]
     */
    private fun badRequest(message: String): Nothing {
        throw IllegalArgumentException(message)
    }

    /**
     * To indicate a not found error throw an [FileNotFoundException] with [message]
     */
    private fun notFound(message: String): Nothing {
        throw FileNotFoundException(message)
    }

    /**
     * Encapsulates the possible query parameters
     */
    data class FunctionParameters(
        val reportId: ReportId?,
        val reportFileName: String?,
        val messageId: String?,
        val onlyDestinationReportItems: Boolean,
        val offset: Int,
        val limit: Int
    )

    /**
     * Look at the queryParameters of the request and extract function parameters.
     * Check for valid values with defaults are assumed if not specified.
     * Throws [IllegalArgumentException] if any parameter is invalid.
     */
    internal fun checkParameters(request: HttpRequestMessage<String?>): FunctionParameters {
        val messageId = request.queryParameters[MESSAGE_ID_PARAM]

        var covidResultMetadataRecord: CovidResultMetadata? = null

        if (messageId != null) {
            covidResultMetadataRecord = dbAccess.fetchSingleMetadata(messageId)
            if (covidResultMetadataRecord == null) {
                badRequest("$MESSAGE_ID_PARAM not found in covid_result_metadata table.")
            }
        }

        val reportId = try {
            if (messageId != null) {
                covidResultMetadataRecord?.reportId ?: badRequest("No reportID found for messageID: $MESSAGE_ID_PARAM.")
            } else {
                request.queryParameters[REPORT_ID_PARAM]?.let { UUID.fromString(it) }
            }
        } catch (e: Exception) {
            badRequest("Bad $REPORT_ID_PARAM parameter. Details: ${e.message}")
        }
        val reportFileName = request.queryParameters[REPORT_FILE_NAME_PARAM]?.replace("/", "%2F")
        val fullReport = try {
            if (messageId != null) {
                true
            } else {
                request.queryParameters[ONLY_REPORT_ITEMS]?.toBoolean() ?: false
            }
        } catch (e: Exception) {
            badRequest("Bad $ONLY_REPORT_ITEMS parameter. Details: ${e.message}")
        }
        val offset = try {
            if (messageId != null) {
                covidResultMetadataRecord?.reportIndex?.minus(1) ?: badRequest("Index not found: $MESSAGE_ID_PARAM.")
            } else {
                request.queryParameters[OFFSET_PARAM]?.toInt() ?: 0
            }
        } catch (e: Exception) {
            badRequest("Bad $OFFSET_PARAM parameter. Details: ${e.message}")
        }
        val limit = try {
            request.queryParameters[LIMIT_PARAM]?.toInt() ?: DEFAULT_LIMIT_PARAM
        } catch (e: Exception) {
            badRequest("Bad $LIMIT_PARAM parameter. Details: ${e.message}")
        }
        if (reportId == null && reportFileName == null) {
            badRequest("Expected either a $REPORT_ID_PARAM or a $REPORT_FILE_NAME_PARAM parameter")
        }

        return FunctionParameters(
            reportId,
            reportFileName,
            messageId,
            fullReport,
            offset,
            limit
        )
    }

    data class ProcessResult(
        val payload: String,
        val reportsIds: List<String>? = null
    )

    /**
     * Main logic of the Azure function. Useful for unit testing.
     */
    internal fun processRequest(parameters: FunctionParameters): ProcessResult {
        var senderItems: List<SenderItems>
        if (parameters.messageId != null) {
            senderItems = listOf(SenderItems(parameters.reportId, parameters.offset, null, null))
        } else {
            val receiverReportFile = findOutputFile(parameters)
            senderItems = findSenderItems(receiverReportFile.reportId, parameters.offset, parameters.limit)
            if (senderItems.isEmpty()) {
                notFound("No sender reports found for report: ${parameters.reportId}")
            }
        }

        val senderReports = downloadSenderReports(senderItems, parameters)
        val payload = senderReports.serialize()
        return ProcessResult(payload, senderReports.map { it.reportId })
    }

    private fun findOutputFile(parameters: FunctionParameters): ReportFile {
        return when {
            parameters.reportId != null -> dbAccess.fetchReportFile(parameters.reportId)
            parameters.reportFileName != null -> dbAccess.fetchReportFileByBlobURL(parameters.reportFileName)
            else -> null
        } ?: notFound("Could not find the specified report-file")
    }

    private fun findSenderItems(reportId: UUID, offset: Int, limit: Int): List<SenderItems> {
        return dbAccess.fetchSenderItems(reportId, offset, limit)
    }

    /**
     * Given a list of receiver items with their associated sender items in [items],
     * download the sender blobs and extract the content of each sender item.
     */
    private fun downloadSenderReports(
        items: List<SenderItems>,
        parameters: FunctionParameters
    ): List<ReportFileMessage> {
        // Group by senderReportId to avoid downloading blobs multiple times
        val itemsByReport = items.groupBy { it.senderReportId!! }
        return itemsByReport.map { (reportId, senderItems) ->
            val reportFile = dbAccess.fetchReportFile(reportId)
            val blob = try {
                String(BlobAccess.downloadBlob(reportFile.bodyUrl))
            } catch (e: IOException) {
                logger.info("Unable to download $reportId at ${reportFile.bodyUrl}. Details: ${e.message}")
                notFound("Could not fetch a report file, may have been deleted: $reportId")
            }

            val senderIndices = senderItems.map { it.senderReportIndex!! }
            val senderFormat = mapBodyFormatToSenderFormat(reportFile.bodyFormat!!)
            val body = if (parameters.onlyDestinationReportItems) {
                cutContent(blob, senderFormat, senderIndices)
            } else {
                blob
            }

            ReportFileMessage(
                reportId = reportFile.reportId.toString(),
                schemaTopic = reportFile.schemaTopic,
                schemaName = reportFile.schemaName,
                contentType = senderFormat.mimeType,
                content = body,
                origin = ReportFileMessage.Origin(
                    bodyUrl = reportFile.bodyUrl,
                    sendingOrg = reportFile.sendingOrg,
                    sendingOrgClient = reportFile.sendingOrgClient,
                    indices = senderIndices,
                    createdAt = reportFile.createdAt.toString()
                ),
                request = ReportFileMessage.Request(
                    reportId = senderItems.first().receiverReportId?.toString().orEmpty(),
                    receiverReportIndices = senderItems.map { it.receiverReportIndex },
                    messageID = parameters.messageId.toString().orEmpty(),
                    senderReportIndices = parameters.offset
                )
            )
        }
    }

    private fun cutContent(reportBlob: String, senderFormat: Sender.Format, itemIndices: List<Int>): String {
        return when (senderFormat) {
            Sender.Format.CSV -> CsvUtilities.cut(reportBlob, itemIndices)
            Sender.Format.HL7 -> Hl7Utilities.cut(reportBlob, itemIndices)
            else -> throw IllegalStateException("Sender format $senderFormat is not supported")
        }
    }

    /**
     * Write as a JSON string
     */
    private fun List<ReportFileMessage>.serialize(): String {
        return mapper.writeValueAsString(this)
    }

    /**
     * Create a log message for the purpose of recording who downloaded what.
     * [reportIds] tell the what. [userName] tells the who.
     */
    private fun fromAuditMessage(userName: String, reportIds: List<String>?): String {
        return "User $userName has downloaded these reports through the sender-file API: $reportIds"
    }

    companion object {
        /**
         * Query parameter for the report-id option
         */
        const val MESSAGE_ID_PARAM = "message-id"

        /**
         * Query parameter for the report-id option
         */
        const val REPORT_ID_PARAM = "report-id"

        /**
         * Query parameter for the report-file-name option
         */
        const val REPORT_FILE_NAME_PARAM = "report-file-name"

        /**
         * Query parameter for the only destination report options
         */
        const val ONLY_REPORT_ITEMS = "only-report-items"

        /**
         * Query parameter the offset in the receiver report
         */
        const val OFFSET_PARAM = "offset"

        /**
         * Query parameter to limit the receiver report
         */
        const val LIMIT_PARAM = "limit"

        /**
         * Default limit parameter
         */
        const val DEFAULT_LIMIT_PARAM = 10000

        private val mapper = JacksonMapperUtilities.defaultMapper

        private fun mapBodyFormatToSenderFormat(bodyFormat: String): Sender.Format {
            return when (bodyFormat) {
                "CSV", "CSV_SINGLE", "INTERNAL" -> Sender.Format.CSV
                "HL7", "HL7_BATCH" -> Sender.Format.HL7
                else -> error("Unknown body format type: $bodyFormat")
            }
        }
    }
}