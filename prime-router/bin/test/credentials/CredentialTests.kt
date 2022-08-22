package gov.cdc.prime.router.credentials

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
    fun `test replacePrefixAndFollowingChars`() {
        assertThat(
            Credential.replacePrefixAndFollowingChars(
                "", "", 10, ""
            )
        ).isEqualTo("")
        assertThat(
            Credential.replacePrefixAndFollowingChars(
                "foo", "bar", 10, "quux"
            )
        ).isEqualTo("foo")
        assertThat(
            Credential.replacePrefixAndFollowingChars(
                "When in the course of human events", "en", 3, "XXX"
            )
        ).isEqualTo("WhXXX the course of human evXXX")
        assertThat(
            Credential.replacePrefixAndFollowingChars(
                "foo", "o", 10, ""
            )
        ).isEqualTo("f")
        assertThat(
            Credential.replacePrefixAndFollowingChars(
                "abcde", "c", 0, "XXX"
            )
        ).isEqualTo("abXXXde")
        assertThat(
            Credential.replacePrefixAndFollowingChars(
                // Remember it does search and destroy from the end, backwards
                "abcabcabcxxxxx", "b", 3, ""
            )
        ).isEqualTo("ax")
    }

    @Test
    fun `test passwordectomy`() {
        assertThat(Credential.passwordectomy("\"pass 2345678901234567890 key xxx")).isEqualTo("XXXX XXXX")
        assertThat(Credential.passwordectomy("and it came to \"pass\" in those days that a decree went out"))
            .isEqualTo("and it came to XXXX a decree went out")
        assertThat(
            Credential.passwordectomy(
                "a\"PASS 2345678901234567890b" +
                    "ckey 2345678901234567890d" +
                    "e\"Pass\"2345678901234567890f"
            )
        ).isEqualTo("aXXXXbcXXXXdeXXXXf")
        val json = "{\"@type\":\"UserPass\",\"user\":\"Mo\",\"pass\":\"supersecretpassword\"}"
        assertThat(Credential.passwordectomy(json)).isEqualTo("{\"@type\":\"UserPass\",\"user\":\"Mo\",XXXXrd\"}")
    }

    @Test
    fun `test deserialization errors`() {
        // Note:  I could not think of a straightforward way to force an exception
        // in credential.toJSON(), so that exception path is currently untested.

        // badJson has a missing " at the end
        val badJson = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"superdupersecret}"
        val err = assertFailsWith<Exception> {
            Credential.fromJSON(badJson)
        }
        // println(err.message)
        assertThat(err.message?.indexOf("pass")).isEqualTo(-1)
        assertThat { err.message?.contains("XXXX") }
        val longStr = "abcdefghijklmnopqrstuvwxyz"
        val badJson2 = """
            "@type":"UserPpk","user":"$longStr","pass":"$longStr","key":"$longStr","keyPass":"$longStr"
        """.trimIndent()
        val err2 = assertFailsWith<Exception> {
            Credential.fromJSON(badJson2)
        }
        // println(err2.message)
        assertThat(err2.message?.indexOf("pass", ignoreCase = true)).isEqualTo(-1)
        assertThat(err2.message?.indexOf("key", ignoreCase = true)).isEqualTo(-1)
        assertThat(err2.message?.count { it == 'X' }).isEqualTo(12)
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