package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.USTimeZone
import gov.cdc.prime.router.common.DateUtilities.toLocalDateTime
import gov.cdc.prime.router.common.DateUtilities.toOffsetDateTime
import gov.cdc.prime.router.common.DateUtilities.toYears
import gov.cdc.prime.router.common.DateUtilities.toZonedDateTime
import gov.cdc.prime.router.unittest.UnitTestUtils.createConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import kotlin.test.Ignore
import kotlin.test.Test

private const val dateFormat = "yyyy-MM-dd"

class DateUtilitiesTests {
    @Test
    fun `test getDate output format`() {
        // Check LocalDate output formate
        val localDate = LocalDate.parse("2020-12-01")
        DateUtilities.getDateAsFormattedString(localDate, dateFormat).run {
            assertThat(this).isEqualTo("2020-12-01")
        }

        // Check LocalDateTime output format.
        val localDateTime = LocalDateTime.parse("2018-12-12T13:30:30")
        DateUtilities.getDateAsFormattedString(localDateTime, "M/d/yyyy HH:nn").run {
            assertThat(this).isEqualTo("12/12/2018 13:00")
        }

        // Check OffsetDateTime output format.
        val offsetDateTime = OffsetDateTime.parse("2018-12-12T13:30:30+05:00")
        DateUtilities.getDateAsFormattedString(offsetDateTime, "$dateFormat HH:mm:ss").run {
            assertThat(this).isEqualTo("2018-12-12 13:30:30")
        }

        // Check OffsetDateTime output format.
        val instant = Instant.parse("2020-12-03T13:30:30.000Z")
        DateUtilities.getDateAsFormattedString(instant, dateFormat).run {
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
            DateUtilities.getDateAsFormattedString(odt, "$dateFormat HH:mm:ss").run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30")
            }
            DateUtilities.getDateAsFormattedString(odt, "$dateFormat HH:mm:ssZZZ").run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30+0000")
            }
            // now check converting the date time to the negative offset
            DateUtilities.getDateAsFormattedString(odt, "$dateFormat HH:mm:ssZZZ", true).run {
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

    @Test
    @Ignore // works locally but not on GitHub. ignoring for now.
    fun `test offset date time extension method`() {
        // verify that offset date time to offset date time matches
        val offsetDateTime: TemporalAccessor = OffsetDateTime.now()
        assertThat(offsetDateTime).isEqualTo(offsetDateTime.toOffsetDateTime())
        // do the references match?
        assertThat(offsetDateTime).isSameAs(offsetDateTime.toOffsetDateTime())
        // now local date time to offset date time also matches
        LocalDateTime.from(offsetDateTime).run {
            assertThat(this.toOffsetDateTime(ZoneId.systemDefault())).isEqualTo(offsetDateTime)
        }
        // local date will coerce to "start of day". can't do a clean equals here
        // can't even do a clean "dates match" because coercion could cross the
        // midnight boundary which means dates could be different
        // we could convert this to PARSE a date time instead of using "now"
        LocalDate.from(offsetDateTime).run {
            val ofd = this.toOffsetDateTime(ZoneId.systemDefault())
            assertThat(ofd.hour).isEqualTo(0)
            assertThat(ofd.minute).isEqualTo(0)
            assertThat(ofd.second).isEqualTo(0)
            // handles PST to EDT in this range
            assertThat(ofd.offset.totalSeconds).isBetween(-28000, -14000)
        }
        // convert and check again
        ZonedDateTime.from(offsetDateTime).run {
            assertThat(this.toOffsetDateTime(ZoneId.systemDefault())).isEqualTo(offsetDateTime)
        }
    }

    @Test
    fun `test local date time extension method`() {
        val localDateTime = LocalDateTime.now()
        assertThat(localDateTime).isEqualTo(localDateTime.toLocalDateTime())
        // do the references match
        assertThat(localDateTime).isSameAs(localDateTime.toLocalDateTime())
        OffsetDateTime.from(localDateTime.atZone(ZoneId.systemDefault())).run {
            assertThat(this.toLocalDateTime()).isEqualTo(localDateTime)
        }
        ZonedDateTime.from(localDateTime.atZone(ZoneId.systemDefault())).run {
            assertThat(this.toLocalDateTime()).isEqualTo(localDateTime)
        }
        LocalDate.from(localDateTime).run {
            assertThat(this.toLocalDateTime().hour).isEqualTo(0)
            assertThat(this.toLocalDateTime().minute).isEqualTo(0)
            assertThat(this.toLocalDateTime().second).isEqualTo(0)
        }
    }

    @Test
    fun `test zoned date time extension method`() {
        val zonedDateTime = ZonedDateTime.parse("2022-01-04T11:00:00-05:00[US/Eastern]")
        assertThat(zonedDateTime.hour).isEqualTo(11)
        assertThat(zonedDateTime.minute).isEqualTo(0)
        // changing a zoned date time to a zoned date time should return the exact same object
        zonedDateTime.toZonedDateTime(ZoneId.of("US/Eastern")).run {
            assertThat(this).isEqualTo(zonedDateTime)
            assertThat(this).isSameAs(zonedDateTime)
        }
        // convert the zoned date time to an offset date time
        zonedDateTime.toOffsetDateTime().run {
            assertThat(this.hour).isEqualTo(zonedDateTime.hour)
            assertThat(this.minute).isEqualTo(zonedDateTime.minute)
            assertThat(this.year).isEqualTo(zonedDateTime.year)
            assertThat(this.dayOfMonth).isEqualTo(zonedDateTime.dayOfMonth)
            // do these point to the same date in time?
            assertThat(Instant.from(this)).isEqualTo(Instant.from(zonedDateTime))
        }
        // what happens if we try to upsize an offset date time with NO time zone info
        // into a zoned date time? let's find out
        val offsetDateTime = OffsetDateTime.parse("2022-01-04T11:00:00-05:00")
        offsetDateTime.toZonedDateTime().run {
            assertThat(this.hour).isEqualTo(zonedDateTime.hour)
            assertThat(this.minute).isEqualTo(zonedDateTime.minute)
            assertThat(this.year).isEqualTo(zonedDateTime.year)
            assertThat(this.dayOfMonth).isEqualTo(zonedDateTime.dayOfMonth)
            // do these point to the same date in time?
            assertThat(Instant.from(this)).isEqualTo(Instant.from(zonedDateTime))
        }
    }

    @Test
    fun `test now timestamp logic`() {
        // arrange our regexes
        val report = mockkClass(Report::class)
        val receiver = mockkClass(Receiver::class)
        every { receiver.dateTimeFormat }.returns(null)
        every { receiver.translation }.returns(
            createConfig(
                useHighPrecisionHeaderDateTimeFormat = false,
                convertPositiveDateTimeOffsetToNegative = false
            )
        )
        every { report.destination }.returns(receiver)
        every { report.getTimeZoneForReport() }.returns(ZoneId.of("UTC"))
        (report.destination?.translation as Hl7Configuration).let {
            DateUtilities.nowTimestamp(
                report.getTimeZoneForReport(),
                report.destination?.dateTimeFormat,
                it.convertPositiveDateTimeOffsetToNegative,
                it.useHighPrecisionHeaderDateTimeFormat
            ).run {
                assertThat(lowPrecisionTimeStampRegex.containsMatchIn(this)).isTrue()
            }
        }

        every { receiver.translation }.returns(
            createConfig(
                useHighPrecisionHeaderDateTimeFormat = true,
                convertPositiveDateTimeOffsetToNegative = false
            )
        )
        every { report.destination }.returns(receiver)
        (report.destination?.translation as Hl7Configuration).let {
            DateUtilities.nowTimestamp(
                report.getTimeZoneForReport(),
                report.destination?.dateTimeFormat,
                it.convertPositiveDateTimeOffsetToNegative,
                it.useHighPrecisionHeaderDateTimeFormat
            ).run {
                assertThat(highPrecisionTimeStampRegex.containsMatchIn(this)).isTrue()
            }
        }
    }

    @Test
    fun `check temporal accessor coercion`() {
        // set up our variables and mock
        // arrange
        val timeZoneId = ZoneId.of("US/Eastern")
        val rightNow = Instant.now().atZone(timeZoneId)
        val hl7Configuration = mockk<Hl7Configuration>()
        every { hl7Configuration.convertDateTimesToReceiverLocalTime } returns true
        val destination = mockk<Receiver>()
        every { destination.timeZone } returns USTimeZone.EASTERN
        every { destination.translation } returns hl7Configuration
        val report = mockk<Report>()
        every { report.destination } returns destination
        every { report.getTimeZoneForReport() } returns timeZoneId
        // act & assert
        rightNow.let {
            assertThat(it.toLocalDateTime()).isEqualTo(rightNow.toLocalDateTime())
        }
        OffsetDateTime.from(rightNow).let {
            assertThat(it.toLocalDateTime()).isEqualTo(rightNow.toLocalDateTime())
        }
    }

    @Test
    fun `test now timestamp logic to local time zone`() {
        fun getNowTimestamp(report: Report): String {
            val hl7Config = report.destination?.translation as? Hl7Configuration
            return DateUtilities.nowTimestamp(
                report.getTimeZoneForReport(),
                report.destination?.dateTimeFormat,
                hl7Config?.convertPositiveDateTimeOffsetToNegative,
                hl7Config?.useHighPrecisionHeaderDateTimeFormat
            )
        }

        val report = mockkClass(Report::class)
        val receiver = mockkClass(Receiver::class)
        every { receiver.timeZone }.returns(USTimeZone.EASTERN)
        every { receiver.translation }.returns(
            createConfig(convertDateTimesToReceiverLocalTime = true)
        )
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.OFFSET)
        every { report.destination }.returns(receiver)
        every { report.getTimeZoneForReport() }.returns(ZoneId.of("US/Eastern"))
        val easternTimeStampValue = getNowTimestamp(report)
        every { receiver.timeZone }.returns(USTimeZone.PACIFIC)
        every { report.getTimeZoneForReport() }.returns(ZoneId.of("US/Pacific"))
        val pacificTimeStampValue = getNowTimestamp(report)
        val easternParsedDate = DateUtilities.parseDate(easternTimeStampValue) as OffsetDateTime
        val pacificParsedDate = DateUtilities.parseDate(pacificTimeStampValue) as OffsetDateTime
        val duration = Duration.between(pacificParsedDate.toLocalDateTime(), easternParsedDate.toLocalDateTime())
        assertThat(duration.toHours()).isBetween(3, 4)
    }

    @Test
    fun `test format date for receiver`() {
        fun formatDateForReceiver(dateTimeValue: ZonedDateTime, report: Report): String {
            val hl7Config = report.destination?.translation as? Hl7Configuration
            return DateUtilities.formatDateForReceiver(
                dateTimeValue,
                report.getTimeZoneForReport(),
                report.destination?.dateTimeFormat ?: DateUtilities.DateTimeFormat.OFFSET,
                hl7Config?.convertPositiveDateTimeOffsetToNegative ?: false,
                hl7Config?.useHighPrecisionHeaderDateTimeFormat ?: false
            )
        }
        // arrange
        val report = mockkClass(Report::class)
        val receiver = mockkClass(Receiver::class)
        every { receiver.timeZone }.returns(USTimeZone.EASTERN)
        every { receiver.translation }.returns(
            createConfig(convertDateTimesToReceiverLocalTime = true)
        )
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.LOCAL)
        every { report.destination }.returns(receiver)
        every { report.getTimeZoneForReport() }.returns(ZoneId.of("US/Eastern"))
        // act
        val dateTimeValue = ZonedDateTime.parse(zonedDateTimeValue)
        formatDateForReceiver(dateTimeValue, report).run {
            // assert
            assertThat(this).isEqualTo("20220104110000")
        }
        // again
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.OFFSET)
        formatDateForReceiver(dateTimeValue, report).run {
            // assert
            assertThat(this).isEqualTo("20220104110000-0500")
        }
        // once more
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.HIGH_PRECISION_OFFSET)
        formatDateForReceiver(dateTimeValue, report).run {
            // assert
            assertThat(this).isEqualTo("20220104110000.0000-0500")
        }
        // now let's change the receiver to be PST and see what happens
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.LOCAL)
        every { receiver.timeZone }.returns(USTimeZone.PACIFIC)
        every { report.getTimeZoneForReport() }.returns(ZoneId.of("US/Pacific"))
        // act
        formatDateForReceiver(dateTimeValue, report).run {
            // assert
            assertThat(this).isEqualTo("20220104080000")
        }
        // again
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.OFFSET)
        formatDateForReceiver(dateTimeValue, report).run {
            // assert
            assertThat(this).isEqualTo("20220104080000-0800")
        }
        // once more
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.HIGH_PRECISION_OFFSET)
        formatDateForReceiver(dateTimeValue, report).run {
            // assert
            assertThat(this).isEqualTo("20220104080000.0000-0800")
        }
        // and now let's check that both methods return the same values
        every { receiver.dateTimeFormat }.returns(DateUtilities.DateTimeFormat.OFFSET)
        formatDateForReceiver(dateTimeValue, report).run {
            // get comparison
            val compareValue = DateUtilities.formatDateForReceiver(
                dateTimeValue,
                ZoneId.of(USTimeZone.PACIFIC.zoneId),
                DateUtilities.DateTimeFormat.OFFSET,
                convertPositiveDateTimeOffsetToNegative = false,
                useHighPrecisionHeaderDateTimeFormat = false,
            )
            // assert
            assertThat(this).isEqualTo(compareValue)
        }
    }

    // test the parse date feature
    @Test
    fun `test parse date`() {
        mapOf(
            "09/12/2021" to "20210912000000",
            "20220101" to "20220101000000",
            "01/02/2022 05:00" to "20220102050000",
            "1975-08-01T11:39:00Z" to "19750801113900",
            "2022-04-29T15:43:02.307Z" to "20220429154302",
            "3/1/1999" to "19990301000000",
            "12/1/1900" to "19001201000000",
            "2/3/02" to "20020203000000",
            "2/3/02 8:00" to "20020203080000",
        ).forEach { (input, expected) ->
            val parsed = DateUtilities.parseDate(input)
            assertThat(
                DateUtilities.formatDateForReceiver(
                    parsed,
                    DateUtilities.utcZone,
                    DateUtilities.DateTimeFormat.LOCAL,
                    false,
                    false
                )
            ).isEqualTo(expected)
        }
    }

    @Test
    fun `test calculate duration as years`() {
        // arrange our test cases
        val testCases = mapOf(
            Pair("2022-01-01", "2022-01-01") to 0,
            Pair("2022-01-01", "2022-12-31") to 0,
            Pair("2022-01-01", "2023-12-31") to 1,
            Pair("2023-12-31", "2022-01-01") to 1,
            Pair("2100-05-01", "2000-05-01") to 100,
            Pair("2100-05-01T05:01:00", "2000-05-01") to 100,
        )
        // act and assert
        testCases.forEach {
            it.run {
                val d1 = DateUtilities.parseDate(this.key.first).toOffsetDateTime()
                val d2 = DateUtilities.parseDate(this.key.second).toOffsetDateTime()
                Duration.between(d1, d2).run {
                    assertThat(this.toYears()).isEqualTo(it.value)
                }
            }
        }
    }

    companion object {
        // this regex checks for 14 digits, then a period, three digits, and then the offset
        val highPrecisionTimeStampRegex = "\\d{14}\\.\\d{4}[-|+]\\d{4}".toRegex()

        // this regex checks for 12 digits, and then the offset sign, and then four more digits
        val lowPrecisionTimeStampRegex = "^\\d{14}[-|+]\\d{4}".toRegex()

        // a const value for the zoned date time type
        const val zonedDateTimeValue = "2022-01-04T11:00:00-05:00[US/Eastern]"
    }
}