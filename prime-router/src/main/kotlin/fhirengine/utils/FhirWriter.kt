package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Bundle

// inline fun <reified T : IBase> IBase.getValue(path: String): List<T> {
//    FHIR.defaultPathEngine.parse(path)
//    return FHIR.defaultPathEngine.evaluate<T>(this, path, T::class.java)
// }

/**
 * A set of behaviors and defaults for FHIR encoding, decoding, and translation.
 */
object FhirWriter : Logging {
    private val defaultContext: FhirContext = FhirContext.forR4()
    private val defaultParser: IParser = defaultContext.newJsonParser()
//    val defaultPathEngine = defaultContext.newFhirPath()

    fun encode(bundle: Bundle, parser: IParser = defaultParser): String {
        return parser.encodeResourceToString(bundle)
    }

    fun decode(json: String, parser: IParser = defaultParser): Bundle {
        return parser.parseResource(Bundle::class.java, json)
    }
}