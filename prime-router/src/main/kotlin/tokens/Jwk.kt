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

    companion object {

        /**
         *
         * Reads the MAX_NUM_KEY_PER_SCOPE environment variable, otherwise defaulting to 10 that dictates
         * how many keys can be linked to a scope for an organization
         *
         * @return the maximum number of keys can be configured for a scope
         */
        fun getMaximumNumberOfKeysPerScope(): Int {
            return (System.getenv("MAX_NUM_KEY_PER_SCOPE") ?: "10").toInt()
        }

        /**
         * Copy an old set of authorizations to a new set, and add one to it, if needed.
         * This whole list of lists thing is confusing:
         * The 'orig' obj, and the return val, are list of JwkSets.   And each JwkSet has a list of Jwks.
         */
        fun addJwkSet(orig: List<JwkSet>?, newScope: String, newJwk: Jwk): List<JwkSet> {
            if (orig == null) {
                return listOf(JwkSet(newScope, listOf(newJwk))) // create brand new
            }
            val newJwkSetList = mutableListOf<JwkSet>()
            var done = false
            orig.forEach {
                if (it.scope == newScope) {
                    if (it.keys.contains(newJwk)) {
                        // The orig already has this key with this scope.  Just use it.
                        newJwkSetList.add(it)
                    } else {
                        // Don't create a whole new JwkSet.   Instead, add this Jwk to existing JwkSet.
                        val newJwkList = it.keys.toMutableList()
                        newJwkList.add(newJwk)
                        newJwkSetList.add(JwkSet(newScope, newJwkList))
                    }
                    done = true
                } else {
                    newJwkSetList.add(it) // existing different scope, make sure we keep it.
                }
            }
            // If the old/new scopes didn't match, then the new scope was never added.  Add a new JwkSet now.
            if (!done) {
                newJwkSetList.add(JwkSet(newScope, listOf(newJwk)))
            }
            return newJwkSetList
        }

        /**
         *
         * Adds a new key to the passed scope if the number of keys is less than the
         * configured maximumNumberOfKeysPerScope, which is configured with
         * an environment variable MAX_NUM_KEY_PER_SCOPE and defaults to 10.
         *
         * If there are already the maximum number of keys associated with the scope, the oldest (first in the list) key
         * is dropped and the new key is added
         *
         * @param jwkSets - The current list of JwkSets
         * @param scope - The scope to add the key for
         * @param jwk - The Jwk to add to the scope
         * @return The updated organization and keys
         *
         */
        fun addKeyToScope(
            jwkSets: List<JwkSet>?,
            scope: String,
            jwk: Jwk
        ): List<JwkSet> {
            val nullSafeJwkSets = jwkSets ?: emptyList()
            val updatedJwkSet =
                ((jwkSets ?: emptyList()).find { it.scope == scope } ?: JwkSet(scope, emptyList())).let { jwkSet ->
                    {
                        if (jwkSet.keys.size >= getMaximumNumberOfKeysPerScope()) {
                            val updatedKeys = jwkSet.keys.drop(1) + listOf(jwk)
                            JwkSet(scope, updatedKeys)
                        } else {
                            JwkSet(scope, jwkSet.keys + listOf(jwk))
                        }
                    }
                }()

            return nullSafeJwkSets.filter { jwkSet -> jwkSet.scope != scope } + listOf(updatedJwkSet)
        }

        /**
         * Removes a key from an JwkSet.  If either the scope or key are found, returns the passed
         * JwkSets unmodified
         *
         * @param jwkSets - the sets to try to remove the key from
         * @param scope -  the scope that the key needs to be removed from
         * @param jwk - the jwk to remove
         * @return updated JwkSets
         *
         */
        fun removeKeyFromScope(jwkSets: List<JwkSet>, scope: String, jwk: Jwk): List<JwkSet> {

            val updatedJwkSet = jwkSets.find { it.scope == scope }.let { jwkSet ->
                {
                    if (jwkSet == null) {
                        null
                    } else {
                        JwkSet(scope, jwkSet.keys.filter { it.kid != jwk.kid })
                    }
                }
            }() ?: return jwkSets

            return jwkSets.filter { jwkSet -> jwkSet.scope != scope } + listOf(updatedJwkSet)
        }

        /**
         * Checks that the kid is unique within the JwkSet for the requested scope.  This implements the SMART on FHIR
         * spec.
         * http://hl7.org/fhir/uv/bulkdata/authorization/index.html#signature-verification:~:text=The%20identifier%20of%20the%20key%2Dpair%20used%20to%20sign%20this%20JWT.%20This%20identifier%20SHALL%20be%20unique%20within%20the%20client%27s%20JWK%20Set.
         *
         * @param jwkSets - The list of JwkSet that a key wants to be added to
         * @param scope - The scope for the key
         * @param kid - The kid for the key
         * @return Whether the key can be added
         */
        fun isValidKidForScope(jwkSets: List<JwkSet>?, scope: String, kid: String?): Boolean {
            val nullSafeJwkSets = jwkSets ?: emptyList()
            val jwkSet = nullSafeJwkSets.find { it.scope == scope }

            // The SMART on FHIR specifies that the KID must be unique within the JwkSet
            // http://hl7.org/fhir/uv/bulkdata/authorization/index.html#protocol-details
            if (jwkSet == null || jwkSet.keys.find { it.kid == kid } == null) {
                return true
            }

            return false
        }
    }
}