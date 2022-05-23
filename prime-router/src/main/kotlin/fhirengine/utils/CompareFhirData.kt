package gov.cdc.prime.router.fhirengine.utils

import gov.cdc.prime.router.cli.tests.CompareData
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
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
    private val dynamicProperties: List<String> = defaultDynamicProperties
) : Logging {
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
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {
        val expectedJson = expected.bufferedReader().readText()
        val actualJson = actual.bufferedReader().readText()
        val expectedBundle = FhirTranscoder.decode(expectedJson)
        val actualBundle = FhirTranscoder.decode(actualJson)

        compareBundle(actualBundle, expectedBundle, result)
        return result
    }

    /**
     * Compare an [actualBundle] to an [expectedBundle] and provide the [result].
     */
    internal fun compareBundle(actualBundle: Bundle, expectedBundle: Bundle, result: CompareData.Result) {
        result.passed = true
        val resourcesToCompare = mutableMapOf<Resource, List<Base>>(expectedBundle to listOf(actualBundle))

        // Only use entries we want.
        val entriesToCompare = expectedBundle.entry.filter {
            comparedBundleEntries.contains(it.resource.fhirType())
        }

        // Make a map of resources that can be compared.
        entriesToCompare.forEach { expectedEntry ->
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

        // Compare all the resources.
        resourcesToCompare.keys.forEach { expectedResource ->
            val resourceMatches = resourcesToCompare[expectedResource]?.all { actualResource ->
                logger.info("")
                logger.info("Comparing ${expectedResource.fhirType()}...")
                compareResource(actualResource, expectedResource, expectedResource.fhirType(), result)
            }
            result.passed = result.passed and (resourceMatches ?: true)
            logger.info("Resource ${expectedResource.fhirType()} matches = $resourceMatches")
        }
        logger.debug("FHIR bundles are ${if (result.passed) "IDENTICAL" else "DIFFERENT"}")
    }

    /**
     * Compare an [actualResource] to an [expectedResource] and provide the [result].
     * @param parentTypePath a string representing a type hirearchy for the parent of the given resource
     * @param suppressOutput set false to not log comparison results
     */
    internal fun compareResource(
        actualResource: Base,
        expectedResource: Base,
        parentTypePath: String,
        result: CompareData.Result,
        suppressOutput: Boolean = false
    ): Boolean {
        var isEqual = true
        filterResourceProperties(expectedResource).forEach { expectedChild ->
            val actualChild = actualResource.getChildByName(expectedChild.name)
            val thisTypePath = getFhirTypePath(parentTypePath, expectedChild.name)
            if (actualChild != null) {
                val actualValues = actualChild.values
                val expectedValues = expectedChild.values
                // Skip any empty expected properties
                if (expectedValues.isNotEmpty()) {
                    // Search for a value that is the same in the list of values
                    val isPropertyEqual = expectedValues.all { expectedValue ->
                        // Note that here we look for the first good match, and note we are comparing all values which
                        // we expect only one match.
                        actualValues.any { actualValue ->
                            compareValue(actualValue, expectedValue, thisTypePath, result, actualValues.size > 1)
                        }
                    }

                    if (!suppressOutput)
                        if (!isPropertyEqual) {
                            result.errors.add("Property $thisTypePath does not match.")
                            logger.debug("Property $thisTypePath does not match.")
                        } else logger.debug("Property $thisTypePath matches.")
                    isEqual = isEqual && isPropertyEqual
                }
            } else {
                if (!suppressOutput) {
                    result.errors.add("Property $thisTypePath does not match - different type.")
                }
                isEqual = false
            }
        }
        return isEqual
    }

    /**
     * Compare an [actualReference] to an [expectedReference] and provide the [result].
     * @param thisTypePath a string representing a type hirearchy for the given value
     */
    internal fun compareReference(
        actualReference: Reference,
        expectedReference: Reference,
        thisTypePath: String,
        result: CompareData.Result
    ): Boolean {
        logger.debug("Comparing expected reference ${expectedReference.reference} from type $thisTypePath ...")
        return compareResource(actualReference.resource as Base, expectedReference.resource as Base, "", result)
    }

    /**
     * Compare an [actualValue] to an [expectedValue] and provide the [result].
     * @param thisTypePath a string representing a type hirearchy for the given value
     * @param suppressOutput set false to not log comparison results
     */
    internal fun compareValue(
        actualValue: Base,
        expectedValue: Base,
        thisTypePath: String = "",
        result: CompareData.Result,
        suppressOutput: Boolean = false
    ): Boolean {
        return when {
            // Dissimilar types are not a match.
            expectedValue.fhirType() != actualValue.fhirType() -> false

            // Dynamic values are only checked to exist
            dynamicProperties.contains(thisTypePath) -> true

            // References
            expectedValue.hasType("Reference") ->
                compareReference(actualValue as Reference, expectedValue as Reference, thisTypePath, result)

            !expectedValue.isPrimitive ->
                compareResource(actualValue, expectedValue, thisTypePath, result, suppressOutput)

            // Use the built-in equals for anything else.
            else -> {
                expectedValue.equalsDeep(actualValue)
            }
        }
    }

    /**
     * Filter the properties of a given [resource], so only the properties we want to compare are listed.
     * @return the list of properties to compare.
     */
    internal fun filterResourceProperties(resource: Base): List<Property> {
        return resource.children().filter {
            // Skip any properties to be ignored
            val isSkipped = skippedProperties.contains("${resource.fhirType()}.${it.name}")
            // Skip any resource IDs
            val isResourceId = (resource.isResource && it.name == "id")
            // Skip the entry property of the bundle as those resources are handled elsewhere
            val isBundleEntry = (resource.hasType("Bundle") && it.name == "entry")
            !(isSkipped || isResourceId || isBundleEntry)
        }
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
         * The list of bundle entries to test.  Do not include entries that are referenced in other resources listed
         * here (e.g. Organization is part of MessageHeader and Provenance. If you add a resource that already referenced
         * it will not break anything just generate more output.
         */
        private val comparedBundleEntries = listOf(
            "MessageHeader", "Provenance", "Patient", "Encounter",
            "Observation", "Practitioner", "DiagnosticReport", "Specimen"
        )

        /**
         * Get the FHIR type path for a given [parentTypePath] and [valueName].  The FHIR type path
         * is NOT the same as a FHIR path, and we use it to log the types we are comparing and to match types we want to
         * ignore.  E.g. Bundle.meta.lastUpdated, Organization.name.
         */
        internal fun getFhirTypePath(parentTypePath: String, valueName: String): String {
            require(valueName.isNotBlank())
            val parentPath = if (parentTypePath.isNotBlank()) "$parentTypePath." else ""
            return parentPath + valueName
        }
    }
}