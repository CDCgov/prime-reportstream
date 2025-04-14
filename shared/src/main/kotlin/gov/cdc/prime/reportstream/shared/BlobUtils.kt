package gov.cdc.prime.reportstream.shared

import java.security.MessageDigest

object BlobUtils {

    /**
     * Create a hex string style of a digest.
     */
    fun digestToString(
        digest: ByteArray,
    ): String = digest.joinToString(separator = "", limit = 40) { Integer.toHexString(it.toInt()) }

    /**
     * Hash a ByteArray [input] with SHA 256
     */
    fun sha256Digest(input: ByteArray): ByteArray = hashBytes("SHA-256", input)

    /**
     * Hash a ByteArray [input] with method [type]
     */
    private fun hashBytes(type: String, input: ByteArray): ByteArray = MessageDigest
            .getInstance(type)
            .digest(input)
}