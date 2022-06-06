package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.tokens.AuthenticationStrategy
import gov.cdc.prime.router.tokens.authenticationFailure
import gov.cdc.prime.router.tokens.authorizationFailure
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */
abstract class ReportFileFunction(
    internal val workflowEngine: WorkflowEngine = WorkflowEngine(),
) : Logging {
    abstract fun userOrgName(organization: String): String?

    abstract fun historyAsJson(request: HttpRequestMessage<String?>, userOrgName: String): String

    /**
     * Get a list of reports for a given organization.
     *
     * @param request HTML request body.
     * @param organization Name of the organization sending or receiving this report.
     * @return JSON of the report list or errors.
     */
    fun getListByOrg(
        request: HttpRequestMessage<String?>,
        organization: String,
    ): HttpResponseMessage {
        try {
            // Do authentication
            val claims = AuthenticationStrategy.authenticate(request)
                ?: return HttpUtilities.unauthorizedResponse(request, authenticationFailure)

            val userOrgName = this.userOrgName(organization)
                ?: return HttpUtilities.notFoundResponse(request, "$organization: unknown ReportStream user")

            // Authorize based on: org name in the path == org name in claim.  Or be a prime admin.
            if ((claims.organizationNameClaim != userOrgName) && !claims.isPrimeAdmin) {
                logger.warn(
                    "Invalid Authorization for user ${claims.userName}:" +
                        " ${request.httpMethod}:${request.uri.path}." +
                        " ERR: Claim org is ${claims.organizationNameClaim} but client id is $userOrgName"
                )
                return HttpUtilities.unauthorizedResponse(request, authorizationFailure)
            }
            logger.info(
                "Authorized request by org ${claims.organizationNameClaim}" +
                    " to via client id $userOrgName."
            )

            return HttpUtilities.okResponse(request, this.historyAsJson(request, userOrgName))
        } catch (e: IllegalArgumentException) {
            return HttpUtilities.badRequestResponse(request, HttpUtilities.errorJson(e.message ?: "Invalid Request"))
        }
    }

    data class HistoryApiParameters(
        val sortDir: ReportFileAccess.SortDir,
        val sortColumn: ReportFileAccess.SortColumn,
        val cursor: OffsetDateTime?,
        val since: OffsetDateTime?,
        val until: OffsetDateTime?,
        val pageSize: Int,
        val showFailed: Boolean
    ) {
        constructor(query: Map<String, String>) : this (
            sortDir = extractSortDir(query),
            sortColumn = extractSortCol(query),
            cursor = extractDateTime(query, "cursor"),
            since = extractDateTime(query, "since"),
            until = extractDateTime(query, "until"),
            pageSize = extractPageSize(query),
            showFailed = extractShowFailed(query)
        )

        companion object {
            /**
             * Convert sorting direction from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractSortDir(query: Map<String, String>): ReportFileAccess.SortDir {
                val sort = query["sortdir"]
                return if (sort == null)
                    ReportFileAccess.SortDir.DESC
                else
                    ReportFileAccess.SortDir.valueOf(sort)
            }

            /**
             * Convert sorting column from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractSortCol(query: Map<String, String>): ReportFileAccess.SortColumn {
                val col = query["sortcol"]
                return if (col == null)
                    ReportFileAccess.SortColumn.CREATED_AT
                else
                    ReportFileAccess.SortColumn.valueOf(col)
            }

            /**
             * Convert date time fields from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractDateTime(query: Map<String, String>, name: String): OffsetDateTime? {
                val dt = query[name]
                return if (dt != null) {
                    try {
                        OffsetDateTime.parse(dt)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("\"$name\" must be a valid datetime")
                    }
                } else null
            }

            /**
             * Convert page size from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractPageSize(query: Map<String, String>): Int {
                val size = query.getOrDefault("pagesize", "50").toInt()
                require(size > 0) { "Page size must be a positive integer" }
                return size
            }

            /**
             * Convert show failed from query into param used for the DB
             * @param query Incoming query params
             * @return converted params
             */
            fun extractShowFailed(query: Map<String, String>): Boolean {
                return query["showfailed"]?.toBoolean() ?: false
            }
        }
    }

    /**
     * Utility function.  Mimic String.toLongOrNull()
     *
     * @param str
     * @return a valid UUID, or null if this [str] cannot be parsed into a valid UUID.
     */
    fun toUuidOrNull(str: String): UUID? {
        return try {
            UUID.fromString(str)
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid format for report ID: $str", e)
            null
        }
    }
}