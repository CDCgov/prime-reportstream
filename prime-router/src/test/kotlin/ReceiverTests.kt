package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.mockkClass
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test

internal class ReceiverTests {
    private val translatorConfig = mockkClass(TranslatorConfiguration::class)

    @Test
    fun `test receiver full name`() {
        val receiver = Receiver("elr", "IGNORE", "covid-19", CustomerStatus.INACTIVE, translatorConfig)
        assertThat(receiver.fullName).isEqualTo("IGNORE.elr")
    }

    @Test
    fun `test receiver external name when present`() {
        val receiver = Receiver(
            "elr", "IGNORE", "covid-19", CustomerStatus.INACTIVE, translatorConfig,
            externalName = "Ignore ELR"
        )
        assertThat(receiver.displayName).isEqualTo("Ignore ELR")
    }

    @Test
    fun `test receiver external name when not present`() {
        val receiver = Receiver(
            "elr", "IGNORE", "covid-19", CustomerStatus.INACTIVE, translatorConfig,
            externalName = null
        )
        assertThat(receiver.displayName).isEqualTo("elr")
    }

    @Test
    fun `test batchInPrevious60Seconds`() {
        // create a receiver that should batch every other minute
        val timing = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            720,
            "04:00",
            USTimeZone.MOUNTAIN
        )
        assertThat(timing.isValid()).isTrue()

        val evenMinute =
            ZonedDateTime.of(2020, 10, 2, 0, 1, 30, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual1 = timing.batchInPrevious60Seconds(evenMinute)
        assertThat(actual1).isTrue()

        val oddMinute =
            ZonedDateTime.of(2020, 10, 2, 0, 0, 30, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual2 = timing.batchInPrevious60Seconds(oddMinute)
        assertThat(actual2).isFalse()
    }
}