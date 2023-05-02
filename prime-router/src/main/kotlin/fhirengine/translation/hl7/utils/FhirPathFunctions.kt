package fhirengine.translation.hl7.utils

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * This interface contains the required method signatures required to implement custom FHIR functions
 */
interface FhirPathFunctions {

    /**
     * Get the function details for a given [functionName].
     * @return the function details
     */
    fun resolveFunction(
        functionName: String?,
        additionalFunctions: FhirPathFunctions? = null
    ): FHIRPathEngine.IEvaluationContext.FunctionDetails?

    /**
     * Execute the function on a [focus] resource for a given [functionName] and [parameters].
     * @return the function result
     */
    fun executeFunction(
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?,
        additionalFunctions: FhirPathFunctions? = null
    ): MutableList<Base>
}