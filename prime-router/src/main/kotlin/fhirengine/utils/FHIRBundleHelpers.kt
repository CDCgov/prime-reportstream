package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.Message
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamConditionFilter
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.azure.ConditionStamper.Companion.BUNDLE_CODE_IDENTIFIER
import gov.cdc.prime.router.azure.ConditionStamper.Companion.BUNDLE_VALUE_IDENTIFIER
import gov.cdc.prime.router.azure.ConditionStamper.Companion.CONDITION_CODE_EXTENSION_URL
import gov.cdc.prime.router.codes
import gov.cdc.prime.router.fhirengine.engine.RSMessageType
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirBundleUtils.deleteResource
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference

/**
 * A collection of helper functions that modify an existing FHIR bundle.
 */

// Constant URLs
const val bundleIdentifierURL = "https://reportstream.cdc.gov/prime-router"
const val conditionExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/reportable-condition"

/**
 * Retrieves loinc/snomed codes from [this] observation in known locations (code.coding and valueCodeableConcept.coding)
 * @return a map of lists of codings keyed by their origin as a printable string
 */
fun Observation.getCodeSourcesMap(): Map<String, List<Coding>> {
    val codeSourcesMap = mutableMapOf<String, List<Coding>>()

    // This guards against the auto create behavior configuration getting changed. As currently, configured if code
    // is null, it will be auto created, but a configuration change would cause this to blow up.
    if (this.code != null) {
        codeSourcesMap[BUNDLE_CODE_IDENTIFIER] = this.code.coding
    }

    if (this.value is CodeableConcept) {
        codeSourcesMap[BUNDLE_VALUE_IDENTIFIER] = this.valueCodeableConcept.coding
    }

    return codeSourcesMap
}

/**
 * Gets mapped condition extensions present on an [Observation]
 */
fun Observation.getMappedConditionExtensions(): List<Extension> = this.getCodeSourcesMap()
        .flatMap { it.value }
        .flatMap { it.extension }
        .filter { it.url == CONDITION_CODE_EXTENSION_URL }

/**
 * Gets mapped conditions present on an [Observation]
 */
fun Observation.getMappedConditions(): List<Coding> =
    this.getMappedConditionExtensions().map { it.castToCoding(it.value) }

/**
 * Gets mapped condition codes present on an [Observation]
 */
fun Observation.getMappedConditionCodes(): List<String> = this.getMappedConditions().map { it.code }

fun Bundle.getObservations() = this.entry.map { it.resource }.filterIsInstance<Observation>()

fun Bundle.getObservationsWithCondition(codes: List<String>): List<Observation> =
    if (codes.isEmpty()) {
        throw IllegalArgumentException("Invalid mapped condition filter")
    } else {
        this.getObservations().filter {
            it.getMappedConditionCodes().any(codes::contains)
        }
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
 * Return true if Bundle contains an ELR in the MessageHeader.
 *
 * @return true if has a MesssageHeader that contains an R01 or ORU_R01, otherwise false.
 */
fun Bundle.isElr(): Boolean {
    val code = FhirPathUtils.evaluate(
        null,
        this,
        this,
        "Bundle.entry.resource.ofType(MessageHeader).event.code"
    )
        .filterIsInstance<CodeType>()
        .firstOrNull()
        ?.code
    return ((code == "R01") || (code == "ORU_R01"))
}

/**
 * Return RSMessageType based on grouping logic.
 *
 * @return RSMessageType of this Bundle.
 */
fun Bundle.getRSMessageType(): RSMessageType = when {
        isElr() -> RSMessageType.LAB_RESULT
        else -> RSMessageType.UNKNOWN
    }

/**
 * Gets all diagnostic report that have no observations from a [bundle]
 *
 * @return a list of [Base] diagnostic reports that have no observations
 */
fun Bundle.getDiagnosticReportNoObservations(): List<Base> = FhirPathUtils.evaluate(
        null,
        this,
        this,
        "Bundle.entry.resource.ofType(DiagnosticReport).where(result.empty())"
    )

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
 * The [fhirBundle] will be used to evaluate whether the observation passes the filter
 *
 * @return is a list of extensions to add to the bundle
 */
internal fun getObservationExtensions(
    fhirBundle: Bundle,
    receiver: Receiver,
): List<Extension> {
    val (observationsToKeep, allObservations) =
        getFilteredObservations(fhirBundle, receiver.conditionFilter)

    val observationExtensionsToKeep = mutableListOf<Extension>()
    if (observationsToKeep.size < allObservations.size) {
        observationsToKeep.forEach {
            observationExtensionsToKeep.add(Extension(conditionExtensionURL, Reference(it.idBase)))
        }
    }
    return observationExtensionsToKeep
}

/**
 * Filter out observations that pass the condition filter for a [receiver]
 * The [bundle] will be used to evaluate whether
 * the observation passes the filter
 *
 * @return copy of the bundle with filtered observations removed
 */
fun Bundle.filterObservations(
    conditionFilter: ReportStreamFilter,
    ): Bundle {
    val (observationsToKeep, allObservations) =
        getFilteredObservations(this, conditionFilter)
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
 * The [bundle] will be used to evaluate whether the observation passes the filter
 *
 * @return a pair containing a list of the filtered ids and copy of the bundle with filtered observations removed
 */
fun Bundle.filterMappedObservations(
    conditionFilter: ReportStreamConditionFilter,
): Pair<List<String>, Bundle> {
    val codes = conditionFilter.codes()
    val observations = this.getObservations()
    val toKeep = observations.filter { it.getMappedConditionCodes().any(codes::contains) }.map { it.idBase }
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
): Pair<List<Base>, List<Base>> {
    val allObservationsExpression = "Bundle.entry.resource.ofType(DiagnosticReport).result.resolve()"
    val allObservations = FhirPathUtils.evaluate(
        CustomContext(fhirBundle, fhirBundle, mutableMapOf(), CustomFhirPathFunctions()),
        fhirBundle,
        fhirBundle,
        allObservationsExpression
    )

    val observationsToKeep = mutableListOf<Base>()
    allObservations.forEach { observation ->
        val passes = conditionFilter.any { conditionFilter ->
            FhirPathUtils.evaluateCondition(
                CustomContext(fhirBundle, observation, mutableMapOf(), CustomFhirPathFunctions()),
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
    this.identifier.value = when (val mshSegment = hl7Message["MSH"]) {
        is ca.uhn.hl7v2.model.v27.segment.MSH -> mshSegment.messageControlID.value
        is ca.uhn.hl7v2.model.v251.segment.MSH -> mshSegment.messageControlID.value
        else -> ""
    }
    this.identifier.system = bundleIdentifierURL
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
    }
}