package gov.cdc.prime.router.credentials

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import org.apache.logging.log4j.kotlin.Logging

abstract class CredentialService : Logging {

    companion object {
        private val URL_SAFE_KEY_PATTERN = Regex("^[a-zA-Z0-9_-]*$")
    }


    /* Methods to implement in subclasses */

    internal abstract fun fetchCredential(connectionId: String): Credential?
    internal abstract fun saveCredential(connectionId: String, credential: Credential)


    /* Base implementation for credentialService with validations */

    fun fetchCredential(connectionId: String, callerId: String, reason: CredentialRequestReason): Credential? {
        require(URL_SAFE_KEY_PATTERN.matches(connectionId)) { "connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}" }
        logger.info { "CREDENTIAL REQUEST: $callerId requested connectionId($connectionId) credential for $reason" }
        return fetchCredential(connectionId)
    }

    fun saveCredential(connectionId: String, credential: Credential, callerId: String) {
        require(URL_SAFE_KEY_PATTERN.matches(connectionId)) { "connectionId must match: ${URL_SAFE_KEY_PATTERN.pattern}" }
        logger.info { "CREDENTIAL UPDATE: $callerId updated connectionId($connectionId) credential" }
        return saveCredential(connectionId, credential)
    }

}

enum class CredentialRequestReason {
    SEND_BATCH,
    SEND_SINGLE,
    AUTOMATED_TEST
}