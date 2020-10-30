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
    }

    val systemCode
        get() = when (system) {
            SetSystem.HL7 -> "HL7"
            SetSystem.SNOMED_CT -> "SCT"
            SetSystem.LOINC -> "L"
            SetSystem.LOCAL -> "LOCAL"
            SetSystem.FHIR -> "F"
        }

    data class Value(
        val code: String? = null,
        val alt_codes: List<String> = emptyList(),
        val display: String? = null,
    )

    fun toDisplay(code: String): String? {
        return values.find { code.equals(it.code, ignoreCase = true) || it.alt_codes.contains(code) }?.display
    }

    fun toCode(display: String): String? {
        return values.find { display.equals(it.display, ignoreCase = true) }?.code
    }
}