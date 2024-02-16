package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class MetadataIntegrationTests {
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
    fun `test loading mappedConditionFilter`() {
        val settings = FileSettings("src/testIntegration/resources/settings")
        assertThat(settings).isNotNull()
        val receiver = settings.receivers.find { it.mappedConditionFilter.isNotEmpty() }
        assertThat(receiver).isNotNull()
        assertThat(receiver!!.mappedConditionFilter).isNotEmpty()
        assertThat(receiver.mappedConditionFilter.filterIsInstance<ConditionCodeFilter>().first().codeList)
            .isEqualTo(listOf("840539006", "1234"))
    }
}