package gov.cdc.prime.router.tokens

import com.okta.jwt.Jwt
import java.time.Instant
import java.util.Collections
import kotlin.collections.LinkedHashMap

class TestDefaultJwt(
    tokenValue: String,
    issuedAt: Instant,
    expiresAt: Instant,
    claims: Map<String, Any>
) : Jwt {
    private val tokenValue: String
    private val claims: Map<String, Any>
    private val issuedAt: Instant
    private val expiresAt: Instant
    override fun getTokenValue(): String {
        return tokenValue
    }

    override fun getIssuedAt(): Instant {
        return issuedAt
    }

    override fun getExpiresAt(): Instant {
        return expiresAt
    }

    override fun getClaims(): Map<String, Any> {
        return claims
    }

    init {
        this.tokenValue = tokenValue
        this.issuedAt = issuedAt
        this.expiresAt = expiresAt
        this.claims = Collections.unmodifiableMap(LinkedHashMap(claims)) as Map<String, Any>
    }
}