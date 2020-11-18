package gov.cdc.prime.router



data class Receiver(
    val name: String,
    val topic: String,
    val schema: String,
    val description: String = "",
    val patterns: Map<String, String> = emptyMap(),
    val transforms: Map<String, String> = emptyMap(),
    val address: String = "",
    val format: TopicFormat = TopicFormat.CSV,
    val transport: Transport = Transport( Transport.TransportType.SFTP, "localhost", "22" )
) {

    enum class TopicFormat {
        CSV,
        HL7;
        //FHIR

        fun toExt(): String {
            return when (this) {
                TopicFormat.CSV -> "csv"
                TopicFormat.HL7 -> "hl7"
            }
        }
    }

    data class Transport(
        val type: TransportType = TransportType.SFTP,
        val host: String = "localhost",
        val port: String = "22" ){
        enum class TransportType {
            SFTP;
            // EMAIL
            // DROPBOX
            // API
        }
    }



    companion object {
        fun mapByReceivers(input: MappableTable, receivers: List<Receiver>): List<MappableTable> {
            return receivers.map { receiver -> mapByReceiver(input, receiver) }
        }

        fun filterAndMapByReceiver(
            input: MappableTable,
            receivers: List<Receiver>
        ): List<Pair<MappableTable, Receiver>> {
            if (input.isEmpty()) return emptyList()
            return receivers.filter { receiver ->
                receiver.topic == input.schema.topic
            }.mapNotNull { receiver ->
                val mappedTable = mapByReceiver(input, receiver)
                if (mappedTable.rowCount == 0) return@mapNotNull null
                Pair(mappedTable, receiver)
            }
        }

        private fun mapByReceiver(input: MappableTable, receiver: Receiver): MappableTable {
            val outputName = "${receiver.name}-${input.name}"

            // Filter according to receiver patterns
            val filteredTable = input.filter(name = outputName, patterns = receiver.patterns)

            // Apply mapping to change schema
            val toTable: MappableTable = if (receiver.schema != filteredTable.schema.name) {
                val toSchema =
                    Metadata.findSchema(receiver.schema)
                        ?: error("${receiver.schema} schema is missing from catalog")
                val mapping = filteredTable.schema.buildMapping(toSchema)
                filteredTable.applyMapping(outputName, mapping)
            } else {
                filteredTable
            }

            // Transform tables
            var transformed = toTable
            receiver.transforms.forEach { (transform, transformValue) ->
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