package gov.cdc.prime.router.fhirengine.utils

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.fhirengine.engine.FHIRRouter
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType

/**
 * A collection of helper functions that modify an existing FHIR bundle.
 */
object FHIRBundleHelpers {
    /**
     * Adds [receiverList] to the [fhirBundle] as targets
     */
    internal fun addReceivers(fhirBundle: Bundle, receiverList: List<FHIRRouter.RoutedReceiver>) {
        val provenanceResource = try {
            fhirBundle.entry.first { it.resource.resourceType.name == "Provenance" }.resource as Provenance
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("The FHIR bundle does not contain a Provenance resource")
        }

        // Create the list of target receivers to be added to the Provenance of the bundle
        val targetList = mutableListOf<Reference>()

        // check all active customers for receiver data
        receiverList.filter { it.receiver.customerStatus != CustomerStatus.INACTIVE }.forEach {
            val endpoint = Endpoint()
            endpoint.id = Hl7RelatedGeneralUtils.generateResourceId()
            endpoint.name = it.receiver.displayName
            when (it.receiver.customerStatus) {
                CustomerStatus.TESTING -> endpoint.status = Endpoint.EndpointStatus.TEST
                else -> endpoint.status = Endpoint.EndpointStatus.ACTIVE
            }
            val rsIdentifier = Identifier()
            rsIdentifier.value = it.receiver.fullName
            rsIdentifier.system = "https://reportstream.cdc.gov/prime-router"
            endpoint.identifier.add(rsIdentifier)
            val entry = fhirBundle.addEntry()
                .setFullUrl("${endpoint.fhirType()}/${endpoint.id}")
                .setResource(endpoint)

            //
            endpoint.addExtension(
                "https://reportstream.cdc.gov/prime-router/extensions#itemCount",
                IntegerType(it.itemCount)
            )
            endpoint.addExtension(
                "https://reportstream.cdc.gov/prime-router/extensions#logs",
                StringType(it.filterResults.toString())
            )

            val reference = Reference()
            reference.reference = entry.fullUrl
            reference.resource = endpoint
            targetList.add(reference)
        }

        if (targetList.isNotEmpty()) provenanceResource.target.addAll(targetList)
    }
}