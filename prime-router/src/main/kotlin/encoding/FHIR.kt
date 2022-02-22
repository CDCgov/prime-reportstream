package gov.cdc.prime.router.encoding

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

/**
 * Extend the fhir Bundle object with a default encoder
 */
fun Bundle.encode(): String {
    return FHIR.encode(this)
}

/**
 * A set of behaviors and defaults for FHIR encoding, decoding, and translation.
 */
object FHIR : Logging {
    val defaultContext = FhirContext.forR4()
    val defaultParser = defaultContext.newJsonParser()

    fun encode(bundle: Bundle, parser: IParser = defaultParser): String {
        return parser.encodeResourceToString(bundle)
    }

    fun decode(json: String, parser: IParser = defaultParser): Bundle {
        return parser.parseResource(Bundle::class.java, json)
    }
}