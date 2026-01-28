package gov.cdc.prime.router.fhirengine.translation.hl7

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.fhirconverter.translation.hl7.ConfigSchemaProcessor
import gov.cdc.prime.fhirconverter.translation.hl7.RequiredElementException
import gov.cdc.prime.fhirconverter.translation.hl7.SchemaException
import gov.cdc.prime.fhirconverter.translation.hl7.schema.ConfigSchemaElementProcessingException
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.fhirconverter.translation.hl7.schema.fhirTransform.FhirTransformSchemaElementAction
import gov.cdc.prime.fhirconverter.translation.hl7.utils.CustomContext
import gov.cdc.prime.fhirconverter.translation.hl7.utils.FhirBundleUtils
import gov.cdc.prime.fhirconverter.translation.hl7.utils.FhirBundleUtils.deleteResource
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import org.apache.logging.log4j.Level
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Property

/**
 * Transform a FHIR bundle based on the [schemaRef].
 */
class FhirTransformer(
    private val schemaRef: FhirTransformSchema,
    errors: MutableList<String> = mutableListOf(),
    warnings: MutableList<String> = mutableListOf(),
) : ConfigSchemaProcessor<
        Bundle,
        Bundle,
        FhirTransformSchema,
        FhirTransformSchemaElement
    >(schemaRef, errors, warnings) {
    private val extensionRegex = """^extension\(["'](?<extensionUrl>[^'"]+)["']\)""".toRegex()
    private val valueXRegex = Regex("""value[A-Z][a-z]*""")
    private val indexRegex = Regex("""(?<child>.*)\[%?(?<indexVar>[0-9A-Za-z]*)\]""")

    /**
     * Transform the given [bundle]. The bundle passed in will be updated directly, and will also be returned.
     * @return the transformed bundle
     */
    override fun process(input: Bundle): Bundle {
        transformWithSchema(schemaRef, bundle = input, focusResource = input)
        return input
    }

    override fun checkForEquality(converted: Bundle, expectedOutput: Bundle): Boolean =
        converted.equalsDeep(expectedOutput)

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

        val warnings = mutableListOf<String>()
        val eligibleFocusResources =
            focusResources.filter { canEvaluate(element, bundle, it, focusResource, elementContext) }
        when (element.action) {
            FhirTransformSchemaElementAction.SET -> {
                eligibleFocusResources.forEach { singleFocusResource ->
                    elementContext.focusResource = singleFocusResource
                    val value = getValue(element, bundle, singleFocusResource, elementContext)
                    val function = element.function
                    if (value != null && function != null) {
                        throw SchemaException("Element can only set function or value")
                    }
                    val bundleProperty = element.bundleProperty
                        ?: throw SchemaException("bundleProperty must be set for element ${element.name}")
                    if (value != null) {
                        updateBundle(
                            bundleProperty,
                            value,
                            elementContext,
                            bundle,
                            singleFocusResource
                        )
                    } else if (function != null) {
                        updateBundle(
                            bundleProperty,
                            function,
                            elementContext,
                            bundle,
                            singleFocusResource
                        )
                    } else {
                        logger.warn(
                            "Element ${element.name} is updating a bundle property," +
                                " but did not specify a value or function"
                        )
                        warnings.add(
                            "Element ${element.name} is updating a bundle property, " +
                            "but did not specify a value"
                        )
                    }
                    debugMsg += "condition: true, resourceType: ${singleFocusResource.fhirType()}, " +
                        "value: $value"
                }
            }
            FhirTransformSchemaElementAction.APPEND -> {
                val existing =
                    if (element.appendToProperty != null) {
                        FhirPathUtils.evaluate(elementContext, bundle, bundle, element.appendToProperty!!).size
                    } else {
                        0
                    }
                eligibleFocusResources.forEachIndexed { index, singleFocusResource ->
                    elementContext.focusResource = singleFocusResource
                    val value = getValue(element, bundle, singleFocusResource, elementContext)
                    val appendToProperty = element.appendToProperty
                        ?: throw SchemaException("appendToProperty must be set if the action is append")
                    // The limitation here is that nested-nested schemas cannot do appends, but that seems fine
                    val appendContext = if (CustomContext.getAppendToIndex(elementContext) != null) {
                        elementContext
                    } else {
                        CustomContext.setAppendToIndex(existing + index, elementContext)
                    }
                    if (value != null) {
                        val bundleProperty = element.bundleProperty
                            ?: throw SchemaException("bundleProperty must be set for element ${element.name}")
                        updateBundle(
                            bundleProperty,
                            value,
                            appendContext,
                            bundle,
                            singleFocusResource,
                            appendToProperty
                        )
                    } else if (element.schemaRef != null) {
                        transformWithSchema(
                            element.schemaRef!!,
                            bundle,
                            singleFocusResource,
                            appendContext,
                            element.debug || debug
                        )
                    }
                }
            }
            FhirTransformSchemaElementAction.APPLY_SCHEMA -> {
                eligibleFocusResources.forEachIndexed { index, singleFocusResource ->
                    elementContext.focusResource = singleFocusResource
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
            }
            FhirTransformSchemaElementAction.DELETE -> {
                eligibleFocusResources.forEach { singleFocusResource ->
                    elementContext.focusResource = singleFocusResource
                    bundle.deleteResource(singleFocusResource, removeOrphanedDiagnosticReport = false)
                }
            }
        }
        // Only log for elements that require values
        if (element.schemaRef == null) logger.log(logLevel, debugMsg)
        logger.trace("End processing of element ${element.name}.")
    }

    /**
     * Data class that holds extracted information from processing an element in a FHIR path
     *
     * @param rawProperty the original string from splitting up the FHIR path
     * @param propertyString the string version of property with indices and extension URLs removed, i.e. `note` or `extension`
     * @param property the actual [Property], will not be populated if the [rawProperty] is a FHIR function, i.e. `ofType(Observation)
     * @param extensionUrl the url associated with the extension
     * @param index the index if one exists in the [rawProperty]
     */
    data class ElementInformation(
        val rawProperty: String,
        val propertyString: String,
        val property: Property?,
        val extensionUrl: String?,
        val index: Int?,
    ) {
        fun isExtension(): Boolean = propertyString == "extension"

        fun isValue(): Boolean = propertyString == "value"
    }

    /**
     * Parses a part of a FHIR path expression and extracts out the relevant information required while updating
     * the bundle
     *
     * @param rawProperty the part of the property to extract information
     * @param context contains any variables that have been
     * @param parent the parent FHIR element that the property is chained from
     * @return [ElementInformation]
     */
    private fun extractChildProperty(rawProperty: String, context: CustomContext, parent: Base): ElementInformation {
        val indexMatch = indexRegex.find(rawProperty)
        val valueMatch = valueXRegex.find(rawProperty)
        val extensionMatch = extensionRegex.find(rawProperty)
        val propertyString = if (extensionMatch != null) {
            "extension"
        } else if (valueMatch != null) {
            "value"
        } else if (indexMatch?.groups?.get("child") != null) {
            indexMatch.groups["child"]?.value
        } else {
            rawProperty
        }
        if (propertyString == null) {
            throw SchemaException("Could not part child property from $rawProperty")
        }
        val property: Property? = propertyString.let { parent.getNamedProperty(propertyString) }
        val extensionUrl = extensionMatch?.groups?.get("extensionUrl")?.value
        val indexVariable = indexMatch?.groups?.get("indexVar")?.value
        val index = if (indexVariable?.all { it.isDigit() } == true) {
            indexVariable.toInt()
        } else {
            context.constants[indexVariable]?.toInt()
        }

        return ElementInformation(
            rawProperty,
            propertyString,
            property,
            extensionUrl,
            index
        )
    }

    /**
     * Updates a bundle by setting a value at a specified spot
     *
     * @param bundleProperty the property to update
     * @param value the value to set the property to
     * @param context the context to evaluate the bundle under
     * @param focusResource the focus resource for any FHIR path evaluations
     */
    internal fun updateBundle(
        bundleProperty: String,
        value: Base,
        context: CustomContext,
        bundle: Bundle,
        focusResource: Base,
    ) {
        val (lastElement, penultimateElements) = createMissingElementsInBundleProperty(
            bundleProperty,
            context,
            bundle,
            focusResource,
            null
        )
        setBundleProperty(penultimateElements, lastElement, value, context)
    }

    /**
     * Updates a bundle by setting a value at a specified spot
     *
     * @param bundleProperty the property to update
     * @param function the function to apply to the bundle property
     * @param context the context to evaluate the bundle under
     * @param focusResource the focus resource for any FHIR path evaluations
     */
    internal fun updateBundle(
        bundleProperty: String,
        function: String,
        context: CustomContext,
        bundle: Bundle,
        focusResource: Base,
    ) {
        val (lastElement, penultimateElements) = createMissingElementsInBundleProperty(
            bundleProperty,
            context,
            bundle,
            focusResource,
            null
        )
        applyFunction(penultimateElements, lastElement, function, context, bundle)
    }

    /**
     * Updates a bundle by appending new elements at a FHIR path.
     *
     * @param bundleProperty the property that should be set for each appended resource
     * @param value the value that the bundle property should be set
     * @param context the context to evaluate the bundle with
     * @param bundle the bundle getting modified
     * @param focusResource the focus resource for any FHIR path evaluations
     * @param appendToProperty the property in that the bundle where elements should added to
     */
    private fun updateBundle(
        bundleProperty: String,
        value: Base,
        context: CustomContext,
        bundle: Bundle,
        focusResource: Base,
        appendToProperty: String,
    ) {
        val (lastAppendToProperty, penultimateElements) = createMissingElementsInBundleProperty(
            appendToProperty,
            context,
            bundle,
            focusResource,
            null
        )

        val appendToStartingIndex = CustomContext.getAppendToIndex(context) ?: 0
        val appendToElements = penultimateElements.map { penultimateElement ->
            val childInformation = extractChildProperty(lastAppendToProperty, context, penultimateElement)
            val existingChildren = getExistingElementsForChildProperty(
                penultimateElement,
                childInformation,
                context,
                bundle
            )
            if (existingChildren.isEmpty() || existingChildren.getOrNull(appendToStartingIndex) == null) {
                createNewChild(childInformation, penultimateElement)
            } else {
                existingChildren[appendToStartingIndex]
            }
        }

        val (lastBundlePropertyElement, bundlePenultimateElements) = createMissingElementsInBundleProperty(
            bundleProperty,
            context,
            bundle,
            focusResource,
            appendToElements
        )
        setBundleProperty(bundlePenultimateElements, lastBundlePropertyElement, value, context)
    }

    /**
     * Updates a list of [Base] by applying the passed FHIR [function]
     *
     * @param elementsToUpdate the list of [Base] to update
     * @param propertyName the property to set on each element
     * @param function the function to apply
     */
    private fun applyFunction(
        elementsToUpdate: List<Base>,
        propertyName: String,
        function: String,
        context: CustomContext,
        bundle: Bundle,
    ) {
        elementsToUpdate.forEach { penultimateElement ->
            val propertyInfo = extractChildProperty(propertyName, context, penultimateElement)
            FhirPathUtils.evaluate(
                context, penultimateElement, bundle, "%resource.${propertyInfo.propertyString}.$function"
            )
        }
    }

    /**
     * Updates a list of [Base] setting a property named [propertyName] to the passed value
     *
     * @param elementsToUpdate the list of [Base] to update
     * @param propertyName the property to set on each element
     * @param value what the property will get set to
     */
    private fun setBundleProperty(
        elementsToUpdate: List<Base>,
        propertyName: String,
        value: Base,
        context: CustomContext,
    ) {
        elementsToUpdate.forEach { penultimateElement ->
            val propertyInfo = extractChildProperty(propertyName, context, penultimateElement)
            if (propertyInfo.index != null) {
                throw SchemaException("Schema is attempting to set a value for a particular index which is not allowed")
            }
            val property = penultimateElement.getNamedProperty(propertyName)
            val newValue = FhirBundleUtils.convertFhirType(value, value.fhirType(), property.typeCode, logger)
                penultimateElement.setProperty(propertyName, newValue.copy())
            }
        }

    private fun createMissingElementsInBundleProperty(
        fhirPath: String,
        context: CustomContext,
        bundle: Bundle,
        focusResource: Base,
        startingElements: List<Base>?,
    ): Pair<String, List<Base>> {
        val pathParts = validateAndSplitBundleProperty(fhirPath)
        val lastElement = pathParts.last()
        val tailElements = if (startingElements != null) {
            pathParts.dropLast(1)
        } else {
            pathParts.drop(1).dropLast(1)
        }
        val foldStart = startingElements ?: FhirPathUtils.evaluate(context, focusResource, bundle, pathParts.first())
        val penultimateElements = tailElements.fold(foldStart) { elements, child ->
            elements.flatMap { element ->
                val childInformation = extractChildProperty(child, context, element)
                val existingChildren = getExistingElementsForChildProperty(
                    element,
                    childInformation,
                    context,
                    bundle
                )
                if (existingChildren.isEmpty() && childInformation.property != null) {
                    listOf(createNewChild(childInformation, element))
                } else {
                    if (childInformation.index != null) {
                        if (existingChildren.getOrNull(childInformation.index) == null) {
                            // TODO this should be disallowed as it leads to the chance that a value will be overwritten
                            listOf(createNewChild(childInformation, element))
                        } else {
                            listOf(existingChildren[childInformation.index])
                        }
                    } else {
                        existingChildren
                    }
                }
            }
        }
        return Pair(lastElement, penultimateElements)
    }

    /**
     * Creates a new child element on the passed [element].  If the child getting added is
     * also an extension, the URL for extension will be set too
     *
     * @param childInformation details on the child that should be created
     * @param element the element to add a child to
     * @return the created element
     */
    private fun createNewChild(
        childInformation: ElementInformation,
        element: Base,
    ): Base = if (childInformation.isExtension()) {
        val childResource = element.addChild("extension")
        (childResource as Extension).url = childInformation.extensionUrl
        childResource
    } else if (childInformation.isValue()) {
        element.addChild(childInformation.rawProperty)
    } else {
        element.addChild(childInformation.propertyString)
    }

    /**
     * Finds existing children of the passed in [element] by either using the [ElementInformation.propertyString]
     * and [Base.listChildrenByName] or evaluating a FHIR path if the child is a FHIR function (i.e. ofType(ServiceRequest)
     *
     * @param element - The FHIR element to search for children
     * @param childInformation - details on the children to find
     * @param context - [CustomContext] to use when evaluating the FHIR epxression
     * @param bundle - the bundle getting transformed
     * @return the list of children
     */
    private fun getExistingElementsForChildProperty(
        element: Base,
        childInformation: ElementInformation,
        context: CustomContext,
        bundle: Bundle,
    ): List<Base> = try {
        val children = if (childInformation.isExtension()) {
            // Extension is both a property and a function so we evaluate it as a FHIR path using the original
            // parsed property, i.e. extension("https://reportstream.cdc.gov/fhir/StructureDefinition/nte-annotation")
            FhirPathUtils.evaluate(context, element, bundle, "%resource.${childInformation.rawProperty}")
        } else {
            element.listChildrenByName(childInformation.propertyString)
        }
        children
    } catch (ex: FHIRException) {
        // This is thrown by listChildrenByName when the property does not exist on the element
        // if it doesn't exist we try evaluating as a FHIR path
        val evaluated =
            FhirPathUtils.evaluate(context, element, bundle, "%resource.${childInformation.rawProperty}")
        evaluated
    }

    /**
     * Returns a non-empty list of path parts represented by the `bundleProperty`,
     * or an empty list if the input was not usable.
     */
    internal fun validateAndSplitBundleProperty(bundleProperty: String?): List<String> {
        if (bundleProperty.isNullOrBlank()) {
            logger.error("bundleProperty was not set.")
            throw SchemaException("bundleProperty was not set.")
        }

        val pathParts = splitBundlePropertyPath(bundleProperty)
        if (pathParts.isEmpty()) {
            logger.error("Invalid FHIR path for '$bundleProperty'.")
            throw SchemaException("Invalid FHIR path for '$bundleProperty'.")
        }

        if (pathParts.last().contains('%')) {
            logger.error(
                "Constants not supported in lowest level component of bundle property, found" +
                    " '${pathParts.last()}'."
            )
            throw SchemaException(
                "Constants not supported in lowest level component of bundle property, found" +
                " '${pathParts.last()}'."
            )
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