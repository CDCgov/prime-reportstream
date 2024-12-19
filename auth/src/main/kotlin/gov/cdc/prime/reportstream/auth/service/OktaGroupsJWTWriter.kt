package gov.cdc.prime.reportstream.auth.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import gov.cdc.prime.reportstream.shared.StringUtilities.base64Decode
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWT
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWTConstants
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.util.Date
import java.util.UUID

@Service
class OktaGroupsJWTWriter(
    private val jwtConfig: OktaGroupsJWTConfig,
    private val clock: Clock,
) {

    /**
     * generate and sign our custom JWT containing Okta group information for a particular application
     */
    fun write(model: OktaGroupsJWT): String {
        val now = clock.instant()
        val expires = now.plus(jwtConfig.ttl)
        val nbf = now.minus(jwtConfig.nbf)
        val claimsSetBuilder = JWTClaimsSet.Builder()
            .subject(model.appId)
            .issuer(jwtConfig.issuer)
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(nbf))
            .expirationTime(Date.from(expires))
            .claim(OktaGroupsJWTConstants.OKTA_GROUPS_JWT_GROUP_CLAIM, model.groups)

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256).build(),
            claimsSetBuilder.build()
        )

        signedJWT.sign(RSASSASigner(jwtConfig.jwtPrivateKeyJWK.toRSAKey()))

        return signedJWT.serialize()
    }

    /**
     * Configuration for Submissions microservice
     */
    @ConfigurationProperties(prefix = "okta.jwt")
    data class OktaGroupsJWTConfig(
        private val jwtEncodedPrivateKeyJWK: String,
        val ttl: Duration,
        val nbf: Duration,
        val issuer: String,
    ) {
        // JWK json format
        val jwtPrivateKeyJWK: JWK = JWK.parse(jwtEncodedPrivateKeyJWK.base64Decode())
    }
}