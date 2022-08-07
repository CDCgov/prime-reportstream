package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import org.apache.commons.lang3.math.NumberUtils
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TypeDetails
import org.hl7.fhir.r4.model.ValueSet
import org.hl7.fhir.r4.utils.FHIRPathEngine

/**
 * Context used for resolving [constants].
 */
data class CustomContext(val constants: MutableMap<String, String> = mutableMapOf()) {
    companion object {
        /**
         * Add [constants] to a context.
         * @return a new context with the [constants] added or the existing context of no new constants are specified
         */
        fun addConstants(constants: Map<String, String>, previousContext: CustomContext?): CustomContext {
            return if (constants.isEmpty()) previousContext ?: CustomContext()
            else {
                val newContext = previousContext?.copy() ?: CustomContext()
                constants.forEach { newContext.constants[it.key] = it.value }
                newContext
            }
        }

        /**
         * Add constant with [key] and [value] to a context.
         * @return a new context with the constant added or the existing context of no new constant is specified
         */
        fun addConstant(key: String, value: String, previousContext: CustomContext?): CustomContext {
            return addConstants(mapOf(key to value), previousContext)
        }
    }
}

/**
 * String substitution for constants.
 */
object ConstantSubstitutor {
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
            if (context == null) throw HL7ConversionException("No context available to resolve constant '$key'")
            else if (!context.constants.contains(key))
                throw HL7ConversionException("'$key' was not found in the provided context")
            return context.constants[key] ?: ""
        }
    }
}

/**
 * Custom resolver for the FHIR path engine.
 */
class FhirPathCustomResolver : FHIRPathEngine.IEvaluationContext {
    override fun resolveConstant(appContext: Any?, name: String?, beforeContext: Boolean): Base? {
        // Name is always passed in from the FHIR path engine
        require(!name.isNullOrBlank())

        return when {
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
                            StringType(constantValue + constantNameParts[1].trimEnd('`'))
                        }
                    }

                    // 1 part means the constant is surrounded by `, useful for separating other text from the constant name
                    1 -> {
                        val constantValue = appContext.constants[constantNameParts[0].trimEnd('`')]
                        constantValue?.let {
                            StringType(constantValue)
                        }
                    }

                    else -> null
                }
            }

            // Just a straight constant replacement
            appContext.constants.contains(name) -> {
                // Return the type depending on the contents of the variable
                if (NumberUtils.isDigits(appContext.constants[name]))
                    IntegerType(NumberUtils.createInteger(appContext.constants[name]))
                else StringType(appContext.constants[name])
            }
            // Must return null as the resolver is called by the FhirPathEngine to test for non-constants too
            else -> null
        }
    }

    override fun resolveConstantType(appContext: Any?, name: String?): TypeDetails {
        throw NotImplementedError("Not implemented")
    }

    override fun log(argument: String?, focus: MutableList<Base>?): Boolean {
        throw NotImplementedError("Not implemented")
    }

    override fun resolveFunction(functionName: String?): FHIRPathEngine.IEvaluationContext.FunctionDetails {
        throw NotImplementedError("Not implemented")
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
        throw NotImplementedError("Not implemented")
    }

    override fun resolveReference(appContext: Any?, url: String?): Base {
        throw NotImplementedError("Not implemented")
    }

    override fun conformsToProfile(appContext: Any?, item: Base?, url: String?): Boolean {
        throw NotImplementedError("Not implemented")
    }

    override fun resolveValueSet(appContext: Any?, url: String?): ValueSet {
        throw NotImplementedError("Not implemented")
    }
}