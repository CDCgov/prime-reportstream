package gov.cdc.prime.router.azure.observability.event

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import java.io.File
import kotlin.test.Test

class AzureEventUtilsTest {

    private val validFhirUrl = "src/test/resources/fhirengine/engine/routing/valid.fhir"

    @Test
    fun `get all conditions from bundle and map them correctly`() {
        val fhirData = File(validFhirUrl).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val expected = setOf(
            ConditionSummary(
                "840539006",
                "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
            )
        )
        val actual = AzureEventUtils.getConditions(bundle)

        assertThat(actual).isEqualTo(expected)
    }
}