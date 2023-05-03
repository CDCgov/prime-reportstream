package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import gov.cdc.prime.router.Organization
import io.jsonwebtoken.Jwts
import java.io.File
import java.security.PrivateKey
import java.util.Date
import java.util.UUID

class AuthUtils {

    companion object {

        /**
         * Generate a signed JWT, representing a request for authentication from a Organization, using a private key.
         * This is done by the Organization, not by ReportStream. This method is here for testing, and as an example.
         * @param issuer -  the issuer for the JWT
         * @param baseUrl - the audience
         * @param privateKey - the private key to sign the JWT with
         * @param keyId - the unique identifier for the registered public key
         * @param expirationSecondsFromNow - when the JWT should expire
         * @param jti - unique identifier for this JWT
         * @return a signed JWT
         */
        fun generateToken(
            issuer: String,
            baseUrl: String,
            privateKey: PrivateKey,
            keyId: String,
            jti: String?,
            expirationSecondsFromNow: Int = 300,
        ): String {
            val jwsObj = Jwts.builder()
                .setHeaderParam("kid", keyId) // kid
                .setHeaderParam("typ", "JWT") // typ
                .setIssuer(issuer) // iss
                .setSubject(issuer) // sub
                .setAudience(baseUrl) // aud
                .setExpiration(Date(System.currentTimeMillis() + expirationSecondsFromNow * 1000)) // exp
                .signWith(privateKey)
            if (jti != null) {
                jwsObj.setId(jti) // jti
            }
            val jws = jwsObj.compact()
            return jws
        }

        /**
         * Generate a signed JWT, representing a request for authentication from a Organization, using a private key.
         * This is done by the Organization, not by ReportStream. This method is here for testing, and as an example.
         *
         * @param organization -  the issuer for the JWT
         * @param baseUrl - the audience
         * @param privateKey - the private key to sign the JWT with
         * @param keyId - the unique identifier for the registered public key
         * @param expirationSecondsFromNow - when the JWT should expire
         * @param jti - unique identifier for this JWT
         * @return a signed JWT
         */
        fun generateOrganizationToken(
            organization: Organization,
            baseUrl: String,
            privateKey: PrivateKey,
            keyId: String,
            expirationSecondsFromNow: Int = 300,
            jti: String? = UUID.randomUUID().toString()
        ): String {
            return generateToken(organization.name, baseUrl, privateKey, keyId, jti, expirationSecondsFromNow)
        }

        /**
         * [organizationToken] is a signed JWT from this organization, to go to the api/token endpoint.
         * [scope] is the desired scope being requested.   See [Scope] for details on format.
         * @return a map of the standard parameters needed to create an acceptable token request.
         */
        private fun generateOrganizationUrlParameterMap(organizationToken: String, scope: String): Map<String, String> {
            return mapOf<String, String>(
                "scope" to scope,
                "grant_type" to "client_credentials",
                "client_assertion_type" to "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "client_assertion" to organizationToken,
            )
        }

        /**
         * [organizationToken] is a signed JWT from this organization, to go to the api/token endpoint.
         * [scope] is the desired scope being requested.   See [Scope] for details on format.
         * @return a string of the standard parameters needed to create an acceptable token request.
         */
        fun generateOrganizationUrlParameterString(organizationToken: String, scope: String): String {
            return generateOrganizationUrlParameterMap(organizationToken, scope)
                .map { "${it.key}=${it.value}" }.joinToString("&")
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