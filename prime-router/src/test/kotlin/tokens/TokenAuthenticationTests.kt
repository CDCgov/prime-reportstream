package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.util.decodeBase64
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.*
import gov.cdc.prime.router.cli.tests.CoolTest
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SigningKeyResolverAdapter
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import java.math.BigInteger
import java.security.Key
import java.time.OffsetDateTime
import java.util.*
import javax.crypto.SecretKey
import kotlin.test.*

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

val end2EndExampleECPublicKeyStr = """
-----BEGIN PUBLIC KEY-----
MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE78eOugxhQPd/tUKOhsfcZ04bp/xgL2ku
JN6ZrNgWv6qZXHXqoqVKVXlzO/Q9NXdnZo7eBcyQpAarTANsPKB95xT69Ue/cCyp
1DBmTRk3nJBBhF6XZkT+AaYaXmGhPNWG
-----END PUBLIC KEY-----
""".trimIndent()

val end2EndExampleECPrivateKeyStr = """
-----BEGIN EC PRIVATE KEY-----
MIGkAgEBBDDkoWxCGOmihAVm/LXwR9HTf5T2Exh1VAf0yLkdgrPGrchvS7Mxj3T5
QtsvMxyppw+gBwYFK4EEACKhZANiAATvx466DGFA93+1Qo6Gx9xnThun/GAvaS4k
3pms2Ba/qplcdeqipUpVeXM79D01d2dmjt4FzJCkBqtMA2w8oH3nFPr1R79wLKnU
MGZNGTeckEGEXpdmRP4BphpeYaE81YY=
-----END EC PRIVATE KEY-----
""".trimIndent()

val end2EndExampleECPrivateInvalidKeyStr = """
-----BEGIN EC PRIVATE KEY-----
MIGkAgEBBDBQR+wve3RP3tql7U4SW/D9F55dPSqAlUSdZaAnTDhnAfPB+OFFtibp
9QSe2z3MLHegBwYFK4EEACKhZANiAAT+LmhSlMb0CV/+e+y/tVYmDcY/wP+9xE3a
5OI+jtP7sDKee2MtMf5V5DHsw7alm8IeOAr86tMwOWw8WTq2c+i7IRdAFLGVoUPA
P1wSycc45JCHVjiqRPoluf33W9ObVbQ=
-----END EC PRIVATE KEY-----
""".trimIndent()

val end2EndExampleRSAPublicKeyStr = """
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu5aC+itz1IXrtbCow40B
kF1A5NZFk8/bHadxvPSZGJfvzNwZS4MHf99nFAEq8JNIkbL8cls972JjqqvofIzw
zTSMtdnBBoci3BCaOguRRC0r5gOykOsq2kID8o0eH4ntnWrmnjr3VJ0FZ5WLq3pB
dlauXp+tUaCTnsIfrQvM3OuBabzZXIxTYGyqW5qGkjXP9u8ysqwNuTzr4WzKKbZB
oMG4MG8xTklLKZ1kM1tflMtdALl+5jqdb96hIt+3C/WvWXw/hxRXBNie1og33gpk
5iNBCIDgB4WATHYfXQgk0LWlFv06GL1M1BpnRvlXAOIIajvoh4fThR4fwWgKVu3O
FQIDAQAB
-----END PUBLIC KEY-----
""".trimIndent()

val end2EndExampleRSAPrivateKeyStr = """
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAu5aC+itz1IXrtbCow40BkF1A5NZFk8/bHadxvPSZGJfvzNwZ
S4MHf99nFAEq8JNIkbL8cls972JjqqvofIzwzTSMtdnBBoci3BCaOguRRC0r5gOy
kOsq2kID8o0eH4ntnWrmnjr3VJ0FZ5WLq3pBdlauXp+tUaCTnsIfrQvM3OuBabzZ
XIxTYGyqW5qGkjXP9u8ysqwNuTzr4WzKKbZBoMG4MG8xTklLKZ1kM1tflMtdALl+
5jqdb96hIt+3C/WvWXw/hxRXBNie1og33gpk5iNBCIDgB4WATHYfXQgk0LWlFv06
GL1M1BpnRvlXAOIIajvoh4fThR4fwWgKVu3OFQIDAQABAoIBAAnPJgw053qughPf
KAQJxJIq/jC5L6w6C0gysFTkKXlKwKRiwgPb1zGNmhNGuFsaKIpN5LuKH+P7riCH
msGgkRr563265EgWGvGNALOWVUNOZWRCvzyqv3PoTdKKJJAbo0w/Ac42YSaQi27O
OB6AZxnsEHQsP2DsV6vOlN90pYLLyuxtCYVdiao6EdwUugN3J/5PqldIOayUEdjb
4z14jXgjimZA5RDhm32Px6YFzUXVwLMoJ7W8aATBetvyS9Xg2SRQjyU3EcV4oTUJ
hnmmI7q8RThRux/XJK3Pdq1bYfIRV2qzNkHJPCqCvTOu9pBDRG0xgf0s4zqQ2q6d
6oDdtyECgYEA4QTNQZsoJvBXFShCbSvhNeZ776h+Xb1YSuZhCeQYYhi05V1ki7wv
YOraIpO8+rKemag2eLy5rGcNEIoBqY+rrz221ZIleIJbactdL1ecvwwhnW7hWIAN
Ng8tK0x68xvWIho9LUn3aoqsyawOGkmDgzcGoc+yM7L4MFblhk1ow20CgYEA1Wpk
WrHZYIWeNE1e9vLBaCEPMEszXjN9XELacOms5HwY4obmPlnNGD+QcavC5pdluEv5
D7tix9y1NQ6CfFQfpLyzAMt1QdJQfEIq/rntDzBvc+WY/06HHd7aNdCaZMSwMJbP
qm1AZaFIleEgG2H/rGIzK8ZV7iX01pSFkcxl5EkCgYEAp+8cfN0eL0lpxHmCcdWw
w7hbQLaAcNdSILwlKeuYowWLZC66TmtI9MzxtaKLBJLwOP9If/1hmSBjqLdGnFSE
LkohvOzQmEq5jJBg4GdDrXWRVNyew5z1vyW+cTUoAW4B9vucMsOkKliKsgx9jfLV
esVDZtoKRflIr1L7A6ucB1UCgYEApOh2LUK6NwRoz/9tPyMr4duR0f557fOZjb42
7wMRzug5jmkw5sMbYP5VDhDsJKSePD+wb8CbPtbDywCwQYP7g58wLpAIxljOSoYS
lQx0KsWBiavDgpxaefFm6iiL9QurHZCbXRTYqu9qmC4CUkZyevDSm6PBaKk5vMm9
QIERxskCgYBN7pF+VskMx3ZGayfYTfJidXySefT1X93uDevMGShbPx+PW0ZL+Te4
xN40PdPzy1hcpGL/TKB+jRghYNd2inYjFk/CFqqXSj4EMRlfiGKjY1NEFFLhmrUO
8xrAjw+G25D5Wal0/g36XhxwPgtiYV1x9mumqD90hNTk5j7zC648Jg==
-----END RSA PRIVATE KEY-----
""".trimIndent()

val end2EndExampleRSAPrivateInvalidKeyStr = """
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA17leyFgR4/WGFAh85rpV1ePmjxgLuiUgA3mtyjNJd4AlX1a+
WHyKqLZBJuFWfqdIfg/mgt4vyWy8niCyEZbbZpO6bNxHc6iU8spA1VBGE2qRByK+
MmO6cYJZHPTSH/QNbQEn5YMpciFNyJZxcKgcZbnSXxoYSvRkHkMM9Q8KoqzUPl1t
BjXR6mbsJ2EVw+lO8UifGE4SX7JgBO5I2zjKdfzZA9+lYswasBdQEVqpURLzQNhp
b3OJ1Fnx96+XSlaYEf8XmJn2RIsWHxdZSn3YhYNsYfvjsp346ItzpObsgTz2cVml
F+IFXn/W8gQ+swH/XJzvZSRw7WKbcvW10JuPZQIDAQABAoIBAFlpKGLLNecQzVii
R/ptgsQbKGVopvupBYLLPP/QkAOqplLEpjIQtHvGxmwx+2KVPROazYSySIYovmif
zo5Bw3/ZfOw/xJGobsvOjl6bXeAQTDnz6XcDJLSFPSAmTK0XvzTNxNZ4rYXzTcT0
reHum46CHTJzo1v1vUVZrxYm/NZ9Hr2/G2ySR7ChaUelLjMwM0g24c+ddBhPfgu4
A4gxp8MLOS8xG38Oki4c3KW+Jh8Uo4ML7S1u/ZlxhpzE1AzQfgsGLIggxDjk3C7e
nknCBlInXK4YTddZwlv03zItMQewO2G/yNTIF6mi0gwGBtRAGQ3FbuQSBW9z7uo/
tGN62SUCgYEA8a7fNS3/Om7OG637v6fxSdhC6sbGyiSpAXmgpm8Ic8Nk7tF9qRTg
FvV59KdLfTLyVzgsxyUJkDzJAze5582vYYHzsJCZwgz9kUpx6Q7RvMrE5X1OUNQv
o9M8yu1fcKx0N3WyuE/ApRTqpzteoB7WOHejecjpTeLRvOvHXKdAqacCgYEA5IDU
bsFXsqUyM0fw0lme5YfpEBS6W1FPj21vqg34L/mPiML0AOu+0kpi5g286nRWGQ3P
6DVR9fgLYFUInJzjutAhMkAv9udi8oHQwJfN2Pud3OXRjEZg5Ug1D3kDJrM2GUyK
rInYEUigVrHsrqUlNuz7Ev8AYRaY/WHmpkMoSBMCgYBk/RaGCT9iMlTrmgrdLhcU
LUrhAcilRSZd2G35veHBRb+ST3V7xp5Q2ahpQ9K2cSh0q6OCX4acf9na/1kudHM5
gmzKtdGaFYWLRZlNsoSPqAcYggDMo614flcj0IaV9WnmlsbkX2b3VEMtOBC1Rc1r
8QodZIegpQvRLpCytay+dwKBgG625TcMwvsyA5LJRqwE9HJuWcSK5oicaxopgjM5
NYm5N4yiOSvBDeJCXIzvFxvaZmUZRiVSwHWXS5vPV67abZT0h0EbzKGrF0w9DfJj
G0AJGkIPsGpxJz2wsNTgY2B68LltVrumxmQJdnbLGsy8A74LMNPRblOcaWBL8T+Z
xoi1AoGBAJ3tFYoDRzLBDrd+5c0xliHKXIGqotc8QUkBJKjRiA5CLWOQpEziB/bq
an1MIPCeAMGQZKKZbhT4mjZtZm5SJIVRmijRB/re8+6DJbRinEKkM+ulZL5qXGJv
HCRgDKGQ0+bMAS8TkY7Td4fmefbKOeb+4YaPCVrFqFTqTAjVYi2O
-----END RSA PRIVATE KEY-----
""".trimIndent()

class TokenAuthenticationTests {

    private val sender = Sender(
        "foo",
        "bar",
        Sender.Format.CSV,
        "covid-19",
        "mySchema",
        keys = null
    )

    private val tokenAuthentication = TokenAuthentication(MemoryJtiCache())

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
        assertTrue(tokenAuthentication.checkSenderToken(senderToken, UseTestKey(exampleRsaPublicKeyStr)))
    }

    @Test
    fun `test createAccessToken and checkAccessToken happy path`() {
        val rslookup = GetTestSecret() // callback to look up the Reportstream secret, using to sign token.
        val token = tokenAuthentication.createAccessToken("foobar", rslookup)
        assertTrue(token.accessToken.isNotEmpty())
        // must expire later than now, but in less than 10 minutes
        val now: Int = (System.currentTimeMillis() / 1000).toInt()
        assertTrue(token.expiresAtSeconds >= now)
        assertTrue(token.expiresAtSeconds < now + 600)
        assertEquals("foobar", token.scope)
        assertEquals("bearer", token.tokenType)

        // Now read the token back in, and confirm its valid.
        val claims = tokenAuthentication.checkAccessToken(token.accessToken, "foobar", rslookup)
        // if claims is non-null then the sender's accessToken is valid.
        assertNotNull(claims)
        assertEquals(token.expiresAtSeconds, claims["exp"])
        assertEquals("foobar", claims["scope"])
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
        val accessToken = if (tokenAuthentication.checkSenderToken(senderToken, senderLookup)) {
            // Step 3:  Report stream creates a new accessToken
            tokenAuthentication.createAccessToken("myScope", rslookup)
        } else error("Unauthorized connection")

        // Step 4: Now pretend the sender has used the accessToken to make a request to reportstream...

        // Step 5: ... and ReportStream checks it:
        val claims = tokenAuthentication.checkAccessToken(accessToken.accessToken, "myScope", rslookup)
        // if claims is non-null then the sender's accessToken is valid.
        assertNotNull(claims)
        assertEquals(accessToken.expiresAtSeconds, claims["exp"])
        assertEquals("myScope", claims["scope"])
    }

    @Test
    fun `test mismatched Sender key`() {
        val privateKey = jacksonObjectMapper().readValue(exampleRsaPrivateKeyStr, Jwk::class.java).toRSAPrivateKey()
        val senderToken = SenderUtils.generateSenderToken(sender, "http://baz.quux", privateKey, exampleKeyId)

        val senderLookup = UseTestKey(differentRsaPublicKeyStr) // Its the wrong trousers!
        // false means we failed to validate the sender's jwt.
        assertFalse(tokenAuthentication.checkSenderToken(senderToken, senderLookup))
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
        assertFalse(tokenAuthentication.checkSenderToken(senderToken, senderLookup))
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
        assertFalse(tokenAuthentication.checkSenderToken(senderToken, senderLookup))
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
        assertTrue(tokenAuthentication.checkSenderToken(senderToken, senderLookup))
        // Then fail the second time
        assertFalse(tokenAuthentication.checkSenderToken(senderToken, senderLookup))
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
        assertTrue(TokenAuthentication.isExpiredToken(exp1))

        val exp2 = Date(System.currentTimeMillis() + 1000)
        assertFalse(TokenAuthentication.isExpiredToken(exp2))
    }

    @Test
    fun `test checkAccessToken happy path`() {
        val rslookup = GetTestSecret() // callback to look up the Reportstream secret, using to sign RS token.
        val accessToken = tokenAuthentication.createAccessToken("myScope", rslookup)
        val claims = tokenAuthentication.checkAccessToken(accessToken.accessToken, "myScope", rslookup)
        // if claims is non-null then the sender's accessToken is valid.
        assertNotNull(claims)
        assertEquals(accessToken.expiresAtSeconds, claims["exp"])
        assertEquals("myScope", claims["scope"])
    }

    @Test
    fun `test empty scope to createAccessToken`() {
        val rslookup = GetTestSecret()
        assertFails { tokenAuthentication.createAccessToken("", rslookup) }
        assertFails { tokenAuthentication.createAccessToken(" ", rslookup) }
    }

    @Test
    fun `test checkAccessToken wrong reportstream secret`() {
        val rslookup1 = GetTestSecret()
        val accessToken = tokenAuthentication.createAccessToken("MyScope", rslookup1)
        val rslookup2 = GetTestSecret() // new/different secret
        val claims = tokenAuthentication.checkAccessToken(accessToken.accessToken, "MyScope", rslookup2)
        // if claims is non-null then the sender's accessToken is valid.
        assertNull(claims)
    }

    @Test
    fun `test scopeListContainsScope`() {
        assertTrue(Scope.scopeListContainsScope("a", "a"))
        assertTrue(Scope.scopeListContainsScope("a:b c:d e:f", "a:b"))
        assertFalse(Scope.scopeListContainsScope("a:b c:d e:f", "a:b "))
        assertFalse(Scope.scopeListContainsScope("", ""))
        assertFalse(Scope.scopeListContainsScope("xx", "x"))
        assertFalse(Scope.scopeListContainsScope("x   x", ""))
        assertFalse(Scope.scopeListContainsScope("x   x", " "))
    }

    @Test
    fun `end to end happy path -- full round-trip`() {

        // create sender in 'ignore' organization
        val environment = SettingCommand.Environment("local", "localhost:7071", useHttp = true)
        val accessTokenDummy = "dummy"
        val organization = "simple_report"
        val senderName = "temporary_sender_auth_test"

        val newSender = Sender(
            name = senderName,
            organizationName = organization,
            format = Sender.Format.CSV,
            topic = "covid-19",
            schemaName = "primedatainput/pdi-covid-19"
        )

        // save the new sender
        PutSenderSetting()
            .put(
                environment,
                accessTokenDummy,
                SettingCommand.SettingType.SENDER,
                "$organization.$senderName",
                jacksonObjectMapper().writeValueAsString(newSender)
            )

        // get the sender previously written 
        val savedSenderJson = GetSenderSetting().get(
            environment,
            accessTokenDummy,
            SettingCommand.SettingType.SENDER,
            "$organization.$senderName"
        )

        // deserialize the written sender
        var savedSender = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(savedSenderJson, Sender::class.java)

        // create a fake report
        val fakeReportFile = FileUtilities.createFakeFile(
            metadata = CoolTest.metadata,
            sender = savedSender,
            count = 1,
            format = Report.Format.CSV,
            directory = System.getProperty("java.io.tmpdir"),
            targetStates = null,
            targetCounties = null
        )

        // try to send a report without having assigned a public key
        val (responseCodePostReportWithoutKeys) =
            HttpUtilities.postReportFileFhir(ReportStreamEnv.LOCAL, fakeReportFile, savedSender)

        assertEquals(401, responseCodePostReportWithoutKeys, "Should get a 401 response while sending the report")

        /**
         * Associate a public key to the sender
         * and store in on the database
         */
        fun saveSendersKey(key: String, kid: String) {

            // associate a public key to the sender
            val publicKeyStr = SenderUtils.readPublicKeyPem(key)
            publicKeyStr.kid = kid
            savedSender = Sender(savedSender, "$organization.$senderName.report", publicKeyStr)

            // save the sender with the new key
            PutSenderSetting()
                .put(
                    environment,
                    accessTokenDummy,
                    SettingCommand.SettingType.SENDER,
                    "$organization.$senderName",
                    jacksonObjectMapper().writeValueAsString(savedSender)
                )
        }

        /**
         * Given a private key and the kid tries to retrieve an access token
         */
        fun getToken(privateKey: String, kid: String): Pair<Int, String> {

            val baseUrl = "http://localhost:7071/api/token"
            val invalidPrivateKey = SenderUtils.readPrivateKeyPem(privateKey)
            val senderSignedJWT = SenderUtils.generateSenderToken(savedSender, baseUrl, invalidPrivateKey, kid)
            val senderTokenUrl =
                SenderUtils.generateSenderUrl(environment, senderSignedJWT, "$organization.$senderName.report")

            return HttpUtilities.postHttp(senderTokenUrl.toString(), "".toByteArray())
        }

        "testing-kid-ec".let { kid ->

            // associate a key to the sender
            saveSendersKey(end2EndExampleECPublicKeyStr, kid)

            // try to get an access token with an invalid private key
            assertEquals(
                401,
                getToken(end2EndExampleECPrivateInvalidKeyStr, kid).first,
                "Should get a 401 response while trying to get a token with an invalid private key"
            )

            // get an access token with a valid private key
            val (httpStatusGetToken, responseGetToken) = getToken(end2EndExampleECPrivateKeyStr, kid)
            val accessToken = jacksonObjectMapper().readTree(responseGetToken).get("access_token").textValue()

            // try to send a report with a tampered access token
            val (httpStatusPostReportTamperedToken) =
                HttpUtilities.postReportFileFhir(
                    ReportStreamEnv.LOCAL,
                    fakeReportFile,
                    savedSender,
                    accessToken.reversed()
                )

            assertEquals(
                401,
                httpStatusPostReportTamperedToken,
                "Should get a 401 response while sending the report with a tampered token"
            )

            // try to send a report with a valid access token
            val (httpStatusPostReportValidToken) =
                HttpUtilities.postReportFileFhir(ReportStreamEnv.LOCAL, fakeReportFile, savedSender, accessToken)

            assertEquals(201, httpStatusPostReportValidToken, "Should get a 200 response while sending the report")
        }

        "testing-kid-rsa".let { kid ->

            // associate a key to the sender
            saveSendersKey(end2EndExampleRSAPublicKeyStr, kid)

            // try to get an access token with an invalid private key
            assertEquals(
                401,
                getToken(end2EndExampleRSAPrivateInvalidKeyStr, kid).first,
                "Should get a 401 response while trying to get a token with an invalid private key"
            )

            // get an access token with a valid private key
            val (httpStatusGetToken, responseGetToken) = getToken(end2EndExampleRSAPrivateKeyStr, kid)
            val accessToken = jacksonObjectMapper().readTree(responseGetToken).get("access_token").textValue()

            // try to send a report with a tampered access token
            val (httpStatusPostReportTamperedToken) =
                HttpUtilities.postReportFileFhir(
                    ReportStreamEnv.LOCAL,
                    fakeReportFile,
                    savedSender,
                    accessToken.reversed()
                )

            assertEquals(
                401,
                httpStatusPostReportTamperedToken,
                "Should get a 401 response while sending the report with a tampered token"
            )

            // try to send a report with a valid access token
            val (httpStatusPostReportValidToken) =
                HttpUtilities.postReportFileFhir(ReportStreamEnv.LOCAL, fakeReportFile, savedSender, accessToken)

            assertEquals(201, httpStatusPostReportValidToken, "Should get a 200 response while sending the report")
        }

        // delete the sender
        DeleteSenderSetting()
            .delete(
                environment,
                accessTokenDummy,
                SettingCommand.SettingType.SENDER,
                "simple_report.$senderName"
            )
    }
}