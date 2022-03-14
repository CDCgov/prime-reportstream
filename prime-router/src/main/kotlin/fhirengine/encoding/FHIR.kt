package gov.cdc.prime.router.encoding

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
fun Bundle.getValue(path: String): List<Base> {
    return FHIR.evaluate(this, path)
}

/**
 * A set of behaviors and defaults for FHIR encoding, decoding, and translation.
 */
object FHIR : Logging {
    val defaultContext = FhirContext.forR4()
    val defaultParser = defaultContext.newJsonParser()
    // val defaultPathEngine = defaultContext.newFhirPath()
    val defaultPathEngine = FHIRPathEngine(SimpleWorkerContext())
    /*
    init {
        defaultPathEngine.hostServices = 
    }
    */
    fun evaluate(bundle: Bundle, path: String): List<Base> {
        FHIR.defaultPathEngine.parse(path)
        val resources = FHIR.defaultPathEngine.evaluate(bundle, path)
        return resources
    }

    fun encode(bundle: Bundle, parser: IParser = defaultParser): String {
        return parser.encodeResourceToString(bundle)
    }

    fun decode(json: String, parser: IParser = defaultParser): Bundle {
        return parser.parseResource(Bundle::class.java, json)
    }
}