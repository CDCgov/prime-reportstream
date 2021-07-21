package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.mockkClass
import kotlin.test.Test

internal class ReceiverTests {
    private val translatorConfig = mockkClass(TranslatorConfiguration::class)

    @Test
    fun `test receiver full name`() {
        val receiver = Receiver("elr", "IGNORE", "covid-19", translatorConfig)
        assertThat(receiver.fullName).isEqualTo("IGNORE.elr")
    }

    @Test
    fun `test receiver external name when present`() {
        val receiver = Receiver("elr", "IGNORE", "covid-19", translatorConfig, externalName = "Ignore ELR")
        assertThat(receiver.displayName).isEqualTo("Ignore ELR")
    }

    @Test
    fun `test receiver external name when not present`() {
        val receiver = Receiver("elr", "IGNORE", "covid-19", translatorConfig, externalName = null)
        assertThat(receiver.displayName).isEqualTo("elr")
    }
}