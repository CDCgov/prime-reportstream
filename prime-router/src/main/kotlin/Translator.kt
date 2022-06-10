package gov.cdc.prime.router

import gov.cdc.prime.router.metadata.Mapper
import gov.cdc.prime.router.metadata.Mappers
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
        val missing: Set<String>
    )

    data class RoutedReport(
        val report: Report,
        val receiver: Receiver,
    )

    data class RoutedReportsResult(
        val reports: List<RoutedReport>,
        val details: List<ActionLog>,
    )

    /**
     * Translate and filter by the list of receiver in metadata. Only return reports that have items.
     */
    fun filterAndTranslateByReceiver(
        input: Report,
        defaultValues: DefaultValues = emptyMap(),
        limitReceiversTo: List<String> = emptyList(),
    ): RoutedReportsResult {
        val warnings = mutableListOf<ActionLog>()
        if (input.isEmpty()) return RoutedReportsResult(emptyList(), warnings)
        val routedReports = settings.receivers.filter { receiver ->
            receiver.topic == input.schema.topic &&
                (limitReceiversTo.isEmpty() || limitReceiversTo.contains(receiver.fullName))
        }.mapNotNull { receiver ->
            try {
                // Filter the report
                val filteredReport = filterByAllFilterTypes(settings, input, receiver) ?: return@mapNotNull null
                if (filteredReport.isEmpty()) return@mapNotNull RoutedReport(filteredReport, receiver)

                // Translate the filteredReport
                val translatedReport = translateByReceiver(filteredReport, receiver, defaultValues)
                RoutedReport(translatedReport, receiver)
            } catch (e: IllegalStateException) {
                // catching individual translation exceptions enables overall work to continue
                warnings.add(
                    ActionLog(
                        InvalidTranslationMessage(e.localizedMessage),
                        "TO:${receiver.fullName}:${receiver.schemaName}",
                        reportId = input.id,
                    )
                )
                return@mapNotNull null
            }
        }
        return RoutedReportsResult(routedReports, warnings)
    }

    /**
     * Determine which data in [input] should be sent to the [receiver] based on that receiver's set of filters.
     * Apply all the different filter types (at this writing, jurisdictionalFilter, qualityFilter, routingFilter).
     *
     * @return the filtered Report. Returns null if jurisdictionalFilter had no matches, which is quite common,
     * since most geographic locations don't match, and we don't need to log this.   Returns empty report if any
     * of the later filters removed everything, but even when empty, this report has useful [ReportStreamFilterResult]
     * to be logged.
     */
    fun filterByAllFilterTypes(settings: SettingsProvider, input: Report, receiver: Receiver): Report? {
        val organization = settings.findOrganization(receiver.organizationName)
            ?: error("No org for ${receiver.fullName}")

        // This has to be the trackingElement of the incoming data, not the outgoing receiver.
        var trackingElement = input.schema.trackingElement // might be null
        if (!trackingElement.isNullOrBlank() && !input.schema.containsElement(trackingElement)) {
            // I've seen cases where the trackingElement is not in the schema(!!) (see az/az-covid-19-csv)
            // Nulling this out to avoid exceptions later.
            trackingElement = null
        }

        // Do jurisdictionalFiltering on the input
        val jurisFilteredReport = filterByOneFilterType(
            input,
            receiver,
            organization,
            ReportStreamFilterType.JURISDICTIONAL_FILTER,
            trackingElement,
            doLogging = false,
        )
        // vast majority of receivers will return here, which speeds subsequent filters.
        // ok to just return null, we don't need any info about what was eliminated.
        if (jurisFilteredReport.isEmpty()) return null

        // keep track of how many items passed the juris filter prior to quality filtering.
        // For informational/reporting purposes only.
        // Normally this value is passed from parent to child Report, like mitochondrial DNA.  This overrides that.
        jurisFilteredReport.itemCountBeforeQualFilter = jurisFilteredReport.itemCount

        // Do qualityFiltering on the jurisFilteredReport
        val qualityFilteredReport = filterByOneFilterType(
            jurisFilteredReport,
            receiver,
            organization,
            ReportStreamFilterType.QUALITY_FILTER,
            trackingElement,
            doLogging = !receiver.reverseTheQualityFilter
        )
        if (qualityFilteredReport.isEmpty()) return qualityFilteredReport

        // Do routingFiltering on the qualityFilteredReport
        val routingFilteredReport = filterByOneFilterType(
            qualityFilteredReport,
            receiver,
            organization,
            ReportStreamFilterType.ROUTING_FILTER,
            trackingElement,
            doLogging = true // quality and routing info will go together into the report's filteredItems
        )
        if (routingFilteredReport.isEmpty()) return routingFilteredReport

        // Do processingModeFiltering on the routingFilteredReport
        val processingModeFilteredReport = filterByOneFilterType(
            routingFilteredReport,
            receiver,
            organization,
            ReportStreamFilterType.PROCESSING_MODE_FILTER,
            trackingElement,
            doLogging = true
        )
        if (processingModeFilteredReport.isEmpty()) return processingModeFilteredReport

        return processingModeFilteredReport
    }

    /**
     * Apply a set of ReportStreamFilters associated with a [filterType] to report [input]. eg, Apply one of:
     * jurisdictionalFilter, qualityFilter, and routingFilter.
     *
     * Filter usages can be defined at three different levels:  default, organization-level, and receiver-level.
     * The [receiver] has only one topic (eg, 'covid-19'), but its [organization] can handle many topics, so
     * we must look up default- and organization-level filters per topic.
     * Any/all of the three levels are allowed to be null.  If all are null, we do no filtering for this filterType.
     *
     * @return the filtered report.   Might be empty.  Might be unchanged if no filtering was done.
     */
    fun filterByOneFilterType(
        input: Report,
        receiver: Receiver,
        organization: Organization,
        filterType: ReportStreamFilterType,
        trackingElement: String?,
        doLogging: Boolean,
    ): Report {
        // First, retrieve the default filter for this topic and filterType
        val defaultFilters = ReportStreamFilters.defaultFiltersByTopic[receiver.topic]
        val defaultFilter = if (defaultFilters != null)
            filterType.filterProperty.get(defaultFilters)
        else
            null

        // Next, retrieve the organization-level filter for this topic and filterType
        val orgFilters = organization.filters?.find { it.topic == receiver.topic }
        var orgFilter = if (orgFilters != null)
            filterType.filterProperty.get(orgFilters)
        else
            null
        if (orgFilter.isNullOrEmpty()) orgFilter = null // force null to avoid empty strings

        // Last, retrieve the receiver-level filter for this filter type.
        var receiverFilter: ReportStreamFilter? = filterType.receiverFilterProperty.get(receiver)
        if (receiverFilter.isNullOrEmpty()) receiverFilter = null // force null to be consistent with org and default.

        // Use the "and" of the  org filter and receiver filter if either or both exists - and override the default.
        // Otherwise use the default.
        val filterToApply: ReportStreamFilter = when {
            (orgFilter != null && receiverFilter != null) -> orgFilter + receiverFilter
            (orgFilter == null && receiverFilter != null) -> receiverFilter
            (orgFilter != null && receiverFilter == null) -> orgFilter
            (defaultFilter != null) -> defaultFilter
            else -> {
                // Probably an error if there's no defaultFilter
                logger.error("NOT ${filterType.name} filtering for topic ${receiver.topic}. No filters found.")
                emptyList()
            }
        }

        // Warn if this receiver/org does not have a jurisdictionalFilter that overrides the default 'allowNone()'.
        // This may be intentional, or may be an oversight.
        if (filterType == ReportStreamFilterType.JURISDICTIONAL_FILTER &&
            filterToApply.contains(AllowNone().name)
        ) {
            logger.warn(
                "Possible error: jurisdictionalFilter ${AllowNone().name} is eliminating ALL data " +
                    "for receiver ${receiver.fullName} in report ${input.id}, schema ${input.schema.name}"
            )
        }

        // This weird obj is of type List<Pair<ReportStreamFilterDef, List<String>>>
        val filterAndArgs = filterToApply.map { filterSpec ->
            val (fnName, fnArgs) = ReportStreamFilterDefinition.parseReportStreamFilter(filterSpec)
            val filterFn = metadata.findReportStreamFilterDefinitions(fnName)
                ?: error("Cannot find ReportStreamFilter Definition for $fnName")
            Pair(filterFn, fnArgs)
        }

        val filteredReport = input.filter(
            filterAndArgs,
            receiver,
            doLogging,
            trackingElement,
            // the reverseTheQualityFilter flag only applies for qualityFilters
            if (filterType == ReportStreamFilterType.QUALITY_FILTER) receiver.reverseTheQualityFilter else false,
            filterType
        )
        if (doLogging && filteredReport.itemCount != input.itemCount) {
            logger.info(
                "Filtering occurred in report ${input.id}, receiver ${receiver.fullName}: " +
                    "There were ${input.itemCount} rows prior to ${filterType.name}, and " +
                    "${filteredReport.itemCount} rows after ${filterType.name}."
            )
        }
        return filteredReport
    }

    /**
     * Filter a [input] report for a [receiver] by that receiver's qualityFilter and
     * then translate the filtered report based on the receiver's schema.
     */
    fun translateByReceiver(
        input: Report,
        receiver: Receiver,
        defaultValues: DefaultValues = emptyMap()
    ): Report {
        // Apply mapping to change schema
        val toReport: Report = if (receiver.schemaName != input.schema.name) {
            val toSchema = metadata.findSchema(receiver.schemaName)
                ?: error("${receiver.schemaName} schema is missing from catalog")
            val receiverDefaults = receiver.translation.defaults
            val defaults = if (receiverDefaults.isNotEmpty()) receiverDefaults.plus(defaultValues) else defaultValues
            val mapping = buildMapping(toSchema, input.schema, defaults)
            if (mapping.missing.isNotEmpty()) {
                error(
                    "Error: To translate to ${receiver.fullName}, ${toSchema.name}, these elements are missing: ${
                    mapping.missing.joinToString(
                        ", "
                    )
                    }"
                )
            }
            input.applyMapping(mapping)
        } else {
            input
        }

        // Transform reports
        var transformed = toReport
        if (receiver.deidentify)
            transformed = transformed.deidentify(receiver.deidentifiedValue)
        val copy = transformed.copy(destination = receiver, bodyFormat = receiver.format)
        copy.filteringResults.addAll(input.filteringResults)
        return copy
    }

    /**
     * Translate one report to another schema. Translate all items.
     * @todo get rid of this - not used?
     */
    fun translate(input: Report, toSchema: Schema, defaultValues: DefaultValues = emptyMap()): Report {
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
    fun buildMapping(
        toSchema: Schema,
        fromSchema: Schema,
        defaultValues: DefaultValues
    ): Mapping {
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