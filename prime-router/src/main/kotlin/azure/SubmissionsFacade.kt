package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.ActionResponse
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Submission
import gov.cdc.prime.router.SubmissionsProvider
import java.time.OffsetDateTime

/**
 * Submissions / history API
 * Contains all business logic regarding submissions and JSON serialization.
 */
class SubmissionsFacade(
    private val metadata: Metadata,
    private val db: DatabaseSubmissionsAccess = DatabaseSubmissionsAccess()
) : SubmissionsProvider {
    enum class AccessResult {
        SUCCESS,
        CREATED,
        NOT_FOUND,
        BAD_REQUEST
    }

    // Ignoring unknown props because we don't need them all. -DK
    private val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun findSubmissionsAsJson(
        organizationName: String,
    ): String {
        val result = findSubmissions(organizationName)
        return mapper.writeValueAsString(result)
    }

    private fun findSubmissions(organizationName: String): List<SubmissionAPI> {
        // TODO: VERIFY sendingOrg is being populated from the claim
        val submissions = db.fetchSubmissions(organizationName)

        return submissions.map {
            val actionResponse = mapper.readValue(it.actionResponse.toString(), ActionResponseAPI::class.java)
            val result = SubmissionAPI(it.actionId, it.createdAt, it.sendingOrg, it.httpStatus, actionResponse)
            result
        }
    }

    companion object {
        val metadata: Metadata by lazy {
            val baseDir = System.getenv("AzureWebJobsScriptRoot")
            Metadata("$baseDir/metadata")
        }

        // The SubmissionFacade is heavy-weight object (because it contains a Jackson Mapper) so reuse it when possible
        val common: SubmissionsFacade by lazy {
            SubmissionsFacade(metadata, DatabaseSubmissionsAccess())
        }

        private fun errorJson(message: String): String {
            return """{"error": "$message"}"""
        }
    }
}

/**
 * Classes for JSON serialization
 */

/**
 * TODO: see Github Issues #2314 for expected filename field
 */
class SubmissionAPI
@JsonCreator constructor(
    actionId: Long,
    createdAt: OffsetDateTime,
    sendingOrg: String,
    httpStatus: Int,
    actionResponse: ActionResponseAPI
) : Submission(
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

class ActionResponseAPI
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