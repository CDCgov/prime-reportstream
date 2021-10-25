package gov.cdc.prime.router.azure

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.TranslatorConfiguration
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import org.jooq.JSONB
import java.time.OffsetDateTime

/**
 * Settings for Organization, Receivers, and Senders from the Azure Database.
 * Contains all business logic regarding settings as well as JSON serialization.
 */
class SubmissionsFacade(
    private val metadata: Metadata,
    private val db: DatabaseSubmissionsAccess = DatabaseSubmissionsAccess()
) : SettingsProvider {
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
        get() = findSubmissions(SubmissionsAPI::class.java)


    fun getLastModified(): OffsetDateTime? {
        return db.fetchLastModified()
    }

    fun <T : SettingAPI> findSubmissionsAsJson(
        clazz: Class<T>
    ): String? {
        val result = findSubmissions(clazz) ?: return null
        return mapper.writeValueAsString(result)
    }

    private fun <T : SettingAPI> findSubmissions(clazz: Class<T>): List<T> {
        val submissions = db.transactReturning { txn ->
            db.fetchSubmissions(txn)
        }
        return submissions.map {
            val result = mapper.readValue(it.values.data(), clazz)
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

data class SubmissionMetadata(
    val version: Int,
    val createdBy: String,
    val createdAt: OffsetDateTime
)

interface SubmissionAPI {
    val actionId: Long
    fun consistencyErrorMessage(metadata: Metadata): String?
}