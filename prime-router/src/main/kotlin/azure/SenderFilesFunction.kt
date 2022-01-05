package gov.cdc.prime.router.azure

import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.SenderItems
import gov.cdc.prime.router.common.CsvUtilities
import gov.cdc.prime.router.messages.ReportFileListMessage
import gov.cdc.prime.router.messages.ReportFileMessage
import gov.cdc.prime.router.tokens.OktaAuthentication
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
                val parameters = try {
                    checkParameters(request)
                } catch (e: IllegalArgumentException) {
                    return@checkAccess HttpUtilities.badRequestResponse(request, e.message ?: "")
                }
                val (status, payload) = processRequest(parameters)
                when (status) {
                    Status.OK -> HttpUtilities.okResponse(request, payload)
                    Status.BAD_REQUEST -> HttpUtilities.badRequestResponse(request, payload)
                    Status.NOT_FOUND -> HttpUtilities.notFoundResponse(request, payload)
                }
            } catch (e: Exception) {
                logger.error("Internal error: $e")
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    data class FunctionParameters(
        val reportId: ReportId?,
        val reportFileName: String?,
        val synthesize: Boolean,
        val offset: Int,
        val limit: Int,
    )

    enum class Status {
        OK,
        NOT_FOUND,
        BAD_REQUEST,
    }

    /**
     * Look at the request and extract parameters. Check for valid values.
     * Defaults are assumed if not specified.
     * Throws [IllegalArgumentException] if any parameter is invalid.
     */
    internal fun checkParameters(request: HttpRequestMessage<String?>): FunctionParameters {
        val reportId = try {
            request.queryParameters["report-id"]?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad report-id parameter")
        }
        val reportFileName = request.queryParameters["report-file-name"]
        val synthesize = try {
            request.queryParameters["synthesize"]?.toBoolean() ?: false
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad synthesize parameter")
        }
        val offset = try {
            request.queryParameters["offset"]?.toInt() ?: 0
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad offset parameter")
        }
        val limit = try {
            request.queryParameters["limit"]?.toInt() ?: 100
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad limit parameter")
        }
        if (reportId == null && reportFileName == null) {
            throw IllegalArgumentException("Expected either a report-id or a report-file-name")
        }

        return FunctionParameters(
            reportId,
            reportFileName,
            synthesize,
            offset,
            limit
        )
    }

    /**
     * Main logic of the Azure function. Useful for testing.
     */
    internal fun processRequest(parameters: FunctionParameters): Pair<Status, String> {
        val receiverReportFile = try {
            findOutputFile(parameters)
        } catch (e: Exception) {
            return Pair(Status.NOT_FOUND, "Receiver report file not found: ${e.message}")
        }

        val senderItems = findSenderItems(receiverReportFile.reportId, parameters.offset, parameters.limit)
        if (senderItems.isEmpty()) {
            return Pair(Status.NOT_FOUND, "No sender reports found for report: ${parameters.reportId}")
        }

        val senderReports = downloadSenderReports(senderItems)
        val emptyReport = senderReports.find { it.content.isEmpty() }
        if (emptyReport != null) {
            return Pair(
                Status.NOT_FOUND,
                "Could not fetch the specified file, may have been deleted: ${emptyReport.origin?.bodyUrl}"
            )
        }

        val payload = senderReports
            .synthesize(parameters)
            .serialize()
        return Pair(Status.OK, payload)
    }

    private fun findOutputFile(parameters: FunctionParameters): ReportFile {
        return when {
            parameters.reportId != null -> dbAccess.fetchReportFile(parameters.reportId)
            parameters.reportFileName != null -> dbAccess.fetchReportFileByBlobURL(parameters.reportFileName)
            else -> null
        } ?: throw FileNotFoundException("Could not find the specified report-file")
    }

    private fun findSenderItems(reportId: UUID, offset: Int, limit: Int): List<SenderItems> {
        return dbAccess.fetchSenderItems(reportId, offset, limit)
    }

    /**
     * Given a list of receiver items with their associated sender items in [items],
     * download the sender blobs and extract the content of each sender item.
     */
    private fun downloadSenderReports(
        items: List<SenderItems>
    ): List<ReportFileMessage> {
        // Group by senderReportId to avoid downloading blobs multiple times
        val itemsByReport = items.groupBy { it.senderReportId!! }
        return itemsByReport.map { (reportId, senderItems) ->
            val reportFile = dbAccess.fetchReportFile(reportId)
            val blob = try {
                String(blobAccess.downloadBlob(reportFile.bodyUrl))
            } catch (e: IOException) {
                ""
            }
            val senderIndices = senderItems.map { it.senderReportIndex!! }
            val senderFormat = mapBodyFormatToSenderFormat(reportFile.bodyFormat!!)
            val body = extractContent(blob, senderFormat, senderIndices)

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
                    reportId = senderItems.first().receiverReportId.toString(),
                    indices = senderItems.map { it.receiverReportIndex!! }
                )
            )
        }
    }

    private fun List<ReportFileMessage>.synthesize(parameters: FunctionParameters): List<ReportFileMessage> {
        return if (parameters.synthesize) {
            TODO()
        } else {
            this
        }
    }

    private fun List<ReportFileMessage>.serialize(): String {
        val payload = ReportFileListMessage(this)
        return mapper.writeValueAsString(payload)
    }

    companion object {
        private val mapper = jacksonMapperBuilder().build()

        private fun mapBodyFormatToSenderFormat(bodyFormat: String): Sender.Format {
            return when (bodyFormat) {
                "CSV", "CSV_SINGLE", "INTERNAL" -> Sender.Format.CSV
                "HL7", "HL7_BATCH" -> Sender.Format.HL7
                else -> error("Unknown body format type: $bodyFormat")
            }
        }

        internal fun extractContent(reportBlob: String, senderFormat: Sender.Format, itemIndices: List<Int>): String {
            if (reportBlob.isBlank()) return ""
            return when (senderFormat) {
                Sender.Format.CSV -> CsvUtilities.cut(reportBlob, itemIndices)
                Sender.Format.HL7 -> TODO("Support for HL7 is not implemented")
            }
        }
    }
}