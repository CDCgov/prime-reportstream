package gov.cdc.prime.router.common

import gov.cdc.prime.router.Element
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * A collection of methods for dealing with dates and date times, parsing and formatting
 * in different directions.
 */
object DateUtilities {
    /** The variable date time pattern that we use to parse dates */
    const val variableDateTimePattern = "[yyyyMMddHHmmssZ]" +
        "[yyyyMMddHHmmZ]" +
        "[yyyyMMddHHmmss][yyyy-MM-dd HH:mm:ss.ZZZ]" +
        "[yyyy-MM-dd[ HH:mm:ss[.S[S][S]]]]" +
        "[yyyyMMdd[ HH:mm:ss[.S[S][S]]]]" +
        "[M/d/yyyy[ HH:mm[:ss[.S[S][S]]]]]" +
        "[yyyy/M/d[ HH:mm[:ss[.S[S][S]]]]]"

    /**
     * This method takes a date value as a string and returns a
     * TemporalAccessor based on the variable date time pattern
     */
    fun parseDate(dateValue: String): TemporalAccessor {
        return DateTimeFormatter.ofPattern(variableDateTimePattern)
            .parseBest(dateValue, OffsetDateTime::from, LocalDateTime::from, Instant::from, LocalDate::from)
    }

    /**
     * Given a temporal accessor this will check the type that it needs to return
     * and then output based on the format. you can extend this to accept a third
     * variable which would be the element's output format, and do an extra branch
     * based on that
     */
    fun getDate(
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
            Element.convertPositiveOffsetToNegativeOffset(formattedDate)
        } else {
            formattedDate
        }
    }
}