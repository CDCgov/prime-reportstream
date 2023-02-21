package gov.cdc.prime.router.fhirengine.translation.hl7

import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FHIRTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.ConstantSubstitutor
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.convertFhirType
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle

/**
 * Transform a FHIR bundle based on the [schemaRef].
 * The transformer will error out if [strict] is set to true and there is an error during the translation. If [strict]
 * is set to false (the default) then any translation errors are logged as a warning. Note [strict] does not affect
 * the schema validation process.
 * @property constantSubstitutor the constant substitutor. Should be a static instance, but is not thread safe
 */
class FhirTransformer(
    private val schemaRef: FhirTransformSchema,
    private val strict: Boolean = false,
    // the constant substitutor is not thread safe, so we need one instance per converter instead of using a shared copy
    private val constantSubstitutor: ConstantSubstitutor = ConstantSubstitutor()
) : Logging {
    /**
     * Transform a FHIR bundle based on the [schema] in the [schemaFolder] location.
     * The transformer will error out if [strict] is set to true and there is an error during the translation. If [strict]
     * is set to false (the default) then any translation errors are logged as a warning. Note [strict] does not affect
     * the schema validation process.
     */
    constructor(
        schema: String,
        schemaFolder: String,
        strict: Boolean = false,
    ) : this(
        schemaRef = fhirTransformSchemaFromFile(schema, schemaFolder),
        strict = strict,
    )

    /**
     * Transform a FHIR bundle based on the [schema] (which includes a folder location).
     * The transformer will error out if [strict] is set to true and there is an error during the translation. If [strict]
     * is set to false (the default) then any translation errors are logged as a warning. Note [strict] does not affect
     * the schema validation process.
     */
    constructor(
        schema: String,
        strict: Boolean = false,
    ) : this(
        schemaRef = fhirTransformSchemaFromFile(
            FilenameUtils.getName(schema),
            FilenameUtils.getPathNoEndSeparator(schema)
        ),
        strict = strict,
    )

    /**
     * Convert the given [bundle] to an HL7 message.
     * @return the HL7 message
     */
    fun transform(bundle: Bundle): Bundle {
        var transformedBundle = bundle.copy()
        val dupes = schemaRef.duplicateElements
        if (dupes.isNotEmpty()) { // value is the number of matches
            throw SchemaException("Schema ${schemaRef.name} has multiple elements with the same name: ${dupes.keys}")
        }
        transformWithSchema(schemaRef, bundle = bundle, focusResource = bundle, transformedBundle = transformedBundle)
        return transformedBundle
    }

    /**
     * Generate HL7 data for the elements for the given [schema] using [bundle] and [context] starting at the
     * [focusResource] in the bundle. Set [debug] to true to enable debug statements to the logs.
     */
    private fun transformWithSchema(
        schema: FhirTransformSchema,
        bundle: Bundle,
        focusResource: Base,
        transformedBundle: Bundle,
        context: CustomContext = CustomContext(bundle, focusResource),
        debug: Boolean = false
    ) {
        val logLevel = if (debug) Level.INFO else Level.DEBUG
        logger.log(logLevel, "Processing schema: ${schema.name} with ${schema.elements.size} elements")
        // Add any schema level constants to the context
        // We need to create a new context, so constants exist only within their specific schema tree
        val schemaContext = CustomContext.addConstants(schema.constants, context)

        schema.elements.forEach { element ->
            transformBasedOnElement(element, bundle, focusResource, transformedBundle, schemaContext, debug)
        }
    }

    /**
     * Generate HL7 data for an [element] using [bundle] and [context] and starting at the [focusResource] in the bundle.
     * Set [debug] to true to enable debug statements to the logs.
     */
    internal fun transformBasedOnElement(
        element: FHIRTransformSchemaElement,
        bundle: Bundle,
        focusResource: Base,
        transformedBundle: Bundle,
        context: CustomContext,
        debug: Boolean = false
    ) {
        val logLevel = if (element.debug || debug) Level.INFO else Level.DEBUG
        logger.trace("Started processing of element ${element.name}...")
        // Add any element level constants to the context
        val elementContext = CustomContext.addConstants(element.constants, context)
        var debugMsg = "Processed element name: ${element.name}, required: ${element.required}, "

        // First we need to resolve a resource value if available.
        val focusResources = getFocusResources(element.resource, bundle, focusResource, elementContext)
        if (focusResources.isEmpty() && element.required == true) {
            // There are no sources to parse, but the element was required
            throw RequiredElementException(element)
        } else if (focusResources.isEmpty()) debugMsg += "resource: NONE"

        focusResources.forEachIndexed { index, singleFocusResource ->
            // The element context must now get the focus resource
            elementContext.focusResource = singleFocusResource
            if (canEvaluate(element, bundle, singleFocusResource, elementContext)) {
                when {
                    // If this is a schema then process it.
                    element.schemaRef != null -> {
                        // Schema references can have new index references
                        val indexContext = if (element.resourceIndex.isNullOrBlank()) elementContext
                        else CustomContext.addConstant(
                            element.resourceIndex!!,
                            index.toString(),
                            elementContext
                        )
                        logger.log(logLevel, "Processing element ${element.name} with schema ${element.schema} ...")
                        transformWithSchema(
                            element.schemaRef!! as FhirTransformSchema,
                            bundle,
                            singleFocusResource,
                            transformedBundle,
                            indexContext,
                            element.debug || debug
                        )
                    }

                    // A value
                    element.value.isNotEmpty() -> {
                        val value = getValue(element, bundle, singleFocusResource, elementContext)
                        if (value != null) {
                            setBundleProperty(element, value, context, transformedBundle)
                        }
                        debugMsg += "condition: true, resourceType: ${singleFocusResource.fhirType()}, " +
                            "value: $value"
                    }

                    // This should never happen as the schema was validated prior to getting here
                    else -> throw IllegalStateException()
                }
            } else if (element.required == true) {
                // The condition was not met, but the element was required
                throw RequiredElementException(element)
            } else {
                debugMsg += "condition: false, resourceType: ${singleFocusResource.fhirType()}"
            }
        }
        // Only log for elements that require values
        if (element.schemaRef == null) logger.log(logLevel, debugMsg)
        logger.trace("End processing of element ${element.name}.")
    }

    /**
     * Get the first valid string from the list of values specified in the schema for a given [element] using
     * [bundle] and [context] starting at the [focusResource].
     * @return the value for the element or an empty string if no value found
     */
    internal fun getValue(
        element: ConfigSchemaElement,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext
    ): Base? {
        var retVal: Base? = null
        run findValue@{
            element.value.forEach {
                val value = if (it.isBlank()) emptyList<Base>()
                else try {
                    FhirPathUtils.evaluate(context, focusResource, bundle, it)
                } catch (e: SchemaException) {
                    logger.error("Error while getting value for element ${element.name}", e)
                    emptyList<Base>()
                }
                logger.trace("Evaluated value expression '$it' to '$value'")
                if (value.isNotEmpty()) {
                    retVal = value[0]
                    return@findValue
                }
            }
        }

        // when valueSet is available, use the matching value else just pass the value as is
        // does a lowerCase comparison
        if (element.valueSet.isNotEmpty()) {
            val lowerSet = element.valueSet.mapKeys { it.key.lowercase() }
            val valStr =
                lowerSet.getOrDefault(retVal?.primitiveValue()?.lowercase() ?: "", retVal?.primitiveValue() ?: "")
            retVal = FhirPathUtils.evaluate(context, focusResource, bundle, valStr)[0]
        }
        return retVal
    }

    /**
     * Determine the focus resource for an [element] using [bundle] and the [previousFocusResource].
     * @return a list of focus resources containing at least one resource.  Multiple resources are returned for collections
     */
    internal fun getFocusResources(
        resourceStr: String?,
        bundle: Bundle,
        previousFocusResource: Base,
        context: CustomContext
    ): List<Base> {
        val resourceList = if (resourceStr == null || resourceStr == "") {
            listOf(previousFocusResource)
        } else {
            val evaluatedResource = FhirPathUtils
                .evaluate(context, previousFocusResource, bundle, resourceStr)
            evaluatedResource
        }

        return resourceList
    }

    /**
     * Test if an [element] can be evaluated based on the [element]'s condition.  Use the [bundle] and [focusResource] * to evaluate the condition expression.
     * @return true if the condition expression evaluates to a boolean or if the condition expression is empty, false otherwise
     */
    internal fun canEvaluate(
        element: ConfigSchemaElement,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext
    ): Boolean {
        return element.condition?.let {
            try {
                FhirPathUtils.evaluateCondition(context, focusResource, bundle, it)
            } catch (e: SchemaException) {
                logger.warn(
                    "Condition for element ${element.name} did not evaluate to a boolean type, " +
                        "so the condition failed."
                )
                false
            }
        } ?: true
    }

    /**
     * Set the [value] an [element]'s bundleProperty.
     */
    internal fun setBundleProperty(
        element: FHIRTransformSchemaElement, // TODO: remove and add only needed fields
        value: Base,
        context: CustomContext,
        bundle: Bundle
    ) {
        var bundleProperty = element.bundleProperty // "%resource.id" "Bundle.something.something.property"
        if (bundleProperty != null) {
            val focusResources = if (bundleProperty.startsWith("%resource.")) {
                bundleProperty = bundleProperty.substringAfter("%resource.")
                getFocusResources(element.resource, bundle, bundle, context)
            } else { // if (bundleProperty.startsWith("Bundle.")
                listOf(bundle)
            } // else error
            focusResources.forEach { focusResource ->
                val path = bundleProperty.substringBeforeLast(".", "")
                val field = bundleProperty.substringAfterLast(".")
                val propertiesToUpdate = getFocusResources(path, bundle, focusResource, context)
                if (propertiesToUpdate.isEmpty()) {
                    // TODO: Create missing properties
                }
                propertiesToUpdate.forEach {
                    val property = it.getNamedProperty(field)
                    var sourceType = property.typeCode
                    var targetType = value.fhirType()

                    val newValue = convertFhirType(value, sourceType, targetType)

                    it.setProperty(field, newValue)
                }
            }
            println(bundle)
        }
    }
}