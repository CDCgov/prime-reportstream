package gov.cdc.prime.router

/**
 * An `OrganizationService` represents the agent that the data hub sends reports
 * (minus the credentials used by that agent, of course). It contains information about
 * the specific topic and schema that the sender needs.
 */
data class OrganizationService(
    val name: String,
    val topic: String,
    val schema: String,
    val jurisdictionalFilter: Map<String, String> = emptyMap(),
    val transforms: Map<String, String> = emptyMap(),
    val address: String = "",
    val format: Format = Format.CSV,
    val transport: Transport = Transport(Transport.TransportType.SFTP, "localhost", "22"),
) {
    lateinit var organization: Organization
    val fullName: String get() = "${organization.name}.${name}"

    enum class Format {
        CSV,
        HL7;
        //FHIR

        fun toExt(): String {
            return when (this) {
                CSV -> "csv"
                HL7 -> "hl7"
            }
        }
    }

    data class Transport(
        val type: TransportType = TransportType.SFTP,
        val host: String = "localhost",
        val port: String = "22",
    ) {
        enum class TransportType {
            SFTP;
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
                if (mappedReport.rowCount == 0) return@mapNotNull null
                Pair(mappedReport, service)
            }
        }

        private fun mapByService(input: Report, organizationService: OrganizationService): Report {
            // Filter according to receiver patterns
            val filteredTable = input.filter(organizationService.jurisdictionalFilter)

            // Apply mapping to change schema
            val toReport: Report = if (organizationService.schema != filteredTable.schema.name) {
                val toSchema = Metadata
                    .findSchema(organizationService.schema)
                    ?: error("${organizationService.schema} schema is missing from catalog")
                val mapping = filteredTable.schema.buildMapping(toSchema)
                filteredTable.applyMapping(mapping)
            } else {
                filteredTable
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
            return transformed
        }
    }
}