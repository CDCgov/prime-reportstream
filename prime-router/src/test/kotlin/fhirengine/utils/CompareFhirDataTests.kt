package gov.cdc.prime.router.fhirengine.utils

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import gov.cdc.prime.router.cli.tests.CompareData
import io.mockk.spyk
import io.mockk.verify
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.random.Random
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

    @Test
    fun `test compare resource`() {
        val comparator = CompareFhirData()

        val resourceA = Organization()
        resourceA.name = "nameA"
        resourceA.alias = listOf(StringType("aliasA"))

        val resourceB = Organization()
        resourceB.name = "nameA"

        val resourceC = Organization()

        // Identical resources
        var result = CompareData.Result()
        assertThat(comparator.compareResource(resourceA, resourceA, "", result)).isTrue()
        assertThat(result.passed).isTrue()
        assertThat(result.errors).isEmpty()

        // Expected has less than actual, but same data
        result = CompareData.Result()
        assertThat(comparator.compareResource(resourceA, resourceB, "", result)).isTrue()
        assertThat(result.errors).isEmpty()

        // Actual has less than expected, but same data
        result = CompareData.Result()
        assertThat(comparator.compareResource(resourceB, resourceA, "", result)).isFalse()
        assertThat(result.errors).isNotEmpty()

        // Expected has less than actual, but same data
        result = CompareData.Result()
        assertThat(comparator.compareResource(resourceA, resourceC, "", result)).isTrue()
        assertThat(result.errors).isEmpty()

        // Actual has less than expected, but same data
        result = CompareData.Result()
        assertThat(comparator.compareResource(resourceC, resourceA, "", result)).isFalse()
        assertThat(result.errors).isNotEmpty()

        // Actual has a primitive instead of a resource
        result = CompareData.Result()
        assertThat(comparator.compareResource(StringType("some string"), resourceA, "", result))
            .isFalse()
        assertThat(result.errors).isNotEmpty()

        // Test suppress output
        result = CompareData.Result()
        assertThat(comparator.compareResource(resourceC, resourceA, "", result, true)).isFalse()
        assertThat(result.errors).isEmpty()
    }

    @Test
    fun `test compare bundle`() {
        val msgHeader = MessageHeader()
        msgHeader.id = UUID.randomUUID().toString()
        val eventCoding = Coding()
        eventCoding.code = "some code"
        msgHeader.event = eventCoding
        val msgHeaderEntry = Bundle.BundleEntryComponent()
        msgHeaderEntry.resource = msgHeader
        msgHeaderEntry.fullUrl = "MessageHeader/${msgHeaderEntry.resource.id}"

        val org = Organization()
        org.name = "some org name"
        org.id = UUID.randomUUID().toString()
        val orgEntry = Bundle.BundleEntryComponent()
        orgEntry.resource = msgHeader
        orgEntry.fullUrl = "MessageHeader/${msgHeaderEntry.resource.id}"

        // No entries
        val bundleA = Bundle()
        bundleA.id = UUID.randomUUID().toString()
        bundleA.type = Bundle.BundleType.MESSAGE
        bundleA.timestamp = Date.from(Instant.now().minusSeconds(Random.nextLong(100000)))

        // With message header, which is compared
        val bundleB = Bundle()
        bundleB.id = UUID.randomUUID().toString()
        bundleB.type = Bundle.BundleType.MESSAGE
        bundleB.timestamp = Date.from(Instant.now().minusSeconds(Random.nextLong(100000)))
        bundleB.addEntry(msgHeaderEntry)

        // With Organization entry, which is not compared
        val bundleC = Bundle()
        bundleC.id = UUID.randomUUID().toString()
        bundleC.type = Bundle.BundleType.MESSAGE
        bundleC.timestamp = Date.from(Instant.now().minusSeconds(Random.nextLong(100000)))
        bundleC.addEntry(msgHeaderEntry)
        bundleC.addEntry(orgEntry)

        val comparator = CompareFhirData()
        var result = CompareData.Result()
        comparator.compareBundle(bundleA, bundleA, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors).isEmpty()

        result = CompareData.Result()
        comparator.compareBundle(bundleB, bundleA, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors).isEmpty()

        result = CompareData.Result()
        comparator.compareBundle(bundleC, bundleA, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors).isEmpty()

        result = CompareData.Result()
        comparator.compareBundle(bundleB, bundleC, result)
        assertThat(result.passed).isTrue()
        assertThat(result.errors).isEmpty()

        result = CompareData.Result()
        comparator.compareBundle(bundleA, bundleB, result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors).isNotEmpty()

        result = CompareData.Result()
        comparator.compareBundle(bundleA, bundleC, result)
        assertThat(result.passed).isFalse()
        assertThat(result.errors).isNotEmpty()
    }

    @Test
    fun `test compare function`() {
        val jsonBundle = """
 {
	"resourceType": "Bundle",
	"id": "d809eafd-97f4-4ff2-bd0d-6a99ea809974",
	"meta": {
		"lastUpdated": "2022-05-19T09:48:24.981-04:00",
		"security": [
			{
				"code": "SECURITY",
				"display": "SECURITY"
			}
		]
	},
	"identifier": {
		"value": "MT_COCAA_ORU_AAPHELR.1.6214638"
	},
	"type": "message",
	"timestamp": "2028-08-08T11:28:05.000-04:00",
    "entry": [
		{
			"fullUrl": "MessageHeader/88a50cd6-72bf-34a1-9025-708e7c29cc32",
			"resource": {
				"resourceType": "MessageHeader",
				"id": "88a50cd6-72bf-34a1-9025-708e7c29cc32"
            }
        }
    ]
 }
        """.trimIndent()

        val result = CompareFhirData().compare(jsonBundle.byteInputStream(), jsonBundle.byteInputStream())
        assertThat(result.passed).isTrue()
        assertThat(result.errors).isEmpty()
    }
}