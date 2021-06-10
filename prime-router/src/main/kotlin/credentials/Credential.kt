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
data class UserPassCredential(val user: String, val pass: String) : Credential(), SftpCredential

/**
 * A PPK credential. Can be used for SFTP transports.
 */
data class UserPpkCredential(val user: String, val key: String, val keyPass: String) : Credential(), SftpCredential

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

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserPassCredential::class, name = "UserPass"),
    JsonSubTypes.Type(value = UserPpkCredential::class, name = "UserPpk"),
    JsonSubTypes.Type(value = UserJksCredential::class, name = "UserJks")
)
sealed class Credential {

    fun toJSON(): String = mapper.writeValueAsString(this)

    companion object {
        private val mapper = ObjectMapper().registerModule(KotlinModule())

        fun fromJSON(json: String?): Credential? {
            if (json == null || json.isBlank()) return null
            return mapper.readValue(json, Credential::class.java)
        }
    }
}

interface SftpCredential