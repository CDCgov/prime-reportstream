package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore

//  Example
//
//  - name: hl7_0136
//  system: HL7
//  reference: used where the HHS guidance specifies
//  system_code: 0136
//  values:
//    - code: YES
//      alt_codes: [true]
//      display: Yes
data class ValueSet(
    val name: String,
    val system: SetSystem,
    val reference: String? = null,
    val referenceUrl: String? = null,
    val values: List<Value> = emptyList(),
    val version: String? = null
) {
    enum class SetSystem {
        HL7,
        SNOMED_CT,
        LOINC,
        LOCAL,
        FHIR,
        UCUM,
        NULLFL,
        ISO,
        CDCREC
    }

    @get:JsonIgnore
    val systemCode
        get() = when (system) {
            SetSystem.HL7 -> name.uppercase()
            SetSystem.SNOMED_CT -> "SCT"
            SetSystem.LOINC -> "LN"
            SetSystem.UCUM -> "UCUM"
            SetSystem.LOCAL -> "LOCAL"
            SetSystem.FHIR -> "FHIR"
            SetSystem.NULLFL -> "NULLFL"
            SetSystem.ISO -> "ISO"
            SetSystem.CDCREC -> "CDCREC"
        }

    data class Value(
        val code: String,
        val display: String? = null,
        val version: String? = null,
        // replaces is used in the case of an altValue that needs to be used instead
        // of what is normally used in the valueSet.
        // for example, a DOH might want to use 'U' instead of 'UNK' for Y/N/UNK values
        val replaces: String? = null,
        val system: SetSystem? = null,
    )

    fun toDisplayFromCode(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) }?.display
    }

    // set a version on the whole value set if you want, but you can still use
    // the value-specific version if you're adding something from a different version
    fun toVersionFromCode(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) }?.version
            ?: this.version
    }

    fun toSystemFromCode(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) }?.system?.toString()?.uppercase()
            ?: this.systemCode
    }

    fun toCodeFromDisplay(display: String): String? {
        return values.find { display.equals(it.display, ignoreCase = true) }?.code
    }

    fun toNormalizedCode(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) }?.code
    }

    fun mergeAltValues(altValues: List<Value>?): ValueSet {
        // if we have alt values then we need to merge them in
        if (!altValues.isNullOrEmpty()) {
            val mergedValues = this.values.map {
                altValues.find { a -> it.code.equals(a.replaces, ignoreCase = true) } ?: it
            } + altValues.filter { it.replaces.isNullOrEmpty() }.map {
                values.find { v -> it.code.equals(v.code, ignoreCase = true) } ?: it
            }

            return this.copy(
                name = this.name,
                system = this.system,
                reference = this.reference,
                referenceUrl = this.referenceUrl,
                values = mergedValues,
                version = this.version,
            )
        }

        // there's nothing to do, return self
        return this
    }
}