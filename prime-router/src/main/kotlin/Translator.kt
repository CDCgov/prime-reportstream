package gov.cdc.prime.router

/**
 * The translator converts reports from one schema to another.
 * The object can be reused for multiple translations.
 *
 * Dev Note: This glue code was originally in the Report class and then
 * in the Metadata class.
 */
class Translator(private val metadata: Metadata) {
    /**
     * A mapping defines how to translate from one schema to another
     */
    data class Mapping(
        val toSchema: Schema,
        val fromSchema: Schema,
        val useDirectly: Map<String, String>,
        val useValueSet: Map<String, String>,
        val useMapper: Map<String, Mapper>,
        val useDefault: Map<String, String>,
        val missing: Set<String>,
    )

    /**
     * Translate this report by the list of services in metadata. One report for every service, reports
     * may be empty.
     */
    fun translateByService(input: Report, defaultValues: DefaultValues = emptyMap()): List<Report> {
        return metadata.organizationServices.map { service -> translateByService(input, service, defaultValues) }
    }

    /**
     * Translate and filter by the list of services in metadata. Only return reports that have items.
     */
    fun filterAndTranslateByService(input: Report, defaultValues: DefaultValues = emptyMap()): List<Pair<Report, OrganizationService>> {
        if (input.isEmpty()) return emptyList()
        return metadata.organizationServices.filter { service ->
            service.topic == input.schema.topic
        }.mapNotNull { service ->
            val mappedReport = translateByService(input, service, defaultValues)
            if (mappedReport.itemCount == 0) return@mapNotNull null
            Pair(mappedReport, service)
        }
    }

    fun translate(input: Report, toService: String, defaultValues: DefaultValues = emptyMap()): Pair<Report, OrganizationService>? {
        if (input.isEmpty()) return null
        val service = metadata.findService(toService) ?: error("invalid service name $toService")
        val mappedReport = translateByService(input, service, defaultValues)
        if (mappedReport.itemCount == 0) return null
        return Pair(mappedReport, service)
    }

    private fun translateByService(input: Report, receiver: OrganizationService, defaultValues: DefaultValues): Report {
        // Filter according to receiver patterns
        val filterAndArgs = receiver.jurisdictionalFilter.map { filterSpec ->
            val (fnName, fnArgs) = JurisdictionalFilters.parseJurisdictionalFilter(filterSpec)
            val filterFn = metadata.findJurisdictionalFilter(fnName)
                ?: error("JurisdictionalFilter $fnName is not found")
            Pair(filterFn, fnArgs)
        }
        val filteredReport = input.filter(filterAndArgs)

        // Always succeed in translating an empty report after filtering (even if the mapping process would fail)
        if (filteredReport.isEmpty()) return buildEmptyReport(receiver, input)

        // Apply mapping to change schema
        val toReport: Report = if (receiver.schema != filteredReport.schema.name) {
            val toSchema = metadata.findSchema(receiver.schema)
                ?: error("${receiver.schema} schema is missing from catalog")
            val defaults = if (receiver.defaults.isNotEmpty()) receiver.defaults.plus(defaultValues) else defaultValues
            val mapping = buildMapping(toSchema, filteredReport.schema, defaults)
            if (mapping.missing.isNotEmpty()) {
                error("Error: To translate to ${toSchema.name}, these elements are missing: ${mapping.missing.joinToString(", ")}")
            }
            filteredReport.applyMapping(mapping)
        } else {
            filteredReport
        }

        // Transform reports
        var transformed = toReport
        receiver.transforms.forEach { (transform, transformValue) ->
            when (transform) {
                "deidentify" -> if (transformValue == "true") {
                    transformed = transformed.deidentify()
                }
            }
        }
        return transformed.copy(destination = receiver)
    }

    /**
     * Translate one report to another schema. Translate all items.
     */
    fun translate(input: Report, toSchema: Schema, defaultValues: DefaultValues): Report {
        val mapping = buildMapping(toSchema = toSchema, fromSchema = input.schema, defaultValues)
        return input.applyMapping(mapping = mapping)
    }

    /**
     * Build the mapping that will translate a one schema to another. The mapping
     * can be used for multiple translations.
     */
    fun buildMapping(toSchema: Schema, fromSchema: Schema, defaultValues: DefaultValues): Mapping {
        if (toSchema.topic != fromSchema.topic) error("Trying to match schema with different topics")

        val useDirectly = mutableMapOf<String, String>()
        val useValueSet: MutableMap<String, String> = mutableMapOf()
        val useMapper = mutableMapOf<String, Mapper>()
        val useDefault = mutableMapOf<String, String>()
        val missing = mutableSetOf<String>()

        toSchema.elements.forEach { toElement ->
            var isMissing = toElement.cardinality == Element.Cardinality.ONE
            fromSchema.findElement(toElement.name)?.let { matchedElement ->
                useDirectly[toElement.name] = matchedElement.name
                isMissing = false
            }
            toElement.mapper?.let {
                val name = Mappers.parseMapperField(it).first
                useMapper[toElement.name] = metadata.findMapper(name) ?: error("Mapper $name is not found")
                isMissing = false
            }
            if (toElement.hasDefaultValue(defaultValues)) {
                useDefault[toElement.name] = toElement.defaultValue(defaultValues)
                isMissing = false
            }
            if (isMissing) {
                missing.add(toElement.name)
            }
        }
        return Mapping(toSchema, fromSchema, useDirectly, useValueSet, useMapper, useDefault, missing)
    }

    private fun buildEmptyReport(receiver: OrganizationService, from: Report): Report {
        val toSchema = metadata.findSchema(receiver.schema)
            ?: error("${receiver.schema} schema is missing from catalog")
        return Report(toSchema, emptyList(), listOf(ReportSource(from.id, "mapping")))
    }
}