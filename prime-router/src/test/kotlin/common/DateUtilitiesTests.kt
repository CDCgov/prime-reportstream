package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.Element
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test

private const val dateFormat = "yyyy-MM-dd"

class DateUtilitiesTests {
    @Test
    fun `test getDate output format`() {
        // Check LocalDate output formate
        val localDate = LocalDate.parse("2020-12-01")
        DateUtilities.getDate(localDate, dateFormat).run {
            assertThat(this).isEqualTo("2020-12-01")
        }

        // Check LocalDateTime output format.
        val localDateTime = LocalDateTime.parse("2018-12-12T13:30:30")
        DateUtilities.getDate(localDateTime, "M/d/yyyy HH:nn").run {
            assertThat(this).isEqualTo("12/12/2018 13:00")
        }

        // Check OffsetDateTime output format.
        val offsetDateTime = OffsetDateTime.parse("2018-12-12T13:30:30+05:00")
        DateUtilities.getDate(offsetDateTime, "$dateFormat HH:mm:ss").run {
            assertThat(this).isEqualTo("2018-12-12 13:30:30")
        }

        // Check OffsetDateTime output format.
        val instant = Instant.parse("2020-12-03T13:30:30.000Z")
        DateUtilities.getDate(instant, dateFormat).run {
            assertThat(this).isEqualTo("2020-12-03T13:30:30Z")
        }

        // now let's check some other date formats
        val parser = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd'T'HH:mm:ssZ]" +
                "[yyyy-MM-dd'T'HH:mm:ssxxx]" +
                "[yyyy-MM-dd'T'HH:mm:ssx]"
        )
        // Check OffsetDateTime output format.
        listOf(
            "2018-12-12T13:30:30+00:00",
            "2018-12-12T13:30:30+00",
            "2018-12-12T13:30:30+0000",
        ).forEach { date ->
            val odt = parser.parseBest(date, OffsetDateTime::from, Instant::from)
            DateUtilities.getDate(odt, "$dateFormat HH:mm:ss").run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30")
            }
            DateUtilities.getDate(odt, "$dateFormat HH:mm:ssZZZ").run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30+0000")
            }
            // now check converting the date time to the negative offset
            DateUtilities.getDate(odt, "$dateFormat HH:mm:ssZZZ", true).run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30-0000")
            }
        }
    }

    @Test
    fun `test convert positive zero offset to negative offset`() {
        Element("a", type = Element.Type.DATE).run {
            // all happy path tests
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+0000").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-0000")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00-0000").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-0000")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+00").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-00")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+00:00").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-00:00")
            }
            // non-zero offsets
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00-0400").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-0400")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+12").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00+12")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+03:30").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00+03:30")
            }
            // some unhappy paths. Don't use these formats.
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00+")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("2022+01+05 08:00:00").run {
                assertThat(this).isEqualTo("2022+01+05 08:00:00")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("").run {
                assertThat(this).isEqualTo("")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("     ").run {
                assertThat(this).isEqualTo("     ")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("+0000").run {
                assertThat(this).isEqualTo("+0000")
            }
            DateUtilities.convertPositiveOffsetToNegativeOffset("+0000 2022-01-05").run {
                assertThat(this).isEqualTo("+0000 2022-01-05")
            }
        }
    }
}