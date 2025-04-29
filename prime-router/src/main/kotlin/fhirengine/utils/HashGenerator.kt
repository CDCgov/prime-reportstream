package gov.cdc.prime.router.fhirengine.utils

import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

object HashGenerator {
    // Used by covid pipeline and universal pipeline
    // The hash generation logic is being moved to a common util method
    // Previously this logic was in Report.kt, but is needed for UP dedupe and ideally both CP and UP dedupe will
    // use the same strategy for creating a hashed string (the inputs and what is done with output may differ)
    // TLDR: Moving out, maybe just as a temp measure.
    fun generateHashFromString(string: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(string.toByteArray())

        return DatatypeConverter.printHexBinary(digest).uppercase()
    }
}