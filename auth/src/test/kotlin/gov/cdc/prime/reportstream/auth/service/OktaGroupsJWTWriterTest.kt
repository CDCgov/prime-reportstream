package gov.cdc.prime.reportstream.auth.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import gov.cdc.prime.reportstream.auth.util.KeyGenerationUtils
import gov.cdc.prime.reportstream.shared.StringUtilities.base64Encode
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWT
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class OktaGroupsJWTWriterTest {

    inner class Fixture {
        val now = Instant.now()
        private val fixedClock = Clock.fixed(now, ZoneId.systemDefault())

        val pair = KeyGenerationUtils.generateRSAKeyPair()
        val privateKey = pair.first
        val publicKey = pair.second

        val verifier = RSASSAVerifier(publicKey)

        val config = OktaGroupsJWTWriter.OktaGroupsJWTConfig(
            jwtEncodedPrivateKeyJWK = privateKey.toJSONString().base64Encode(),
            ttl = Duration.ofMinutes(5),
            nbf = Duration.ofSeconds(5),
            issuer = "I'm the issuer"
        )

        val oktaGroups = OktaGroupsJWT("appId", listOf("DHSender_org"))

        val jwtWriter = OktaGroupsJWTWriter(config, fixedClock)
    }

    @Test
    fun `successfully write JWT`() {
        val f = Fixture()

        val jwt = f.jwtWriter.write(f.oktaGroups)

        val parsed = SignedJWT.parse(jwt)
        val claims = parsed.jwtClaimsSet

        // expected signing algorithm
        assertEquals(parsed.header.algorithm, JWSAlgorithm.RS256)

        // all expected claims there
        assertEquals(claims.subject, f.oktaGroups.appId)
        assertEquals(claims.getStringListClaim("groups"), f.oktaGroups.groups)
        assertEquals(claims.issuer, "I'm the issuer")
        assertEquals(claims.issueTime.toInstant().epochSecond, f.now.epochSecond)
        assertEquals(claims.notBeforeTime.toInstant().epochSecond, f.now.minusSeconds(5).epochSecond)
        assertEquals(claims.expirationTime.toInstant().epochSecond, f.now.plusSeconds(5 * 60).epochSecond)

        // correctly signed
        assertEquals(parsed.verify(f.verifier), true)
    }
}