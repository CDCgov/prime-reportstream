package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.ExpressionNode
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * Utilities to handle FHIR Path parsing.
 */
object FhirPathUtils {
    /**
     * The FHIR path engine.
     */
    private val defaultPathEngine = FHIRPathEngine(SimpleWorkerContext())

    /**
     * Parse a FHIR path from a [fhirPath] string.  This will also provide some format validation.
     * @return the validated FHIR path
     * @throws Exception if the path is invalid
     */
    fun parsePath(fhirPath: String?): ExpressionNode? {
        return if (fhirPath == null) null
        else defaultPathEngine.parse(fhirPath)
    }

    // TODO Add evaluate function for parsing bundles
    // TODO Input custom variables into the FHIR path evaluation
}