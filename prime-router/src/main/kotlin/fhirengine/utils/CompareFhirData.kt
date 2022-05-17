package gov.cdc.prime.router.fhirengine.utils

import gov.cdc.prime.router.cli.tests.CompareData
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import java.io.InputStream

class CompareFhirData(val result: CompareData.Result = CompareData.Result()) : Logging {
    fun compare(
        expected: InputStream,
        actual: InputStream,
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {

        val expectedBundle = FhirTranscoder.decode(expected.bufferedReader().readText())
        val actualBundle = FhirTranscoder.decode(actual.bufferedReader().readText())

        compareBundle(actualBundle, expectedBundle)
        return result
    }

    private fun compareBundle(actualBundle: Bundle, expectedBundle: Bundle) {
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
            resourcesToCompare[expectedEntry.resource] = matchingActualEntries
        }

        // Compare all the resources.
        var isEqual = true
        resourcesToCompare.keys.forEach { expectedResource ->
            println("Comparing ${expectedResource.fhirType()}...")
            val resourceMatches = resourcesToCompare[expectedResource]?.all { actualResource ->
                logger.info("")
                logger.info("Comparing ${expectedResource.fhirType()}...")
                compareResource(actualResource, expectedResource)
            }
            logger.info("Resource ${expectedResource.fhirType()} matches = $resourceMatches")
            isEqual = isEqual && (resourceMatches ?: true)
        }
        logger.info("FHIR bundles are ${if (isEqual) "IDENTICAL" else "DIFFERENT"}")
    }

    private fun compareResource(
        actualResource: Base,
        expectedResource: Base,
        parentTypePath: String = "",
        suppressOutput: Boolean = false
    ): Boolean {
        var isEqual = true
        var thisTypePath = if (parentTypePath.isNotBlank()) "$parentTypePath." else ""
        thisTypePath += expectedResource.fhirType()

        // logger.info("Comparing resource $thisTypePath ...")
        expectedResource.children().filter {
            // Skip any properties we want to ignore
            !skippedResourceProperties.contains("${expectedResource.fhirType()}.${it.name}")
        }.forEach { expectedChild ->
            val actualChild = actualResource.getChildByName(expectedChild.name)
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
                            when {
                                // Dissimilar types are not a match.
                                expectedValue.fhirType() != actualValue.fhirType() -> false

                                // References
                                expectedValue.hasType("Reference") ->
                                    compareReference(actualValue as Reference, expectedValue as Reference, thisTypePath)

                                !expectedValue.isPrimitive ->
                                    compareResource(actualValue, expectedValue, thisTypePath, actualValues.size > 1)

                                // Use the built-in equals for anything else.
                                else -> {
                                    expectedValue.equalsDeep(actualValue)
                                }
                            }
                        }
                    }
                    if (!suppressOutput && !isPropertyEqual)
                        logger.info("Property $thisTypePath.${expectedChild.name} does not match.")
                    isEqual = isEqual && isPropertyEqual
                }
            } else isEqual = false
        }
        return isEqual
    }

    private fun compareReference(actualValue: Reference, expectedValue: Reference, thisTypePath: String): Boolean {
        logger.info("Comparing expected reference ${expectedValue.reference} from type $thisTypePath ...")
        return compareResource(actualValue.resource as Base, expectedValue.resource as Base, "")
    }

    companion object {
        // TODO CHECK FOR DYNAMIC VALUES
        private val skippedResourceProperties = listOf(
            "Bundle.entry", "Bundle.timestamp", "Bundle.id",
            "Meta.lastUpdated",
            "MessageHeader.id",
            "Provenance.id",
            "Organization.id",
            "Device.id"
        )
        private val comparedBundleEntries = listOf(
            "MessageHeader", "Provenance", "Patient", "Encounter",
            "Observation", "Practitioner", "DiagnosticReport", "Specimen"
        )
    }
}