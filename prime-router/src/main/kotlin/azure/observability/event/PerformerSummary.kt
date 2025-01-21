package gov.cdc.prime.router.azure.observability.event

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.DomainResource

data class PerformerSummary (
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
            // For a given reference, resolve the object. This can reference many differing FHIR resource types
            // (Practitioner, Organization, etc). Return a PerformerSummary object from the paths listed above.
            val performerName = performer.getNamedProperty(performerNamePath)
            val performerState = performer.getNamedProperty(performerStatePath)
            val performerCLIA = performer.getNamedProperty(performerCLIAPath)

            return PerformerSummary(
                (performerName ?: PerformerSummary.UNKNOWN).toString(),
                (performerState ?: PerformerSummary.UNKNOWN).toString(),
                (performerCLIA ?: PerformerSummary.UNKNOWN).toString(),
            )
        }
    }
}