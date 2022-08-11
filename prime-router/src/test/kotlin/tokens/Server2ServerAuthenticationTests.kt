package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.util.decodeBase64
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Sender
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.BeforeEach
import java.math.BigInteger
import java.security.Key
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

val exampleRsaPrivateKeyStr = """
    {
        "p": "7IICXiUlkefia-wDH9asy9Tbbinjr01HQmD-2uEz6EHEShu4ZwMkH6DmK-3AAfiazdjO9Nkl-aZQH9lsuI5FVpFzCIF4ljkJDpCJCXlARXdsLpO2mnZsIQ7ZF26evFT0hhG-C2WQ2ESb1vs0aW6Y6-x3K7FU6jQZEdRibDxSr3s",
        "kty":"RSA",
        "q":"1Mf_IShokWL0STqB8NBi-4ajcacmflXFMro8mhf2BTNoHXkpO_i9oZfX4ll6t_uVCEERhevUS93kxsV8DQI4vfi3915GZ5EH1sBGrrxsC8CDwVZQgNeCszLx51rXJhqFb4UdKxzQIATZCKAEqzHV2A9kiJ_aHGNVooBUYg-li9k",
        "d":"XIhGKq42y5XXDqBr8JARwN_ImLHs2568MjaMDp-Zj_ugvlPfxvCTE0ZwPbX53iIyVyQw9hbPJt1p3qB5b5WTmICv3WJjgWrRgRqtchuvavOUeC-W4zstzzvFKBfLKS-zJ23Zd07lXGCDZWyMZ30e9t1hxWJDpsr1agTpyYdHcw1yq2r-gLvllePEqgQCJG_s3wropaWklRza_ar6eVqqBaWmIch86saeOh0yw6pCMO_0TQg9PJIZ7C71j1yYytxQnjUpb5ugLFtaq9jax2y7dGDbUeAjxxmhw23IrV4qFNyH-1XNBL2MYlCGhC2BL7L6LGYnEBmuBxEsVkBuWHNE8Q",
        "e":"AQAB",
        "kid":"8711e745-77e6-49af-bde3-65dc61fc0706",
        "qi":"2zoetnmGAOWT3N8hIXIpZwETEhx4yllO_og_hI4u0gX3vxizHPu2nNxskBVZ4dE4I_31pHDwrqB5vASnl4tmBaeYvh-F0sTZVzgfecySD67It5zUY9wg2oVkfl_7iNIVJx7OxALZxXeGmHupsbzFzQ79hmgKrG5ohvI8cF_nqKs",
        "dp":"G3DqPWop9cfl6Ye0xRjva6cC3sFVfZ2Fyxnd-B9xGl2nHMIinzEqG9FbY3VudcwWihPBz37yfQji-w8LIk6_lM_DfRUonKV5e4shm0vKPUUh9DWHVlyvJxbF8YYQPOHOjU-5sTDToYQ0YLk8147Rh24kVZl5tMLetcbitJQ7M8k",
        "dq":"cEglOkMPgwC7tdS48vGT-fSfVP8GUg5CpDUge5P_T9lDrKHd_3aP4rC0zA25s1J_3z4u2AONIIe0DKvzfQ3aEW0o7tEBx-8BOvJ1mgl13nG1VRWOH58ZqiRNAG-wLrw6A5IzxSdMMEk-mc2PCSOgG4Zr36iyuN42Ny0O2jw1eGk",
        "n":"xJRuufBk_axjyO1Kpy5uwmnAY0VUhCzG8G4OiDVgnaXeLMzj91bcQdYOMQ_82PTGrUbck3qSFXbug_Ljj8NZDT0J1ZSKv8Oce-GdkeNzA5W9yvChvorGotAUWMS7_EXXxz8mjlrwu6kyKfCpuJAMg5VrZaYA0nAlv-e7zoRE9pQ0VHNrEaj6Uikw3M02oVHUNiRtL5Y5tYyz_yRBauVLPdHf5Yf-cZeO2x02qFSGcl_2EzWZcGb6PkQZ_9QeOq_iJ9h-NU_wb9lnbebnBhPGAyc1-_9vnFlFzkH2lt0BVtfhW0E4ieKkntbC0QFxNu91Gf4jfFmsOAsCf3UpVqWIQw"
    }
""".trimIndent()

// corresponding public key to above.
val exampleKeyId = "11209921-860e-4b6d-8d7e-adc8778e1c6c"
val exampleRsaPublicKeyStr = """
        {
          "kty":"RSA",
          "kid":"$exampleKeyId",
          "n":"xJRuufBk_axjyO1Kpy5uwmnAY0VUhCzG8G4OiDVgnaXeLMzj91bcQdYOMQ_82PTGrUbck3qSFXbug_Ljj8NZDT0J1ZSKv8Oce-GdkeNzA5W9yvChvorGotAUWMS7_EXXxz8mjlrwu6kyKfCpuJAMg5VrZaYA0nAlv-e7zoRE9pQ0VHNrEaj6Uikw3M02oVHUNiRtL5Y5tYyz_yRBauVLPdHf5Yf-cZeO2x02qFSGcl_2EzWZcGb6PkQZ_9QeOq_iJ9h-NU_wb9lnbebnBhPGAyc1-_9vnFlFzkH2lt0BVtfhW0E4ieKkntbC0QFxNu91Gf4jfFmsOAsCf3UpVqWIQw",
          "e":"AQAB"
        }
""".trimIndent()

val differentRsaPublicKeyStr = """
    {
        "kty":"RSA",
        "kid":"$exampleKeyId",
        "alg":"RS256",
        "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
        "e":"AQAB"
    }
""".trimIndent()

class Server2ServerAuthenticationTests {

    private val sender = CovidSender(
        "foo",
        "bar",
        Sender.Format.CSV,
        CustomerStatus.INACTIVE,
        "mySchema",
        keys = null
    )

    private val server2ServerAuthentication = Server2ServerAuthentication()
    private val jtiCache = MemoryJtiCache()

    // return the hardcoded public key used with this test.  This is the Sender's public key.
    class UseTestKey(private val rsaPublicKeyStr: String) : SigningKeyResolverAdapter() {
        override fun resolveSigningKey(jwsHeader: JwsHeader<*>?, claims: Claims): Key {
            val jwk = jacksonObjectMapper().readValue(rsaPublicKeyStr, Jwk::class.java)
            return jwk.toRSAPublicKey()
        }
    }

    // return a ReportStream secret, used by ReportStream to sign a short-lived token
    class GetTestSecret : ReportStreamSecretFinder {
        private val TOKEN_SIGNING_KEY_ALGORITHM = SignatureAlgorithm.HS384

        // Good for testing:  Each time you create a new GetTestSecret() obj, its a totally new secret.
        private val tokenSigningSecret = this.generateSecret()

        override fun getReportStreamTokenSigningSecret(): SecretKey {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(tokenSigningSecret))
        }

        private fun generateSecret(): String {
            return Encoders.BASE64.encode(Keys.secretKeyFor(TOKEN_SIGNING_KEY_ALGORITHM).encoded)
        }
    }

    @BeforeEach
    fun reset() {
        clearAllMocks() // If using Companion object mocks, you need to be sure to clear between tests
    }

    @Test
    fun `test reading in Keys`() {
        // This really is a test of Jwk.kt, but I need the key pair here, and didn't
        // feel like keeping two copies of it.
        //
        // 1. Public
        // convert to our obj in two steps, to simulate reading from our db, into our Jwk obj first.
        val jwk = jacksonObjectMapper().readValue(exampleRsaPublicKeyStr, Jwk::class.java)
        val rsaPublicKey = jwk.toRSAPublicKey()
        assertNotNull(rsaPublicKey)
        assertEquals("RSA", rsaPublicKey.algorithm)
        assertEquals(BigInteger(jwk.e?.decodeBase64()), rsaPublicKey.publicExponent)

        // 2. Private.  Again, two step conversion instead of calling Jwk.generateRSAPublicKey() directly
        // to be more like 'real life' where the data is coming from json in our Settings table, into Jwk obj.
        val jwk2 = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java)
        val rsaPrivateKey = jwk2.toRSAPrivateKey()
        assertNotNull(rsaPrivateKey)
        assertEquals("RSA", rsaPrivateKey.algorithm)
        assertEquals(BigInteger(jwk2.d?.decodeBase64()), rsaPrivateKey.privateExponent)
    }

    @Test
    fun `test SenderUtils generateSenderToken`() {
        // Want to just keep one copy of my example keys, so I'm testing SenderUtils.generateSenderToken here.
        val jwk2 = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java)
        val rsaPrivateKey = jwk2.toRSAPrivateKey()
        val senderToken = SenderUtils.generateSenderToken(
            sender,
            "http://asdf",
            rsaPrivateKey,
            exampleKeyId
        )
        // Must be a valid JWT
        assertEquals(3, senderToken.split(".").size)

        // Check that the public key correctly validates.
        assertTrue(
            Server2ServerAuthentication().checkSenderToken(senderToken, UseTestKey(exampleRsaPublicKeyStr), jtiCache)
        )
    }

    @Test
    fun `test createAccessToken, authenticate, authorize happy path`() {
        val rslookup = GetTestSecret() // callback to look up the Reportstream secret, using to sign token.
        val token = server2ServerAuthentication.createAccessToken("*.*.primeadmin", rslookup)
        assertTrue(token.accessToken.isNotEmpty())
        // must expire later than now, but in less than 10 minutes
        val now: Int = (System.currentTimeMillis() / 1000).toInt()
        assertTrue(token.expiresAtSeconds >= now)
        assertTrue(token.expiresAtSeconds < now + 600)
        assertEquals("*.*.primeadmin", token.scope)
        assertEquals("bearer", token.tokenType)
        assertTrue(token.sub.startsWith("*.*.primeadmin_"))

        mockkObject(AuthenticatedClaims.Companion)
        every { AuthenticatedClaims.isLocal(any()) } returns false
        // Now read the token back in, and confirm its valid.
        val claims = server2ServerAuthentication.authenticate(token.accessToken, rslookup)
        // if claims is non-null then the sender's accessToken is valid.
        assertNotNull(claims)
        if (! claims.authorized(setOf("*.*.primeadmin")))
            error("Authorization failed")
        // if claims is non-null then the sender's accessToken is valid.
        assertEquals(token.expiresAtSeconds, claims.jwtClaims["exp"])
        assertTrue(claims.scopes.contains("*.*.primeadmin"))
        assertEquals(token.sub, claims.userName)
    }

    /*
    * I thought it would be nice to show an end-to-end flow, as a happy path example.
    * This is a repeat of all the above tests, but all in one place.
    *
    * Vocabulary note:
     * There are two different tokens, which we're calling the SenderToken and the AccessToken.
     * Upon validation of a SenderToken, ReportStream will return a short duration AccessToken, which can be used
     * to call one of our endpoints, assuming a valid 'scope' for that endpoint.
     *
     * The details:
     * Step 1:  The Sender signs a SenderToken using their private key.
     *          Implemented by generateSenderToken()
     * Step 2:  ReportStream checks the SenderToken using the corresponding public key it has in Settings.
     *          Implemented by checkSenderToken()
     * Step 3:  If validated, ReportStream returns a short-duration AccessToken, signed by the TokenSigningSecret
     *          Implemented by createAccessToken()
     * Step 4:  The Sender uses the AccessToken to make a request to a ReportStream endpoint.
     *
     * Step 5:  The called ReportStream endpoint checks the validity of the AccessToken signature, etc.
                Implemented by checkAccessToken()
     */
    @Test
    fun `end to end happy path -- sender reqs token, rs authorizes, sender uses token, rs authorizes`() {
        val baseUrl = "http://localhost:7071/api/token"
        val privateKey = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java).toRSAPrivateKey()
        // Step 1 - on the Sender side
        val senderToken = SenderUtils.generateSenderToken(sender, baseUrl, privateKey, exampleKeyId)

        // Step 2: ReportStream gets the token and checks it.
        val rslookup = GetTestSecret() // callback to look up the Reportstream secret, using to sign RS token.
        val senderLookup = UseTestKey(exampleRsaPublicKeyStr) // callback to lookup the sender's public key.
        val accessToken = if (Server2ServerAuthentication().checkSenderToken(senderToken, senderLookup, jtiCache)) {
            // Step 3:  Report stream creates a new accessToken
            server2ServerAuthentication.createAccessToken("org.sender.admin", rslookup)
        } else error("Unauthorized connection")

        // Step 4: Now pretend the sender has used the accessToken to make a request to reportstream...

        // Step 5: ... and ReportStream checks it:
        val claims = server2ServerAuthentication.authenticate(accessToken.accessToken, rslookup)
        // if claims is non-null then the sender's accessToken is valid.
        assertNotNull(claims)

        if (! claims.authorized(setOf("org.sender.admin")))
            error("Authorization failed")
        assertEquals(accessToken.expiresAtSeconds, claims.jwtClaims["exp"])
        assertEquals("org.sender.admin", claims.jwtClaims["scope"])
    }

    @Test
    fun `test mismatched Sender key`() {
        val privateKey = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java).toRSAPrivateKey()
        val senderToken = SenderUtils.generateSenderToken(sender, "http://baz.quux", privateKey, exampleKeyId)

        val senderLookup = UseTestKey(differentRsaPublicKeyStr) // Its the wrong trousers!
        // false means we failed to validate the sender's jwt.
        assertFalse(Server2ServerAuthentication().checkSenderToken(senderToken, senderLookup, jtiCache))
    }

    @Test
    fun `test junk Sender key`() {
        val privateKey = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java).toRSAPrivateKey()
        val senderToken = SenderUtils.generateSenderToken(sender, "http://baz.quux", privateKey, exampleKeyId)

        val junkPublicKeyStr = """
            {
              "kty":"RSA",
              "kid":"$exampleKeyId",
              "n":"xJUNKRuufBk_axjyO1Kpy5uwmnAY0VUhCzG8G4OiDVgnaXeLMzj91bcQdYOMQ_82PTGrUbck3qSFXbug_Ljj8NZDT0J1ZSKv8Oce-GdkeNzA5W9yvChvorGotAUWMS7_EXXxz8mjlrwu6kyKfCpuJAMg5VrZaYA0nAlv-e7zoRE9pQ0VHNrEaj6Uikw3M02oVHUNiRtL5Y5tYyz_yRBauVLPdHf5Yf-cZeO2x02qFSGcl_2EzWZcGb6PkQZ_9QeOq_iJ9h-NU_wb9lnbebnBhPGAyc1-_9vnFlFzkH2lt0BVtfhW0E4ieKkntbC0QFxNu91Gf4jfFmsOAsCf3UpVqWIQw",
              "e":"AQAB"
            }
        """.trimIndent()
        val senderLookup = UseTestKey(junkPublicKeyStr) // Its the wrong trousers!
        // false means we failed to validate the sender's jwt.
        assertFalse(Server2ServerAuthentication().checkSenderToken(senderToken, senderLookup, jtiCache))
    }

    @Test
    fun `test expired Sender key`() {
        val privateKey = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java).toRSAPrivateKey()
        val senderToken = SenderUtils.generateSenderToken(
            sender,
            "http://baz.quux",
            privateKey,
            exampleKeyId,
            -65
        ) // expires in the past.  Need to back past the clock skew

        val senderLookup = UseTestKey(exampleRsaPublicKeyStr) // Its the wrong trousers!
        // false means we failed to validate the sender's jwt.
        assertFalse(Server2ServerAuthentication().checkSenderToken(senderToken, senderLookup, jtiCache))
    }

    @Test
    fun `test previously used Sender token`() {
        val privateKey = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java).toRSAPrivateKey()
        val senderToken = SenderUtils.generateSenderToken(
            sender,
            "http://baz.quux",
            privateKey,
            exampleKeyId
        )

        val senderLookup = UseTestKey(exampleRsaPublicKeyStr) // Its the wrong trousers!
        // It should work the first time.
        assertTrue(Server2ServerAuthentication().checkSenderToken(senderToken, senderLookup, jtiCache))
        // Then fail the second time
        assertFalse(Server2ServerAuthentication().checkSenderToken(senderToken, senderLookup, jtiCache))
    }

    @Test
    fun `test MemoryJtiCache`() {
        val jtiCache = MemoryJtiCache()
        val uuid1 = UUID.randomUUID().toString()
        val exp1 = OffsetDateTime.now().plusSeconds(300)
        // First time it works
        assertTrue(jtiCache.isJTIOk(uuid1, exp1))
        // Second time it fails
        assertFalse(jtiCache.isJTIOk(uuid1, exp1))

        val uuid2 = UUID.randomUUID().toString()
        // Very short expiration -
        val exp2 = OffsetDateTime.now().plusSeconds(1)
        // First time it works
        assertTrue(jtiCache.isJTIOk(uuid2, exp2))
        Thread.sleep(2 * 1000)
        // Second time it fails, even if the original expired, due to the min timeout feature
        val exp2_1 = OffsetDateTime.now().plusSeconds(300)
        assertFalse(jtiCache.isJTIOk(uuid2, exp2_1))
    }

    @Test
    fun `test isExpiredToken`() {
        val exp1 = Date(System.currentTimeMillis() - 1)
        assertTrue(Server2ServerAuthentication.isExpiredToken(exp1))

        val exp2 = Date(System.currentTimeMillis() + 1000)
        assertFalse(Server2ServerAuthentication.isExpiredToken(exp2))
    }

    @Test
    fun `test checkAccessToken happy path`() {
        val rslookup = GetTestSecret() // callback to look up the Reportstream secret, using to sign RS token.
        val accessToken = server2ServerAuthentication.createAccessToken("a.b.report", rslookup)
        val claims = server2ServerAuthentication.authenticate(accessToken.accessToken, rslookup)
        assertNotNull(claims)
        if (! claims.authorized(setOf("a.b.report")))
            error("Claims not authorized")
        assertEquals(accessToken.expiresAtSeconds, claims.jwtClaims["exp"])
        assertTrue(claims.scopes.contains("a.b.report"))
    }

    @Test
    fun `test empty scope to createAccessToken`() {
        val rslookup = GetTestSecret()
        assertFails { server2ServerAuthentication.createAccessToken("", rslookup) }
        assertFails { server2ServerAuthentication.createAccessToken(" ", rslookup) }
    }

    @Test
    fun `test checkAccessToken wrong reportstream secret`() {
        val rslookup1 = GetTestSecret()
        val accessToken = server2ServerAuthentication.createAccessToken("MyScope", rslookup1)
        val rslookup2 = GetTestSecret() // new/different secret
        val claims = server2ServerAuthentication.authenticate(accessToken.accessToken, rslookup2)
        // if claims is non-null then the sender's accessToken is valid.
        assertNull(claims)
    }
}