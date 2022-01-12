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
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.Logging
import java.util.UUID

class PreviewFunction(
    private val oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN),
) : Logging {
    /**
     * The sender file end-point retrieves the reports that contributed to the specified output report.
     */
    @FunctionName("preview")
    fun preview(
        @HttpTrigger(
            name = "preview",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "preview"
        ) request: HttpRequestMessage<String?>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request) {
            try {
                val parameters = try {
                    checkParameters(request)
                } catch (e: IllegalArgumentException) {
                    return@checkAccess HttpUtilities.badRequestResponse(request, e.message ?: "")
                }
                val result = processRequest(parameters)
                when (result.status) {
                    Status.OK -> {
                        val auditMsg = fromAuditMessage(it.userName, result.reportsIds)
                        logger.info(auditMsg)
                        HttpUtilities.okResponse(request, result.payload)
                    }
                    Status.BAD_REQUEST -> HttpUtilities.badRequestResponse(request, result.payload)
                    Status.NOT_FOUND -> HttpUtilities.notFoundResponse(request, result.payload)
                }
            } catch (e: Exception) {
                logger.error("Internal error for ${it.userName} request: $e")
                HttpUtilities.internalErrorResponse(request)
            }
        }
    }

    /**
     * Encapsulates the possible query parameters
     */
    data class FunctionParameters(
        val reportId: ReportId?,
        val reportFileName: String?,
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
        val reportId = try {
            request.queryParameters[REPORT_ID_PARAM]?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad $REPORT_ID_PARAM parameter")
        }
        val reportFileName = request.queryParameters[REPORT_FILE_NAME_PARAM]?.replace("/", "%2F")
        val fullReport = try {
            request.queryParameters[ONLY_REPORT_ITEMS]?.toBoolean() ?: false
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad $ONLY_REPORT_ITEMS parameter")
        }
        val offset = try {
            request.queryParameters[OFFSET_PARAM]?.toInt() ?: 0
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad $OFFSET_PARAM parameter")
        }
        val limit = try {
            request.queryParameters[LIMIT_PARAM]?.toInt() ?: DEFAULT_LIMIT_PARAM
        } catch (e: Exception) {
            throw IllegalArgumentException("Bad $LIMIT_PARAM parameter")
        }
        if (reportId == null && reportFileName == null) {
            throw IllegalArgumentException("Expected either a $REPORT_ID_PARAM or a $REPORT_FILE_NAME_PARAM parameter")
        }

        return FunctionParameters(
            reportId,
            reportFileName,
            fullReport,
            offset,
            limit
        )
    }

    enum class Status {
        OK,
        NOT_FOUND,
        BAD_REQUEST,
    }

    data class ProcessResult(
        val status: Status,
        val payload: String,
        val reportsIds: List<String>? = null
    )

    /**
     * Main logic of the Azure function. Useful for unit testing.
     */
    internal fun processRequest(parameters: FunctionParameters): ProcessResult {
        val (_) = parameters
        TODO()
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

        private val mapper = jacksonMapperBuilder().build()

        private fun mapBodyFormatToSenderFormat(bodyFormat: String): Sender.Format {
            return when (bodyFormat) {
                "CSV", "CSV_SINGLE", "INTERNAL" -> Sender.Format.CSV
                "HL7", "HL7_BATCH" -> Sender.Format.HL7
                else -> error("Unknown body format type: $bodyFormat")
            }
        }
    }
}