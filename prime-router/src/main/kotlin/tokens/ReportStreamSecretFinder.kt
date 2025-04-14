package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.secrets.SecretHelper
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey

interface ReportStreamSecretFinder {
    fun getReportStreamTokenSigningSecret(): SecretKey

    companion object {
        const val TOKEN_SIGNING_SECRET_NAME = "TokenSigningSecret"
        private val TOKEN_SIGNING_KEY_ALGORITHM = SignatureAlgorithm.HS384

        // convenience method that knows how to generate the right kind of secret.
        fun generateSecret(): String = Encoders.BASE64.encode(Keys.secretKeyFor(TOKEN_SIGNING_KEY_ALGORITHM).encoded)
    }
}

class FindReportStreamSecretInVault : ReportStreamSecretFinder {
    override fun getReportStreamTokenSigningSecret(): SecretKey {
        val secretServiceAgent = SecretHelper.getSecretService()
        val secret = secretServiceAgent.fetchSecret(ReportStreamSecretFinder.TOKEN_SIGNING_SECRET_NAME)
            ?: error(
                "Unable to find secret ${ReportStreamSecretFinder.TOKEN_SIGNING_SECRET_NAME}." +
                    "  If local, check if its in .vault/env/.env.local" +
                    "  If its there, first try a stop/restart docker (Sorry!)." +
                    "  If not there, maybe init.sh didn't run properly."
            )
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }
}

/**
 * Convenience function to generate a key to be used as a ReportStream secret
 */
fun main() {
    println("Put this env var in your docker compose file:")
    println(
        ReportStreamSecretFinder.TOKEN_SIGNING_SECRET_NAME + "=" +
            ReportStreamSecretFinder.generateSecret()
    )
}

/**
 * Return a ReportStream secret, used by ReportStream to sign a short-lived token
 * This stores a secret in static memory.  For local use.
 */
class GetInMemorySecret : ReportStreamSecretFinder {
    private var tokenSigningSecret: String? = null

    override fun getReportStreamTokenSigningSecret(): SecretKey {
        if (tokenSigningSecret == null) {
            tokenSigningSecret = ReportStreamSecretFinder.generateSecret()
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(tokenSigningSecret))
    }
}