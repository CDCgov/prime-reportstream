package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import io.github.linuxforhealth.fhir.FHIRContext
import io.github.linuxforhealth.hl7.ConverterOptions
import io.github.linuxforhealth.hl7.message.HL7MessageEngine
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * A set of behaviors and defaults for FHIR encoding and decoding.
 */
object FhirTranscoder : Logging {
    /**
     * The FHIR default path engine.
     */
    private val defaultPathEngine = FHIRPathEngine(SimpleWorkerContext())

    /**
     * Build a HL7MessageEngine for converting HL7 -> FHIR with provided [options].
     * @return the message engine
     */
    fun getMessageEngine(options: ConverterOptions = ConverterOptions.SIMPLE_OPTIONS): HL7MessageEngine {
        val context = FHIRContext(
            options.isPrettyPrint,
            options.isValidateResource,
            options.properties,
            options.zoneIdText
        )

        return HL7MessageEngine(context, options.bundleType)
    }

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

    /**
     * Get a list of Resources from the [bundle] that match the FHIR [path].
     * @return a list of resources
     */
    fun getByPath(bundle: Bundle, path: String): List<Base> {
        defaultPathEngine.parse(path) // IS THIS NEEDED?
        return defaultPathEngine.evaluate(bundle, path)
    }
}