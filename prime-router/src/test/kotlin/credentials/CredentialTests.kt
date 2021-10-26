package gov.cdc.prime.router.credentials

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.util.UUID
import kotlin.test.Test

internal class CredentialTests {
    private val user = UUID.randomUUID()
    private val key = UUID.randomUUID()
    private val pass = UUID.randomUUID()
    private val keyPass = UUID.randomUUID()

    private val ppkCredentialWithPass = """
        {"@type":"UserPpk","user":"$user","pass":"$pass","key":"$key","keyPass":"$keyPass"}
    """.trimIndent()
    private val ppkCredentialWithoutPass = """
        {"@type":"UserPpk","user":"$user","key":"$key","keyPass":"$keyPass"}
    """.trimIndent()
    private val pemCredentialWithPass = """
        {"@type":"UserPem","user":"$user","pass":"$pass","key":"$key","keyPass":"$keyPass"}
    """.trimIndent()
    private val pemCredentialWithoutPass = """
        {"@type":"UserPem","user":"$user","key":"$key","keyPass":"$keyPass"}
    """.trimIndent()

    @Test
    fun `test polymorphic serialization of UserPassCredential`() {
        val credential = UserPassCredential("user", "pass")
        val expectedJson = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"pass\"}"
        assertThat(credential.toJSON()).isEqualTo(expectedJson)
    }

    @Test
    fun `test polymorphic deserialization of UserPassCredential`() {
        val json = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"pass\"}"
        val credential = Credential.fromJSON(json)
        @Suppress("USELESS_IS_CHECK")
        assertThat(credential is UserPassCredential).isTrue() // Gives off warning message: instance is always 'true'
        // Needed to gain access to credential.user and credential.pass
        if (credential is UserPassCredential) {
            assertThat(credential.user).isEqualTo("user")
            assertThat(credential.pass).isEqualTo("pass")
        }
    }

    @Test
    fun `test deserialization of key credentials`() {
        Credential.fromJSON(ppkCredentialWithPass).run {
            assertThat(this is UserPpkCredential).isTrue()
            if (this is UserPpkCredential) {
                assertThat(this.user).isEqualTo(this.user)
                assertThat(this.pass).isEqualTo(this.pass)
                assertThat(this.keyPass).isEqualTo(this.keyPass)
                assertThat(this.key).isEqualTo(this.key)
            }
        }

        Credential.fromJSON(ppkCredentialWithoutPass).run {
            assertThat(this is UserPpkCredential).isTrue()
            if (this is UserPpkCredential) {
                assertThat(this.user).isEqualTo(this.user)
                assertThat(this.pass).isNull()
                assertThat(this.keyPass).isEqualTo(this.keyPass)
                assertThat(this.key).isEqualTo(this.key)
            }
        }

        Credential.fromJSON(pemCredentialWithPass).run {
            assertThat(this is UserPemCredential).isTrue()
            if (this is UserPemCredential) {
                assertThat(this.user).isEqualTo(this.user)
                assertThat(this.pass).isEqualTo(this.pass)
                assertThat(this.keyPass).isEqualTo(this.keyPass)
                assertThat(this.key).isEqualTo(this.key)
            }
        }

        Credential.fromJSON(pemCredentialWithoutPass).run {
            assertThat(this is UserPemCredential).isTrue()
            if (this is UserPemCredential) {
                assertThat(this.user).isEqualTo(this.user)
                assertThat(this.pass).isNull()
                assertThat(this.keyPass).isEqualTo(keyPass)
                assertThat(this.key).isEqualTo(key)
            }
        }
    }
}