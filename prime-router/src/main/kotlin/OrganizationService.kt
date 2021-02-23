package gov.cdc.prime.router

import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * An `OrganizationService` represents the agent that the data hub sends reports
 * (minus the credentials used by that agent, of course). It contains information about
 * the specific topic and schema that the sender needs.
 *
 * @param name of the service
 * @param topic defines the set of schemas that can translate to each other
 * @param schema defines the schema that the org wishes to receive
 * @param jurisdictionalFilter defines the set of elements and regexs that filter the topic
 * @param transforms defines the number of transforms to apply to the report before sending
 * @param batch defines how to batch reports to the org. If null, then send immediately
 * @param format that the org wishes to receive
 * @param transports that the org wishes to receive
 */
data class OrganizationService(
    val name: String,
    val topic: String,
    val schema: String,
    val jurisdictionalFilter: List<String> = emptyList(),
    val transforms: Map<String, String> = emptyMap(),
    val defaults: Map<String, String> = emptyMap(),
    val batch: Batch? = null,
    val address: String = "",
    val format: Report.Format = Report.Format.CSV,
    val transports: List<TransportType> = emptyList(),
    val description: String = ""
) {
    lateinit var organization: Organization
    val fullName: String get() = "${organization.name}.$name"

    /**
     * Defines how batching of sending should proceed. Allows flexibility of
     * frequency and transmission time on daily basis, but not complete flexibility.
     *
     * @param operation MERGE will combine all reports in the batch into a single batch
     * @param numberPerDay Number of batches per day must be 1 to 3600
     * @param initialBatch The time of the day to send the first batch. Must be format of hh:mm.
     * @param timeZone the time zone of the initial sending
     */
    data class Batch(
        val operation: BatchOperation = BatchOperation.NONE,
        val numberPerDay: Int = 1,
        val initialBatch: String = "00:00",
        val timeZone: USTimeZone = USTimeZone.EASTERN,
        val maxReportCount: Int = 100,
    ) {
        /**
         * Calculate the next batch time.
         *
         * @param now is the current time
         * @param minDurationInSeconds in the future
         */
        fun nextBatchTime(now: OffsetDateTime = OffsetDateTime.now(), minDurationInSeconds: Int = 10): OffsetDateTime {
            if (minDurationInSeconds < 1) error("MinDuration must be at least 1 second")
            val zoneId = ZoneId.of(timeZone.zoneId)
            val zonedNow = now
                .atZoneSameInstant(zoneId)
                .plusSeconds(minDurationInSeconds.toLong())
                .withNano(0)

            val initialSeconds = LocalTime.parse(initialBatch).toSecondOfDay()
            val durationFromInitial = zonedNow.toLocalTime().toSecondOfDay() - initialSeconds
            val period = (24 * 60 * 60) / numberPerDay
            val secondsLeftInPeriod = period - ((durationFromInitial + (24 * 60 * 60)) % period)
            return zonedNow
                .plusSeconds(secondsLeftInPeriod.toLong())
                .toOffsetDateTime()
        }

        fun isValid(): Boolean {
            return numberPerDay in 1..(24 * 60)
        }
    }

    enum class BatchOperation {
        NONE,
        MERGE
    }
}