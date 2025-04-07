package gov.cdc.prime.reportstream.shared.auth.jwt

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.BadJWTException
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import gov.cdc.prime.reportstream.shared.StringUtilities.base64Decode

/**
 * Common Okta Groups JWT reader and validator
 */
class OktaGroupsJWTReader(publicKey: JWK) {

    constructor(encodedJWK: String) : this(JWK.parse(encodedJWK.base64Decode()))

    private val verifier = RSASSAVerifier(publicKey.toRSAKey())
    private val claimsVerifier = DefaultJWTClaimsVerifier<SecurityContext>(
        null,
        setOf(OktaGroupsJWTConstants.OKTA_GROUPS_JWT_GROUP_CLAIM),
    )

    /**
     * Ensures our JWT is valid, properly signed, active, and contains the correct claims
     */
    fun read(token: String): OktaGroupsJWT {
        val parsedToken = SignedJWT.parse(token)
        return if (parsedToken.verify(verifier)) {
            claimsVerifier.verify(parsedToken.jwtClaimsSet, null)
            OktaGroupsJWT(
                parsedToken.jwtClaimsSet.subject,
                parsedToken.jwtClaimsSet.getStringListClaim(OktaGroupsJWTConstants.OKTA_GROUPS_JWT_GROUP_CLAIM)
            )
        } else {
            throw BadJWTException("Invalid signature")
        }
    }
}