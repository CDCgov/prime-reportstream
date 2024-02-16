package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.Message
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamConditionFilter
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.UnmappableConditionMessage
import gov.cdc.prime.router.cli.ObservationMappingConstants
import gov.cdc.prime.router.codes
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.Companion.getChildProperties
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * A collection of helper functions that modify an existing FHIR bundle.
 */
const val conditionExtensionurl = "https://reportstream.cdc.gov/fhir/StructureDefinition/reportable-condition"
const val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"

/**
 * Looks up a condition code for the passed in [code] (typically a test) using the given [metadata] object
 * @param code test (or other type) code to look up a condition for
 * @param metadata metadata containing an observation-mapping lookup table
 * @return the condition code or null if no code was found
 */
private fun lookupCondition(code: Coding, metadata: Metadata): Coding? {
    val mappingTable = metadata.findLookupTable("observation-mapping").also {
        if (it == null) { // could not load the table
            throw IllegalStateException("Unable to load lookup table 'observation-mapping' for condition stamping")
        }
    }!!
    val condition = mappingTable.caseSensitiveDataRowsMap.find { // search for the code
        it[ObservationMappingConstants.TEST_CODE_KEY] == code.code
    }
    return if (condition.isNullOrEmpty()) { // could not find the code
        null
    } else { // code found; create Coding instance to return
        Coding(
            condition[ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY],
            condition[ObservationMappingConstants.CONDITION_CODE_KEY],
            condition[ObservationMappingConstants.CONDITION_NAME_KEY]
        )
    }
}

/**
 * Retrieves loinc/snomed codes from [this] observation in known locations (code.coding and valueCodeableConcept.coding)
 * @return a map of lists of codings keyed by their origin as a printable string
 */
fun Observation.getCodeSourcesMap(): Map<String, List<Coding>> {
    val codeSourcesMap = mutableMapOf<String, List<Coding>>()

    // This guards against the auto create behavior configuration getting changed. As currently, configured if code
    // is null, it will be auto created, but a configuration change would cause this to blow up.
    if (this.code != null) {
        codeSourcesMap[ObservationMappingConstants.BUNDLE_CODE_IDENTIFIER] = this.code.coding
    }

    if (this.value is CodeableConcept) {
        codeSourcesMap[ObservationMappingConstants.BUNDLE_VALUE_IDENTIFIER] = this.valueCodeableConcept.coding
    }

    return codeSourcesMap
}

/**
 * For every snomed/loinc code in code or valueCodeableConcept, lookup a condition code and add it as an extension
 * @param metadata metadata containing an observation-mapping lookup table
 * @return a list of ActionLogDetail objects with information on any mapping failures
 */
fun Observation.addMappedCondition(metadata: Metadata): List<ActionLogDetail> {
    val codeSourcesMap = this.getCodeSourcesMap().filterValues { it.isNotEmpty() }
    var mappedSomething = false
    if (codeSourcesMap.values.flatten().isEmpty()) return listOf(UnmappableConditionMessage()) // no codes found

    return codeSourcesMap.mapNotNull { codeSourceEntry ->
        codeSourceEntry.value.mapNotNull { code ->
            lookupCondition(code, metadata).let { conditionCode ->
                if (conditionCode == null) { // no code found, track this unmapped code
                    code.code
                } else { // code found, add extension and return null to avoid mapping this as an error
                    code.addExtension(conditionCodeExtensionURL, conditionCode)
                    mappedSomething = true
                    null
                }
            }
        }.let {
            // create log message for any unmapped codes
            if (it.isEmpty() || mappedSomething) null else UnmappableConditionMessage(it, codeSourceEntry.key)
        }
    }
}

fun Observation.getMappedConditions(): List<String> =
    this.getCodeSourcesMap().mapNotNull {
        it.value.flatMap { coding ->
            coding.extension.mapNotNull { extension ->
                if (extension.url == conditionCodeExtensionURL) extension.castToCoding(extension.value).code else null
            }
        }
    }.flatten()

fun Bundle.getObservations() = this.entry.map { it.resource }.filterIsInstance<Observation>()

fun Bundle.getObservationsWithCondition(codes: List<String>): List<Observation> =
    if (codes.isEmpty()) {
        // TODO: consider throwing IllegalArgumentException here while implementing
        //  https://github.com/CDCgov/prime-reportstream/issues/12705
        emptyList()
    } else {
        this.getObservations().filter {
            it.getMappedConditions().any(codes::contains)
        }
    }

/**
 * This will return all mapped conditions in a bundle (no duplicates)
 */
fun Bundle.getAllMappedConditions(): Set<String> {
    return this.getObservations()
        .flatMap { it.getMappedConditions() }
        .toSet()
}

/**
 * Adds references to diagnostic reports within [fhirBundle] as provenance targets
 */
fun Bundle.addProvenanceReference() {
    val provenanceResource = try {
        this.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
    } catch (e: NoSuchElementException) {
        this.addEntry().resource = Provenance()
        this.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
    }

    // Create the list of diagnostic reports to be added to the Provenance of the bundle
    val diagnosticReportList = FhirPathUtils.evaluate(
        null,
        this,
        this,
        "Bundle.entry.resource.ofType(DiagnosticReport)"
    )

    diagnosticReportList.forEach { diagnosticReport ->
        val diagnosticReportReference = Reference(diagnosticReport.idBase)
        diagnosticReportReference.reference = diagnosticReport.idBase
        provenanceResource.target.add(diagnosticReportReference)
    }
}

/**
 * Gets all properties for a [Base] resource recursively and filters only its references
 *
 * @return a list of reference identifiers for a [Base] resource
 *
 */
fun Base.getResourceReferences(): List<String> {
    return FHIRBundleHelpers.filterReferenceProperties(this.getResourceProperties())
}

/**
 * Gets all properties for a [Base] resource recursively
 *
 * @return a list of all [Property] for a [Base] resource
 */
fun Base.getResourceProperties(): List<Property> {
    return this.children().stream().flatMap { getChildProperties(it) }.collect(Collectors.toList())
}

/**
 * Gets all diagnostic report that have no observations from a [bundle]
 *
 * @return a list of [Base] diagnostic reports that have no observations
 */
fun Bundle.getDiagnosticReportNoObservations(): List<Base> {
    return FhirPathUtils.evaluate(
        null,
        this,
        this,
        "Bundle.entry.resource.ofType(DiagnosticReport).where(result.empty())"
    )
}

/**
 * Deletes a [resource] from a bundle, removes all references to the [resource] and any orphaned children.
 * If the [resource] being deleted is an [Observation] and that results in diagnostic reports having no
 * observations, the [DiagnosticReport] will be deleted
 */
fun Bundle.deleteResource(resource: Base) {
    val referencesToClean = mutableSetOf<String>()

    // build up all resources and their references in a map as a starting point
    fun generateAllReferencesMap() = this.entry.associate {
        it.fullUrl to it.getResourceReferences()
    }

    // recursive function to delete resource and orphaned children
    fun deleteResourceInternal(
        resourceInternal: Base,
        referencesMap: Map<String, List<String>> = generateAllReferencesMap(),
    ) {
        if (this.entry.find { it.fullUrl == resourceInternal.idBase } == null) {
            throw IllegalStateException("Cannot delete resource. FHIR bundle does not contain this resource")
        }

        // First remove the resource from the bundle
        this.entry.removeIf { it.fullUrl == resourceInternal.idBase }

        // add resource to set of references to clean up after recursion
        referencesToClean.add(resourceInternal.idBase)

        // Get the resource children references
        val resourceChildren = resourceInternal.getResourceReferences()

        // get all resources except the resource being removed and stick it in a map keyed off the fullUrl
        val allResources = this.entry.associateBy { it.fullUrl }

        // get all references for every remaining resource
        val remainingReferences = referencesMap - resourceInternal.idBase
        val flatRemainingReferences = remainingReferences.flatMap { it.value }.toSet()

        // remove orphaned children
        resourceChildren.forEach { child ->
            if (!flatRemainingReferences.contains(child)) {
                allResources[child]?.let { entryToDelete ->
                    deleteResourceInternal(entryToDelete.resource, remainingReferences)
                }
            }
        }
    }

    // Go through every resource and check if the resource has a reference to the resource being deleted
    fun cleanUpReferences() {
        this.entry
            .map { it.resource }
            .forEach { res ->
                res.children().forEach { child ->
                    child
                        .values
                        .filterIsInstance<Reference>()
                        .filter { referencesToClean.contains(it.reference) }
                        .forEach { it.reference = null }
                }
            }

        referencesToClean.clear()
    }

    // find diagnostic reports without any observations contained in the result field and delete
    fun cleanUpEmptyDiagnosticReports() {
        val diagnosticReportsToDelete = this.entry
            .map { it.resource }
            .filterIsInstance<DiagnosticReport>()
            .filter { it.result.none { it.reference != null } }

        diagnosticReportsToDelete.forEach { deleteResourceInternal(it) }
    }

    // delete provided resource and all references to it
    deleteResourceInternal(resource)
    cleanUpReferences()

    // clean up empty Diagnostic Reports and references to them
    cleanUpEmptyDiagnosticReports()
    cleanUpReferences()
}

/**
 *  Removes PHI data from a [Bundle]
 */
fun Bundle.removePHI() {
    /**
     *  The covid-19.schema file lists which fields contain PII.
     *  This function removes data for the fields listed there.
     */
    this.entry.map { it.resource }.filterIsInstance<Patient>()
        .forEach { patient ->
            patient.name = null
            patient.address.forEach {
                it.line = null
                it.city = null
            }
            patient.telecom = null
            patient.birthDate = null
            patient.deceased = null
            patient.identifier = null
            patient.contact = null
        }
}

/**
 * Deletes resources that have no children after deleting a [resource]
 *
 */
internal fun Bundle.deleteChildlessResource(resource: Base) {
    // if resource being removed is an observation
    // check if there are any diagnostic reports with no observations and remove them
    when (resource) {
        is Observation -> this.getDiagnosticReportNoObservations().forEach { this.deleteResource(it) }
    }
}

/**
 * Gets the observation extensions for those observations that pass the condition filter for a [receiver]
 * The [fhirBundle] and [shortHandLookupTable] will be used to evaluate whether the observation passes the filter
 *
 * @return is a list of extensions to add to the bundle
 */
internal fun getObservationExtensions(
    fhirBundle: Bundle,
    receiver: Receiver,
    shortHandLookupTable: MutableMap<String, String>,
): List<Extension> {
    val (observationsToKeep, allObservations) =
        getFilteredObservations(fhirBundle, receiver.conditionFilter, shortHandLookupTable)

    val observationExtensionsToKeep = mutableListOf<Extension>()
    if (observationsToKeep.size < allObservations.size) {
        observationsToKeep.forEach {
            observationExtensionsToKeep.add(
                Extension(
                    conditionExtensionurl,
                    Reference(it.idBase)
                )
            )
        }
    }
    return observationExtensionsToKeep
}

/**
 * Filter out observations that pass the condition filter for a [receiver]
 * The [bundle] and [shortHandLookupTable] will be used to evaluate whether
 * the observation passes the filter
 *
 * @return copy of the bundle with filtered observations removed
 */
fun Bundle.filterObservations(
    conditionFilter: ReportStreamFilter,
    shortHandLookupTable: MutableMap<String, String>,
): Bundle {
    val (observationsToKeep, allObservations) =
        getFilteredObservations(this, conditionFilter, shortHandLookupTable)
    val filteredBundle = this.copy()
    val listToKeep = observationsToKeep.map { it.idBase }
    allObservations.forEach {
        if (it.idBase !in listToKeep) {
            filteredBundle.deleteResource(it)
        }
    }
    return filteredBundle
}

/**
 * Filter out observations that pass the condition filter for a [receiver]
 * The [bundle] and [shortHandLookupTable] will be used to evaluate whether
 * the observation passes the filter
 *
 * @return a pair containing a list of the filtered ids and copy of the bundle with filtered observations removed
 */
fun Bundle.filterMappedObservations(
    conditionFilter: ReportStreamConditionFilter,
): Pair<List<String>, Bundle> {
    val codes = conditionFilter.codes()
    val observations = this.getObservations()
    val toKeep = observations.filter { it.getMappedConditions().any(codes::contains) }.map { it.idBase }
    val filteredBundle = this.copy()
    val filteredIds = observations.mapNotNull {
        val idBase = it.idBase
        if (idBase !in toKeep) {
            filteredBundle.deleteResource(it)
            idBase
        } else {
            null
        }
    }
    return Pair(filteredIds, filteredBundle)
}

private fun getFilteredObservations(
    fhirBundle: Bundle,
    conditionFilter: ReportStreamFilter,
    shortHandLookupTable: MutableMap<String, String>,
): Pair<List<Base>, List<Base>> {
    val allObservationsExpression = "Bundle.entry.resource.ofType(DiagnosticReport).result.resolve()"
    val allObservations = FhirPathUtils.evaluate(
        CustomContext(fhirBundle, fhirBundle, shortHandLookupTable, CustomFhirPathFunctions()),
        fhirBundle,
        fhirBundle,
        allObservationsExpression
    )

    val observationsToKeep = mutableListOf<Base>()
    allObservations.forEach { observation ->
        val passes = conditionFilter.any { conditionFilter ->
            FhirPathUtils.evaluateCondition(
                CustomContext(fhirBundle, observation, shortHandLookupTable, CustomFhirPathFunctions()),
                observation,
                fhirBundle,
                fhirBundle,
                conditionFilter
            )
        }

        if (passes) {
            observationsToKeep.add(observation)
        }
    }

    return Pair(observationsToKeep, allObservations)
}

/**
 * Enhance the [bundle] metadata with data from an [hl7Message].  This is not part of the library configuration.
 */
fun Bundle.enhanceBundleMetadata(hl7Message: Message) {
    // For bundles of type MESSAGE the timestamp is the time the HL7 was generated.
    this.timestamp = HL7Reader.getMessageTimestamp(hl7Message)

    // The HL7 message ID
    val identifierValue = when (val mshSegment = hl7Message["MSH"]) {
        is ca.uhn.hl7v2.model.v27.segment.MSH -> mshSegment.messageControlID.value
        is ca.uhn.hl7v2.model.v251.segment.MSH -> mshSegment.messageControlID.value
        else -> ""
    }
    this.identifier.value = identifierValue
    this.identifier.system = "https://reportstream.cdc.gov/prime-router"
}

/**
 *  As documented in https://docs.google.com/spreadsheets/d/1_MOAJOykRWct_9cBG-EcPcWSpSObQFLboPB579DIoAI/edit#gid=0,
 *  the birthDate value needs an extension with a valueDateTime if PID.7 length is greater than 8. According to the
 *  fhir documentation https://hl7.org/fhir/json.html#primitive, if a value has an id attribute or extension,
 *  it is represented with an underscore before the name. Currently, it seems hl7v2-fhir-converter library does not
 *  support this, so this method is a workaround to add an extension to birthDate. There is also no support for
 *  getting the length of the field, for which this issue was created:
 *  https://github.com/LinuxForHealth/hl7v2-fhir-converter/issues/499
 *  This method looks in the [hl7Message] for the birthdate and add an extension to the [bundle] if it includes
 *  the time
 */

fun Bundle.handleBirthTime(hl7Message: Message) {
    // If it is an ORM message, we want to check if it is a timestamp and add it as an extension if it is.

    val birthTime = HL7Reader.getBirthTime(hl7Message)
    if (birthTime.length > 8) {
        val patient = try {
            this.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
        } catch (e: NoSuchElementException) {
            this.addEntry().resource = Patient()
            this.entry.first { it.resource.resourceType.name == "Patient" }.resource as Patient
        }

        val extension = Extension(
            "http://hl7.org/fhir/StructureDefinition/patient-birthTime",
            DateTimeType(Hl7RelatedGeneralUtils.dateTimeWithZoneId(birthTime, ""))
        )

        patient.birthDateElement.addExtension(extension)
    }
}

/*
 * This class is not necessary from a functional perspective, although removing it seems to result in out of memory
 * issues when running unit tests. It is harmless to keep this, but if the root cause of the out of memory issues can be
 * addressed, then this can be removed, and its functions can become top level functions like the rest of this file.
 */
class FHIRBundleHelpers {
    companion object {
        /**
         * Returns the given [messages] in fhir+ndjson format, which is generated by putting a new line between each message
         *
         * Each entry in [messages] is assumed to be a single-line string representing a fhir bundle
         */
        fun batchMessages(messages: List<String>): String {
            var result = ""
            messages.forEach {
                result += if (result.isEmpty()) {
                    it
                } else {
                    "\n$it"
                }
            }
            return result
        }

        /**
         * Filters the [properties] by only properties that have a value and are of type [Reference]
         *
         * @return a list containing only the references in [properties]
         */
        fun filterReferenceProperties(properties: List<Property>): List<String> {
            return properties
                .filter { it.hasValues() }
                .flatMap { it.values }
                .filterIsInstance<Reference>()
                .map { it.reference }
        }

        /**
         * Gets all child properties for a resource [property] recursively
         *
         * @return a flatmap stream of all child properties on a [property]
         */
        fun getChildProperties(property: Property): Stream<Property> {
            return Stream.concat(
                Stream.of(property),
                property.values.flatMap { it.children() }.stream().flatMap { getChildProperties(it) }
            )
        }
    }
}