package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.history.SubmissionHistory
import java.time.OffsetDateTime

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val dbSubmissionAccess: ReportFileAccess = DatabaseSubmissionsAccess(),
    dbAccess: DatabaseAccess = WorkflowEngine.databaseAccessSingleton
) : ReportFileFacade(
    dbAccess,
) {
    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Serializes a list of Actions into a String.
     *
     * @param organization from JWT Claim.
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
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean
    ): String {
        val result = findSubmissions(organization, sortDir, sortColumn, cursor, since, until, pageSize, showFailed)
        return mapper.writeValueAsString(result)
    }

    /**
     * Find submissions based on various parameters.
     *
     * @param organization from JWT Claim.
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
        sortDir: ReportFileAccess.SortDir,
        sortColumn: ReportFileAccess.SortColumn,
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

    fun findDetailedSubmissionHistory(
        organizationName: String,
        submissionId: Long,
    ): DetailedSubmissionHistory? {
        val submission = dbSubmissionAccess.fetchAction(
            organizationName,
            submissionId,
            DetailedSubmissionHistory::class.java
        )

        submission?.let {
            val relatedSubmissions = dbSubmissionAccess.fetchRelatedActions(
                submission.actionId,
                DetailedSubmissionHistory::class.java
            )
            it.enrichWithDescendants(relatedSubmissions)
        }

        submission?.enrichWithSummary()

        return submission
    }

    companion object {
        val instance: SubmissionsFacade by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            SubmissionsFacade()
        }
    }
}