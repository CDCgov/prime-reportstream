package gov.cdc.prime.router.azure

import gov.cdc.prime.router.DetailActionLog
import gov.cdc.prime.router.DetailReport
import gov.cdc.prime.router.DetailedSubmissionHistory
import gov.cdc.prime.router.SubmissionHistory
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val dbSubmissionAccess: SubmissionAccess = DatabaseSubmissionsAccess(),
    private val dbAccess: DatabaseAccess = WorkflowEngine.databaseAccessSingleton
) : Logging {

    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = JacksonMapperUtilities.allowUnknownsMapper

    /**
     * Serializes a list of Actions into a String.
     *
     * @param organizationName from JWT Claim.
     * @param sortOrder sort the table by date in ASC or DESC order.
     * @param sortColumn sort the table by a specific column; defaults to sorting by created_at.
     * @param offset String representation of an OffsetDateTime used for paginating results.
     * @param pageSize Int of items to return per page.
     *
     * @return a String representation of an array of actions.
     */
    // Leaving separate from FindSubmissions to encapsulate json serialization
    fun findSubmissionsAsJson(
        organizationName: String,
        sortOrder: String,
        sortColumn: String,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean
    ): String {
        val result = findSubmissions(organizationName, sortOrder, sortColumn, offset, toEnd, pageSize, showFailed)
        return mapper.writeValueAsString(result)
    }

    private fun findSubmissions(
        organizationName: String,
        sortOrder: String,
        sortColumn: String,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean
    ): List<SubmissionHistory> {
        val order = try {
            SubmissionAccess.SortOrder.valueOf(sortOrder)
        } catch (e: IllegalArgumentException) {
            SubmissionAccess.SortOrder.DESC
        }

        val column = try {
            SubmissionAccess.SortColumn.valueOf(sortColumn)
        } catch (e: IllegalArgumentException) {
            SubmissionAccess.SortColumn.CREATED_AT
        }

        /* This will swap the order of the date-time offsets IF both are present
         * and the given cursor is a date prior to the end-date.
         */
        return findSubmissions(organizationName, order, column, offset, toEnd, pageSize, showFailed)
    }

    /**
     * @param organizationName from JWT Claim.
     * @param sortOrder sort the table by date in ASC or DESC order; defaults to DESC.
     * @param sortColumn sort the table by a specific column; defaults to sorting by CREATED_AT.
     * @param offset String representation of an OffsetDateTime used for paginating results.
     * @param pageSize Int of items to return per page.
     *
     * @return a List of Actions
     */
    private fun findSubmissions(
        organizationName: String,
        sortOrder: SubmissionAccess.SortOrder,
        sortColumn: SubmissionAccess.SortColumn,
        offset: OffsetDateTime?,
        toEnd: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean
    ): List<SubmissionHistory> {
        require(organizationName.isNotBlank()) {
            "Invalid organization."
        }
        require(pageSize > 0) {
            "pageSize must be a positive integer."
        }

        val submissions = dbSubmissionAccess.fetchActions(
            organizationName,
            sortOrder,
            sortColumn,
            offset,
            toEnd,
            pageSize,
            showFailed,
            SubmissionHistory::class.java
        )
        return submissions
    }

    fun findDetailedSubmissionHistory(
        organizationName: String,
        submissionId: Long,
    ): DetailedSubmissionHistory? {

        val submission = dbSubmissionAccess.fetchAction(
            organizationName,
            submissionId,
            DetailedSubmissionHistory::class.java,
            DetailReport::class.java,
            DetailActionLog::class.java,
        )

        submission?.let {
            val relatedSubmissions = dbSubmissionAccess.fetchRelatedActions(
                submission.actionId,
                DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java,
            )
            it.enrichWithDescendants(relatedSubmissions)
        }

        return submission
    }

    /**
     * get a single Action associated with this reportId
     */
    fun fetchActionForReportId(reportId: UUID): Action? {
        return dbAccess.fetchActionForReportId(reportId)
    }

    /**
     * get a single Action, given its key
     */
    fun fetchAction(actionId: Long): Action? {
        return dbAccess.fetchAction(actionId)
    }

    /**
     * @return true if [claims] authorizes access to this [action].  Return
     * false if the submissionId is not a proper submission or if the claim does not give access.
     */
    fun checkActionAccessAuthorization(
        action: Action,
        claims: AuthenticatedClaims,
    ): Boolean {
        return when {
            claims.principalLevel == PrincipalLevel.SYSTEM_ADMIN -> true
            action.actionName != TaskAction.receive -> {
                logger.warn("Requested submissionId ${action.actionId} is not a 'receive' action")
                false
            }
            action.sendingOrg == claims.organizationName -> true
            else -> {
                logger.error(
                    "User from org ${claims.organizationName} denied access to action_id ${action.actionId}" +
                        " submitted by ${action.sendingOrg}"
                )
                false
            }
        }
    }

    companion object {

        // The SubmissionFacade is heavy-weight object (because it contains a Jackson Mapper) so reuse it when possible
        val common: SubmissionsFacade by lazy {
            SubmissionsFacade(DatabaseSubmissionsAccess())
        }
    }
}