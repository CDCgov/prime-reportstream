package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.common.Environment
import io.jsonwebtoken.Jwts
import java.io.File
import java.net.URL
import java.security.PrivateKey
import java.util.Date
import java.util.UUID

class SenderUtils {

    companion object {
        /**
         * Generate a signed JWT, representing a request for authentication from a Sender, using a private key.
         * This is done by the Sender, not by ReportStream. This method is here for testing, and as an example.
         */
        fun generateSenderToken(
            sender: Sender,
            baseUrl: String,
            privateKey: PrivateKey,
            keyId: String,
            expirationSecondsFromNow: Int = 300,
        ): String {
            val jwsObj = Jwts.builder()
                .setHeaderParam("kid", keyId) // kid
                .setHeaderParam("typ", "JWT") // typ
                .setIssuer(sender.fullName) // iss
                .setSubject(sender.fullName) // sub
                .setAudience(baseUrl) // aud
                .setExpiration(Date(System.currentTimeMillis() + expirationSecondsFromNow * 1000)) // exp
                .setId(UUID.randomUUID().toString()) // jti
                .signWith(privateKey)
            val jws = jwsObj.compact()
            return jws
        }

        fun generateSenderUrlParameters(senderToken: String, scope: String): Map<String, String> {
            return mapOf<String, String>(
                "scope" to scope,
                "grant_type" to "client_credentials",
                "client_assertion_type" to "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "client_assertion" to senderToken,
            )
        }

        fun generateSenderUrl(environment: Environment, senderToken: String, scope: String): URL {
            return URL(
                environment.formUrl("api/token").toString() + "?" +
                    generateSenderUrlParameters(senderToken, scope)
                        .map { "${it.key}=${it.value}" }.joinToString("&")
            )
        }

        fun readPublicKeyPemFile(pemFile: File): Jwk {
            if (!pemFile.exists()) error("Cannot file file ${pemFile.absolutePath}")
            return readPublicKeyPem(pemFile.readText())
        }

        fun readPublicKeyPem(pem: String): Jwk {
            val nimbusdsJwk = JWK.parseFromPEMEncodedObjects(pem)
            val jwk = jacksonObjectMapper().readValue(nimbusdsJwk.toJSONString(), Jwk::class.java)
            // All the rest of this is sanity checks
            if (jwk.kty.isNullOrEmpty()) error("Key must have a kty keytype")
            if (nimbusdsJwk.keyType == KeyType.EC) {
                val prefix = "Cannot convert pemFile to EC Key. "
                if (jwk.d != null) error("$prefix This looks like a private key.  Key must be a public key.")
                if (jwk.x.isNullOrEmpty() || jwk.y.isNullOrEmpty())
                    error("$prefix. Key missing elliptic point (x,y) value")
                // actually generate an ECPublicKey obj, just to confirm it can be done.
                val ecPublicKey = jwk.toECPublicKey()
                if (ecPublicKey.w == null) error("$prefix.  'w' Point obj not created")
                if (ecPublicKey.algorithm != "EC") error("$prefix.  Alg is ${ecPublicKey.algorithm}.  Expecting 'EC'.")
            } else if (nimbusdsJwk.keyType == KeyType.RSA) {
                val prefix = "Cannot convert pemFile to RSA Key. "
                if (jwk.d != null) error("$prefix This looks like a private key.  Key must be a public key.")
                if (jwk.e.isNullOrEmpty()) error("$prefix. Key missing exponent (e) value")
                if (jwk.n.isNullOrEmpty()) error("$prefix. Key missing modulus (n) value")
                // actually generate an RSAPublicKey obj, just to confirm it can be done.
                val rsaPublicKey = jwk.toRSAPublicKey()
                if (rsaPublicKey.algorithm != "RSA")
                    error("$prefix. Alg is ${rsaPublicKey.algorithm}. Expecting 'RSA'.")
            } else {
                error("keyType is ${nimbusdsJwk.keyType}.  Expecting 'EC or RSA'.")
            }

            return jwk
        }

        fun readPrivateKeyPemFile(pemFile: File): PrivateKey {
            if (!pemFile.exists()) error("Cannot file file ${pemFile.absolutePath}")
            return readPrivateKeyPem(pemFile.readText())
        }

        fun readPrivateKeyPem(pem: String): PrivateKey {
            val nimbusdsJwk = JWK.parseFromPEMEncodedObjects(pem)
            val jwk = jacksonObjectMapper().readValue(nimbusdsJwk.toJSONString(), Jwk::class.java)
            // All the rest of this is sanity checks
            when (nimbusdsJwk.keyType) {
                KeyType.EC -> {
                    val prefix = "Cannot convert pemFile to EC Key. "
                    if (jwk.d == null) error("$prefix This looks like a public key.  Key must be a private key.")
                    if (jwk.x.isNullOrEmpty() || jwk.y.isNullOrEmpty())
                        error("$prefix. Key missing elliptic point (x,y) value")
                    // actually generate an ECPrivateKey obj, just to confirm it can be done.
                    val ecPrivateKey = jwk.toECPrivateKey()
                    if (ecPrivateKey.algorithm != "EC")
                        error("$prefix.  Alg is ${ecPrivateKey.algorithm}.  Expecting 'EC'.")
                    return ecPrivateKey
                }
                KeyType.RSA -> {
                    val prefix = "Cannot convert pemFile to RSA Key. "
                    if (jwk.d == null) error("$prefix This looks like a public key.  Key must be a private key.")
                    if (jwk.e.isNullOrEmpty()) error("$prefix. Key missing exponent (e) value")
                    if (jwk.n.isNullOrEmpty()) error("$prefix. Key missing modulus (n) value")
                    // actually generate an RSAPrivateKey obj, just to confirm it can be done.
                    val rsaPrivateKey = jwk.toRSAPrivateKey()
                    if (rsaPrivateKey.algorithm != "RSA")
                        error("$prefix.  Alg is ${rsaPrivateKey.algorithm}.  Expecting 'RSA'.")
                    return rsaPrivateKey
                }
                else -> {
                    error("keyType is ${nimbusdsJwk.keyType}.  Expecting 'EC or RSA'.")
                }
            }
        }
    }
}