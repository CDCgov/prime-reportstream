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

    private fun translateByService(input: Report, receiver: OrganizationService, defaultValues: DefaultValues): Report {
        // Filter according to receiver patterns
        val filterAndArgs = receiver.jurisdictionalFilter.map { filterSpec ->
            val (fnName, fnArgs) = JurisdictionalFilters.parseJurisdictionalFilter(filterSpec)
            val filterFn = metadata.findJurisdictionalFilter(fnName)
                ?: error("JurisdictionalFilter $fnName is not found")
            Pair(filterFn, fnArgs)
        }
        val filteredReport = input.filter(filterAndArgs)

        // Apply mapping to change schema
        val toReport: Report = if (receiver.schema != filteredReport.schema.name) {
            val toSchema = metadata.findSchema(receiver.schema)
                ?: error("${receiver.schema} schema is missing from catalog")
            val mapping = buildMapping(toSchema, filteredReport.schema, defaultValues)
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
            fromSchema.findElement(toElement.name)?.let { matchedElement ->
                useDirectly[toElement.name] = matchedElement.name
                return@forEach
            }
            toElement.mapper?.let {
                val name = Mappers.parseMapperField(it).first
                useMapper[toElement.name] = metadata.findMapper(name) ?: error("Mapper $name is not found")
                return@forEach
            }
            if (toElement.required == true) {
                missing.add(toElement.name)
            } else {
                useDefault[toElement.name] = toElement.defaultValue(defaultValues)
            }
        }
        return Mapping(toSchema, fromSchema, useDirectly, useValueSet, useMapper, useDefault, missing)
    }
}