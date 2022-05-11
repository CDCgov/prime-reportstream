package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

/**
 * A set of behaviors and defaults for FHIR encoding and decoding.
 */
object FhirTranscoder : Logging {
    /**
     * The FHIR context.
     */
    private val defaultContext: FhirContext = FhirContext.forR4()

    /**
     * Encode a FHIR [bundle] to a string using [parser].
     * @return a JSON string of the bundle
     */
    fun encode(bundle: Bundle, parser: IParser = defaultContext.newJsonParser()): String {
        return parser.encodeResourceToString(bundle)
    }

    /**
     * Decode a FHIR bundle from a [json] string using [parser].
     * @return a FHIR bundle
     */
    fun decode(json: String, parser: IParser = defaultContext.newJsonParser()): Bundle {
        return parser.parseResource(Bundle::class.java, json)
    }
}