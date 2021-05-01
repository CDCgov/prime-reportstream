package gov.cdc.prime.router.tokens

import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime

/**
 * Note:  DPC project implements very similar -
 * See https://github.com/CMSgov/dpc-app/blob/master/dpc-api/src/main/java/gov/cms/dpc/api/auth/jwt/IJTICache.java
 */

abstract class JtiCache: Logging {
    abstract fun cleanupCache()

    abstract fun insertIntoCache(jti: String, expiresAt: OffsetDateTime)

    abstract fun isPresentInCache(jti: String): Boolean

    /**
     * This caches with a minimum expiration time
     *
     * Rather than having a timer based cache cleanup, this only cleans up when its called.
     */
    fun isJTIOk(jti: String, expiresAt: OffsetDateTime): Boolean {
        cleanupCache()
        return if (isPresentInCache(jti)) {
            logger.warn("JTI $jti is being replayed")
            false
        } else {
            val minimumExpirationTime = OffsetDateTime.now().plusMinutes(5)
            if (expiresAt.isBefore(minimumExpirationTime)) {
                insertIntoCache(jti, minimumExpirationTime)
            } else {
                insertIntoCache(jti, expiresAt)
            }
            true
        }
    }
}