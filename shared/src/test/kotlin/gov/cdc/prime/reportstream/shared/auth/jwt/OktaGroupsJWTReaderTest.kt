package gov.cdc.prime.reportstream.shared.auth.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.BadJWTException
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OktaGroupsJWTReaderTest {

    inner class Fixture {
        val clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"))
        val keyPair = generateRSAKeyPair()
        val privateKey = keyPair.first
        val publicKey = keyPair.second
        val service = OktaGroupsJWTReader(publicKey)

        fun generateRSAKeyPair(): Pair<RSAKey, RSAKey> {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val keyPair = keyGen.generateKeyPair()

            val privateJWK = RSAKey.Builder(keyPair.public as RSAPublicKey)
                .privateKey(keyPair.private as RSAPrivateKey)
                .build()
            val publicJWK = privateJWK.toPublicJWK()
            return Pair(privateJWK, publicJWK)
        }

        fun writeJWT(
            extraClaims: Map<String, *>,
            privateKey: RSAKey,
        ): String {
            val now = clock.instant()
            val expires = now.plus(Duration.ofMinutes(10))
            val nbf = now.minus(Duration.ofSeconds(5))
            val claimsSetBuilder = JWTClaimsSet.Builder()
                .subject("appId")
                .issuer("issuer")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(nbf))
                .expirationTime(Date.from(expires))

            extraClaims.forEach { (key, value) -> claimsSetBuilder.claim(key, value) }

            val signedJWT = SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claimsSetBuilder.build()
            )

            signedJWT.sign(RSASSASigner(privateKey))

            return signedJWT.serialize()
        }
    }

    @Test
    fun `successfully read and validate JWT`() {
        val f = Fixture()

        val jwt = f.writeJWT(
            mapOf(OktaGroupsJWTConstants.OKTA_GROUPS_JWT_GROUP_CLAIM to listOf("oktaGroup")),
            f.privateKey
        )

        val parsed = f.service.read(jwt)

        assertEquals(
            parsed,
            OktaGroupsJWT("appId", listOf("oktaGroup"))
        )
    }

    @Test
    fun `missing required claim`() {
        val f = Fixture()

        val jwt = f.writeJWT(emptyMap<String, String>(), f.privateKey)

        assertThrows<BadJWTException> {
            f.service.read(jwt)
        }
    }

    @Test
    fun `invalid signature`() {
        val f = Fixture()

        val (badPrivateKey, _) = f.generateRSAKeyPair()

        val jwt = f.writeJWT(emptyMap<String, String>(), badPrivateKey)

        assertThrows<BadJWTException> {
            f.service.read(jwt)
        }
    }
}