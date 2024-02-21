package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class MetadataIntegrationTests {
    val settings = FileSettings("src/testIntegration/resources/settings")
    val receiver = settings.receivers.find { it.name.equals("DEV_FULL_ELR") }

    @Test
    fun `test loading metadata catalog`() {
        try {
            val metadata = Metadata.getInstance()
            assertThat(metadata).isNotNull()
        } catch (e: Exception) {
            fail("The metadata catalog failed to initialize.  Is the database populated?")
        }
    }

    @Test
    fun `test settings, receiver, and mappedConditionFilter loaded`() {
        assertThat(settings).isNotNull()
        assertThat(receiver).isNotNull()
        assertThat(receiver!!.mappedConditionFilter).isNotEmpty()
    }

    @Test
    fun `test loading condition code filter`() {
        assertThat(receiver!!.mappedConditionFilter.filterIsInstance<BundleObservationCodeFilter>().first().codeList)
            .isEqualTo(listOf("840539006", "1234"))
    }

    @Test
    fun `test loading condition keyword filter`() {
        assertThat(receiver!!.mappedConditionFilter.filterIsInstance<BundleObservationKeywordFilter>().first().codeList)
            .isEqualTo(listOf("115635005", "3398004"))
    }

    @Test
    fun `test loading fhir expression condition filter`() {
        assertThat(
            receiver!!.mappedConditionFilter.filterIsInstance<FHIRExpressionBundleObservationFilter>().first().fhirExpression
        ).isEqualTo("Bundle.identifier.value.empty().not()")
    }
}