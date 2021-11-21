package gov.cdc.prime.router.credentials

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

// All credential classes must exist in this file to inherit from a sealed class
// https://kotlinlang.org/docs/reference/sealed-classes.html

/**
 * A simple user & password credential. Can be used for SFTP transports.
 */
data class UserPassCredential(val user: String, val pass: String) : Credential(), SftpCredential, SoapCredential

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
    JsonSubTypes.Type(value = UserJksCredential::class, name = "UserJks")
)
sealed class Credential {
    /** Converts the [Credential] class to JSON */
    fun toJSON(): String = mapper.writeValueAsString(this)

    companion object {
        private val mapper = ObjectMapper().registerModule(KotlinModule())

        /** Turns a JSON object into a [Credential] object */
        fun fromJSON(json: String?): Credential? {
            if (json == null || json.isBlank()) return null
            return mapper.readValue(json, Credential::class.java)
        }
    }
}

/** Wraps any credentials that can be used by an SFTP connection */
interface SftpCredential

/** Wraps any credentials that can be used by a SOAP connection */
interface SoapCredential