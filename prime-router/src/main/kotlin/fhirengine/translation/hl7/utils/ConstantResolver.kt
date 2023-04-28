package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import fhirengine.translation.hl7.utils.FhirPathFunctions
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TypeDetails
import org.hl7.fhir.r4.model.ValueSet
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.hl7.fhir.r4.utils.FHIRPathEngine.IEvaluationContext.FunctionDetails

/**
 * Context used for resolving [constants] and custom FHIR functions.
 */
data class CustomContext(
    val bundle: Bundle,
    var focusResource: Base,
    val constants: MutableMap<String, String> = mutableMapOf(),
    val customFhirFunctions: FhirPathFunctions? = null
) {
    companion object {
        /**
         * Add [constants] to a context.
         * @return a new context with the [constants] added or the existing context of no new constants are specified
         */
        fun addConstants(constants: Map<String, String>, previousContext: CustomContext): CustomContext {
            return if (constants.isEmpty()) previousContext
            else {
                val newContext = CustomContext(
                    previousContext.bundle,
                    previousContext.focusResource,
                    previousContext.constants.toMap().toMutableMap(), // This makes a copy of the map
                    previousContext.customFhirFunctions
                )
                constants.forEach { newContext.constants[it.key] = it.value }
                newContext
            }
        }

        /**
         * Add constant with [key] and [value] to a context.
         * @return a new context with the constant added or the existing context of no new constant is specified
         */
        fun addConstant(key: String, value: String, previousContext: CustomContext): CustomContext {
            return addConstants(mapOf(key to value), previousContext)
        }
    }
}

/**
 * String substitution for constants.
 */
class ConstantSubstitutor {
    /**
     * The resolver.  Uses %{} instead of the default to match the FHIR path use of %.
     */
    private val constantResolver = StringSubstitutor().setVariablePrefix("%{").setEscapeChar('%')

    /**
     * Replace the constants in a given [inputText] using the [context].
     * @return the resolved string
     */
    fun replace(inputText: String, context: CustomContext?): String {
        return constantResolver.setVariableResolver(StringCustomResolver(context)).replace(inputText)
    }

    /**
     * Custom resolver for the [ConstantSubstitutor] that uses the [context] to resolve the constants.
     */
    internal class StringCustomResolver(val context: CustomContext?) : StringLookup, Logging {
        override fun lookup(key: String?): String {
            require(!key.isNullOrBlank())
            when {
                context == null -> throw HL7ConversionException("No context available to resolve constant '$key'")

                !context.constants.contains(key) || context.constants[key] == null ->
                    throw HL7ConversionException("Constant '$key' was not found in the provided context")

                else -> return context.constants[key]!!
            }
        }
    }
}

/**
 * Custom resolver for the FHIR path engine.
 */
class FhirPathCustomResolver(private val customFhirFunctions: FhirPathFunctions? = null) :
    FHIRPathEngine.IEvaluationContext, Logging {
    override fun resolveConstant(appContext: Any?, name: String?, beforeContext: Boolean): List<Base>? {
        // Name is always passed in from the FHIR path engine
        require(!name.isNullOrBlank())

        val constantValue = when {
            appContext == null || appContext !is CustomContext ->
                throw PathEngineException("No context available to resolve constant '$name'")

            // Support prefix constant replacement like for URLs similar to %ext in FHIRPathEngine
            // E.g., '%resource.source.extension(%`rsext-source-software-vendor-org`).value' where
            // rsext is the defined constant.
            name.startsWith("`") -> {
                val constantNameParts = name.trimStart('`').split("-", limit = 2)
                when (constantNameParts.size) {
                    // 2 parts means the constant is a prefix and there is a suffix
                    2 -> {
                        val constantValue = appContext.constants[constantNameParts[0]]
                        constantValue?.let {
                            "$constantValue + '${constantNameParts[1].trimEnd('`')}'"
                        }
                    }

                    // 1 part means the constant is surrounded by `, useful for separating other text from the constant name
                    1 -> {
                        appContext.constants[constantNameParts[0].trimEnd('`')]
                    }

                    else -> null
                }
            }

            // Just a straight constant replacement
            appContext.constants.contains(name) -> appContext.constants[name]

            // Must return null as the resolver is called by the FhirPathEngine to test for non-constants too
            else -> null
        }

        // Evaluate the constant before it is used.
        return if (constantValue.isNullOrBlank()) null
        else {
            val values = FhirPathUtils.evaluate(appContext, appContext.focusResource, appContext.bundle, constantValue)
            if (values.isEmpty()) {
                null
            } else {
                logger.trace("Evaluated FHIR Path constant $name to: $values")
                // Convert string constants that are whole integers to Integer type to facilitate math operations
                values.map {
                    if (it is StringType && StringUtils.isNumeric(it.primitiveValue())) {
                        IntegerType(it.primitiveValue())
                    } else {
                        it
                    }
                }
            }
        }
    }

    override fun resolveConstantType(appContext: Any?, name: String?): TypeDetails {
        throw NotImplementedError("Not implemented")
    }

    override fun log(argument: String?, focus: MutableList<Base>?): Boolean {
        throw NotImplementedError("Not implemented")
    }

    override fun resolveFunction(functionName: String?): FunctionDetails? {
        return CustomFHIRFunctions.resolveFunction(functionName, customFhirFunctions)
    }

    override fun checkFunction(
        appContext: Any?,
        functionName: String?,
        parameters: MutableList<TypeDetails>?
    ): TypeDetails {
        throw NotImplementedError("Not implemented")
    }

    override fun executeFunction(
        appContext: Any?,
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?
    ): MutableList<Base> {
        check(focus != null)
        return when {
            CustomFHIRFunctions.resolveFunction(functionName, customFhirFunctions) != null -> {
                CustomFHIRFunctions.executeFunction(focus, functionName, parameters, customFhirFunctions)
            }

            else -> throw IllegalStateException("Tried to execute invalid FHIR Path function $functionName")
        }
    }

    override fun resolveReference(appContext: Any?, url: String?): Base? {
        // Name is always passed in from the FHIR path engine
        require(!url.isNullOrBlank())

        return when (appContext) {
            null, !is CustomContext -> throw PathEngineException("No context available to resolve constant '$url'")
            else -> appContext.bundle.entry.find { it.fullUrl == url }?.resource
        }
    }

    override fun conformsToProfile(appContext: Any?, item: Base?, url: String?): Boolean {
        throw NotImplementedError("Not implemented")
    }

    override fun resolveValueSet(appContext: Any?, url: String?): ValueSet {
        throw NotImplementedError("Not implemented")
    }
}