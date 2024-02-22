package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import gov.cdc.prime.router.fhirengine.utils.conditionCodeExtensionURL
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation
import java.util.UUID
import kotlin.test.Test

class ReportStreamFilterTests {
    @Test
    fun `test fhir expression filter passing`() {
        val goodFilter = FHIRExpressionBundleFilter("true")
        val reversedFilter = FHIRExpressionBundleFilter("true", reverseFilter = true)
        val badFilter = FHIRExpressionBundleFilter("false")
        val bundle = Bundle()
        assertThat(goodFilter.pass(bundle)).isEqualTo(true)
        assertThat(reversedFilter.pass(bundle)).isEqualTo(false)
        assertThat(badFilter.pass(bundle)).isEqualTo(false)
    }
}

class ReportStreamObservationFilterTests {
    @Test
    fun `test bundle observation filter passing`() {
        val mockBundleFilter = BundleObservationFilter(MockBundleObservationPruner())
        assertThat(mockBundleFilter.pass(makeBundle(goodObservations))).isEqualTo(true)
        assertThat(mockBundleFilter.pass(makeBundle(goodObservations + badObservations))).isEqualTo(true)
        assertThat(mockBundleFilter.pass(makeBundle(badObservations))).isEqualTo(false)
    }

    @Test
    fun `test condition code filter passing`() {
        val goodFilter = BundleObservationFilter(ConditionCodeBundleObservationPruner("1234,4321"))
        val badFilter = BundleObservationFilter(ConditionCodeBundleObservationPruner("5678,8765"))
        val bundle = makeBundle(makeObservation("someCode", "4321"))
        assertThat(goodFilter.pass(bundle)).isEqualTo(true)
        assertThat(badFilter.pass(bundle)).isEqualTo(false)
    }
}

class ReportStreamObservationPrunerTests {
    @Test
    fun `test observation pruning`() {
        val mockFilter = MockBundleObservationPruner()
        val bundle = makeBundle(goodObservations + badObservations)

        val pruned = mockFilter.prune(bundle)
        assertThat(pruned.size).isEqualTo(2)
        assertThat(pruned).isEqualTo(badObservations)

        val observations = bundle.getObservations()
        assertThat(observations.size).isEqualTo(2)
        assertThat(observations).isEqualTo(goodObservations)
    }

    @Test
    fun `test condition code pruning`() {
        val filter = ConditionCodeBundleObservationPruner("1234,4321")
        val goodObservation = makeObservation("someCode", "4321")
        val badObservation = makeObservation("someCode", "5678")
        val bundle = makeBundle(listOf(goodObservation, badObservation))

        val pruned = filter.prune(bundle)
        assertThat(pruned.size).isEqualTo(1)
        assertThat(pruned.first()).isSameAs(badObservation)

        val observations = bundle.getObservations()
        assertThat(observations.size).isEqualTo(1)
        assertThat(observations.first()).isSameAs(goodObservation)
    }

    @Test
    fun `test fhir expression observation pruning`() {
        val filter = FHIRExpressionBundleObservationPruner("%resource.code.coding.extension.exists()")
        val goodObservation = makeObservation("someCode", "1234")
        val badObservation = makeObservation()

        val bundle = makeBundle(listOf(goodObservation, badObservation))
        assertThat(filter.evaluateResource(bundle, goodObservation)).isEqualTo(true)
        assertThat(filter.evaluateResource(bundle, badObservation)).isEqualTo(false)
    }

    @Test
    fun `test condition code observation evaluation`() {
        val filter = ConditionCodeBundleObservationPruner("1234,4321")
        val goodObservation = makeObservation("someCode", "4321")
        val badObservation = makeObservation("someCode", "5678")

        val bundle = makeBundle(listOf(goodObservation, badObservation))
        assertThat(filter.evaluateResource(bundle, goodObservation)).isEqualTo(true)
        assertThat(filter.evaluateResource(bundle, badObservation)).isEqualTo(false)
    }

    @Test
    fun `test condition keyword code resolution`() {
        val filter = ConditionKeywordBundleObservationPruner("balamuthia,cadmium")
        assertThat(filter.codeList).isEqualTo(listOf("115635005", "3398004"))
    }
}

/**
 * Make a FHIR Bundle containing [observations]
 * @return A HAPI FHIR bundle containing [observations]
 */
private fun makeBundle(observations: List<Observation>): Bundle =
    Bundle().also { bundle ->
        observations.forEach {
            bundle.addEntry(BundleEntryComponent().setResource(it).setFullUrl(it.id))
        }
    }

/**
 * Make a FHIR Bundle containing [observation]
 * @return A HAPI FHIR bundle containing [observation]
 */
private fun makeBundle(observation: Observation) = makeBundle(listOf(observation))

/**
 * Make a FHIR Observation with a(n) [id] and [testCode] stamped with a [conditionCode] extension
 * @return A HAPI FHIR Observation with the specified data
 */
private fun makeObservation(
    testCode: String = "someCode",
    conditionCode: String? = null,
    id: String? = null,
): Observation {
    val coding = Coding("someSystem", testCode, "test code")
    if (!conditionCode.isNullOrEmpty()) {
        coding.addExtension(
            conditionCodeExtensionURL, Coding("someSystem", conditionCode, "condition code")
        )
    }
    val observation = Observation().setCode(CodeableConcept().setCoding(listOf(coding)))
    if (id.isNullOrEmpty()) {
        observation.setId(UUID.randomUUID().toString())
    } else {
        observation.setId(id)
    }
    return observation
}

// fails any observation whose ID starts with `filter`
private class MockBundleObservationPruner() : ObservationPrunable {
    override fun evaluateResource(bundle: Bundle, resource: Observation) =
        !resource.id.startsWith("filter")
}

private val goodObservations = listOf(makeObservation(id = "good1"), makeObservation(id = "good2"))
private val badObservations = listOf(makeObservation(id = "filter1"), makeObservation(id = "filter2"))