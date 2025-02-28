package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.DomainResource

data class PerformerSummary(
    val performerName: String = UNKNOWN,
    val performerState: String = UNKNOWN,
    val performerCLIA: String = UNKNOWN,
) {
    companion object {
        const val UNKNOWN = "Unknown"
        const val performerNamePath = "Bundle.entry.resource.name"
        const val performerStatePath = "Bundle.entry.resource.address.state"
        const val performerCLIAPath = "Bundle.entry.resource.identifier.value.getIdType() = 'CLIA'"

        /**
         * Create an instance of [PerformerSummary] from a [DomainResource]
         */
        fun fromPerformer(performer: Base): PerformerSummary {
            when (performer) {
                is org.hl7.fhir.r4.model.Practitioner -> {
                    // Practitioner has a name list and an address list
                    val performerName = performer.name
                        ?.firstOrNull()
                        ?.nameAsSingleString ?: UNKNOWN

                    val performerState = performer.address
                        ?.firstOrNull()
                        ?.state ?: UNKNOWN

                    // For Practitioner, identifier list may contain CLIA. You need to find it explicitly.
                    val performerCLIA = performer.identifier
                        ?.firstOrNull { it.system?.contains("CLIA", ignoreCase = true) == true }
                        ?.value ?: UNKNOWN

                    return PerformerSummary(performerName, performerState, performerCLIA)
                }

                is org.hl7.fhir.r4.model.Organization -> {
                    // Handle Organization similarly, as a fallback
                    val performerName = performer.name ?: UNKNOWN
                    val performerState = performer.address
                        ?.firstOrNull()
                        ?.state ?: UNKNOWN
                    val performerCLIA = performer.identifier
                        ?.firstOrNull { it.system?.contains("CLIA", ignoreCase = true) == true }
                        ?.value ?: UNKNOWN

                    return PerformerSummary(performerName, performerState, performerCLIA)
                }

                else -> return PerformerSummary()
            }
        }
    }
}