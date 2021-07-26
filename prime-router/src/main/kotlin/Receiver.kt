package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * An `Receiver` represents the agent that the data hub sends reports to
 * (minus the credentials used by that agent, of course). It contains information about
 * the specific topic and schema that the receiver needs.
 *
 * @param name of the receiver
 * @param organizationName of the receiver
 * @param topic defines the set of schemas that can translate to each other
 * @param translation configuration to translate
 * @param jurisdictionalFilter defines the set of elements and regexes that filter the data for this receiver
 * @param qualityFilter defines the set of elements and regexes that do qualiyty filtering on the data for this receiver
 * @param deidentify transform
 * @param timing defines how to delay reports to the org. If null, then send immediately
 * @param description of the receiver
 * @param transport that the org wishes to receive
 * @param externalName an external display name for the receiver. useful for display in the website
 */
open class Receiver(
    val name: String,
    val organizationName: String,
    val topic: String,
    val translation: TranslatorConfiguration,
    val jurisdictionalFilter: List<String> = emptyList(),
    val qualityFilter: List<String> = emptyList(),
    // If this is true, then do the NOT of 'qualityFilter'.  Like a 'grep -v'
    val reverseTheQualityFilter: Boolean = false,
    val deidentify: Boolean = false,
    val timing: Timing? = null,
    val description: String = "",
    val transport: TransportType? = null,
    val externalName: String? = null,
) {
    // Custom constructor
    constructor(
        name: String,
        organizationName: String,
        topic: String,
        schemaName: String,
        format: Report.Format = Report.Format.CSV
    ) : this(
        name, organizationName, topic,
        CustomConfiguration(schemaName = schemaName, format = format, emptyMap(), "standard", null)
    )

    constructor(copy: Receiver) : this(
        copy.name,
        copy.organizationName,
        copy.topic,
        copy.translation,
        copy.jurisdictionalFilter,
        copy.qualityFilter,
        copy.reverseTheQualityFilter,
        copy.deidentify,
        copy.timing,
        copy.description,
        copy.transport,
        copy.externalName,
    )

    @get:JsonIgnore
    val fullName: String get() = "$organizationName$fullNameSeparator$name"
    @get:JsonIgnore
    val schemaName: String get() = translation.schemaName
    @get:JsonIgnore
    val format: Report.Format get() = translation.format
    // adds a display name property that tries to show the external name, or the regular name if there isn't one
    @get:JsonIgnore
    val displayName: String get() = externalName ?: name

    /**
     * Defines how batching of sending should proceed. Allows flexibility of
     * frequency and transmission time on daily basis, but not complete flexibility.
     *
     * @param operation MERGE will combine all reports in the batch into a single batch
     * @param numberPerDay Number of batches per day must be 1 to 3600
     * @param initialTime The time of the day to first send. Must be format of hh:mm.
     * @param timeZone the time zone of the initial sending
     */
    data class Timing(
        val operation: BatchOperation = BatchOperation.NONE,
        val numberPerDay: Int = 1,
        val initialTime: String = "00:00",
        val timeZone: USTimeZone = USTimeZone.EASTERN,
        val maxReportCount: Int = 500,
    ) {
        /**
         * Calculate the next event time.
         *
         * @param now is the current time
         * @param minDurationInSeconds in the future
         */
        fun nextTime(now: OffsetDateTime = OffsetDateTime.now(), minDurationInSeconds: Int = 10): OffsetDateTime {
            if (minDurationInSeconds < 1) error("MinDuration must be at least 1 second")
            val zoneId = ZoneId.of(timeZone.zoneId)
            val zonedNow = now
                .atZoneSameInstant(zoneId)
                .plusSeconds(minDurationInSeconds.toLong())
                .withNano(0)

            val initialSeconds = LocalTime.parse(initialTime).toSecondOfDay()
            val durationFromInitial = zonedNow.toLocalTime().toSecondOfDay() - initialSeconds
            val period = (24 * 60 * 60) / numberPerDay
            val secondsLeftInPeriod = period - ((durationFromInitial + (24 * 60 * 60)) % period)
            return zonedNow
                .plusSeconds(secondsLeftInPeriod.toLong())
                .toOffsetDateTime()
        }

        @JsonIgnore
        fun isValid(): Boolean {
            return numberPerDay in 1..(24 * 60)
        }
    }

    enum class BatchOperation {
        NONE,
        MERGE
    }

    /**
     * Validate the object and return null or an error message
     */
    fun consistencyErrorMessage(metadata: Metadata): String? {
        when (translation) {
            is CustomConfiguration -> {
                if (metadata.findSchema(translation.schemaName) == null)
                    return "Invalid schemaName: ${translation.schemaName}"
            }
            is Hl7Configuration -> {
                if (transport != null && transport is RedoxTransportType)
                    return "HL7 configurations should not have a Redox transport"
            }
            is RedoxConfiguration -> {
                if (transport != null && transport !is RedoxTransportType)
                    return "Redox configurations should have Redox transports"
            }
        }
        return null
    }

    companion object {
        const val fullNameSeparator = "."

        fun parseFullName(fullName: String): Pair<String, String> {
            val splits = fullName.split(Sender.fullNameSeparator)
            return when (splits.size) {
                2 -> Pair(splits[0], splits[1])
                else -> error("Internal Error: Invalid fullName: $fullName")
            }
        }
    }
}