package gov.cdc.prime.router.fhirengine.utils

import com.fasterxml.jackson.databind.node.ArrayNode
import com.flipkart.zjsonpatch.JsonDiff
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.common.JacksonMapperUtilities.jacksonObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import java.io.InputStream

/**
 * Compare two FHIR files.
 * @property result object to contain the result of the comparison
 * @property skippedProperties FHIR type path name for a property to ignore (e.g. Bundle.metadata)
 * @property dynamicProperties FHIR type path name for a property to test, but ignore its value
 */
class CompareFhirData(
    internal val result: CompareData.Result = CompareData.Result(),
    private val skippedProperties: List<String> = defaultSkippedProperties,
    private val dynamicProperties: List<String> = defaultDynamicProperties,
) : Logging {

    // set of visited resources used during comparisons to avoid repeats (and maybe circular references?)
    private val visitedResources = mutableSetOf<String>()

    /**
     * Compare the data in the [actual] report to the data in the [expected] report.  This
     * comparison steps through all the resources in the FHIR bundle and compares all the values in the
     * existing properties.  Note that only resources that exist in the expected FHIR bundle are compared and
     * all others that exist only in the actual are ignored.  Also note that resource/properties do not have to
     * be in any specific order.
     * @return the result for the comparison, with result.passed true if the comparison was successful
     * Errors are generated when:
     *  1. When a resource does not exist in the actual bundle
     *  2. When a property is not the same.
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        result: CompareData.Result = CompareData.Result(),
    ): CompareData.Result {
        val expectedJson = expected.bufferedReader().readText()
        val actualJson = actual.bufferedReader().readText()

        logJsonDiff(expectedJson, actualJson)

        val expectedBundle = FhirTranscoder.decode(expectedJson)
        val actualBundle = FhirTranscoder.decode(actualJson)

        compareBundle(actualBundle, expectedBundle, result)
        return result
    }

    /**
     * Log JSON diff between the expected JSON and actual JSON
     *
     * @see https://datatracker.ietf.org/doc/html/rfc6902
     */
    private fun logJsonDiff(expectedJson: String, actualJson: String) {
        val mapper = jacksonObjectMapper
        val expectedParsedJson = mapper.readTree(expectedJson)
        val actualParsedJson = mapper.readTree(actualJson)

        val patches = JsonDiff.asJson(expectedParsedJson, actualParsedJson) as ArrayNode

        // remove all diffs that are expected
        val filtered = listOf("/id", "/fullUrl", "/lastUpdated", "/timestamp", "/reference", "/recorded")
        val filteredList = patches.filter { patch ->
            !filtered.any { filteredField ->
                patch.at("/path").asText().endsWith(filteredField)
            }
        }

        // pretty print array of diff objects if there are any
        val array = mapper.createArrayNode().addAll(filteredList)
        if (filteredList.isNotEmpty()) {
            logger.info(
                "JSON diff. Make the following changes to the expected JSON " +
                    "for it to match the actual JSON:\n${array.toPrettyString()}"
            )
        }
    }

    /**
     * Compare an [actualBundle] to an [expectedBundle] and provide the [result].
     */
    internal fun compareBundle(actualBundle: Bundle, expectedBundle: Bundle, result: CompareData.Result) {
        result.passed = true
        val resourcesToCompare = mutableMapOf<Resource, List<Base>>(expectedBundle to listOf(actualBundle))

        // Make a map of resources that can be compared.
        // Use all entries even though there will be some repeated comparisons to ensure we do not miss any orphans
        expectedBundle.entry.forEach { expectedEntry ->
            val matchingActualEntries = actualBundle.entry.filter {
                it.resource.fhirType() == expectedEntry.resource.fhirType()
            }.map { it.resource }
            if (matchingActualEntries.isEmpty()) {
                result.errors
                    .add("No matching actual entry resource found to compare to ${expectedEntry.resource.fhirType()}")
                result.passed = false
            }
            resourcesToCompare[expectedEntry.resource] = matchingActualEntries
        }

        // Compare all the resources
        resourcesToCompare.forEach { (expectedResource, actualResources) ->
            val resourceResult = compareResource(
                expectedResource,
                actualResources,
                getFhirIdPath("", expectedResource),
                getFhirTypePath("", expectedResource)
            )
            result.merge(resourceResult)
        }

        logger.info("FINAL RESULT: FHIR bundles are ${if (result.passed) "IDENTICAL" else "DIFFERENT"}")
        result.errors.forEach {
            logger.error(it)
        }

        // clear visited resources set in case we decided to run the same comparison again
        visitedResources.clear()
    }

    /**
     * Compare a list of [actualResources] to an [expectedResource] and provide the comparison result.
     * [parentTypePath] is the type path of the parent resource and [parentIdPath] is the ID path of the parent resource.
     * @return the comparison result
     */
    internal fun compareResource(
        expectedResource: Base,
        actualResources: List<Base>,
        parentIdPath: String,
        parentTypePath: String,
    ): CompareData.Result {
        // early return if we've already checked this resource
        val maybeResourcesId = generateResourcesId(actualResources)
        if (visitedResources.contains(maybeResourcesId)) {
            return CompareData.Result(true)
        }

        // add resources id to Set to avoid repeat resource comparisons
        maybeResourcesId?.also { visitedResources.add(it) }

        var result = CompareData.Result(true)
        result.passed = false
        var actualIdPath = ""
        // Compare the expected against each possible actual
        for (actualResource in actualResources) {
            actualIdPath = getFhirIdPath(parentIdPath, expectedResource)
            val resourceResult = compareProperties(expectedResource, actualResource, parentIdPath, parentTypePath)
            // We cannot just take the results as there may be multiple actuals to compare to.
            // Assume the best match is the one with the least errors or a positive match.
            if (!resourceResult.passed) {
                // Use the list of errors with the least number as that is probably closer to the one we have to compare to
                if (result.errors.isEmpty() || resourceResult.errors.size < result.errors.size) {
                    result = resourceResult
                }
            } else {
                // Actually have a match
                result = CompareData.Result(true)
                break
            }
        }
        val expectedIdPath = getFhirIdPath(parentIdPath, expectedResource)
        if (!result.passed) {
            result.errors.add(0, "FAILED: Resource $parentTypePath in $expectedIdPath has no match.")
        } else {
            logger.debug("MATCH: Resource $parentTypePath in $expectedIdPath matches with $actualIdPath")
        }
        return result
    }

    /**
     * Compare an [actualProperty] to an [expectedProperty] and provide the result.
     * [parentTypePath] is the type path of the parent resource and [parentIdPath] is the ID path of the parent resource.
     * @return the comparison result
     */
    internal fun compareProperties(
        expectedProperty: Base,
        actualProperty: Base,
        parentIdPath: String,
        parentTypePath: String,
    ): CompareData.Result {
        val result = CompareData.Result(true)
        // This is a good place to place breakpoints based on the resource ID.
        val expectedIdPath = getFhirIdPath(parentIdPath, expectedProperty)
        val actualIdPath = getFhirIdPath(parentIdPath, actualProperty)
        if (expectedProperty.isResource) {
            // Does it have an ID?
            logger.debug("PROPERTY: Comparing resource ${expectedProperty.fhirType()} $expectedIdPath to $actualIdPath")
        }
        // Only compare properties we need
        filterResourceProperties(expectedProperty).forEach { expectedChild ->
            val actualChild: Property? = actualProperty.getChildByName(expectedChild.name)
            if (actualChild != null && expectedChild.values.size != actualChild.values.size) {
                val differentChildSizesError = createDifferentChildSizesError(
                    expectedChild,
                    actualChild,
                    expectedIdPath,
                    actualIdPath,
                    parentTypePath
                )
                result.merge(differentChildSizesError)
            } else if (expectedChild.values.isNotEmpty() && actualChild != null) {
                // Properties with no expected values are ignored
                val actualValues = actualChild.values
                val expectedValues = expectedChild.values
                expectedValues.forEach { expectedValue ->
                    val actualsOfSameType = actualValues.filter { expectedValue.fhirType() == it.fhirType() }
                    // This is a good place to place breakpoints based on the resource type.
                    val propertyTypePath = "$parentTypePath.${expectedChild.name}"
                    val valueResult = when {
                        // Dissimilar types are not a match.
                        actualsOfSameType.isEmpty() -> {
                            val msg = "FAILED: No matching property of same type for $expectedIdPath $propertyTypePath"
                            CompareData.Result(
                                false,
                                arrayListOf(msg)
                            )
                        }

                        // Dynamic values are only checked to exist
                        dynamicProperties.contains(propertyTypePath) -> {
                            logger.trace("MATCH: Dynamic property $expectedIdPath $propertyTypePath exists")
                            CompareData.Result(true)
                        }

                        // References need to be compared as resources
                        expectedValue.hasType("Reference") -> {
                            compareReference(expectedValue, actualsOfSameType, expectedIdPath, propertyTypePath)
                        }

                        // Non-primitives are compared as resources
                        !expectedValue.isPrimitive ->
                            compareResource(expectedValue, actualsOfSameType, parentIdPath, propertyTypePath)

                        // Use the built-in equals for anything else.
                        else -> comparePrimitive(expectedValue, actualValues, expectedIdPath, propertyTypePath)
                    }
                    result.merge(valueResult)
                }
            } else if (expectedProperty.isPrimitive) {
                result.merge(comparePrimitive(expectedProperty, listOf(actualProperty), expectedIdPath, parentTypePath))
            }
        }
        return result
    }

    /**
     * Compare a FHIR primitive value of [actualPrimitive] to an [expectedPrimitive] and provide the result.
     * [primitiveTypePath] is the type path of the parent resource and [primitiveIdPath] is the ID path of the parent resource.
     * @return the comparison result
     */
    internal fun comparePrimitive(
        expectedPrimitive: Base,
        actualPrimitive: List<Base>,
        primitiveIdPath: String,
        primitiveTypePath: String,
    ): CompareData.Result {
        val primitiveResult = CompareData.Result()

        primitiveResult.passed = actualPrimitive.any {
            // Dynamic values are only checked to exist
            if (dynamicProperties.contains(primitiveTypePath)) {
                true
            } else {
                expectedPrimitive.equalsDeep(it)
            }
        }
        if (!primitiveResult.passed) {
            val msg = if (expectedPrimitive is PrimitiveType<*>) {
                val actualPrimitivesValues = actualPrimitive.filterIsInstance<PrimitiveType<*>>()
                    .joinToString { it.asStringValue() }

                "FAILED: Property $primitiveIdPath $primitiveTypePath did not match. " +
                    "expected=${expectedPrimitive.asStringValue()}, actuals=$actualPrimitivesValues"
            } else {
                "FAILED: Property $primitiveIdPath $primitiveTypePath did not match."
            }

            primitiveResult.errors.add(msg)
        } else {
            logger.trace("MATCH: Property $primitiveIdPath $primitiveTypePath matches")
        }
        return primitiveResult
    }

    /**
     * Compare a list of [actualReferences] to an [expectedReference] and provide the result.
     * [referenceTypePath] is the type path of the parent resource and [referenceIdPath] is the ID path of the
     * parent resource.
     * @return the comparison result
     */
    internal fun compareReference(
        expectedReference: Base,
        actualReferences: List<Base>,
        referenceIdPath: String,
        referenceTypePath: String,
    ): CompareData.Result {
        require(expectedReference is Reference)
        logger.debug("REFERENCE: Comparing reference from $referenceIdPath $referenceTypePath ...")
        val expectedResource = expectedReference.resource as Base
        val actualResources = actualReferences.mapNotNull { if (it is Reference) it.resource as Base else null }
        val result = compareResource(
            expectedResource, actualResources, getFhirIdPath(referenceIdPath, expectedResource),
            getFhirTypePath("", expectedResource)
        )
        logger.debug("REFERENCE: Done with comparison of $referenceIdPath $referenceTypePath --------------------")
        return result
    }

    /**
     * Filter the properties of a given [resource], so only the properties we want to compare are listed.
     * @return the list of properties to compare.
     */
    internal fun filterResourceProperties(resource: Base): List<Property> = resource.children().filter {
            // Skip any properties to be ignored
            val isSkipped = skippedProperties.contains("${resource.fhirType()}.${it.name}")
            // Skip any resource IDs
            val isResourceId = (resource.isResource && it.name == "id")
            // Skip the entry property of the bundle as those resources are handled elsewhere
            val isBundleEntry = (resource.hasType("Bundle") && it.name == "entry")
            !(isSkipped || isResourceId || isBundleEntry)
        }

    /**
     * There can be many reasons that the expected and actual child values differ.
     *
     * This function creates a more human-readable error for each case
     */
    private fun createDifferentChildSizesError(
        expectedChild: Property,
        actualChild: Property,
        expectedIdPath: String,
        actualIdPath: String,
        parentTypePath: String,
    ): CompareData.Result {
        val failure = CompareData.Result(passed = false)

        val propertyTypePath = "$parentTypePath.${expectedChild.name}"

        val errorMsg = if (actualChild.values.isEmpty() && expectedChild.values.isNotEmpty()) {
            "Expected property at $expectedIdPath $propertyTypePath is missing"
        } else if (actualChild.values.isNotEmpty() && expectedChild.values.isEmpty()) {
            "Property at $actualIdPath $propertyTypePath is not expected to be present"
        } else if (actualChild.values.size > expectedChild.values.size) {
            "Extra properties found at $actualIdPath $propertyTypePath. " +
                "actual=${actualChild.values.size}, expected=${expectedChild.values.size}"
        } else if (actualChild.values.size < expectedChild.values.size) {
            "Fewer properties found at $actualIdPath $propertyTypePath. " +
                "actual=${actualChild.values.size}, expected=${expectedChild.values.size}"
        } else {
            throw IllegalArgumentException("Expected child size and actual child size cannot be equal")
        }

        failure.errors.add(errorMsg)
        return failure
    }

    companion object {
        /**
         * The list of properties that will not be tested.
         */
        private val defaultSkippedProperties = emptyList<String>()

        /**
         * The list of properties that have dynamic values and hence will only be tested to exist.
         */
        private val defaultDynamicProperties = listOf(
            "Bundle.timestamp",
            "Bundle.meta.lastUpdated",
        )

        /**
         * Get the FHIR type path for a given [parentTypePath] and [resource].  The FHIR type path
         * is NOT the same as a FHIR path, and we use it to log the types we are comparing and to match types we want to
         * ignore.  E.g. Bundle.meta.lastUpdated, Organization.name.
         */
        internal fun getFhirTypePath(parentTypePath: String, resource: Base): String {
            val parentPath = if (parentTypePath.isNotBlank()) "$parentTypePath." else ""
            return parentPath + resource.fhirType()
        }

        /**
         * Get the FHIR ID path for a given [parentIdPath] and [resource].  The FHIR ID path
         * is NOT the same as a FHIR path, and we use it to log the resources we are comparing and to match types we
         * want to ignore.  E.g. Bundle.meta.lastUpdated, Organization.name.
         */
        internal fun getFhirIdPath(parentIdPath: String, resource: Base): String = when {
                resource is Extension ->
                    "$parentIdPath->${resource.fhirType()}(${resource.url.substringAfterLast("/")})"

                parentIdPath == resource.idBase -> parentIdPath
                resource.isResource ->
                    if (parentIdPath.isBlank()) {
                        resource.idBase ?: ""
                    } else {
                        "$parentIdPath->${resource.idBase}"
                    }

                else -> parentIdPath
            }

        /**
         * comma delimited list of IDs
         */
        internal fun generateResourcesId(resources: List<Base>): String? = resources
                .mapNotNull { it.idBase }
                .joinToString()
                .ifEmpty { null }
    }
}