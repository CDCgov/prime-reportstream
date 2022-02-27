package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.DetailActionLog
import gov.cdc.prime.router.DetailReport
import gov.cdc.prime.router.DetailedSubmissionHistory
import gov.cdc.prime.router.SubmissionHistory
import java.time.OffsetDateTime

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val db: SubmissionAccess = DatabaseSubmissionsAccess()
) {

    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

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
        pageSize: Int
    ): String {
        val result = findSubmissions(organizationName, sortOrder, sortColumn, offset, pageSize)
        return mapper.writeValueAsString(result)
    }

    private fun findSubmissions(
        organizationName: String,
        sortOrder: String,
        sortColumn: String,
        offset: OffsetDateTime?,
        pageSize: Int,
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
        return findSubmissions(organizationName, order, column, offset, pageSize)
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
        pageSize: Int,
    ): List<SubmissionHistory> {
        require(!organizationName.isNullOrBlank()) {
            "Invalid organization."
        }
        require(pageSize > 0) {
            "pageSize must be a positive integer."
        }

        val submissions = db.fetchActions(
            organizationName,
            sortOrder,
            sortColumn,
            offset,
            pageSize,
            SubmissionHistory::class.java
        )
        return submissions
    }

    fun findSubmission(
        organizationName: String,
        submissionId: Long,
    ): DetailedSubmissionHistory? {

        val submission = db.fetchAction(
            organizationName,
            submissionId,
            DetailedSubmissionHistory::class.java,
            DetailReport::class.java,
            DetailActionLog::class.java,
        )

        submission?.let {
            val relatedSubmissions = db.fetchRelatedActions(
                submission.actionId,
                DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java,
            )
            it.enrichWithDescendants(relatedSubmissions)
        }

        return submission
    }

    companion object {

        // The SubmissionFacade is heavy-weight object (because it contains a Jackson Mapper) so reuse it when possible
        val common: SubmissionsFacade by lazy {
            SubmissionsFacade(DatabaseSubmissionsAccess())
        }
    }
}