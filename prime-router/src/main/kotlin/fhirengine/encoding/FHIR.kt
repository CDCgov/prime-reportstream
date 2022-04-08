package gov.cdc.prime.router.fhirengine.encoding

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * Extend the fhir Bundle object with a default encoder
 */
fun Bundle.encode(): String {
    return FHIR.encode(this)
}

/**
 * Get a list of Resources from the Bundle that match the FHIR path
 *
 * @param path The FHIR path to use to find the resources.
 */
fun Bundle.getValue(path: String): List<Base> {
    return FHIR.evaluate(this, path)
}

/**
 * A set of behaviors and defaults for FHIR encoding, decoding, and translation.
 */
object FHIR : Logging {
    val defaultContext = FhirContext.forR4()
    val defaultParser = defaultContext.newJsonParser()
    val defaultPathEngine = FHIRPathEngine(SimpleWorkerContext())

    /**
     * Get a list of Resources from the Bundle that match the FHIR path
     *
     * @param bundle The FHIR bundle to use as the source data.
     * @param path The FHIR path to use to find the resources.
     */
    fun evaluate(bundle: Bundle, path: String): List<Base> {
        FHIR.defaultPathEngine.parse(path)
        val resources = FHIR.defaultPathEngine.evaluate(bundle, path)
        return resources
    }

    /**
     * encode a fhir bundle into a string format (default JSON)
     *
     * @param bundle The FHIR bundle to encode.
     * @param parser The parser that should be used for the encoding.
     */
    fun encode(bundle: Bundle, parser: IParser = defaultParser): String {
        return parser.encodeResourceToString(bundle)
    }

    /**
     * decode a fhir bundle from a string format (default JSON)
     *
     * @param bundle The FHIR bundle to decode.
     * @param parser The parser that should be used for the decoding.
     */
    fun decode(json: String, parser: IParser = defaultParser): Bundle {
        return parser.parseResource(Bundle::class.java, json)
    }
}