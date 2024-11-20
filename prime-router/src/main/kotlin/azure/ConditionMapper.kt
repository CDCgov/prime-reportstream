package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.fhirengine.utils.getCodeSourcesMap
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType

interface IConditionMapper {
    /**
     * Attempt to find diagnostic conditions for a series of test [codings]
     * @return a map associating test [codings] to their diagnostic conditions as Coding's
     */
    fun lookupConditions(codings: List<Coding>): Map<Coding, List<Coding>>

    /**
     * Lookup test code to Member OID mappings for the given [codings].
     * @return a map associating test codes to their Member OIDs
     */
    fun lookupMemberOid(codings: List<Coding>): Map<String, String>
}

class LookupTableConditionMapper(metadata: Metadata) : IConditionMapper {
    val mappingTable = metadata.findLookupTable("observation-mapping")
        ?: throw IllegalStateException("Unable to load lookup table 'observation-mapping' for code to condition lookup")

    override fun lookupConditions(codings: List<Coding>): Map<Coding, List<Coding>> {
        val codesToCodings = codings.associateBy { it.code }
        return mappingTable.FilterBuilder().isIn(ObservationMappingConstants.TEST_CODE_KEY, codings.map { it.code })
            .filter().caseSensitiveDataRowsMap.fold(mutableMapOf<Coding, List<Coding>>()) { acc, condition ->
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

    override fun lookupMemberOid(codings: List<Coding>): Map<String, String> {
        return mappingTable.FilterBuilder()
            .isIn(ObservationMappingConstants.TEST_CODE_KEY, codings.map { it.code })
            .filter().caseSensitiveDataRowsMap.fold(mutableMapOf()) { acc, condition ->
                val testCode = condition[ObservationMappingConstants.TEST_CODE_KEY] ?: ""
                val memberOid = condition[ObservationMappingConstants.TEST_OID_KEY] ?: ""
                if (testCode.isNotEmpty() && memberOid.isNotEmpty()) {
                    acc[testCode] = memberOid
                }
                acc
            }
    }
}

class ConditionStamper(private val conditionMapper: IConditionMapper) {
    companion object {
        const val CONDITION_CODE_EXTENSION_URL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"
        const val MEMBER_OID_EXTENSION_URL =
            "https://reportstream.cdc.gov/fhir/StructureDefinition/test-performed-member-oid"

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
     * Lookup condition codes and member OIDs for an [observation] and add them as custom extensions
     * @param observation the observation that will be stamped
     * @return a [ObservationStampingResult] including stamping success and any mapping failures
     */
    fun stampObservation(observation: Observation): ObservationStampingResult {
        val codeSourcesMap = observation.getCodeSourcesMap().filterValues { it.isNotEmpty() }
        if (codeSourcesMap.values.flatten().isEmpty()) return ObservationStampingResult(false)

        // Lookup conditions and Member OIDs
        val conditionsToCode = conditionMapper.lookupConditions(codeSourcesMap.values.flatten())
        val memberOidMap = conditionMapper.lookupMemberOid(codeSourcesMap.values.flatten())

        var mappedSomething = false

        // Process condition mappings
        val failures = codeSourcesMap.mapNotNull { codes ->
            val unnmapped = codes.value.mapNotNull { code ->
                val conditions = conditionsToCode.getOrDefault(code, emptyList())
                if (conditions.isEmpty()) {
                    code
                } else {
                    conditions.forEach { code.addExtension(CONDITION_CODE_EXTENSION_URL, it) }
                    mappedSomething = true
                    null
                }
            }
            if (unnmapped.isEmpty()) null else ObservationMappingFailure(codes.key, unnmapped)
        }

        // Add the Member OID extension to the observation, based on the lookup
        observation.code.coding.forEach { coding ->
            val testCode = coding.code
            val memberOid = memberOidMap[testCode]
            if (memberOid != null) {
                val memberOidExtension = Extension(MEMBER_OID_EXTENSION_URL)
                memberOidExtension.setValue(StringType(memberOid))
                observation.addExtension(memberOidExtension)
                mappedSomething = true
            }
        }

        return ObservationStampingResult(mappedSomething, failures)
    }
}