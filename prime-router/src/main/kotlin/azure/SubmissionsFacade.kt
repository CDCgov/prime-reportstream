package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.SubmissionHistory
import java.time.OffsetDateTime

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val db: DatabaseSubmissionsAccess = DatabaseSubmissionsAccess()
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
     * @param resultsAfterDate String representation of an OffsetDateTime used for paginating results.
     * @param pageSize Int of items to return per page.
     *
     * @return a String representation of an array of actions.
     */
    // Leaving separate from FindSubmissions to encapsulate json serialization
    fun findSubmissionsAsJson(
        organizationName: String,
        sortOrder: String,
        offset: OffsetDateTime?,
        pageSize: Int
    ): String {
        val result = findSubmissions(organizationName, sortOrder, offset, pageSize)
        return mapper.writeValueAsString(result)
    }

    /**
     * @param organizationName from JWT Claim.
     * @param sortOrder sort the table by date in ASC or DESC order.
     * @param resultsAfterDate String representation of an OffsetDateTime used for paginating results.
     * @param pageSize Int of items to return per page.
     *
     * @return a List of Actions
     */
    private fun findSubmissions(
        organizationName: String,
        sortOrder: String,
        offset: OffsetDateTime?,
        pageSize: Int,
    ): List<SubmissionHistory> {
        require(!organizationName.isNullOrBlank()) {
            "Invalid organization."
        }
        require(pageSize > 0) {
            "pageSize must be a positive integer."
        }
        // TODO: VERIFY sendingOrg is being populated from the claim on Staging
        val submissions = db.fetchActions(
            organizationName,
            sortOrder == "ASC",
            offset,
            pageSize,
            SubmissionHistory::class.java
        )
        return submissions
    }

    companion object {
        // TODO: Update to new Metadata singleton introduced by Carlos
        val metadata: Metadata by lazy {
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata("$baseDir/metadata")
        }

        // The SubmissionFacade is heavy-weight object (because it contains a Jackson Mapper) so reuse it when possible
        val common: SubmissionsFacade by lazy {
            SubmissionsFacade(DatabaseSubmissionsAccess())
        }
    }
}