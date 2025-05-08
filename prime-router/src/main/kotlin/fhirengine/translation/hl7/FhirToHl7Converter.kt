package gov.cdc.prime.router.fhirengine.translation.hl7

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import fhirengine.translation.hl7.utils.FhirPathFunctions
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import gov.cdc.prime.router.fhirengine.translation.hl7.config.ContextConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElementProcessingException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaReader
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.converterSchemaFromFile
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7Utils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.TranslationFunctions
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle

/**
 * Convert a FHIR bundle to an HL7 message using the [schemaRef] to perform the conversion.
 * The converter will error out if [strict] is set to true and there is an error during the conversion.  if [strict]
 * is set to false (the default) then any conversion errors are logged as a warning.  Note [strict] does not affect
 * the schema validation process. Additional custom FHIR path functions used to convert messages can be passed
 * inside the [context].
 * @property terser the terser to use for building the HL7 message (use for dependency injection)
 * @property constantSubstitutor the constant substitutor. Should be a static instance, but is not thread safe
 */
class FhirToHl7Converter(
    private val schemaRef: HL7ConverterSchema,
    private val strict: Boolean = false,
    private var terser: Terser? = null,
    // the constant substitutor is not thread safe, so we need one instance per converter instead of using a shared copy
    private val constantSubstitutor: ConstantSubstitutor = ConstantSubstitutor(),
    private val context: FhirToHl7Context? = null,
    warnings: MutableList<String>,
    errors: MutableList<String>,
) : ConfigSchemaProcessor<Bundle, Message, HL7ConverterSchema, ConverterSchemaElement>(
    schemaRef,
    warnings, errors
),
Logging {
    /**
     * Convert a FHIR bundle to an HL7 message using the [schema] in the [schemaFolder] location to perform the conversion.
     * The converter will error out if [strict] is set to true and there is an error during the conversion.  If [strict]
     * is set to false (the default) then any conversion errors are logged as a warning.  Note [strict] does not affect
     * the schema validation process. Additional custom FHIR path functions used to convert messages can be passed
     * inside the [context].
     * @property terser the terser to use for building the HL7 message (use for dependency injection)
     */
    constructor(
        schema: String,
        strict: Boolean = false,
        terser: Terser? = null,
        context: FhirToHl7Context? = null,
        blobConnectionInfo: BlobAccess.BlobContainerMetadata,
        warnings: MutableList<String> = mutableListOf(),
        errors: MutableList<String> = mutableListOf(),
    ) : this(
        schemaRef = converterSchemaFromFile(schema, blobConnectionInfo),
        strict = strict,
        terser = terser,
        context = context,
        warnings = warnings,
        errors = errors
    )

    constructor(
        schemaUri: String,
        blobConnectionInfo: BlobAccess.BlobContainerMetadata,
        strict: Boolean = false,
        terser: Terser? = null,
        context: FhirToHl7Context? = null,
        warnings: MutableList<String> = mutableListOf(),
        errors: MutableList<String> = mutableListOf(),
    ) : this(
        ConfigSchemaReader.fromFile(
            schemaUri,
            HL7ConverterSchema::class.java,
            blobConnectionInfo = blobConnectionInfo
        ),
        strict = strict,
        terser = terser,
        context = context,
        warnings = warnings,
        errors = errors

    )

    /**
     * Convert the given [bundle] to an HL7 message.
     * @return the HL7 message
     */
    override fun process(input: Bundle, reportEventService: IReportStreamEventService?): Message {
        // Sanity check, but the schema is assumed good to go here
        check(!schemaRef.hl7Class.isNullOrBlank())
        val message = HL7Utils.getMessageInstance(schemaRef.hl7Class!!)

        terser = Terser(message)
        try {
            processSchema(schemaRef, input, input)
        } catch (e: Exception) {
            if (e.message != null) {
                errors.add(e.message!!)
            }
        }
        return message
    }

    override fun checkForEquality(converted: Message, expectedOutput: Message): Boolean =
        converted.encodePreserveEncodingChars() == expectedOutput.encodePreserveEncodingChars()

    /**
     * Get the first valid string from the list of values specified in the schema for a given [element] using
     * [bundle] and [context] starting at the [focusResource].
     * @return the value for the element or an empty string if no value found
     */
    internal fun getValueAsString(
        element: ConverterSchemaElement,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext,
        constantSubstitutor: ConstantSubstitutor? = null,
    ): String {
        var retVal = ""
        run findValue@{
            element.value?.forEach {
                val value = if (it.isBlank()) {
                    ""
                } else {
                    try {
                        FhirPathUtils.evaluateString(context, focusResource, bundle, it, element, constantSubstitutor)
                    } catch (e: SchemaException) {
                        logger.error("Error while getting value for element ${element.name}", e)
                        ""
                    }
                }
                logger.trace("Evaluated value expression '$it' to '$value'")
                if (value.isNotBlank()) {
                    retVal = value
                    return@findValue
                }
            }
        }

        // when valueSet is available, use the matching value else just pass the value as is
        retVal = element.valueSet?.getMappedValue(retVal) ?: retVal
        return retVal
    }

    /**
     * Generate HL7 data for the elements for the given [schema] using [bundle] and custom [context]
     * that contains bundle, customFhirFunctions, config object (eg, Receiver object which contains receiver setting),
     * and the customTransFunctions (eg, handler function to do custom translation).
     * Starting at the [schemaResource] in the bundle. Set [debug] to true to enable debug statements to the logs.
     */
    private fun processSchema(
        schema: HL7ConverterSchema,
        bundle: Bundle,
        schemaResource: Base,
        context: CustomContext = CustomContext(
            bundle, bundle,
            customFhirFunctions = this.context?.fhirFunctions,
            config = this.context?.config,
            translationFunctions = this.context?.translationFunctions
        ),
        debug: Boolean = false,
    ) {
        val logLevel = if (debug) Level.INFO else Level.DEBUG
        logger.log(logLevel, "Processing schema: ${schema.name} with ${schema.elements.size} elements")
        // Add any schema level constants to the context
        // We need to create a new context, so constants exist only within their specific schema tree
        val schemaContext = CustomContext.addConstants(schema.constants, context)

        schema.elements.forEach { element ->
            try {
                processElement(element, bundle, schemaResource, schemaContext, debug)
            } catch (ex: Exception) {
                throw ConfigSchemaElementProcessingException(schema, element, ex)
            }
        }
    }

    /**
     * Generate HL7 data for an [element] using [bundle] and [context] and starting at the [schemaResource] in the bundle.
     * Set [debug] to true to enable debug statements to the logs.
     */
    internal fun processElement(
        element: ConverterSchemaElement,
        bundle: Bundle,
        schemaResource: Base,
        context: CustomContext,
        debug: Boolean = false,
    ) {
        val logLevel = if (element.debug || debug) Level.INFO else Level.DEBUG
        logger.trace("Started processing of element ${element.name}...")
        // Add any element level constants to the context
        val elementContext = CustomContext.addConstants(element.constants, context)
        var debugMsg = "Processed element name: ${element.name}, required: ${element.required}, "

        // First we need to resolve a resource value if available.
        val focusResources = getFocusResources(element.resource, bundle, schemaResource, elementContext)
        if (focusResources.isEmpty() && element.required == true) {
            // There are no sources to parse, but the element was required
            throw RequiredElementException(element)
        } else if (focusResources.isEmpty()) {
            debugMsg += "resource: NONE"
        }

        focusResources.forEachIndexed { index, focusResource ->
            // The element context must now get the focus resource
            elementContext.focusResource = focusResource
            val indexContext = if (element.resourceIndex.isNullOrBlank()) {
                elementContext
            } else {
                CustomContext.addConstant(
                    element.resourceIndex!!,
                    index.toString(),
                    elementContext
                )
            }
            if (canEvaluate(element, bundle, focusResource, schemaResource, indexContext)) {
                when {
                    // If this is a schema then process it.
                    element.schemaRef != null -> {
                        // Schema references can have new index references

                        logger.log(logLevel, "Processing element ${element.name} with schema ${element.schema} ...")
                        processSchema(
                            element.schemaRef!!,
                            bundle,
                            focusResource,
                            indexContext,
                            element.debug || debug
                        )
                    }

                    // A value
                    !element.value.isNullOrEmpty() && element.hl7Spec.isNotEmpty() -> {
                        val value = getValueAsString(
                            element,
                            bundle,
                            focusResource,
                            elementContext,
                            constantSubstitutor
                        )
                        setHl7Value(element, value, context)
                        debugMsg += "condition: true, resourceType: ${focusResource.fhirType()}, " +
                            "value: $value, hl7Spec: ${element.hl7Spec}"
                    }

                    // This should never happen as the schema was validated prior to getting here
                    else -> throw IllegalStateException()
                }
            } else if (element.required == true) {
                // The condition was not met, but the element was required
                throw RequiredElementException(element)
            } else {
                debugMsg += "condition: false, resourceType: ${focusResource.fhirType()}"
            }
        }
        // Only log for elements that require values
        if (element.schemaRef == null) logger.log(logLevel, debugMsg)
        logger.trace("End processing of element ${element.name}.")
    }

    /**
     * Set the [value] an [element]'s HL7 spec.
     */
    internal fun setHl7Value(element: ConverterSchemaElement, value: String, context: CustomContext) {
        if (value.isBlank() && element.required == true) {
            // The value is empty, but the element was required
            throw RequiredElementException(element)
        }
        element.hl7Spec.forEach { rawHl7Spec ->
            val resolvedHl7Spec = constantSubstitutor.replace(rawHl7Spec, context)
            try {
                val maybeTruncatedValue = context.translationFunctions?.maybeTruncateHL7Field(
                    value,
                    resolvedHl7Spec,
                    terser!!,
                    context
                ) ?: value
                terser!!.set(resolvedHl7Spec, maybeTruncatedValue)
                logger.trace("Set HL7 $resolvedHl7Spec = $value")
            } catch (e: HL7Exception) {
                val msg = "Could not set HL7 value for spec $resolvedHl7Spec for element ${element.name}"
                if (strict) {
                    logger.error(msg, e)
                    throw HL7ConversionException(msg, e)
                } else {
                    logger.warn(msg, e)
                }
            } catch (e: IllegalArgumentException) {
                val msg = "Invalid Hl7 spec $resolvedHl7Spec specified in schema for element ${element.name}"
                if (strict) {
                    logger.error(msg, e)
                    throw SchemaException(msg, e)
                } else {
                    logger.warn(msg, e)
                }
            } catch (e: Exception) {
                val msg = "Unknown error while processing element ${element.name}."
                if (strict) {
                    logger.error(msg, e)
                    throw HL7ConversionException(msg, e)
                } else {
                    logger.warn(msg, e)
                }
            }
        }
    }
}

/**
 * Context used to hold additional custom [FhirPathFunctions] used by [FhirToHl7Converter]
 */
data class FhirToHl7Context(
    val fhirFunctions: FhirPathFunctions,
    val config: ContextConfig? = null,
    val translationFunctions: TranslationFunctions,
)