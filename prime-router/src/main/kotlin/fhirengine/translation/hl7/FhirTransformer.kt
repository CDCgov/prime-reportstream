package gov.cdc.prime.router.fhirengine.translation.hl7

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElementProcessingException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElementAction
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.fhirTransformSchemaFromFile
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirBundleUtils
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.deleteResource
import org.apache.logging.log4j.Level
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension

/**
 * Transform a FHIR bundle based on the [schemaRef].
 */
class FhirTransformer(
    private val schemaRef: FhirTransformSchema,
) : ConfigSchemaProcessor<Bundle, Bundle, FhirTransformSchema, FhirTransformSchemaElement>(schemaRef) {
    private val extensionRegex = """^extension\(["']([^'"]+)["']\)""".toRegex()

    /**
     * Transform a FHIR bundle based on the [schema] in the [schemaFolder] location.
     */
    constructor(
        schema: String,
        blobConnectionInfo: BlobAccess.BlobContainerMetadata = BlobAccess.BlobContainerMetadata.build(
            "metadata",
            Environment.get().blobEnvVar
        ),
    ) : this(
        schemaRef = fhirTransformSchemaFromFile(schema, blobConnectionInfo),
    )

    /**
     * Transform the given [bundle]. The bundle passed in will be updated directly, and will also be returned.
     * @return the transformed bundle
     */
    override fun process(input: Bundle): Bundle {
        transformWithSchema(schemaRef, bundle = input, focusResource = input)
        return input
    }

    override fun checkForEquality(converted: Bundle, expectedOutput: Bundle): Boolean {
        return converted.equalsDeep(expectedOutput)
    }

    /**
     * Transform the [bundle] using the elements in the given [schema] using [context] starting at the
     * [focusResource] in the bundle. Set [debug] to true to enable debug statements to the logs.
     */
    private fun transformWithSchema(
        schema: FhirTransformSchema,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext = CustomContext(bundle, focusResource, customFhirFunctions = CustomFhirPathFunctions()),
        debug: Boolean = false,
    ) {
        val logLevel = if (debug) Level.INFO else Level.DEBUG
        logger.log(logLevel, "Processing schema: ${schema.name} with ${schema.elements.size} elements")
        // Add any schema level constants to the context
        // We need to create a new context, so constants exist only within their specific schema tree
        val schemaContext = CustomContext.addConstants(schema.constants, context)

        schema.elements.forEach { element ->
            try {
                transformBasedOnElement(element, bundle, focusResource, schemaContext, debug)
            } catch (ex: Exception) {
                throw ConfigSchemaElementProcessingException(schema, element, ex)
            }
        }
    }

    /**
     * Transform the [bundle] using [element] and [context] starting at the
     * [focusResource] in the bundle. Set [debug] to true to enable debug statements to the logs.
     */
    internal fun transformBasedOnElement(
        element: FhirTransformSchemaElement,
        bundle: Bundle,
        focusResource: Base,
        context: CustomContext,
        debug: Boolean = false,
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
        } else if (focusResources.isEmpty()) {
            debugMsg += "resource: NONE"
        }

        focusResources.forEachIndexed { index, singleFocusResource ->
            // The element context must now get the focus resource
            elementContext.focusResource = singleFocusResource
            if (canEvaluate(element, bundle, singleFocusResource, focusResource, elementContext)) {
                when (element.action) {
                    FhirTransformSchemaElementAction.SET -> {
                        val value = getValue(element, bundle, singleFocusResource, elementContext)
                        if (value != null) {
                            setBundleProperty(
                                element.bundleProperty,
                                value,
                                context,
                                bundle,
                                singleFocusResource
                            )
                        } else {
                            logger.warn(
                                "Element ${element.name} is updating a bundle property, but did not specify a value"
                            )
                        }
                        debugMsg += "condition: true, resourceType: ${singleFocusResource.fhirType()}, " +
                            "value: $value"
                    }
                    FhirTransformSchemaElementAction.APPEND -> {
                        // https://github.com/CDCgov/prime-reportstream/issues/15169
                        throw NotImplementedError()
                    }
                    FhirTransformSchemaElementAction.APPLY_SCHEMA -> {
                        // Schema references can have new index references
                        val indexContext = if (element.resourceIndex.isNullOrBlank()) {
                            elementContext
                        } else {
                            CustomContext.addConstant(
                                element.resourceIndex!!,
                                index.toString(),
                                elementContext
                            )
                        }
                        logger.log(logLevel, "Processing element ${element.name} with schema ${element.schema} ...")
                        transformWithSchema(
                            element.schemaRef!!,
                            bundle,
                            singleFocusResource,
                            indexContext,
                            element.debug || debug
                        )
                    }
                    FhirTransformSchemaElementAction.DELETE -> {
                        bundle.deleteResource(singleFocusResource, removeOrphanedDiagnosticReport = false)
                    }
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
        focusResource: Base,
    ) {
        val pathParts = validateAndSplitBundleProperty(bundleProperty)
        if (pathParts.isNullOrEmpty() || bundleProperty == null) {
            return
        }
        // We start one level down as we use the addChild function to set the value at the end
        var pathToEvaluate = bundleProperty.dropLast(pathParts.last().length + 1)
        val childrenNames = pathParts.dropLast(1).reversed()
        val missingChildren = mutableListOf<String>()
        childrenNames.forEach { childName ->
            val pathToEvaluatedFixedForFhirPath = convertToValidFhirPath(pathToEvaluate)
            if (FhirPathUtils.evaluate(context, focusResource, bundle, pathToEvaluatedFixedForFhirPath).isEmpty()) {
                if (childName.contains('%')) {
                    logger.error(
                        "Could not evaluate path '$pathToEvaluate', and cannot dynamically create" +
                            " components relying on constants."
                    )
                    return
                }
                pathToEvaluate = pathToEvaluate.dropLast(childName.length + 1)
                missingChildren.add(childName)
            } else {
                return@forEach
            }
        }
        if (missingChildren.isNotEmpty()) {
            logger.trace("Missing $missingChildren children. Stopped at: $pathToEvaluate")
            check(missingChildren.last() != "entry") { "Can't add missing entry." } // We do not need to support entries
        }
        // Now go on reverse and create the needed children
        val pathToEvaluatedFixedForFhirPath = convertToValidFhirPath(pathToEvaluate)
        val parent = FhirPathUtils.evaluate(context, focusResource, bundle, pathToEvaluatedFixedForFhirPath)
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
                else -> childResource = childResource.addChild(childName.replace("""\[[0-9]+\]""".toRegex(), ""))
            }
        }
        // Finally set the value
        val property = childResource.getNamedProperty(pathParts.last())
        if (property != null) {
            val newValue = FhirBundleUtils.convertFhirType(value, value.fhirType(), property.typeCode, logger)
            // Use a copy to prevent endless looping on extensions
            childResource.setProperty(pathParts.last(), newValue.copy())
        } else {
            logger.error("Could not find property '${pathParts.last()}'.")
        }
    }

    private fun convertToValidFhirPath(pathToEvaluate: String) =
        pathToEvaluate.replace(Regex("value[a-zA-Z]*"), "value")

    /**
     * Returns a non-empty list of path parts represented by the `bundleProperty`,
     * or an empty list if the input was not usable.
     */
    internal fun validateAndSplitBundleProperty(bundleProperty: String?): List<String> {
        if (bundleProperty.isNullOrBlank()) {
            logger.error("bundleProperty was not set.")
            return emptyList()
        }

        val pathParts = splitBundlePropertyPath(bundleProperty)
        if (pathParts.isEmpty()) {
            logger.error("Invalid FHIR path for '$bundleProperty'.")
            return emptyList()
        }
        if (pathParts.size < 2) {
            logger.error("Expected at least 2 parts in bundle property '$bundleProperty'.")
            return emptyList()
        } else if (pathParts.last().contains('%')) {
            logger.error(
                "Constants not supported in lowest level component of bundle property, found" +
                    " '${pathParts.last()}'."
            )
            return emptyList()
        }
        return pathParts
    }

    /**
     * Splits a [bundleProperty] '.' delimited into a list of path parts
     * or an empty list if the input was not usable.
     */
    internal fun splitBundlePropertyPath(bundleProperty: String): List<String> {
        val parts: MutableList<String> = mutableListOf()
        var foundParenthesis = false
        var part = ""
        bundleProperty.toList().forEach {
            // Only add parts if outside parenthesis. To make sure things
            // like extensions are not split up
            if (!foundParenthesis && it == '.') {
                parts += part
                part = ""
            } else {
                part += it
                if (it == '(') {
                    foundParenthesis = true
                } else if (foundParenthesis && it == ')') {
                    foundParenthesis = false
                }
            }
        }
        if (part != "") {
            parts += part
        }

        // This is an invalid path if a closing parenthesis is not found
        return if (foundParenthesis) {
            mutableListOf()
        } else {
            parts
        }
    }
}