package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.ActionResponse
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.SubmissionHistory
import gov.cdc.prime.router.SubmissionsProvider
import java.time.OffsetDateTime

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */

// TODO: Add test/SubmissionsFacadeTests
class SubmissionsFacade(
    private val db: DatabaseSubmissionsAccess = DatabaseSubmissionsAccess()
) : SubmissionsProvider {

    // Ignoring unknown properties because we don't require them. -DK
    private val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun findSubmissionsAsJson(
        organizationName: String,
        limit: Int
    ): String {
        val result = findSubmissions(organizationName, limit)
        return mapper.writeValueAsString(result)
    }

    private fun findSubmissions(organizationName: String, limit: Int): List<SubmissionHistoryAPI> {
        // TODO: VERIFY sendingOrg is being populated from the claim on Staging
        val submissions = db.fetchSubmissions(organizationName, limit)

        return submissions.map {
            val actionResponse = mapper.readValue(it.actionResponse.toString(), ActionResponseColumnAPI::class.java)
            val result = SubmissionHistoryAPI(it.actionId, it.createdAt, it.sendingOrg, it.httpStatus, actionResponse)
            result
        }
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

    /*
     * Classes for JSON serialization
     */

    // TODO: see Github Issues #2314 for expected filename field
    private class SubmissionHistoryAPI
    @JsonCreator constructor(
        actionId: Long,
        createdAt: OffsetDateTime,
        sendingOrg: String,
        httpStatus: Int,
        actionResponse: ActionResponseColumnAPI
    ) : SubmissionHistory(
        actionId,
        createdAt,
        sendingOrg,
        httpStatus,
        actionResponse.id,
        actionResponse.topic,
        actionResponse.reportItemCount,
        actionResponse.warningCount,
        actionResponse.errorCount,
    )

    private class ActionResponseColumnAPI
    @JsonCreator constructor(
        id: String?,
        topic: String?,
        reportItemCount: Int?,
        warningCount: Int?,
        errorCount: Int?,
    ) : ActionResponse(
        id,
        topic,
        reportItemCount,
        warningCount,
        errorCount,
    )
}