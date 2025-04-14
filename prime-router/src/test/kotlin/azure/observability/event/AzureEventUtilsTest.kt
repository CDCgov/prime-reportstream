package gov.cdc.prime.router.azure.observability.event

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import java.io.File
import kotlin.test.Test

class AzureEventUtilsTest {

    private val validFhirReport = "src/test/resources/fhirengine/engine/routing/valid2.fhir"
    private val validFhirReportNoIdentifier = "src/test/resources/fhirengine/engine/routing/valid_no_identifier.fhir"
    private val loincSystem = "http://loinc.org"
    private val snomedSystem = "SNOMEDCT"

    @Test
    fun `get all observations from bundle and map them correctly`() {
        val fhirData = File(validFhirReport).readText()
        val bundle = FhirTranscoder.decode(fhirData)

        @Suppress("ktlint:standard:max-line-length")
        val expected = listOf(
            ObservationSummary(
                listOf(
                    TestSummary(
                        listOf(
                            CodeSummary(
                                snomedSystem,
                                "840539006",
                                "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)",
                                memberOid = "Unknown"
                            ),
                            CodeSummary(
                                snomedSystem,
                                "7180009",
                                "Meningitis (disorder)",
                                memberOid = "Unknown"
                            )
                        ),
                        loincSystem,
                        "94558-4",
                        "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"
                    ),
                    TestSummary(
                        listOf(
                            CodeSummary(
                                snomedSystem,
                                "840539006",
                                "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)",
                                memberOid = "Unknown"
                            ),
                            CodeSummary(
                                snomedSystem,
                                "7180009",
                                "Meningitis (disorder)",
                                memberOid = "Unknown"
                            )
                        ),
                        "Local",
                        "12345",
                        "Covid 19 Test"
                    )
                ),
                listOf(
                    CodeSummary(
                        system = "http://terminology.hl7.org/CodeSystem/v2-0078",
                        code = "N",
                        display = "Normal",
                        memberOid = "Unknown"
                    )
                )
            ),
            ObservationSummary(
                listOf(
                    TestSummary(
                        testPerformedCode = "95418-0",
                    )
                ),
                emptyList()
            ),
            ObservationSummary(
                listOf(
                    TestSummary(
                        testPerformedSystem = loincSystem
                    )
                ),
                emptyList()
            ),
            ObservationSummary(
                listOf(
                    TestSummary(
                        testPerformedDisplay = "SARS-CoV-2 (COVID-19) N gene [Presence] in Saliva (oral fluid) by Nucleic acid amplification using CDC primer-probe set N1"
                    )
                ),
                emptyList()
            ),
            ObservationSummary(
                listOf(
                    TestSummary(
                        testPerformedCode = "95419-8",
                        testPerformedSystem = loincSystem
                    )
                ),
                emptyList()
            )
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