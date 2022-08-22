package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.RSAKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * I could not find a good library that would easily let us parse and create Jwk data
 * in both JSON and Yaml.  (Yes, a yaml jwk is an oxymoron)
 *
 * A.  com.nimbusds.jose.jwk.JWKSet does not deserialize and serialize properly to/from JSON.
 * When you serialize, it puts the important values down inside a generic 'requiredParams' field.
 * Then when you later attempt to deserialize it back into a JWKSet, it fails.
 *
 * ECPublicKey does NOT track the kid (keyId), which is a requirement for FHIR Auth, since we only use
 * keys that match the kid in the sender's signed JWT.
 *
 * And you can't create an ECPublicKey directly from a JWT object - you have to re-create the JSON string,
 * then parse that back in!
 *
 * However, nimbusds has two features that redeem it somewhat:
 * 1) It has "usds" in its name
 * 2) Its ECKey and RSAKey classes know how to parse PEM files, into its ECPublicKey or RSAPublicKey objects
 *
 * JJWT does not have good support for creating JWKs from raw JSON.
 *
 * mime type application/jwk+json
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Jwk(
    val kty: String, // Key type (Alg family).  eg, RSA, EC, oct
    val use: String? = null, // Intended use.  eg, sig, enc.
    val keyOps: String? = null, // key_ops: Intended use operations.  eg, sign, verify, encrypt
    val alg: String? = null,
    var kid: String? = null, // key Id
    val x5u: String? = null, // URI ref to certificate
    val x5c: List<String>? = null, // PKIX certificates. JSON array of String
    val x5t: String? = null, // certificate thumbprint
    // Algorithm specific fields
    val n: String? = null, // RSA
    val e: String? = null, // RSA
    val d: String? = null, // EC and RSA private
    val crv: String? = null, // EC
    val p: String? = null, // RSA private
    val q: String? = null, // RSA private
    val dp: String? = null, // RSA private
    val dq: String? = null, // RSA private
    val qi: String? = null, // RSA private
    val x: String? = null, // EC
    val y: String? = null, // EC
    val k: String? = null, // symmetric key, eg oct
) {

    fun toECPublicKey(): ECPublicKey {
        if (kty != "EC") error("Cannot convert key type $kty to ECPublicKey")
        return generateECPublicKey(jacksonObjectMapper().writeValueAsString(this))
    }

    fun toECPrivateKey(): ECPrivateKey {
        if (kty != "EC") error("Cannot convert key type $kty to ECPrivateKey")
        return generateECPrivateKey(jacksonObjectMapper().writeValueAsString(this))
    }

    fun toRSAPublicKey(): RSAPublicKey {
        if (kty != "RSA") error("Cannot convert key type $kty to RSAPublicKey")
        return generateRSAPublicKey(jacksonObjectMapper().writeValueAsString(this))
    }

    fun toRSAPrivateKey(): RSAPrivateKey {
        if (kty != "RSA") error("Cannot convert key type $kty to RSAPrivateKey")
        return generateRSAPrivateKey(jacksonObjectMapper().writeValueAsString(this))
    }

    companion object {
        fun generateECPublicKey(jwkString: String): ECPublicKey {
            return ECKey.parse(jwkString).toECPublicKey()
        }

        fun generateECPrivateKey(jwkString: String): ECPrivateKey {
            return ECKey.parse(jwkString).toECPrivateKey()
        }

        fun generateRSAPublicKey(jwkString: String): RSAPublicKey {
            return RSAKey.parse(jwkString).toRSAPublicKey()
        }

        fun generateRSAPrivateKey(jwkString: String): RSAPrivateKey {
            return RSAKey.parse(jwkString).toRSAPrivateKey()
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JwkSet(
    // A scope is a space separated list of allowed scopes, per OpenID.
    // The official JWK Set spec only calls for 'keys', but custom fields like this are allowed.
    // Syntax for scopes:
    // System scopes have the format system/(:resourceType|*).(read|write|*),
    // which conveys the same access scope as the matching user format user/(:resourceType|*).(read|write|*).
    val scope: String,
    // Each scope has a list of keys associated with it.  Having a list of keys allows for
    // overlapping key rotation
    val keys: List<Jwk>
) {
    fun filterByKid(kid: String): List<Jwk> {
        return keys.filter { !it.kid.isNullOrEmpty() && kid == it.kid }
    }
}