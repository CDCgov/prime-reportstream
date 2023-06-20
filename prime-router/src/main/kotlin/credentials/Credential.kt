package gov.cdc.prime.router.credentials

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

// All credential classes must exist in this file to inherit from a sealed class
// https://kotlinlang.org/docs/reference/sealed-classes.html

/**
 * A simple user & password credential. Can be used for SFTP transports.
 */
data class UserPassCredential(val user: String, val pass: String) :
    Credential(), SftpCredential, SoapCredential, RestCredential

/**
 * A PPK credential. Can be used for SFTP transports.
 */
data class UserPpkCredential(
    val user: String,
    val key: String,
    val keyPass: String,
    val pass: String? = null,
) : Credential(), SftpCredential

/**
 * A PEM credential. Can be used for SFTP transports.
 */
data class UserPemCredential(
    val user: String,
    val key: String,
    val keyPass: String,
    val pass: String? = null,
) : Credential(), SftpCredential

/**
 * A credential that is saved in a Java Key Store (JKS)
 */
data class UserJksCredential(
    /**
     * [user] is the name of the person who writes this credential.
     */
    val user: String,

    /**
     * [jks] is the keystore data base-64 encoded
     */
    val jks: String,

    /**
     * [jksPasscode] is the passcode for the JKS
     */
    val jksPasscode: String,

    /**
     * [privateAlias] is the alias for the public/private certificate stored in the JKS.
     */
    val privateAlias: String,

    /**
     * [trustAlias] is the alias for the trust/public certificate stored in the JKS
     */
    val trustAlias: String
) : Credential()

/**
 * An API Key credential along with the user who stored it
 */
data class UserApiKeyCredential(
    /**
     * [user] is the name of the person who writes this credential.
     */
    val user: String,
    /**
     * [apiKey] is the api key
     */
    val apiKey: String
) : Credential(), RestCredential

/**
 * An Assertion credential
 */
data class UserAssertionCredential(

    /**
     * [assertion] is the api key
     */
    val assertion: String
) : Credential(), RestCredential

/**
 * The credential base class for all other credentials to inherit from
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserPassCredential::class, name = "UserPass"),
    JsonSubTypes.Type(value = UserPemCredential::class, name = "UserPem"),
    JsonSubTypes.Type(value = UserPpkCredential::class, name = "UserPpk"),
    JsonSubTypes.Type(value = UserJksCredential::class, name = "UserJks"),
    JsonSubTypes.Type(value = UserApiKeyCredential::class, name = "UserApiKey"),
    JsonSubTypes.Type(value = UserAssertionCredential::class, name = "UserAssertion"),
)
sealed class Credential {
    /** Converts the [Credential] class to JSON */
    fun toJSON(): String {
        try {
            return mapper.writeValueAsString(this)
        } catch (e: Exception) {
            throw Exception("Exception in jackson writeValueAsString: " + passwordectomy(e.message))
        }
    }

    companion object {
        private val mapper = ObjectMapper().registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )

        /** Turns a JSON object into a [Credential] object */
        fun fromJSON(json: String?): Credential? {
            if (json == null || json.isBlank()) return null
            try {
                return mapper.readValue(json, Credential::class.java)
            } catch (e: Exception) {
                throw Exception("Exception in jackson readValue: " + passwordectomy(e.message))
            }
        }

        /**
         * Since this is to be called on exceptions, this is written assuming there may be garbled data
         * in the [str] that prevent using the power of json to examine it.
         * Hence the brute force removal of string data following certain words.
         * When this thing is done, it certainly will not be valid json any more.
         */
        fun passwordectomy(str: String?): String? {
            if (str == null || str.isNullOrBlank()) return str
            // Remove data after the string "pass, including the initial quote.
            // This strangeness will catch "pass": but not "UserPass", which is helpful to keep.
            val passRemoved = replacePrefixAndFollowingChars(str, "\"pass", 20, "XXXX")
            // Remove data after the string key   This will catch "key" and "keyPass".
            return replacePrefixAndFollowingChars(passRemoved, "key", 20, "XXXX")
        }

        /**
         * Remove all occurrences of [prefix] from [input] string, and an additional [addlCharsToRemove] beyond that,
         * and replace with the string [replaceWith].
         * Useful in attempting to removing passwords and other secrets from the [input] string.
         * Search for [prefix] is case insensitive.
         */
        fun replacePrefixAndFollowingChars(
            input: String,
            prefix: String,
            addlCharsToRemove: Int,
            replaceWith: String
        ): String {
            if (input.isEmpty() || prefix.isEmpty()) return input
            if (replaceWith.contains(prefix)) error("Infinite loop")
            val tmp = StringBuilder(input)
            while (true) {
                // Work from the back to the front, to avoid erasing the prefix but not the secret.
                val index = tmp.lastIndexOf(prefix, ignoreCase = true)
                if (index < 0)
                    break
                tmp.replace(index, index + prefix.length + addlCharsToRemove, replaceWith)
            }
            return tmp.toString()
        }
    }
}

/** Wraps any credentials that can be used by an SFTP connection */
interface SftpCredential

/** Wraps any credentials that can be used by a SOAP connection */
interface SoapCredential

/** Wraps any credentials that can be used by an REST connection */
interface RestCredential