package gov.cdc.prime.router.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/**
 * A collection of methods for dealing with dates and date times, parsing and formatting
 * in different directions.
 */
object DateUtilities {
    const val datePattern = "yyyyMMdd"
    const val datePatternMMddyyyy = "MMddyyyy"
    const val datetimePattern = "yyyyMMddHHmmZZZ"
    /** includes seconds  */
    const val highPrecisionDateTimePattern = "yyyyMMddHHmmss.SSSZZZ"
    /** wraps around all the possible variations of a date for finding something that matches */
    const val variableDateTimePattern = "[yyyyMMdd]" +
        "[yyyyMMddHHmmssZ]" +
        "[yyyyMMddHHmmZ]" +
        "[yyyyMMddHHmmss][yyyy-MM-dd HH:mm:ss.ZZZ]" +
        "[yyyy-MM-dd[ H:mm:ss[.S[S][S]]]]" +
        "[yyyyMMdd[ H:mm:ss[.S[S][S]]]]" +
        "[M/d/yyyy[ H:mm[:ss[.S[S][S]]]]]" +
        "[yyyy/M/d[ H:mm[:ss[.S[S][S]]]]]" +
        "[MMddyyyy]"

    /** A simple date formatter */
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH)
    /** A default formatter for date and time */
    val datetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(datetimePattern, Locale.ENGLISH)
    /** a higher precision date time formatter that includes seconds, and can be used */
    val highPrecisionDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        highPrecisionDateTimePattern,
        Locale.ENGLISH
    )

    /**
     * This method takes a date value as a string and returns a
     * TemporalAccessor based on the variable date time pattern
     */
    fun parseDate(dateValue: String): TemporalAccessor {
        return DateTimeFormatter.ofPattern(variableDateTimePattern)
            .parseBest(
                dateValue,
                OffsetDateTime::from,
                LocalDateTime::from,
                Instant::from,
                LocalDate::from,
                ZonedDateTime::from
            )
    }

    /**
     * Given a temporal accessor this will check the type that it needs to return
     * and then output based on the format. you can extend this to accept a third
     * variable which would be the element's output format, and do an extra branch
     * based on that
     */
    fun getDateAsFormattedString(
        temporalAccessor: TemporalAccessor,
        outputFormat: String,
        convertPositiveOffsetToNegative: Boolean = false
    ): String {
        val outputFormatter = DateTimeFormatter.ofPattern(outputFormat)
        val formattedDate = when (temporalAccessor) {
            is LocalDate -> LocalDate.from(temporalAccessor)
                .atStartOfDay()
                .format(outputFormatter)
            is LocalDateTime -> LocalDateTime.from(temporalAccessor)
                .format(outputFormatter)
            is OffsetDateTime -> OffsetDateTime.from(temporalAccessor)
                .format(outputFormatter)
            is Instant -> Instant.from(temporalAccessor).toString()
            else -> error("Unsupported format!")
        }

        return if (convertPositiveOffsetToNegative) {
            convertPositiveOffsetToNegativeOffset(formattedDate)
        } else {
            formattedDate
        }
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
     * Given a temporal accessor of some sort, coerce it to an offset date time value.
     * If the temporal accessor is of type LocalDate, then we don't have a time, and we coerce it
     * to use the local "start of day", and then convert to the date time offset.
     */
    fun TemporalAccessor.toOffsetDateTime(): OffsetDateTime {
        return when (this) {
            // coerce the local date to the start of the day. it's not great, but if we did not
            // get the time, then pushing it to start of day is *probably* okay. At some point
            // we should probably throw a coercion warning when we do this
            is LocalDate -> OffsetDateTime.from(LocalDate.from(this).atStartOfDay())
            is LocalDateTime, is OffsetDateTime, is Instant -> OffsetDateTime.from(this)
            else -> error("Unsupported format!")
        }
    }
}