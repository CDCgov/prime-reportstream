package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Submission
import gov.cdc.prime.router.SubmissionsProvider

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

    private val mapper = jacksonObjectMapper()

    init {
        // Format OffsetDateTime as an ISO string
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override val submissions: Collection<Submission>
        get() = findSubmissions(SubmissionAPI::class.java)

    fun <T : SubmissionAPI> findSubmissionsAsJson(
        clazz: Class<T>
    ): String {
        val result = findSubmissions(clazz)
        return mapper.writeValueAsString(result)
    }

    private fun <T : SubmissionAPI> findSubmissions(clazz: Class<T>): List<T> {
        val submissions = db.fetchSubmissions()

        // TODO: going to need to get this to properly map back to the type you want
        return submissions.map {
            val result = mapper.readValue(it.toString(), clazz)
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

class SubmissionAPI
@JsonCreator constructor(
    actionId: Long
) : Submission(
    actionId
)