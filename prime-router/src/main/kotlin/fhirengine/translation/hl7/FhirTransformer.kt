package gov.cdc.prime.router.fhirengine.translation.hl7

import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FHIRTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirBundleUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.Level
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension

/**
 * Transform a FHIR bundle based on the [schemaRef].
 * The transformer will error out if [strict] is set to true and there is an error during the translation. If [strict]
 * is set to false (the default) then any translation errors are logged as a warning. Note [strict] does not affect
 * the schema validation process.
 */
class FhirTransformer(
    private val schemaRef: FhirTransformSchema,
    private val strict: Boolean = false,
) : ConfigSchemaProcessor() {
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
     * Transform the given [bundle]. The bundle passed in will be updated directly, and will also be returned.
     * @return the transformed bundle
     */
    fun transform(bundle: Bundle): Bundle {
        val dupes = schemaRef.duplicateElements
        if (dupes.isNotEmpty()) { // value is the number of matches
            throw SchemaException("Schema ${schemaRef.name} has multiple elements with the same name: ${dupes.keys}")
        }
        transformWithSchema(schemaRef, bundle = bundle, focusResource = bundle)
        return bundle
    }

    /**
     * Transform the [bundle] using the elements in the given [schema] using [context] starting at the
     * [focusResource] in the bundle. Set [debug] to true to enable debug statements to the logs.
     */
    private fun transformWithSchema(
        schema: FhirTransformSchema,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext = CustomContext(bundle, focusResource),
        debug: Boolean = false
    ) {
        val logLevel = if (debug) Level.INFO else Level.DEBUG
        logger.log(logLevel, "Processing schema: ${schema.name} with ${schema.elements.size} elements")
        // Add any schema level constants to the context
        // We need to create a new context, so constants exist only within their specific schema tree
        val schemaContext = CustomContext.addConstants(schema.constants, context)

        schema.elements.forEach { element ->
            transformBasedOnElement(element, bundle, focusResource, schemaContext, debug)
        }
    }

    /**
     * Transform the [bundle] using [element] and [context] starting at the
     * [focusResource] in the bundle. Set [debug] to true to enable debug statements to the logs.
     */
    internal fun transformBasedOnElement(
        element: FHIRTransformSchemaElement,
        bundle: Bundle,
        focusResource: Base,
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
                            indexContext,
                            element.debug || debug
                        )
                    }

                    // A value
                    element.value.isNotEmpty() -> {
                        val value = getValue(element, bundle, singleFocusResource, elementContext)
                        if (value != null) {
                            setBundleProperty(
                                element.bundleProperty,
                                value,
                                context,
                                bundle,
                                singleFocusResource
                            )
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
     * Set the [value] on [bundleProperty] using [bundle] as the root resource and [focusResource] as the focus resource
     */
    internal fun setBundleProperty(
        bundleProperty: String?,
        value: Base,
        context: CustomContext,
        bundle: Bundle,
        focusResource: Base
    ) {
        if (bundleProperty == null) return

        val pathParts = bundleProperty.split(".")
        // We start one level down as we use the addChild function to set the value at the end
        var pathToEvaluate = bundleProperty.dropLast(pathParts.last().length + 1)
        val childrenNames = pathParts.dropLast(1).reversed()
        val missingChildren = mutableListOf<String>()
        childrenNames.forEach { childName ->
            if (FhirPathUtils.evaluate(context, focusResource, bundle, pathToEvaluate).isEmpty()) {
                pathToEvaluate = pathToEvaluate.dropLast(childName.length + 1)
                missingChildren.add(childName)
            } else return@forEach
        }
        if (missingChildren.isNotEmpty()) {
            println("Missing $missingChildren children. Stopped at: $pathToEvaluate")
            check(missingChildren.last() != "entry") // We do not need to support entries
        }
        // Now go on reverse and create the needed children
        val parent = FhirPathUtils.evaluate(context, focusResource, bundle, pathToEvaluate)
        if (parent.size != 1) throw Exception()
        var childResource = parent[0]
        missingChildren.reversed().forEach { childName ->
            when {
                childName.startsWith("extension(") -> {
                    val matchResult = extensionRegex.find(childName)
                    if (matchResult != null) {
                        childResource = childResource.addChild("extension")
                        (childResource as Extension).url = matchResult.groupValues[1]
                    }
                }
                else -> childResource = childResource.addChild(childName)
            }
        }
        // Finally set the value
        val property = childResource.getNamedProperty(pathParts.last())
        val newValue = FhirBundleUtils.convertFhirType(value, value.fhirType(), property.typeCode)
        childResource.setProperty(pathParts.last(), newValue)
    }
}

private val extensionRegex = """^extension\(["']([^'"]+)["']\)""".toRegex()