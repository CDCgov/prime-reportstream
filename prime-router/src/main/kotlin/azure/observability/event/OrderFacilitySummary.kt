package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Organization

data class OrderingFacilitySummary(
    val orderingFacilityName: String = UNKNOWN,
    val orderingFacilityState: String = UNKNOWN,
) {
    companion object {
        const val UNKNOWN = "Unknown"

        /**
         * Create an instance of [OrderingFacilitySummary] from a [Organization]
         */
        fun fromOrganization(requester: Base): OrderingFacilitySummary {
            val org = requester as? Organization
                ?: return OrderingFacilitySummary()

            val name = org.name ?: UNKNOWN
            val state = org.address
                ?.firstOrNull() // pick the first address if present
                ?.state ?: UNKNOWN
            return OrderingFacilitySummary(name, state)
        }
    }
}