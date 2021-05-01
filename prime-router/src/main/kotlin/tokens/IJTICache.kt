package gov.cdc.prime.router.azure

import java.time.OffsetDateTime

/**
 * Note:  DPC project implements this same cache in-memory.
 * See https://github.com/CMSgov/dpc-app/blob/master/dpc-api/src/main/java/gov/cms/dpc/api/auth/jwt/IJTICache.java
 * We are implementing a database version of it.
 */
interface IJTICache {
    /**
     * Determines whether or not the JWT JTI has been seen in the past time interval.
     * This avoids replay attack vectors.
     *
     * @param jti     - [String] jti value
     * @return - `true` JTI value is OK and has not been used before. `false` JTI has been used before
     */
    fun isJTIOk(jti: String, expiresAt: OffsetDateTime?): Boolean
}
