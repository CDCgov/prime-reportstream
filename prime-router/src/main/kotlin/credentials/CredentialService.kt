package gov.cdc.prime.router.credentials

import org.apache.logging.log4j.kotlin.Logging

abstract class CredentialService : Logging {
    internal abstract fun fetchCredential(connectionId: String): Credential?
    internal abstract fun saveCredential(connectionId: String, credential: Credential)

    fun fetchCredential(connectionId: String, callerId: String, reason: CredentialRequestReason): Credential? {
        logger.info { "CREDENTIAL REQUEST: $callerId requested connectionId($connectionId) credential for $reason" }
        return fetchCredential(connectionId)
    }

    fun saveCredential(connectionId: String, credential: Credential, callerId: String) {
        logger.info { "CREDENTIAL UPDATE: $callerId updated connectionId($connectionId) credential" }
        return saveCredential(connectionId, credential)
    }
}

enum class CredentialRequestReason {
    SEND_BATCH,
    SEND_SINGLE,
    AUTOMATED_TEST
}