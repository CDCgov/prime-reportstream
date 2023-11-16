package gov.cdc.prime.router.credentials

import io.ktor.client.HttpClient
import org.apache.logging.log4j.kotlin.Logging

abstract class CredentialService() : Logging {

    companion object {
        private val URL_SAFE_KEY_PATTERN = Regex("^[a-zA-Z0-9-]*$")
    }

    /* Methods to implement in subclasses */

    protected abstract fun fetchCredential(connectionId: String, httpClient: HttpClient? = null): Credential?
    protected abstract fun saveCredential(connectionId: String, credential: Credential, httpClient: HttpClient? = null)

    /* Base implementation for credentialService with validations */

    fun fetchCredential(
        connectionId: String,
                        callerId: String,
                        reason: CredentialRequestReason,
                        httpClient: HttpClient? = null,
    ): Credential? {
        require(URL_SAFE_KEY_PATTERN.matches(connectionId)) {
            "connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}"
        }
        logger.info { "CREDENTIAL REQUEST: $callerId requested connectionId($connectionId) credential for $reason" }
        return fetchCredential(connectionId, httpClient = httpClient)
    }

    fun saveCredential(connectionId: String, credential: Credential, callerId: String, httpClient: HttpClient? = null) {
        require(URL_SAFE_KEY_PATTERN.matches(connectionId)) {
            "connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}"
        }
        logger.info { "CREDENTIAL UPDATE: $callerId updating connectionId($connectionId) credential..." }
        saveCredential(connectionId, credential, httpClient = httpClient)
        logger.info { "CREDENTIAL UPDATE: $callerId updated connectionId($connectionId) credential successfully." }
    }
}

enum class CredentialRequestReason {
    SFTP_UPLOAD,
    AS2_UPLOAD,
    AUTOMATED_TEST,
    PERSIST_VERIFY,
    SOAP_UPLOAD,
    GAEN_NOTIFICATION,
    REST_UPLOAD,
}