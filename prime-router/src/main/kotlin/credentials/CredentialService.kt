package gov.cdc.prime.router.credentials

import org.apache.logging.log4j.kotlin.Logging

abstract class CredentialService : Logging {
    private val URL_SAFE_KEY_PATTERN = Regex("^[a-zA-Z0-9_-]*$")

    internal abstract fun fetchCredential(connectionId: String): Credential?
    internal abstract fun saveCredential(connectionId: String, credential: Credential)

    fun fetchCredential(connectionId: String, callerId: String, reason: CredentialRequestReason): Credential? {
        logger.info { "CREDENTIAL REQUEST: $callerId requested connectionId($connectionId) credential for $reason" }
        validateConnectionId(connectionId)
        return fetchCredential(connectionId)
    }

    fun saveCredential(connectionId: String, credential: Credential, callerId: String) {
        logger.info { "CREDENTIAL UPDATE: $callerId updated connectionId($connectionId) credential" }
        validateConnectionId(connectionId)
        return saveCredential(connectionId, credential)
    }

    private fun validateConnectionId(connectionId: String) {
        if ( !URL_SAFE_KEY_PATTERN.matches(connectionId) ) {
            throw IllegalArgumentException("connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}")
        }
    }
}

enum class CredentialRequestReason {
    SEND_BATCH,
    SEND_SINGLE,
    AUTOMATED_TEST
}