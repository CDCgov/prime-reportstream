package gov.cdc.prime.router.history.azure

import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.SubmissionHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val dbSubmissionAccess: HistoryDatabaseAccess = DatabaseSubmissionsAccess(),
    dbAccess: DatabaseAccess = BaseEngine.databaseAccessSingleton
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
        showFailed: Boolean
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
        showFailed: Boolean
    ): List<SubmissionHistory> {
        require(organization.isNotBlank()) {
            "Invalid organization."
        }

        return dbSubmissionAccess.fetchActions(
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
        action: Action
    ): DetailedSubmissionHistory? {
        // This assumes that ReportFileFunction.authSingleBlocks has already run, and has checked that the
        // sendingOrg is good.  If that assumption is incorrect, die here.
        assert(action.sendingOrg != null && action.actionName == TaskAction.receive)
        val submission = dbSubmissionAccess.fetchAction(
            action.actionId,
            action.sendingOrg,
            DetailedSubmissionHistory::class.java
        )

        // Submissions with a report ID (means had no errors) can have a lineage
        submission?.reportId?.let {
            val relatedSubmissions = dbSubmissionAccess.fetchRelatedActions(
                UUID.fromString(it),
                DetailedSubmissionHistory::class.java
            )
            submission.enrichWithDescendants(relatedSubmissions)
        }

        submission?.enrichWithSummary()

        return submission
    }

    /**
     * Check whether these [claims] allow access to this [orgName].
     * @return true if [claims] authorizes access to this [orgName].  Return
     * false if the [orgName] is empty or if the claim does not give access.
     */
    override fun checkAccessAuthorization(
        claims: AuthenticatedClaims,
        orgName: String?,
        senderOrReceiver: String?,
        request: HttpRequestMessage<String?>,
    ): Boolean {
        if (orgName.isNullOrEmpty()) {
            logger.warn(
                "Unauthorized.  Action had no sending-organization name. " +
                    " For user ${claims.userName}: ${request.httpMethod}:${request.uri.path}."
            )
            return false
        }
        return claims.authorizedForSendOrReceive(orgName, senderOrReceiver, request)
    }

    companion object {
        val instance: SubmissionsFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            SubmissionsFacade()
        }
    }
}