package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.utils.getCodeSourcesMap
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation

interface IConditionMapper {
    /**
     * Attempt to find diagnostic conditions for a series of test [codings]
     * @return a map associating test [codings] to their diagnostic conditions as Coding's
     */
    fun lookupConditions(codings: List<Coding>): Map<Coding, List<Coding>>
}

class LookupTableConditionMapper(metadata: Metadata) : IConditionMapper {
    val mappingTable = metadata.findLookupTable("observation-mapping")
        ?: throw IllegalStateException("Unable to load lookup table 'observation-mapping' for code to condition lookup")

    override fun lookupConditions(codings: List<Coding>): Map<Coding, List<Coding>> {
        val codesToCodings = codings.associateBy { it.code } // for mapping codes back into their mappings
        return mappingTable.FilterBuilder() // constrain the underlying query for the search
            .isIn(ObservationMappingConstants.TEST_CODE_KEY, codesToCodings.keys.toList())
            .filter().caseSensitiveDataRowsMap.fold(mutableMapOf<Coding, List<Coding>>()) { acc, condition ->
                // map the resulting data to the original coding
                val code = codesToCodings[condition[ObservationMappingConstants.TEST_CODE_KEY]]!!
                val conditions = acc[code] ?: mutableListOf()
                acc[code] = conditions.plus(
                    Coding(
                        condition[ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY],
                        condition[ObservationMappingConstants.CONDITION_CODE_KEY],
                        condition[ObservationMappingConstants.CONDITION_NAME_KEY]
                    )
                )
                acc
            }
    }
}

class ConditionStamper(private val conditionMapper: IConditionMapper) {
    companion object {
        const val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"

        const val BUNDLE_CODE_IDENTIFIER = "observation.code.coding.code"
        const val BUNDLE_VALUE_IDENTIFIER = "observation.valueCodeableConcept.coding.code"
        const val MAPPING_CODES_IDENTIFIER = "observation.{code|valueCodeableConcept}.coding.code"
    }
    data class ObservationMappingFailure(val source: String, val failures: List<Coding>)

    data class ObservationStampingResult(
        val success: Boolean,
        val failures: List<ObservationMappingFailure> = emptyList(),
    )

    /**
     * Lookup condition codes for an [observation] and add them as custom extensions
     * @param observation the observation that will be stamped
     * @return a [ObservationStampingResult] including stamping success status and any mapping failures. stamping is
     *         successful if at least one code is identified and mapped
     */
    fun stampObservation(observation: Observation): ObservationStampingResult {
        val codingsSourceMap = observation.getCodeSourcesMap().filterValues { it.isNotEmpty() }
        val codings = codingsSourceMap.values.flatten()
        if (codings.isEmpty()) return ObservationStampingResult(false)

        val codingsToConditions = conditionMapper.lookupConditions(codings)
        var mappedSomething = false

        val failures = codingsSourceMap.mapNotNull { codingsSource ->
            // stamp conditions and gather mapping failures on a per code source
            val unmappedCodings = codingsSource.value.mapNotNull { code ->
                val conditions = codingsToConditions.getOrDefault(code, emptyList())
                if (conditions.isEmpty()) {
                    code // could not be mapped -- add as failure
                } else {
                    conditions.forEach { code.addExtension(conditionCodeExtensionURL, it) } // add condition
                    mappedSomething = true
                    null
                }
            }
            if (unmappedCodings.isEmpty()) null else ObservationMappingFailure(codingsSource.key, unmappedCodings)
        }
        // there may be failures even if we were successful - only one code source need succeed
        return ObservationStampingResult(mappedSomething, failures)
    }
}