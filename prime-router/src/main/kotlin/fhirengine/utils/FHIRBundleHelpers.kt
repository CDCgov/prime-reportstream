package gov.cdc.prime.router.fhirengine.utils

import ca.uhn.hl7v2.model.Message
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilter
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FHIRBundleHelpers.Companion.getChildProperties
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * A collection of helper functions that modify an existing FHIR bundle.
 */
const val conditionExtensionurl = "https://reportstream.cdc.gov/fhir/StructureDefinition/reportable-condition"

/**
 * Adds [receiverList] to the [fhirBundle] as targets using the [shortHandLookupTable] to evaluate conditions
 * to determine which observation extensions to add to each receiver.
 */
fun Bundle.addReceivers(
    receiverList: List<Receiver>,
    shortHandLookupTable: MutableMap<String, String>
) {
    val provenanceResource = try {
        this.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
    } catch (e: NoSuchElementException) {
        this.addProvenanceReference()
        this.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
    }

    // Create the list of target receivers to be added to the Provenance of the bundle
    val targetList = mutableListOf<Reference>()

    // check all active customers for receiver data
    receiverList.filter { it.customerStatus != CustomerStatus.INACTIVE }.forEach { receiver ->
        val endpoint = Endpoint()
        getObservationExtensions(this, receiver, shortHandLookupTable).forEach { endpoint.addExtension(it) }

        endpoint.id = Hl7RelatedGeneralUtils.generateResourceId()
        endpoint.name = receiver.displayName
        when (receiver.customerStatus) {
            CustomerStatus.TESTING -> endpoint.status = Endpoint.EndpointStatus.TEST
            else -> endpoint.status = Endpoint.EndpointStatus.ACTIVE
        }
        val rsIdentifier = Identifier()
        rsIdentifier.value = receiver.fullName
        rsIdentifier.system = "https://reportstream.cdc.gov/prime-router"
        endpoint.identifier.add(rsIdentifier)
        val entry = this.addEntry()
            .setFullUrl("${endpoint.fhirType()}/${endpoint.id}")
            .setResource(endpoint)

        val reference = Reference()
        reference.reference = entry.fullUrl
        reference.resource = endpoint
        targetList.add(reference)
    }

    // Clear out any existing endpoints if they exist
    provenanceResource.target.map { it.resource }.filterIsInstance<Endpoint>()
        .forEach { this.deleteResource(it) }

    if (targetList.isNotEmpty()) provenanceResource.target.addAll(targetList)
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
 *
 */
fun Bundle.deleteResource(resource: Base) {

    if (this.entry.find { it.fullUrl == resource.idBase } == null) {
        throw IllegalStateException("Cannot delete resource. FHIR bundle does not contain this resource")
    }
    // First remove the resource from the bundle
    this.entry.removeIf { it.fullUrl == resource.idBase }

    // Get the resource children references
    val resourceChildren = resource.getResourceReferences()
    // get all resources except the resource being removed
    val allResources = this.entry
    // get all references for every resource
    val allReferences = allResources.flatMap { it.getResourceReferences() }

    // remove orphaned children
    resourceChildren.forEach { child ->
        if (!allReferences.contains(child)) {
            allResources.firstOrNull { it.fullUrl == child }?.let { this.deleteResource(it.resource) }
        }
    }

    // Go through every resource and check if the resource has a reference to the resource being deleted
    // if there is remove the reference
    allResources.forEach { res ->
        res.resource::class.memberProperties.forEach { it ->
            it.isAccessible = true
            val value = it.getter.call(res.resource)
            if (value is MutableList<*>) {
                value.removeIf { it is Reference && it.reference == resource.idBase }
            } else if (value is Reference && value.reference == resource.idBase) {
                value.reference = null
            }
        }
    }

    this.deleteChildlessResource(resource)
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
    shortHandLookupTable: MutableMap<String, String>
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
    shortHandLookupTable: MutableMap<String, String>
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

private fun getFilteredObservations(
    fhirBundle: Bundle,
    conditionFilter: ReportStreamFilter,
    shortHandLookupTable: MutableMap<String, String>
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
                result += if (result.isEmpty()) it
                else "\n$it"
            }
            return result
        }

        /**
         * Filters the [properties] by only properties that have a value and are of type [Reference]
         *
         * @return a list containing only the references in [properties]
         */
        fun filterReferenceProperties(properties: List<Property>): List<String> {
            return properties.filter { it.hasValues() }.flatMap { it.values }
                .filterIsInstance<Reference>().map { it.reference }
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