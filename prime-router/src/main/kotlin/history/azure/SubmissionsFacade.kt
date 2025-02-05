package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.azure.DataAccessTransaction
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DetailedActionLog
import gov.cdc.prime.router.history.DetailedReport
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.SubmissionHistory
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val dbSubmissionAccess: HistoryDatabaseAccess = DatabaseSubmissionsAccess(),
    val dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton,
    private val reportGraph: ReportGraph = ReportGraph(),
) : ReportFileFacade(
    dbAccess,
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Serializes a list of Actions into a String.
     *
     * @param organization from JWT Claim.
     * @param sendingOrgService is a specifier for the sending organization's client.
     * @param sortDir sort the table by date in ASC or DESC order.
     * @param sortColumn sort the table by a specific column; defaults to sorting by created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime minimum date to get results for.
     * @param until is the OffsetDateTime maximum date to get results for.
     * @param pageSize Int of items to return per page.
     * @param showFailed whether to include actions that failed to be sent.
     *
     * @return a String representation of an array of actions.
     */
    fun findSubmissionsAsJson(
        organization: String,
        sendingOrgService: String?,
        sortDir: HistoryDatabaseAccess.SortDir,
        sortColumn: HistoryDatabaseAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
    ): String {
        val result = findSubmissions(
            organization,
            sendingOrgService,
            sortDir,
            sortColumn,
            cursor,
            since,
            until,
            pageSize,
            showFailed
        )
        return mapper.writeValueAsString(result)
    }

    /**
     * Find submissions based on various parameters.
     *
     * @param organization from JWT Claim.
     * @param sendingOrgService is a specifier for the sending organization's client.
     * @param sortDir sort the table by date in ASC or DESC order; defaults to DESC.
     * @param sortColumn sort the table by a specific column; defaults to sorting by CREATED_AT.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime minimum date to get results for.
     * @param until is the OffsetDateTime maximum date to get results for.
     * @param pageSize Int of items to return per page.
     * @param showFailed whether to include actions that failed to be sent.
     *
     * @return a List of Actions
     */
    private fun findSubmissions(
        organization: String,
        sendingOrgService: String?,
        sortDir: HistoryDatabaseAccess.SortDir,
        sortColumn: HistoryDatabaseAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
    ): List<SubmissionHistory> {
        require(organization.isNotBlank()) {
            "Invalid organization."
        }

        return dbSubmissionAccess.fetchActionsForSubmissions(
            organization,
            sendingOrgService,
            sortDir,
            sortColumn,
            cursor,
            since,
            until,
            pageSize,
            showFailed,
            SubmissionHistory::class.java
        )
    }

    /**
     * Get expanded details for a single report
     *
     * @param action the action being used
     * @return Report details
     */
    fun findDetailedSubmissionHistory(
        txn: DataAccessTransaction,
        reportId: UUID?,
        action: Action,
    ): DetailedSubmissionHistory? {
        if (reportId == null) {
            return dbSubmissionAccess.fetchAction(
                action.actionId,
                action.sendingOrg,
                DetailedSubmissionHistory::class.java
            )
        }
        val graph = reportGraph.getDescendantReports(txn, reportId)
        val detailedReports = graph.map { reportFile ->
            DetailedReport(
                reportFile.reportId,
                reportFile.receivingOrg,
                reportFile.receivingOrgSvc,
                reportFile.sendingOrg,
                reportFile.sendingOrgClient,
                reportFile.schemaTopic,
                reportFile.externalName,
                reportFile.createdAt,
                reportFile.nextActionAt,
                reportFile.itemCount,
                reportFile.itemCountBeforeQualFilter,
                reportFile.transportResult != null,
                reportFile.transportResult,
                reportFile.downloadedBy,
                reportFile.nextAction
            )
        }.toMutableList()
        val reportIds = graph.map { it.reportId }
        val logs = DSL
            .using(txn)
            .select()
            .from(Tables.ACTION_LOG)
            .where(Tables.ACTION_LOG.REPORT_ID.`in`(reportIds))
            .fetchInto(DetailedActionLog::class.java)
        val history =
            DetailedSubmissionHistory(
                action.actionId,
                action.actionName,
                action.createdAt,
                httpStatus = action.httpStatus,
                logs = logs,
                reports = detailedReports

            )
        history.enrichWithSummary()
        return history
    }

    /**
     * Check whether these [claims] from this [request]
     * allow access to the sender associated with this [action].
     * @return true if authorized, false otherwise.
     * Because this is a Submission request, this checks the [Action.sendingOrg]
     */
    override fun checkAccessAuthorizationForAction(
        claims: AuthenticatedClaims,
        action: Action,
        request: HttpRequestMessage<String?>,
    ): Boolean = claims.authorizedForSendOrReceive(action.sendingOrg, null, request)

    companion object {
        val instance: SubmissionsFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            SubmissionsFacade()
        }
    }
}