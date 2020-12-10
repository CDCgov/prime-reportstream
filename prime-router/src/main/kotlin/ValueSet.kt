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
}