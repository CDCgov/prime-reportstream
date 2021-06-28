package gov.cdc.prime.router.credentials

import org.apache.logging.log4j.kotlin.Logging

abstract class CredentialService : Logging {

    companion object {
        private val URL_SAFE_KEY_PATTERN = Regex("^[a-zA-Z0-9-]*$")
    }

    /* Methods to implement in subclasses */

    protected abstract fun fetchCredential(connectionId: String): Credential?
    protected abstract fun saveCredential(connectionId: String, credential: Credential)

    /* Base implementation for credentialService with validations */

    fun fetchCredential(connectionId: String, callerId: String, reason: CredentialRequestReason): Credential? {
        require(URL_SAFE_KEY_PATTERN.matches(connectionId)) {
            "connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}"
        }
        logger.info { "CREDENTIAL REQUEST: $callerId requested connectionId($connectionId) credential for $reason" }
        return fetchCredential(connectionId)
    }

    fun saveCredential(connectionId: String, credential: Credential, callerId: String) {
        require(URL_SAFE_KEY_PATTERN.matches(connectionId)) {
            "connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}"
        }
        logger.info { "CREDENTIAL UPDATE: $callerId updating connectionId($connectionId) credential..." }
        saveCredential(connectionId, credential)
        logger.info { "CREDENTIAL UPDATE: $callerId updated connectionId($connectionId) credential successfully." }
    }
}

enum class CredentialRequestReason {
    SFTP_UPLOAD,
    AS2_UPLOAD,
    AUTOMATED_TEST,
    PERSIST_VERIFY,
    FTPS_UPLOAD
}