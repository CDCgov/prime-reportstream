package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import fhirengine.translation.hl7.utils.FhirPathFunctions
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.config.ContextConfig
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.exceptions.PathEngineException
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine
import org.hl7.fhir.r4.fhirpath.FHIRPathUtilityClasses
import org.hl7.fhir.r4.fhirpath.TypeDetails
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.ValueSet
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException

private const val s = "appendToIndex"

/**
 * Context used for resolving [constants] and custom FHIR functions. The class is for us to add our customer function
 * [customFhirFunctions], customer [config] object for us to pass any object to our custom translation function
 * [translationFunctions] (eg, handler function to do custom translation).
 */
data class CustomContext(
    val bundle: Bundle,
    var focusResource: Base,
    val constants: MutableMap<String, String> = mutableMapOf(),
    val customFhirFunctions: FhirPathFunctions? = null,
    val config: ContextConfig? = null,
    val translationFunctions: TranslationFunctions? = Hl7TranslationFunctions(),
) {
    companion object {
        val appendToIndexKey = "appendToIndex"
        private val reservedConstantNames =
            listOf(
                "loinc",
                "ucum",
                "resource",
                "rootResource",
                "context",
                "us-zip",
                "`vs-",
                "`cs-",
                "`ext",
                appendToIndexKey
            )

        /**
         * Add [constants] to a context.
         * @return a new context with the [constants] added or the existing context if no new constants are specified
         */
        fun addConstants(
            constants: Map<String, String>,
            previousContext: CustomContext,
        ): CustomContext = addConstants(constants, previousContext, true)

        private fun addConstants(
            constants: Map<String, String>,
            previousContext: CustomContext,
            checkReservedNames: Boolean,
        ): CustomContext = if (constants.isEmpty()) {
                previousContext
            } else {
                if (checkReservedNames && constants.keys.any { reservedConstantNames.contains(it) }) {
                    throw SchemaException(
                        """Constants contained reserved name,
                        reserved constants are: $reservedConstantNames
                        """.trimMargin()
                    )
                }
                val newContext = CustomContext(
                    previousContext.bundle,
                    previousContext.focusResource,
                    previousContext.constants.toMap().toMutableMap(), // This makes a copy of the map
                    previousContext.customFhirFunctions,
                    previousContext.config,
                    previousContext.translationFunctions
                )
                constants.forEach { newContext.constants[it.key] = it.value }
                newContext
            }

        /**
         * Add constant with [key] and [value] to a context.
         * @return a new context with the constant added or the existing context of no new constant is specified
         */
        fun addConstant(
            key: String,
            value: String,
            previousContext: CustomContext,
        ): CustomContext = addConstants(mapOf(key to value), previousContext, true)

        fun setAppendToIndex(index: Int, previousContext: CustomContext): CustomContext = addConstants(
            mapOf(
            appendToIndexKey to index.toString()
            ),
                previousContext, false
        )

        fun getAppendToIndex(context: CustomContext): Int? = context.constants[appendToIndexKey]?.toInt()
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
    fun replace(
        inputText: String,
        context: CustomContext?,
    ): String = constantResolver.setVariableResolver(StringCustomResolver(context)).replace(inputText)

    /**
     * Custom resolver for the [ConstantSubstitutor] that uses the [context] to resolve the constants.
     */
    internal class StringCustomResolver(val context: CustomContext?) :
        StringLookup,
        Logging {
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
    FHIRPathEngine.IEvaluationContext,
    Logging {
    override fun resolveConstant(
        engine: FHIRPathEngine?,
        appContext: Any?,
        name: String?,
        beforeContext: Boolean,
        explicitConstant: Boolean,
    ): List<Base> {
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
        return if (constantValue.isNullOrBlank()) {
            emptyList()
        } else {
            val values = FhirPathUtils.evaluate(appContext, appContext.focusResource, appContext.bundle, constantValue)
            if (values.isEmpty()) {
                emptyList()
            } else {
                logger.trace("Evaluated FHIR Path constant $name to: $values")
                // Convert string constants that are whole integers to Integer type to facilitate math operations
                values.map {
                    if (it is StringType && StringUtils.isNumeric(it.primitiveValue())) {
                        try {
                            IntegerType(it.primitiveValue())
                        } catch (e: IllegalArgumentException) {
                            // fallback to string; see https://github.com/CDCgov/prime-reportstream/issues/12609
                            if (e.cause !is NumberFormatException) throw e
                            it
                        }
                    } else {
                        it
                    }
                }
            }
        }
    }

    override fun resolveConstantType(
        engine: FHIRPathEngine?,
        appContext: Any?,
        name: String?,
        explicitConstant: Boolean,
    ): TypeDetails = throw NotImplementedError("Not implemented")

    override fun log(argument: String?, focus: MutableList<Base>?): Boolean =
        throw NotImplementedError("Not implemented")

    override fun resolveFunction(
        engine: FHIRPathEngine?,
        functionName: String?,
    ): FHIRPathUtilityClasses.FunctionDetails? = CustomFHIRFunctions.resolveFunction(functionName, customFhirFunctions)

    override fun checkFunction(
        engine: FHIRPathEngine?,
        appContext: Any?,
        functionName: String?,
        focus: TypeDetails?,
        parameters: MutableList<TypeDetails>?,
    ): TypeDetails = throw NotImplementedError("Not implemented")

    override fun executeFunction(
        engine: FHIRPathEngine?,
        appContext: Any?,
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?,
    ): MutableList<Base> {
        check(focus != null)
        return when {
            CustomFHIRFunctions.resolveFunction(functionName, customFhirFunctions) != null -> {
                CustomFHIRFunctions.executeFunction(focus, functionName, parameters, customFhirFunctions)
            }

            else -> throw IllegalStateException("Tried to execute invalid FHIR Path function $functionName")
        }
    }

    override fun resolveReference(engine: FHIRPathEngine?, appContext: Any?, url: String?, refContext: Base?): Base? {
        // Name is always passed in from the FHIR path engine
        require(!url.isNullOrBlank())

        return when (appContext) {
            null, !is CustomContext -> throw PathEngineException("No context available to resolve constant '$url'")
            else -> appContext.bundle.entry.find { it.fullUrl == url }?.resource
        }
    }

    override fun conformsToProfile(engine: FHIRPathEngine?, appContext: Any?, item: Base?, url: String?): Boolean =
        throw NotImplementedError("Not implemented")

    override fun resolveValueSet(engine: FHIRPathEngine?, appContext: Any?, url: String?): ValueSet =
        throw NotImplementedError("Not implemented")
}