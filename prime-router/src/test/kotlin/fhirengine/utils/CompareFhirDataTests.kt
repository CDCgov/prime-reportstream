package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.cli.tests.CompareData
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Reference
import java.util.Date
import kotlin.test.Test

class CompareFhirDataTests {
    @Test
    fun `test get FHIR type path`() {
        assertThat(CompareFhirData.getFhirTypePath("", "value")).isEqualTo("value")
        assertThat(CompareFhirData.getFhirTypePath("parent", "value")).isEqualTo("parent.value")
        assertThat { CompareFhirData.getFhirTypePath("parent", "") }.isFailure()
        assertThat { CompareFhirData.getFhirTypePath("", "") }.isFailure()
    }

    @Test
    fun `test filter resource properties`() {
        val testBundle = Bundle()
        testBundle.type = Bundle.BundleType.MESSAGE
        testBundle.id = "someid"
        testBundle.timestamp = Date()
        testBundle.addEntry()

        // Test the default filter
        var properties = CompareFhirData().filterResourceProperties(testBundle)
        assertThat(properties.any { it.name == "type" }).isTrue()
        assertThat(properties.any { it.name == "id" }).isFalse()
        assertThat(properties.any { it.name == "timestamp" }).isTrue()
        assertThat(properties.any { it.name == "entry" }).isFalse()

        // Now test custom ones.
        properties = CompareFhirData(skippedProperties = listOf("Bundle.type", "Bundle.timestamp"))
            .filterResourceProperties(testBundle)
        assertThat(properties.any { it.name == "type" }).isFalse()
        assertThat(properties.any { it.name == "id" }).isFalse()
        assertThat(properties.any { it.name == "timestamp" }).isFalse()
        assertThat(properties.any { it.name == "entry" }).isFalse()
    }

    @Test
    fun `test compare value`() {
        val actualBundleA = Bundle()
        actualBundleA.type = Bundle.BundleType.MESSAGE
        actualBundleA.id = "someid"
        actualBundleA.timestamp = Date()
        actualBundleA.total = 1

        val actualBundleB = Bundle()
        actualBundleB.type = Bundle.BundleType.COLLECTION
        actualBundleB.id = "someid"

        val expectedBundle = Bundle()
        expectedBundle.type = Bundle.BundleType.COLLECTION
        expectedBundle.id = "someotherid"
        expectedBundle.timestamp = Date.from(Date().toInstant().minusMillis(9999999))
        expectedBundle.total = 2

        var comparator = spyk(CompareFhirData(dynamicProperties = listOf("Bundle.timestamp")))

        // Dissimilar types
        assertThat(
            comparator.compareValue(
                actualBundleA.getChildByName("type").values[0],
                expectedBundle.getChildByName("timestamp").values[0],
                "Bundle.type", result = CompareData.Result()
            )
        ).isFalse()

        // Dynamic types
        assertThat(
            comparator.compareValue(
                actualBundleA.getChildByName("timestamp").values[0],
                expectedBundle.getChildByName("timestamp").values[0], "Bundle.timestamp",
                result = CompareData.Result()
            )
        ).isTrue()

        // References
        comparator = spyk(CompareFhirData())
        val reference = Reference()
        reference.resource = Organization()
        comparator.compareValue(
            reference,
            reference,
            result = CompareData.Result()
        )
        verify(exactly = 1) { comparator.compareReference(reference, reference, any(), any()) }

        // Resources
        comparator = spyk(CompareFhirData())
        val messageHeader = MessageHeader()
        messageHeader.source = MessageHeader.MessageSourceComponent()
        messageHeader.source.name = "name"
        comparator.compareValue(
            messageHeader,
            messageHeader,
            result = CompareData.Result()
        )
        verify(exactly = 1) { comparator.compareResource(messageHeader, messageHeader, any(), any()) }

        // Primitive type
        assertThat(
            comparator.compareValue(
                actualBundleA.getChildByName("type").values[0],
                expectedBundle.getChildByName("type").values[0],
                "Bundle.type", result = CompareData.Result()
            )
        ).isFalse()
        assertThat(
            comparator.compareValue(
                actualBundleB.getChildByName("type").values[0],
                expectedBundle.getChildByName("type").values[0],
                "Bundle.type", result = CompareData.Result()
            )
        ).isTrue()
    }
}