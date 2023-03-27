package gov.cdc.prime.router.common

import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * A collection of methods for dealing with dates and date times, parsing and formatting
 * in different directions.
 */
object DateUtilities {
    /** the default date pattern yyyyMMdd */
    const val datePattern = "yyyyMMdd"

    /** an alternate date pattern MMddyyyy */
    const val alternateDatePattern = "[yyyy-MM-dd][yyyy-dd-MM][MMdduuuu][uuuuMMdd]"

    /** a local date time pattern to use when formatting in local date time instead */
    const val localDateTimePattern = "yyyyMMddHHmmss"

    /** our standard offset date time pattern */
    const val datetimePattern = "yyyyMMddHHmmssxx"

    /** includes seconds and milliseconds in the offset for higher precision  */
    const val highPrecisionDateTimePattern = "yyyyMMddHHmmss.SSSSxx"

    /** wraps around all the possible variations of a date for finding something that matches */
    const val variableDateTimePattern = "[yyyyMMdd]" +
        "[yyyyMMdd[HHmm][ss][.S][Z]]" +
        "[yyyy-MM-dd HH:mm:ss.ZZZ]" +
        // nano seconds
        "[uuuuMMddHHmmss[.nnnn]Z][uuuuMMddHHmm[.nnnn]Z]" +
        // fractional seconds
        "[uuuuMMddHHmmss[.SSS]Z][uuuuMMddHHmm[.SSS]Z]" +
        // ISO-ish date time format, optional seconds, optional 'Z', and optional offset
        "[uuuu-MM-dd'T'HH:mm[:ss]['Z'][xxx]]" +
        "[uuuu-MM-dd'T'HH:mm[:ss][.S[S][S]]['Z'][xxx]]" +
        "[uuuu-MM-dd'T'HH:mm[:ss][.nnnn]['Z'][xxx]]" +
        "[yyyy-MM-dd[ H:mm:ss[.S[S][S]]]]" +
        "[yyyyMMdd[ H:mm:ss[.S[S][S]]]]" +
        "[M/d/yyyy[ H:mm[:ss[.S[S][S]]]]]" +
        "[yyyy/M/d[ H:mm[:ss[.S[S][S]]]]]"

    /**
     * A list of accepted date formats to try and parse to, one by one. In some instances it is
     * better to try and parse a date with a single pattern instead of the large variable date time
     * pattern above.
     **/
    val allowedDateFormats = listOf(
        "[yyyyMMdd[HHmm][ss][.S][Z]]", "[yyyy-MM-dd HH:mm:ss.ZZZ]",
        "[uuuuMMddHHmmss[.nnnn][Z]][uuuuMMddHHmm[.nnnn][Z]]",
        "[uuuuMMddHHmmss[.SSS][Z]][uuuuMMddHHmm[.SSS][Z]]",
        "[uuuu-MM-dd'T'HH:mm[:ss]['Z'][xxx]]",
        "[uuuu-MM-dd'T'HH:mm[:ss][.S[S][S]]['Z']]",
        "[uuuu-MM-dd'T'HH:mm[:ss][.nnnn]['Z'][xxx]]",
        "[yyyy-MM-dd[ H:mm:ss[.S[S][S]]]]",
        "[yyyyMMdd[ H:mm:ss[.S[S][S]]]]",
        "[yyyy/M/d[ H:mm[:ss[.S[S][S]]]]]",
        "yyyy-MM-dd", "yyyy-dd-MM", "MMdduuuu", "uuuuMMdd",
        "M/d/yy[ H:mm[:ss]]",
        "[M/d/yyyy[ H:mm[:ss[.S[S][S]]]]]",
    )

    /** A simple date formatter */
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH)

    /** A default formatter for date and time */
    val datetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datetimePattern, Locale.ENGLISH)

    /** a higher precision date time formatter that includes seconds, and can be used */
    val highPrecisionDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        highPrecisionDateTimePattern,
        Locale.ENGLISH
    )

    /** A formatter for local date times */
    val localDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(localDateTimePattern, Locale.ENGLISH)

    /** The zone ID for UTC */
    val utcZone = ZoneId.of("UTC")

    /**
     * The format to output the date time values as. A receiver could want date time values as an
     * offset or as their local time. This is independent of the actual time zone their data will
     * be presented in. For example, someone could have their date and time data written out at their
     * local timezone (PST for example), but also as an offset value. Or they could have their date
     * time written as a local date time at their local time zone. There are, of course, some ways
     * this could present some complications. For example, if you choose to have the time encoded to
     * local date time, but don't set a time zone for the receiver, we would end up setting the time
     * to UTC in local date format, which receivers may not expect. This should probably throw a warning
     * when saving the receiver.
     */
    enum class DateTimeFormat(val formatString: String) {
        OFFSET(datetimePattern),
        LOCAL(localDateTimePattern),
        HIGH_PRECISION_OFFSET(highPrecisionDateTimePattern),
        DATE_ONLY(datePattern)
    }

    /**
     * Returns our correct date time formatter for the params provided
     */
    fun getFormatter(
        dateTimeFormat: DateTimeFormat? = null,
        useHighPrecisionOffset: Boolean? = null
    ): DateTimeFormatter {
        return when (dateTimeFormat) {
            DateTimeFormat.HIGH_PRECISION_OFFSET -> highPrecisionDateTimeFormatter
            DateTimeFormat.LOCAL -> localDateTimeFormatter
            DateTimeFormat.DATE_ONLY -> dateFormatter
            else -> {
                if (useHighPrecisionOffset == true) {
                    highPrecisionDateTimeFormatter
                } else {
                    datetimeFormatter
                }
            }
        }
    }

    /**
     * This method takes a date value as a string and returns a
     * TemporalAccessor based on the variable date time pattern
     */
    fun parseDate(dateValue: String): TemporalAccessor {
        // check to see if the value has something in it
        if (dateValue.trimToNull() == null) {
            throw DateTimeException("Invalid value passed in for date value. Received $dateValue")
        }
        // parse out the date
        return try {
            DateTimeFormatter.ofPattern(variableDateTimePattern)
                .parseBest(
                    dateValue,
                    OffsetDateTime::from,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    Instant::from,
                    LocalDate::from
                )
        } catch (t: Throwable) {
            // our variable pattern has failed. let's try each one-by-one. if none of them
            // work, throw an error
            if (dateValue.indexOf('Z', ignoreCase = true) > -1) {
                val isoDate = tryParseIsoDate(dateValue)
                if (isoDate != null) {
                    return isoDate.toOffsetDateTime(utcZone)
                }
            }
            allowedDateFormats.forEach { format ->
                val parsedDate = parseDate(dateValue, format)
                if (parsedDate != null) return parsedDate
            }
            throw DateTimeParseException("Unable to parse $dateValue.", dateValue, 0, t)
        }
    }

    /** Parse the date according to the single pattern passed in, or return null */
    fun parseDate(dateValue: String, formatString: String): TemporalAccessor? {
        return try {
            DateTimeFormatter.ofPattern(formatString)
                .parseBest(
                    dateValue,
                    OffsetDateTime::from,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from,
                    Instant::from
                )
        } catch (_: Throwable) {
            null
        }
    }

    fun tryParseIsoDate(dateValue: String): TemporalAccessor? {
        return try {
            Instant.parse(dateValue)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /**
     * Given a [temporalAccessor] this will check the type that it needs to return
     * and then output based on the [outputFormat]. It finally checks to see if
     * [convertPositiveOffsetToNegative] is true, and if it is, it will flip
     * the bit in the offset from +0000 to -0000 for those locations that want to
     * or can only accept a negative offset
     */
    fun getDateAsFormattedString(
        temporalAccessor: TemporalAccessor,
        outputFormat: String = datetimePattern,
        convertPositiveOffsetToNegative: Boolean = false
    ): String {
        val outputFormatter = DateTimeFormatter.ofPattern(outputFormat)
        val formattedDate = when (temporalAccessor) {
            is LocalDate -> LocalDate.from(temporalAccessor)
                .atStartOfDay()
                .format(outputFormatter)
            is LocalDateTime -> LocalDateTime.from(temporalAccessor).format(outputFormatter)
            is OffsetDateTime -> OffsetDateTime.from(temporalAccessor).format(outputFormatter)
            is ZonedDateTime -> ZonedDateTime.from(temporalAccessor).format(outputFormatter)
            is Instant -> Instant.from(temporalAccessor).toString()
            else -> error("Unsupported format!")
        }

        return if (convertPositiveOffsetToNegative) {
            convertPositiveOffsetToNegativeOffset(formattedDate)
        } else {
            formattedDate
        }
    }

    fun isTimeGreaterThanZero(temporalAccessor: TemporalAccessor): Boolean {
        val hour = temporalAccessor.get(ChronoField.HOUR_OF_DAY)
        val minute = temporalAccessor.get(ChronoField.MINUTE_OF_HOUR)
        val second = temporalAccessor.get(ChronoField.SECOND_OF_MINUTE)

        return hour > 0 || minute > 0 || second > 0
    }

    /**
     * this looks to see if there is an "all zero offset" preceded by a plus sign on the
     * date time value. if there is, then we're going to flip this bit over to be
     * a negative offset. Note, according to the ISO-8601 specification, UTC is *NEVER*
     * supposed to be represented by `-0000`, only ever `+0000`. That said, RFC3339 does
     * offer the opportunity to use `-0000` to reflect an "unknown offset time", so it
     * is still valid and does parse. Also, Java understands and can parse from `-0000`
     * to `+0000`, so we are not breaking our implementation there.
     *
     * In addition to RFC3339 allowing for `-0000`, the HL7 spec allows for that value too,
     * so we should be good in a system that is HL7 compliant.
     *
     * RFC Link: https://datatracker.ietf.org/doc/html/rfc3339#section-4.3
     */
    fun convertPositiveOffsetToNegativeOffset(value: String): String {
        // look for the +0 offset
        val re = Regex(".+?\\+(00|0000|00:00)$")
        val match = re.find(value)
        // check to see if there is a match, if there isn't return the date as expected
        return when (match?.groups?.isNotEmpty()) {
            true -> {
                // get the offset value at the end of the string
                val offsetValue = match.groups.last()?.value
                // if there's actually a match, and all of the values in the offset are zero
                // because we only want to do this conversion IF the offset is zero. we never
                // want to do this if the offset is some other value
                if (offsetValue != null && offsetValue.all { it == '0' || it == ':' }) {
                    // create our replacement values
                    // I am doing it this way because it's possible that our desired level of
                    // precision for date time offset could change. I don't want my code to
                    // assume that it will always be +0000. +00 and +00:00 are also acceptable values,
                    // so I want this to be able to handle those options as well
                    val searchValue = "+$offsetValue"
                    val replaceValue = "-$offsetValue"
                    // replace the positive offset with the negative offset
                    value.replace(searchValue, replaceValue)
                } else {
                    // we had an offset, but it's not what we expected, so just return the
                    // original value we were passed in to be safe
                    value
                }
            }
            // the regex didn't match, so return the original value we passed in
            else -> value
        }
    }

    /**
     * Given a [dateTimeValue] and a [timeZone] and a [dateTimeFormat], this will attempt to format the date for a
     * receiver, by doing some checking on what is set for the receiver of the report.
     */
    fun formatDateForReceiver(
        dateTimeValue: TemporalAccessor,
        timeZone: ZoneId,
        dateTimeFormat: DateTimeFormat,
        convertPositiveDateTimeOffsetToNegative: Boolean,
        useHighPrecisionHeaderDateTimeFormat: Boolean
    ): String {
        // get the formatter based on the high precision header date time format
        val formatter: DateTimeFormatter = getFormatter(
            dateTimeFormat,
            useHighPrecisionHeaderDateTimeFormat
        )
        // return the actual date
        return if (convertPositiveDateTimeOffsetToNegative) {
            convertPositiveOffsetToNegativeOffset(formatter.format(dateTimeValue.toZonedDateTime(timeZone)))
        } else {
            formatter.format(dateTimeValue.toZonedDateTime(timeZone))
        }
    }

    /**
     * Returns now at the zone provided, or UTC if null
     * @param timeZone - The time zone to get the current date time of
     */
    fun nowAtZone(timeZone: ZoneId?): ZonedDateTime {
        val tz = timeZone ?: utcZone
        return ZonedDateTime.now(tz)
    }

    /**
     * Outputs the correctly formatted timestamp for our needs. This is primarily used by the HL7 serializer,
     * but could be generalized out further to allow for the CSV serializer and others to use it too
     */
    fun nowTimestamp(
        timeZone: ZoneId?,
        dateTimeFormat: DateTimeFormat?,
        convertPositiveDateTimeOffsetToNegative: Boolean? = false,
        useHighPrecisionHeaderDateTimeFormat: Boolean? = false
    ): String {
        val tz = timeZone ?: utcZone
        // now format the date to what the receiver wants
        return formatDateForReceiver(
            nowAtZone(tz),
            tz,
            dateTimeFormat ?: DateTimeFormat.OFFSET,
            convertPositiveDateTimeOffsetToNegative ?: false,
            useHighPrecisionHeaderDateTimeFormat ?: false
        )
    }

    /** tries to parse the string [value], and if it passes, returns true, otherwise false */
    fun tryParse(value: String? = null): Boolean {
        if (value == null || value.trimToNull() == null) return false
        return try {
            parseDate(value)
            true
        } catch (_: Exception) {
            // we don't care about the exception, just return false
            false
        }
    }

    /**
     * The getDateTime function return the OffsetDatetime.  If it can't parse, it will throw either
     * DateTimeParseException or DateTimeException.  Which allows the caller to catch the exception.
     * @param [cleanedFormattedValue] datetime value to be parsed.
     * @param [format] format to parse
     * @return [OffsetDateTime] the best parsed datetime value
     */
    fun getDateTime(cleanedFormattedValue: String, format: String?): OffsetDateTime {
        val dateTime = try {
            // Try an ISO pattern
            OffsetDateTime.parse(cleanedFormattedValue)
        } catch (e: DateTimeParseException) {
            null
        } ?: try {
            // Try a HL7 pattern
            val formatter = DateTimeFormatter.ofPattern(format ?: datetimePattern, Locale.ENGLISH)
            OffsetDateTime.parse(cleanedFormattedValue, formatter)
        } catch (e: DateTimeParseException) {
            null
        } ?: try {
            // Try to parse using a LocalDate pattern assuming it is in our canonical dateFormatter. Central timezone.
            val date = LocalDate.parse(cleanedFormattedValue, dateFormatter)
            OffsetDateTime.of(date, LocalTime.of(0, 0), Environment.rsTimeZone)
        } catch (e: DateTimeParseException) {
            null
        } ?: try {
            // Try to parse using a LocalDate pattern, assuming it follows a non-canonical format value.
            // Example: 'yyyy-mm-dd' - the incoming data is a Date, but not our canonical date format.
            val formatter = DateTimeFormatter.ofPattern(format ?: datetimePattern, Locale.ENGLISH)
            val date = LocalDate.parse(cleanedFormattedValue, formatter)
            OffsetDateTime.of(date, LocalTime.of(0, 0), Environment.rsTimeZone)
        } catch (e: DateTimeParseException) {
            null
        } ?: try {
            getBestDateTime(cleanedFormattedValue)
        } catch (e: DateTimeParseException) {
            null
        } catch (e: DateTimeException) {
            null
        } ?: try {
            getBestDateTime(cleanedFormattedValue)
        } catch (e: DateTimeParseException) {
            throw DateTimeParseException(e.message, e.parsedString, e.errorIndex)
        } catch (e: DateTimeException) {
            throw DateTimeException(e.message)
        }

        return dateTime
    }

    /**
     * The getBestDateTime function parse to get the best match and return OffsetDatetime.
     * If it can't parse, it will throw either DateTimeParseException or DateTimeException.
     * Which allows the caller to catch the exception.
     * @param [value] datetime value to be parsed.
     * @return [OffsetDateTime] the best parsed datetime value
     */
    private fun getBestDateTime(value: String): OffsetDateTime {
        return parseDate(value).toOffsetDateTime()
    }

    /**
     * Convert a [Duration] to an integer years value. It takes the [Duration.toDays] value
     * and divides it by 365.0, and then takes the [floor] of that value. This is obviously
     * a rough calculation given the fact that there is the possibility of a shift in the year
     * based on the number of leap years in the duration. For example, someone born in 2000, and
     * living until 2100 would experience 25 leap years, which means that we could shift on the
     * number of years calculated from this slightly based on birthdate, but it's an outside
     * chance, so it's acceptable
     */
    fun Duration.toYears(): Int {
        return floor(abs(this.toDays() / 365.0)).toInt()
    }

    /**
     * Given a temporal accessor of some sort, coerce it to an offset date time value.
     * If the temporal accessor is of type LocalDate, then we don't have a time, and we coerce it
     * to use the local "start of day", and then convert to the date time offset.
     */
    fun TemporalAccessor.toOffsetDateTime(zoneId: ZoneId? = null): OffsetDateTime {
        return when (this) {
            // coerce the local date to the start of the day. it's not great, but if we did not
            // get the time, then pushing it to start of day is *probably* okay. At some point
            // we should probably throw a coercion warning when we do this
            is LocalDate ->
                OffsetDateTime
                    .from(this.atStartOfDay().atZone(zoneId ?: utcZone))
                    .toOffsetDateTime()
            is LocalDateTime ->
                OffsetDateTime
                    .from(this.atZone(zoneId ?: utcZone))
                    .toOffsetDateTime()
            is Instant -> OffsetDateTime.from(this.atZone(zoneId ?: utcZone)).toOffsetDateTime()
            is OffsetDateTime, is ZonedDateTime -> OffsetDateTime.from(this)
            else -> error("Unsupported format!")
        }
    }

    /**
     * Given a temporal accessor, it converts it to a local date time instant. It can cleanly convert
     * ZonedDateTime, OffsetDateTime, Instant, and LocalDateTime. For LocalDate, it makes the assumption
     * that we are working off the start of day. Making assumptions is *bad*, but unfortunately have to
     * sometimes do this. In this case, if the sender did not give us a time value, then most likely the
     * time is not important, so putting the time marker at the start of the date is maybe probably
     * possibly okay. As an example, consider date of birth. For the patient demographic information,
     * having the time someone was born is not as important as having the date. Therefore, setting the
     * time value to start of day is okay. Probably. Maybe. For future work we should probably throw
     * a coercion error when this happens and root these out of the system.
     */
    fun TemporalAccessor.toLocalDateTime(): LocalDateTime {
        return when (this) {
            is LocalDateTime -> this
            // we are coercing local date to start of date for the local time and then casting it to
            // local date time. This is a dicey proposition, and we should probably elicit some kind
            // of warning when doing this. Perhaps in the future we should disable this or make this
            // throw an error
            is LocalDate -> LocalDateTime.from(this.atStartOfDay())
            is ZonedDateTime, is OffsetDateTime, is Instant -> LocalDateTime.from(this)
            else -> error("Unsupported format")
        }
    }

    /** Convert to a local date */
    fun TemporalAccessor.toLocalDate(): LocalDate {
        return when (this) {
            is LocalDate -> this
            is ZonedDateTime, is OffsetDateTime, is LocalDateTime, is Instant -> LocalDate.from(this)
            else -> error("Unsupported format")
        }
    }

    /**
     * Given a Temporal Accessor, this attempts to convert to other date time types available. Some
     * conversions require a ZoneId. Having just a date and time, for example, is not enough. Even
     * having the offset is sometimes not good enough, but it is better than nothing. Therefore, if you
     * try to convert a LocalDate to a ZonedDateTime without telling it what time zone "local" is, it's
     * going to fail.
     */
    fun TemporalAccessor.toZonedDateTime(zoneId: ZoneId? = null): ZonedDateTime {
        return when (this) {
            is ZonedDateTime -> {
                if (zoneId != null && this.zone != zoneId) {
                    this.withZoneSameInstant(zoneId)
                } else {
                    this
                }
            }
            is OffsetDateTime, is Instant -> {
                if (zoneId != null && isTimeGreaterThanZero(this)) {
                    ZonedDateTime.from(this).withZoneSameInstant(zoneId)
                } else {
                    ZonedDateTime.from(this)
                }
            }
            is LocalDateTime -> {
                if (zoneId == null) error("Cannot determine time zone to use for conversion")
                ZonedDateTime.from(this.atZone(zoneId))
            }
            is LocalDate -> {
                if (zoneId == null) error("Cannot determine time zone to use for conversion")
                ZonedDateTime.from(this.atStartOfDay().atZone(zoneId))
            }
            else -> error("Unsupported format for converting to ZonedDateTime")
        }
    }

    /**
     * An extension method to TemporalAccessor that will format the date for us by calling the internal method
     **/
    fun TemporalAccessor.formatDateTimeForReceiver(report: Report? = null): String {
        val hl7Config = report?.destination?.translation as? Hl7Configuration

        return formatDateForReceiver(
            this,
            report?.getTimeZoneForReport() ?: ZoneId.of("UTC"),
            report?.destination?.dateTimeFormat ?: DateTimeFormat.OFFSET,
            hl7Config?.convertPositiveDateTimeOffsetToNegative ?: false,
            hl7Config?.useHighPrecisionHeaderDateTimeFormat ?: false
        )
    }

    /** Format the date for the receiver, but overriding the format with one specifically passed in */
    fun TemporalAccessor.formatDateTimeForReceiver(dateTimeFormat: DateTimeFormat, report: Report? = null): String {
        val hl7Config = report?.destination?.translation as? Hl7Configuration
        return formatDateForReceiver(
            this,
            report?.getTimeZoneForReport() ?: ZoneId.of("UTC"),
            dateTimeFormat,
            hl7Config?.convertPositiveDateTimeOffsetToNegative ?: false,
            hl7Config?.useHighPrecisionHeaderDateTimeFormat ?: false
        )
    }

    /**
     * Given any [dateTimeFormat] and [convertPositiveDateTimeOffsetToNegative] value, attempts to format the
     * TemporalAccessor to a string value
     **/
    fun TemporalAccessor.asFormattedString(
        dateTimeFormat: String? = null,
        convertPositiveDateTimeOffsetToNegative: Boolean = false
    ): String {
        return getDateAsFormattedString(
            this,
            dateTimeFormat ?: DateUtilities.datetimePattern,
            convertPositiveDateTimeOffsetToNegative
        )
    }
}