package gov.cdc.prime.router.azure

import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.UnmappableConditionMessage
import gov.cdc.prime.router.fhirengine.utils.getCodeSourcesMap
import gov.cdc.prime.router.fhirengine.utils.getObservations
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Observation

class ConditionMapper(val metadata: Metadata) {

    data class ObservationMappingFailure(val observationId: String, val failures: List<ActionLogDetail>)

    fun lookupConditions(codes: List<String>): MutableMap<String, List<Coding>> {
        val mappingTable = metadata.findLookupTable("observation-mapping")
            ?: throw IllegalStateException("Unable to load lookup table 'observation-mapping' for condition stamping")
        return mappingTable.FilterBuilder().isIn(TEST_CODE_KEY, codes)
            .filter().caseSensitiveDataRowsMap.fold(mutableMapOf<String, List<Coding>>()) { acc, condition ->
                val code = condition[TEST_CODE_KEY]!!
                val conditions = acc[code] ?: mutableListOf()
                acc[code] = conditions.plus(
                    Coding(
                        condition[CONDITION_CODE_SYSTEM_KEY],
                        condition[CONDITION_CODE_KEY],
                        condition[CONDITION_NAME_KEY]
                    )
                )
                acc
            }
    }

    /**
     * For every snomed/loinc code in code or valueCodeableConcept, lookup condition codes and add them as extensions
     * @param metadata metadata containing an observation-mapping lookup table
     * @return a list of ActionLogDetail objects with information on any mapping failures
     */
    fun stampBundle(bundle: Bundle): List<ObservationMappingFailure> =
        bundle.getObservations().mapNotNull { observation ->
            val logs = stampObservation(observation)
            if (logs.isEmpty()) null else ObservationMappingFailure(observation.id, logs)
        }

    /**
     * For every snomed/loinc code in code or valueCodeableConcept, lookup condition codes and add them as extensions
     * @param metadata metadata containing an observation-mapping lookup table
     * @return a list of ActionLogDetail objects with information on any mapping failures
     */
    fun stampObservation(observation: Observation): List<ActionLogDetail> {
        val codeSourcesMap = observation.getCodeSourcesMap().filterValues { it.isNotEmpty() }
        var mappedSomething = false
        if (codeSourcesMap.values.flatten().isEmpty()) return listOf(UnmappableConditionMessage()) // no codes found
        val codes = codeSourcesMap.values.flatten().mapNotNull { it.code }

        val conditionsToCode = lookupConditions(codes)

        return codeSourcesMap.mapNotNull { codeSourceEntry ->
            codeSourceEntry.value.mapNotNull { code ->
                val conditions = conditionsToCode.getOrDefault(code.code ?: "", emptyList())
                if (conditions.isEmpty()) { // no codes found, track this unmapped code
                    code.code
                } else { // codes found; add extensions and return null to avoid mapping this as an error
                    conditions.forEach { code.addExtension(conditionCodeExtensionURL, it) }
                    mappedSomething = true
                    null
                }
            }.let {
                // create log message for any unmapped codes
                if (it.isEmpty() || mappedSomething) null else UnmappableConditionMessage(it, codeSourceEntry.key)
            }
        }
    }

    companion object {
        const val TEST_CODE_KEY = "Code"
        const val TEST_CODESYSTEM_KEY = "Code System"
        const val TEST_OID_KEY = "Member OID"
        const val TEST_NAME_KEY = "Name"
        const val TEST_DESCRIPTOR_KEY = "Descriptor"
        const val TEST_VERSION_KEY = "Version"
        const val TEST_STATUS_KEY = "Status"

        const val CONDITION_NAME_KEY = "condition_name"
        const val CONDITION_CODE_KEY = "condition_code"
        const val CONDITION_CODE_SYSTEM_KEY = "Condition Code System"
        const val CONDITION_CODE_SYSTEM_VERSION_KEY = "Condition Code System Version"
        const val CONDITION_VALUE_SOURCE_KEY = "Value Source"
        const val CONDITION_CREATED_AT = "Created At"

        const val TEST_CODESYSTEM_LOINC = "LOINC"
        const val TEST_CODESYSTEM_SNOMEDCT = "SNOMEDCT"
        const val VSAC_CODESYSTEM_LOINC = "http://loinc.org"
        const val VSAC_CODESYSTEM_SNOMEDCT = "http://snomed.info/sct"

        const val BUNDLE_CODE_IDENTIFIER = "observation.code.coding.code"
        const val BUNDLE_VALUE_IDENTIFIER = "observation.valueCodeableConcept.coding.code"
        const val MAPPING_CODES_IDENTIFIER = "observation.{code|valueCodeableConcept}.coding.code"

        const val conditionCodeExtensionURL = "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code"

        val TEST_KEYS = listOf(
            TEST_CODE_KEY, TEST_CODESYSTEM_KEY, TEST_OID_KEY, TEST_NAME_KEY,
            TEST_DESCRIPTOR_KEY, TEST_VERSION_KEY, TEST_STATUS_KEY
        )
        val CONDITION_KEYS = listOf(
            CONDITION_NAME_KEY, CONDITION_CODE_KEY, CONDITION_CODE_SYSTEM_KEY,
            CONDITION_CODE_SYSTEM_VERSION_KEY, CONDITION_VALUE_SOURCE_KEY, CONDITION_CREATED_AT
        )

        val TEST_CODESYSTEM_MAP = mapOf(
            VSAC_CODESYSTEM_SNOMEDCT to TEST_CODESYSTEM_SNOMEDCT,
            VSAC_CODESYSTEM_LOINC to TEST_CODESYSTEM_LOINC
        )

        val ALL_KEYS = TEST_KEYS + CONDITION_KEYS
    }
}