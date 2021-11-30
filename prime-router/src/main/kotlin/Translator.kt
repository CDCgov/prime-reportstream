package gov.cdc.prime.router

import org.apache.logging.log4j.kotlin.Logging

/**
 * The translator converts reports from one schema to another.
 * The object can be reused for multiple translations.
 *
 * Dev Note: This glue code was originally in the Report class and then
 * in the Metadata class.
 */
class Translator(private val metadata: Metadata, private val settings: SettingsProvider) : Logging {
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
     * Translate and filter by the list of receiver in metadata. Only return reports that have items.
     */
    fun filterAndTranslateByReceiver(
        input: Report,
        defaultValues: DefaultValues = emptyMap(),
        limitReceiversTo: List<String> = emptyList(),
        warnings: MutableList<ResultDetail>? = null,
    ): List<Pair<Report, Receiver>> {
        if (input.isEmpty()) return emptyList()
        return settings.receivers.filter { receiver ->
            receiver.topic == input.schema.topic &&
                (limitReceiversTo.isEmpty() || limitReceiversTo.contains(receiver.fullName))
        }.mapNotNull { receiver ->
            try {
                val jurisFilteredReport = filterByJurisdiction(input, receiver)
                if (jurisFilteredReport.isEmpty()) return@mapNotNull null
                val mappedReport = translateByReceiver(jurisFilteredReport, receiver, defaultValues)
                Pair(mappedReport, receiver)
            } catch (e: IllegalStateException) {
                // catching individual translation exceptions enables overall work to continue
                warnings?.let {
                    warnings.add(
                        ResultDetail(
                            ResultDetail.DetailScope.TRANSLATION,
                            "TO:${receiver.fullName}:${receiver.schemaName}",
                            InvalidTranslationMessage.new(e.localizedMessage)
                        )
                    )
                }
                return@mapNotNull null
            }
        }
    }

    /**
     * Determine if a report should be sent to the reciever based on that receiver's
     * jurisdiction filter.
     */
    private fun filterByJurisdiction(input: Report, receiver: Receiver): Report {
        // Filter according to this receiver's desired JurisdictionalFilter patterns
        val jurisFilterAndArgs = receiver.jurisdictionalFilter.map { filterSpec ->
            val (fnName, fnArgs) = JurisdictionalFilters.parseJurisdictionalFilter(filterSpec)
            val filterFn = metadata.findJurisdictionalFilter(fnName)
                ?: error("JurisdictionalFilter $fnName is not found")
            Pair(filterFn, fnArgs)
        }
        val jurisFilteredReport = input.filter(jurisFilterAndArgs, receiver, isQualityFilter = false)

        return jurisFilteredReport
    }

    /**
     * Filter a report for a reciever by that receiver's qualityFilter
     * then translate the filtered report based on the receiver's schema.
     */
    private fun translateByReceiver(input: Report, receiver: Receiver, defaultValues: DefaultValues): Report {

        // Now filter according to this receiver's desired qualityFilter, or default filter if none found.
        val qualityFilter = when {
            receiver.qualityFilter.isNotEmpty() -> receiver.qualityFilter
            JurisdictionalFilters.defaultQualityFilters[receiver.topic] != null ->
                JurisdictionalFilters.defaultQualityFilters[receiver.topic]!!
            else -> {
                logger.info("No default qualityFilter found for topic ${receiver.topic}. Not doing qual filtering")
                emptyList<String>()
            }
        }
        val qualityFilterAndArgs = qualityFilter.map { filterSpec ->
            val (fnName, fnArgs) = JurisdictionalFilters.parseJurisdictionalFilter(filterSpec)
            val filterFn = metadata.findJurisdictionalFilter(fnName)
                ?: error("qualityFilter $fnName is not found in list of JurisdictionalFilters")
            Pair(filterFn, fnArgs)
        }
        val qualityFilteredReport = input.filter(
            qualityFilterAndArgs,
            receiver,
            isQualityFilter = true,
            receiver.reverseTheQualityFilter
        )
        if (qualityFilteredReport.itemCount != input.itemCount) {
            logger.warn(
                "Data quality problem in report ${input.id}, receiver ${receiver.fullName}: " +
                    "There were ${input.itemCount} rows prior to qualityFilter, and " +
                    "${qualityFilteredReport.itemCount} rows after qualityFilter."
            )
        }

        if (qualityFilteredReport.isEmpty()) return qualityFilteredReport

        // Apply mapping to change schema
        val toReport: Report = if (receiver.schemaName != qualityFilteredReport.schema.name) {
            val toSchema = metadata.findSchema(receiver.schemaName)
                ?: error("${receiver.schemaName} schema is missing from catalog")
            val receiverDefaults = receiver.translation.defaults
            val defaults = if (receiverDefaults.isNotEmpty()) receiverDefaults.plus(defaultValues) else defaultValues
            val mapping = buildMapping(toSchema, qualityFilteredReport.schema, defaults)
            if (mapping.missing.isNotEmpty()) {
                error(
                    "Error: To translate to ${receiver.fullName}, ${toSchema.name}, these elements are missing: ${
                    mapping.missing.joinToString(
                        ", "
                    )
                    }"
                )
            }
            qualityFilteredReport.applyMapping(mapping)
        } else {
            qualityFilteredReport
        }

        // Transform reports
        var transformed = toReport
        if (receiver.deidentify)
            transformed = transformed.deidentify()
        var copy = transformed.copy(destination = receiver, bodyFormat = receiver.format)
        copy.filteredItems.addAll(qualityFilteredReport.filteredItems)
        return copy
    }

    /**
     * Translate one report to another schema. Translate all items.
     */
    fun translate(input: Report, toSchema: Schema, defaultValues: DefaultValues): Report {
        val mapping = buildMapping(toSchema = toSchema, fromSchema = input.schema, defaultValues)
        return input.applyMapping(mapping = mapping)
    }

    fun translate(
        input: Report,
        toReceiver: String,
        defaultValues: DefaultValues = emptyMap()
    ): Pair<Report, Receiver>? {
        if (input.isEmpty()) return null
        val receiver = settings.findReceiver(toReceiver) ?: error("invalid receiver name $toReceiver")
        val mappedReport = translateByReceiver(input, receiver, defaultValues)
        if (mappedReport.itemCount == 0) return null
        return Pair(mappedReport, receiver)
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
}