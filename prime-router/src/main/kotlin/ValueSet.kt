package gov.cdc.prime.router

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
) {
    enum class SetSystem {
        HL7,
        SNOMED_CT,
        LOINC,
        LOCAL,
        FHIR,
        UCUM,
    }

    val systemCode
        get() = when (system) {
            SetSystem.HL7 -> name.toUpperCase()
            SetSystem.SNOMED_CT -> "SCT"
            SetSystem.LOINC -> "LN"
            SetSystem.UCUM -> "UCUM"
            SetSystem.LOCAL -> "LOCAL"
            SetSystem.FHIR -> "FHIR"
        }

    data class Value(
        val code: String,
        val display: String? = null,
        val version: String? = null,
        // replaces is used in the case of an altValue that needs to be used instead
        // of what is normally used in the valueSet.
        // for example, a DOH might want to use 'U' instead of 'UNK' for Y/N/UNK values
        val replaces: String? = null
    )

    fun toDisplayFromCode(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) }?.display
    }

    fun toVersionFromCode(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) }?.version
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
                values = mergedValues
            )
        }

        // there's nothing to do, return self
        return this
    }
}