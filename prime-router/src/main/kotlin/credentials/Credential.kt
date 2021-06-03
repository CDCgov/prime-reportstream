package gov.cdc.prime.router.credentials

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

// All credential classes must exist in this file to inherit from a sealed class
// https://kotlinlang.org/docs/reference/sealed-classes.html

data class UserPassCredential(val user: String, val pass: String) : Credential(), SftpCredential
data class UserPpkCredential(val user: String, val key: String, val keyPass: String) : Credential(), SftpCredential
data class UserJksCredential(val user: String, val key: String, val keyPass: String) : Credential()
data class UserPemCredential(val user: String, val key: String) : Credential()

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserPassCredential::class, name = "UserPass"),
    JsonSubTypes.Type(value = UserPpkCredential::class, name = "UserPpk"),
    JsonSubTypes.Type(value = UserJksCredential::class, name = "UserJks"),
    JsonSubTypes.Type(value = UserJksCredential::class, name = "UserPem")
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