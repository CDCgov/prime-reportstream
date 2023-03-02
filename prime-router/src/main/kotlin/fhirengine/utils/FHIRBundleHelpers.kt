package gov.cdc.prime.router.fhirengine.utils

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * A collection of helper functions that modify an existing FHIR bundle.
 */
object FHIRBundleHelpers {
    /**
     * Adds [receiverList] to the [fhirBundle] as targets
     */
    internal fun addReceivers(fhirBundle: Bundle, receiverList: List<Receiver>) {
        val provenanceResource = try {
            fhirBundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("The FHIR bundle does not contain a Provenance resource")
        }

        // Create the list of target receivers to be added to the Provenance of the bundle
        val targetList = mutableListOf<Reference>()

        // check all active customers for receiver data
        receiverList.filter { it.customerStatus != CustomerStatus.INACTIVE }.forEach { receiver ->
            val endpoint = Endpoint()
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
            val entry = fhirBundle.addEntry()
                .setFullUrl("${endpoint.fhirType()}/${endpoint.id}")
                .setResource(endpoint)

            val reference = Reference()
            reference.reference = entry.fullUrl
            reference.resource = endpoint
            targetList.add(reference)
        }

        if (targetList.isNotEmpty()) provenanceResource.target.addAll(targetList)
    }

    /**
     * Gets all properties for a [Base] resource recursively and filters only its references
     *
     * @return a list of reference identifiers for a [Base] resource
     *
     */
    fun Base.getResourceReferences(): List<String> {
        return filterReferenceProperties(this.getResourceProperties())
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
     * Filters the [properties] by only properties that have a value and are of type [Reference]
     *
     * @return a list containing only the references in [properties]
     */
    internal fun filterReferenceProperties(properties: List<Property>): List<String> {
        return properties.filter { it.hasValues() }.flatMap { it.values }
            .filterIsInstance<Reference>().map { it.reference }
    }

    /**
     * Gets all child properties for a resource [property] recursively
     *
     * @return a flatmap stream of all child properties on a [property]
     */
    internal fun getChildProperties(property: Property): Stream<Property> {
        return Stream.concat(
            Stream.of(property),
            property.values.flatMap { it.children() }.stream().flatMap { getChildProperties(it) }
        )
    }

    /**
     * Gets all diagnostic report that have no observations from a [bundle]
     *
     * @return a list of [Base] diagnostic reports that have no observations
     */
    fun getDiagnosticReportNoObservations(bundle: Bundle): List<Base> {
        return FhirPathUtils.evaluate(
            null,
            bundle,
            bundle,
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
            res.resource.getResourceProperties().forEach { property ->
                property.values.forEach {
                    if (it is Reference && it.reference == resource.idBase) {
                        it.reference = null
                        it.resource = null
                    }
                }
            }
        }

        this.deleteChildlessResource(resource)
    }

    /**
     * Deletes resources that have no children after deleting a [resource]
     *
     */
    internal fun Bundle.deleteChildlessResource(resource: Base) {
        // if resource being removed is an observation
        // check if there are any diagnostic reports with no observations and remove them
        when (resource) {
            is Observation -> getDiagnosticReportNoObservations(this).forEach { this.deleteResource(it) }
        }
    }
}