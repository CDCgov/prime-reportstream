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
        val receiver = Receiver("elr", "IGNORE", Topic.COVID_19, CustomerStatus.INACTIVE, translatorConfig)
        assertThat(receiver.fullName).isEqualTo("IGNORE.elr")
    }

    @Test
    fun `test receiver external name when present`() {
        val receiver = Receiver(
            "elr",
            "IGNORE",
            Topic.COVID_19,
            CustomerStatus.INACTIVE,
            translatorConfig,
            externalName = "Ignore ELR"
        )
        assertThat(receiver.displayName).isEqualTo("Ignore ELR")
    }

    @Test
    fun `test receiver external name when not present`() {
        val receiver = Receiver(
            "elr",
            "IGNORE",
            Topic.COVID_19,
            CustomerStatus.INACTIVE,
            translatorConfig,
            externalName = null
        )
        assertThat(receiver.displayName).isEqualTo("elr")
    }

    @Test
    fun `test batch decider for every other minute`() {
        val timing = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            720,
            "04:00",
            USTimeZone.MOUNTAIN
        )
        assertThat(timing.isValid()).isTrue()

        val evenMinute =
            ZonedDateTime.of(2020, 10, 2, 0, 2, 30, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual1 = timing.batchInPrevious60Seconds(evenMinute)
        assertThat(actual1).isTrue()

        val oddMinute =
            ZonedDateTime.of(2020, 10, 2, 0, 3, 30, 0, ZoneId.of("UTC")).toOffsetDateTime()
        val actual2 = timing.batchInPrevious60Seconds(oddMinute)
        assertThat(actual2).isFalse()
    }

    @Test
    fun `test batch decider with timing once per day at 2am`() {
        val timing2 = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            1,
            "02:00",
            USTimeZone.PACIFIC
        )
        assertThat(timing2.isValid()).isTrue()

        // if runs at 1:59:59:999 SHOULD BE FALSE
        val edgeCase1 =
            ZonedDateTime.of(2020, 10, 2, 1, 59, 59, 999, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val edgeResult1 = timing2.batchInPrevious60Seconds(edgeCase1)
        assertThat(edgeResult1).isFalse()

        // if runs at 2:00:00:000 SHOULD BE TRUE
        val twoAm =
            ZonedDateTime.of(2020, 10, 2, 2, 0, 0, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual3 = timing2.batchInPrevious60Seconds(twoAm)
        assertThat(actual3).isTrue()

        // if runs at 2:00:59:999 SHOULD BE TRUE
        val edgeCase2 =
            ZonedDateTime.of(2020, 10, 2, 2, 0, 59, 999, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val edgeResult2 = timing2.batchInPrevious60Seconds(edgeCase2)
        assertThat(edgeResult2).isTrue()

        // if runs at 2:01:00:000 SHOULD BE FAlSE
        val edgeCase3 =
            ZonedDateTime.of(2020, 10, 2, 2, 1, 0, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val edgeResult3 = timing2.batchInPrevious60Seconds(edgeCase3)
        assertThat(edgeResult3).isFalse()

        // some false cases
        val notTwoAmOne =
            ZonedDateTime.of(2020, 10, 2, 2, 5, 30, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual4 = timing2.batchInPrevious60Seconds(notTwoAmOne)
        assertThat(actual4).isFalse()

        val notTwoAmTwo =
            ZonedDateTime.of(2020, 10, 2, 12, 5, 30, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual5 = timing2.batchInPrevious60Seconds(notTwoAmTwo)
        assertThat(actual5).isFalse()

        val notTwoAmThree =
            ZonedDateTime.of(2020, 10, 2, 1, 59, 30, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual6 = timing2.batchInPrevious60Seconds(notTwoAmThree)
        assertThat(actual6).isFalse()
    }

    @Test
    fun `test batch decider once per hour at 7 after`() {
        val timing3 = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            24,
            "02:07",
            USTimeZone.PACIFIC
        )
        assertThat(timing3.isValid()).isTrue()

        val shouldWork1 =
            ZonedDateTime.of(2020, 10, 2, 2, 7, 1, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual7 = timing3.batchInPrevious60Seconds(shouldWork1)
        assertThat(actual7).isTrue()

        val shouldWork2 =
            ZonedDateTime.of(2020, 10, 2, 2, 7, 59, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual8 = timing3.batchInPrevious60Seconds(shouldWork2)
        assertThat(actual8).isTrue()

        val notSevenAfterOne =
            ZonedDateTime.of(2020, 10, 2, 2, 8, 0, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual9 = timing3.batchInPrevious60Seconds(notSevenAfterOne)
        assertThat(actual9).isFalse()
    }

    @Test
    fun `test batch decider every 15 minutes, starting at quarter after`() {
        val timing = Receiver.Timing(
            Receiver.BatchOperation.NONE,
            96,
            "02:15",
            USTimeZone.PACIFIC
        )
        assertThat(timing.isValid()).isTrue()

        val shouldWork1 =
            ZonedDateTime.of(2020, 10, 2, 2, 0, 1, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual1 = timing.batchInPrevious60Seconds(shouldWork1)
        assertThat(actual1).isTrue()

        val shouldWork2 =
            ZonedDateTime.of(2020, 10, 2, 2, 15, 1, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual2 = timing.batchInPrevious60Seconds(shouldWork2)
        assertThat(actual2).isTrue()

        val shouldWork3 =
            ZonedDateTime.of(2020, 10, 2, 2, 30, 1, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual3 = timing.batchInPrevious60Seconds(shouldWork3)
        assertThat(actual3).isTrue()

        val shouldWork4 =
            ZonedDateTime.of(2020, 10, 2, 2, 45, 1, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual4 = timing.batchInPrevious60Seconds(shouldWork4)
        assertThat(actual4).isTrue()

        val noWork1 =
            ZonedDateTime.of(2020, 10, 2, 2, 50, 1, 0, ZoneId.of(USTimeZone.PACIFIC.zoneId)).toOffsetDateTime()
        val actual5 = timing.batchInPrevious60Seconds(noWork1)
        assertThat(actual5).isFalse()
    }

    @Test
    fun `test receiver schema fields`() {
        val receiver = Receiver(
            "elr",
            "co-phd",
            Topic.TEST,
            CustomerStatus.INACTIVE,
            "CO",
            Report.Format.CSV,
            null,
            null,
            null
        )
        assertThat(receiver.schemaName).isEqualTo("CO")
    }
}