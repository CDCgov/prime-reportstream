package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Organization

data class OrderingFacilitySummary (
    val orderingFacilityName: String = UNKNOWN,
    val orderingFacilityState: String = UNKNOWN,
) {
    companion object {
        const val UNKNOWN = "Unknown"
        const val orderingFacilityNamePath = "Bundle.entry.resource.name"
        const val orderingFacilityStatePath = "Bundle.entry.resource.address.state"

        /**
         * Create an instance of [OrderingFacilitySummary] from a [Organization]
         */
        fun fromOrganization(requester: Base): OrderingFacilitySummary {
            // For a given organization, return a OrderingFacilitySummary object from the paths listed above.
            val organizationName = requester.getNamedProperty(orderingFacilityNamePath)
            val organizationState = requester.getNamedProperty(orderingFacilityStatePath)

            return OrderingFacilitySummary(
                (organizationName ?: PerformerSummary.UNKNOWN).toString(),
                (organizationState ?: PerformerSummary.UNKNOWN).toString(),
            )
        }
    }
}