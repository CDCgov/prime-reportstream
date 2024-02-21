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
    private fun makeObservation(testCode: String = "someCode", conditionCode: String? = null): Observation {
        val coding = Coding("someSystem", testCode, "test code")
        if (!conditionCode.isNullOrEmpty()) {
            coding.addExtension(
                conditionCodeExtensionURL, Coding("someSystem", conditionCode, "condition code")
            )
        }
        val observation = Observation().setCode(CodeableConcept().setCoding(listOf(coding)))
        observation.setId(UUID.randomUUID().toString())
        return observation
    }

    private fun makeBundle(observations: List<Observation>): Bundle =
        Bundle().also { bundle ->
            observations.forEach {
                bundle.addEntry(BundleEntryComponent().setResource(it).setFullUrl(it.id))
            }
        }

    private fun makeBundle(observation: Observation) = makeBundle(listOf(observation))

    @Test
    fun `test condition code filter passing`() {
        val goodFilter = ConditionCodeFilter("1234,4321")
        val badFilter = ConditionCodeFilter("5678,8765")
        val bundle = makeBundle(makeObservation("someCode", "4321"))
        assertThat(goodFilter.pass(bundle)).isEqualTo(true)
        assertThat(badFilter.pass(bundle)).isEqualTo(false)
    }

    @Test
    fun `test condition code filter pruning`() {
        val filter = ConditionCodeFilter("1234,4321")
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
    fun `test fhir expression filter passing`() {
        val goodFilter = FHIRExpressionFilter("true")
        val reversedFilter = FHIRExpressionFilter("true", reverseFilter = true)
        val badFilter = FHIRExpressionFilter("false")
        val bundle = Bundle()
        assertThat(goodFilter.pass(bundle)).isEqualTo(true)
        assertThat(reversedFilter.pass(bundle)).isEqualTo(false)
        assertThat(badFilter.pass(bundle)).isEqualTo(false)
    }

    @Test
    fun `test fhir expression condition filter pruning`() {
        val filter = FHIRExpressionConditionFilter("%resource.code.coding.extension.exists()")
        val goodObservation = makeObservation("someCode", "1234")
        val badObservation = makeObservation()
        val bundle = makeBundle(listOf(goodObservation, badObservation))

        val pruned = filter.prune(bundle)
        assertThat(pruned.size).isEqualTo(1)
        assertThat(pruned.first()).isSameAs(badObservation)

        val observations = bundle.getObservations()
        assertThat(observations.size).isEqualTo(1)
        assertThat(observations.first()).isSameAs(goodObservation)
    }
}