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
 * @param batch defines how to batch reports to the org. If null, then send immediatlely
 * @param format that the org wishes to receive
 * @param transport that the org wishes to recieve
 */
data class OrganizationService(
    val name: String,
    val topic: String,
    val schema: String,
    val jurisdictionalFilter: Map<String, String> = emptyMap(),
    val transforms: Map<String, String> = emptyMap(),
    val batch: Batch? = null,
    val address: String = "",
    val format: Format = Format.CSV,
    val transport: Transport = Transport(Transport.TransportType.DEFAULT)
) {
    lateinit var organization: Organization
    val fullName: String get() = "${organization.name}.$name"

    enum class Format {
        CSV,
        HL7;
        // FHIR

        fun toExt(): String {
            return when (this) {
                CSV -> "csv"
                HL7 -> "hl7"
            }
        }
    }

    /**
     * Defines how batching of sending should proceed. Allows flexibility of
     * frequency and transmission time on daily basis, but not complete flexibility.
     *
     * @param operation MERGE will combine all reports in the batch into a single batch
     * @param numberPerDay Number of batches per day must be 1 to 3600
     * @param initialTime The time of the day to send the first batch. Must be format of hh:mm.
     * @param timeZone the time zone of the initial sending
     */
    data class Batch(
        val operation: BatchOperation = BatchOperation.NONE,
        val numberPerDay: Int = 1,
        val initialBatch: String = "00:00",
        val timeZone: BatchTimeZone = BatchTimeZone.EASTERN,
        val maxBatchSize: Int = 100,
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
            var zonedNow = now
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

    enum class BatchTimeZone(val zoneId: String) {
        PACIFIC("US/Pacific"),
        MOUNTAIN("US/Mountain"),
        ARIZONA("US/Arizona"),
        CENTRAL("US/Central"),
        EASTERN("US/Eastern"),
        SOMOA("US/Somoa"),
        HAWAII("US/Hawaii"),
        EAST_INDIANA("US/East-Indiana"),
        INDIANA_STARKE("US/Indiana-Starke"),
        MICHIGAN("US/Michigan"),
    }

    data class Transport(
        val type: TransportType = TransportType.DEFAULT,
        val host: String = "localhost",
        val port: String = "22",
        val filePath: String = "."
    ) {
        enum class TransportType {
            SFTP,
            DEFAULT
            // EMAIL
            // DROPBOX
            // API
        }
    }

    companion object {
        fun mapByServices(input: Report, organizationServices: List<OrganizationService>): List<Report> {
            return organizationServices.map { service -> mapByService(input, service) }
        }

        fun filterAndMapByService(
            input: Report,
            organizationServices: List<OrganizationService>,
        ): List<Pair<Report, OrganizationService>> {
            if (input.isEmpty()) return emptyList()
            return organizationServices.filter { service ->
                service.topic == input.schema.topic
            }.mapNotNull { service ->
                val mappedReport = mapByService(input, service)
                if (mappedReport.itemCount == 0) return@mapNotNull null
                Pair(mappedReport, service)
            }
        }

        private fun mapByService(input: Report, organizationService: OrganizationService): Report {
            // Filter according to receiver patterns
            val filteredReport = input.filter(organizationService.jurisdictionalFilter)

            // Apply mapping to change schema
            val toReport: Report = if (organizationService.schema != filteredReport.schema.name) {
                val toSchema = Metadata
                    .findSchema(organizationService.schema)
                    ?: error("${organizationService.schema} schema is missing from catalog")
                val mapping = filteredReport.schema.buildMapping(toSchema)
                filteredReport.applyMapping(mapping)
            } else {
                filteredReport
            }

            // Transform reports
            var transformed = toReport
            organizationService.transforms.forEach { (transform, transformValue) ->
                when (transform) {
                    "deidentify" -> if (transformValue == "true") {
                        transformed = transformed.deidentify()
                    }
                }
            }
            return transformed.copy(destination = organizationService)
        }
    }
}