package gov.cdc.prime.router.azure.observability.event

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import java.io.File
import kotlin.test.Test

class AzureEventUtilsTest {

    private val validFhirReport = "src/test/resources/fhirengine/engine/routing/valid.fhir"
    private val validFhirReportNoIdentifier = "src/test/resources/fhirengine/engine/routing/valid_no_identifier.fhir"

    @Test
    fun `get all observations from bundle and map them correctly`() {
        val fhirData = File(validFhirReport).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val expected = listOf(
            ObservationSummary(
                ConditionSummary(
                    "840539006",
                    "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
                )
            ),
            ObservationSummary.EMPTY,
            ObservationSummary.EMPTY,
            ObservationSummary.EMPTY,
            ObservationSummary.EMPTY
        )
        val actual = AzureEventUtils.getObservationSummaries(bundle)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `returns message id if bundle identifier present`() {
        val fhirData = File(validFhirReport).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val expected = AzureEventUtils.MessageID(
            "1234d1d1-95fe-462c-8ac6-46728dba581c",
            "https://reportstream.cdc.gov/prime-router"
        )
        val actual = AzureEventUtils.getIdentifier(bundle)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `returns no message id if bundle identifier identifier is not present`() {
        val fhirData = File(validFhirReportNoIdentifier).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        val expected = AzureEventUtils.MessageID(null, null)
        val actual = AzureEventUtils.getIdentifier(bundle)

        assertThat(actual).isEqualTo(expected)
    }
}